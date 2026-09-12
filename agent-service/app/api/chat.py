import json
from collections.abc import AsyncIterator
from time import perf_counter
from uuid import uuid4

from fastapi import APIRouter, Header, HTTPException, Response, status
from fastapi.responses import StreamingResponse
from openai import OpenAIError
from redis.exceptions import RedisError

from app.agent.graph import agent_graph, initial_agent_state, stream_agent_events
from app.core.langsmith_observability import agent_trace_context
from app.core.logging_config import log_agent_request
from app.core.observability import reset_trace_id, set_trace_id
from app.schemas.chat import ChatRequest, ChatResponse
from app.services.llm_service import (
    DeepSeekConfigurationError,
    ModelServiceUnavailableError,
)
from app.services.backend_client import BackendServiceError
from app.services.identity_service import resolve_authenticated_identity
from app.services.request_limit_service import check_chat_quota
from app.services.memory_service import save_conversation_exchange
from app.services.safety_service import UnsafeInputError, validate_user_message


router = APIRouter(prefix="/api/agent", tags=["agent"])


def encode_sse_event(event: str, payload: dict) -> str:
    data = json.dumps(payload, ensure_ascii=False, separators=(",", ":"))
    return f"event: {event}\ndata: {data}\n\n"


@router.post("/chat", response_model=ChatResponse)
async def chat(
    request: ChatRequest,
    response: Response,
    authorization: str | None = Header(default=None),
) -> ChatResponse:
    trace_id = uuid4().hex
    trace_token = set_trace_id(trace_id)
    started_at = perf_counter()
    request_status = "error"
    user_id = None
    try:
        try:
            validate_user_message(request.message)
            user_id, role_code = await resolve_authenticated_identity(authorization)
            await check_chat_quota(user_id)
            with agent_trace_context(
                trace_id=trace_id,
                conversation_id=request.conversation_id,
                role_code=role_code,
                streaming=False,
            ) as request_trace:
                state = await initial_agent_state(
                    request.message,
                    request.conversation_id,
                    user_id,
                    role_code,
                    authorization,
                )
                result = await agent_graph.ainvoke(
                    state,
                    config={
                        "run_name": "rental-agent-graph",
                        "tags": ["non-stream"],
                        "metadata": {
                            "traceId": trace_id,
                            "conversationId": request.conversation_id,
                            "roleCode": role_code,
                        },
                    },
                )
                await save_conversation_exchange(
                    request.conversation_id,
                    user_id,
                    request.message,
                    result["answer"],
                    authorization,
                    result.get("knowledge_sources", []),
                )
                if request_trace is not None:
                    request_trace.end(
                        outputs={
                            "status": "success",
                            "sourceCount": len(
                                result.get("knowledge_sources", [])
                            ),
                        }
                    )
        except DeepSeekConfigurationError as exc:
            raise HTTPException(
                status_code=status.HTTP_503_SERVICE_UNAVAILABLE,
                detail=str(exc),
            ) from exc
        except ModelServiceUnavailableError as exc:
            raise HTTPException(
                status_code=status.HTTP_503_SERVICE_UNAVAILABLE,
                detail=str(exc),
            ) from exc
        except UnsafeInputError as exc:
            raise HTTPException(
                status_code=status.HTTP_400_BAD_REQUEST,
                detail=str(exc),
            ) from exc
        except OpenAIError as exc:
            raise HTTPException(
                status_code=status.HTTP_502_BAD_GATEWAY,
                detail="DeepSeek 模型服务调用失败，请稍后重试。",
            ) from exc
        except RedisError as exc:
            raise HTTPException(
                status_code=status.HTTP_503_SERVICE_UNAVAILABLE,
                detail="Redis 会话服务暂不可用，请确认 Redis 已启动。",
            ) from exc
        except (BackendServiceError, ValueError) as exc:
            raise HTTPException(
                status_code=status.HTTP_401_UNAUTHORIZED,
                detail=str(exc),
            ) from exc

        request_status = "success"
        response.headers["X-Trace-Id"] = trace_id
        return ChatResponse(
            conversation_id=request.conversation_id,
            trace_id=trace_id,
            answer=result["answer"],
            sources=result.get("knowledge_sources", []),
        )
    finally:
        log_agent_request(
            status=request_status,
            duration_ms=round((perf_counter() - started_at) * 1000),
            conversation_id=request.conversation_id,
            user_id=user_id,
            error_type=None if request_status == "success" else "RequestFailed",
        )
        reset_trace_id(trace_token)


