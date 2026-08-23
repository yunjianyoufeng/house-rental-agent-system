import json
from collections.abc import AsyncIterator

from fastapi import APIRouter, Header, HTTPException, status
from fastapi.responses import StreamingResponse
from openai import OpenAIError
from redis.exceptions import RedisError

from app.agent.graph import agent_graph, initial_agent_state, stream_agent_events
from app.schemas.chat import ChatRequest, ChatResponse
from app.services.llm_service import (
    DeepSeekConfigurationError,
    ModelServiceUnavailableError,
)
from app.services.backend_client import BackendServiceError
from app.services.identity_service import resolve_authenticated_identity
from app.services.memory_service import save_conversation_exchange
from app.services.safety_service import UnsafeInputError, validate_user_message


router = APIRouter(prefix="/api/agent", tags=["agent"])


def encode_sse_event(event: str, payload: dict) -> str:
    data = json.dumps(payload, ensure_ascii=False, separators=(",", ":"))
    return f"event: {event}\ndata: {data}\n\n"


@router.post("/chat", response_model=ChatResponse)
async def chat(
    request: ChatRequest,
    authorization: str | None = Header(default=None),
) -> ChatResponse:
    try:
        validate_user_message(request.message)
        user_id, role_code = await resolve_authenticated_identity(authorization)
        state = await initial_agent_state(
            request.message,
            request.conversation_id,
            user_id,
            role_code,
            authorization,
        )
        result = await agent_graph.ainvoke(state)
        await save_conversation_exchange(
            request.conversation_id,
            user_id,
            request.message,
            result["answer"],
            authorization,
            result.get("knowledge_sources", []),
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

    return ChatResponse(
        conversation_id=request.conversation_id,
        answer=result["answer"],
        sources=result.get("knowledge_sources", []),
    )


@router.post("/chat/stream")
async def chat_stream(
    request: ChatRequest,
    authorization: str | None = Header(default=None),
) -> StreamingResponse:
    async def generate_events() -> AsyncIterator[str]:
        try:
            validate_user_message(request.message)
            user_id, role_code = await resolve_authenticated_identity(authorization)
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
                    await save_conversation_exchange(
                        request.conversation_id,
                        user_id,
                        request.message,
                        item["answer"],
                        authorization,
                        item.get("sources", []),
                    )
                yield encode_sse_event(event, item)
        except DeepSeekConfigurationError as exc:
            yield encode_sse_event("error", {"message": str(exc)})
        except ModelServiceUnavailableError as exc:
            yield encode_sse_event("error", {"message": str(exc)})
        except UnsafeInputError as exc:
            yield encode_sse_event("error", {"message": str(exc)})
        except OpenAIError:
            yield encode_sse_event(
                "error",
                {"message": "DeepSeek 模型服务调用失败，请稍后重试。"},
            )
        except RedisError:
            yield encode_sse_event(
                "error",
                {"message": "Redis 会话服务暂不可用，请确认 Redis 已启动。"},
            )
        except (BackendServiceError, ValueError) as exc:
            yield encode_sse_event("error", {"message": str(exc)})

    return StreamingResponse(
        generate_events(),
        media_type="text/event-stream",
        headers={
            "Cache-Control": "no-cache",
            "X-Accel-Buffering": "no",
        },
    )