@router.post("/chat/stream")
async def chat_stream(
    request: ChatRequest,
    authorization: str | None = Header(default=None),
) -> StreamingResponse:
    trace_id = uuid4().hex
    # 在响应头发出前完成认证和限额，让调用方收到真实的 401/429，而不是 HTTP 200。
    try:
        validate_user_message(request.message)
        authenticated_user_id, role_code = await resolve_authenticated_identity(authorization)
        await check_chat_quota(authenticated_user_id)
    except UnsafeInputError as exc:
        raise HTTPException(status_code=400, detail=str(exc)) from exc
    except (BackendServiceError, ValueError) as exc:
        raise HTTPException(status_code=401, detail=str(exc)) from exc
    except RedisError as exc:
        raise HTTPException(status_code=503, detail="会话服务暂不可用。") from exc

    async def generate_events() -> AsyncIterator[str]:
        trace_token = set_trace_id(trace_id)
        started_at = perf_counter()
        request_status = "error"
        user_id = authenticated_user_id
        try:
            source_count = 0
            with agent_trace_context(
                trace_id=trace_id,
                conversation_id=request.conversation_id,
                role_code=role_code,
                streaming=True,
            ) as request_trace:
                state = await initial_agent_state(
                    request.message,
                    request.conversation_id,
                    user_id,
                    role_code,
                    authorization,
                )
                async for item in stream_agent_events(state):
                    event = item["event"]
                    if event == "done":
                        sources = item.get("sources", [])
                        source_count = len(sources)
                        await save_conversation_exchange(
                            request.conversation_id,
                            user_id,
                            request.message,
                            item["answer"],
                            authorization,
                            sources,
                        )
                        request_status = "success"
                    yield encode_sse_event(event, {**item, "traceId": trace_id})
                if request_trace is not None:
                    request_trace.end(
                        outputs={
                            "status": request_status,
                            "sourceCount": source_count,
                        }
                    )
        except DeepSeekConfigurationError as exc:
            yield encode_sse_event(
                "error", {"message": str(exc), "traceId": trace_id}
            )
        except ModelServiceUnavailableError as exc:
            yield encode_sse_event(
                "error", {"message": str(exc), "traceId": trace_id}
            )
        except UnsafeInputError as exc:
            yield encode_sse_event(
                "error", {"message": str(exc), "traceId": trace_id}
            )
        except OpenAIError:
            yield encode_sse_event(
                "error",
                {
                    "message": "DeepSeek 模型服务调用失败，请稍后重试。",
                    "traceId": trace_id,
                },
            )
        except RedisError:
            yield encode_sse_event(
                "error",
                {
                    "message": "Redis 会话服务暂不可用，请确认 Redis 已启动。",
                    "traceId": trace_id,
                },
            )
        except (BackendServiceError, ValueError) as exc:
            yield encode_sse_event(
                "error", {"message": str(exc), "traceId": trace_id}
            )
        finally:
            log_agent_request(
                status=request_status,
                duration_ms=round((perf_counter() - started_at) * 1000),
                conversation_id=request.conversation_id,
                user_id=user_id,
                error_type=None if request_status == "success" else "RequestFailed",
            )
            reset_trace_id(trace_token)

    return StreamingResponse(
        generate_events(),
        media_type="text/event-stream",
        headers={
            "Cache-Control": "no-cache",
            "X-Accel-Buffering": "no",
            "X-Trace-Id": trace_id,
        },
    )
