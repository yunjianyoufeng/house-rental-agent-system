import json
from collections.abc import AsyncIterator

from fastapi import APIRouter, Header, HTTPException, status
from fastapi.responses import StreamingResponse
from openai import OpenAIError
from redis.exceptions import RedisError

from app.agent.graph import agent_graph, initial_agent_state, stream_agent_events
from app.schemas.chat import ChatRequest, ChatResponse
from app.services.llm_service import DeepSeekConfigurationError
from app.services.memory_service import save_conversation_exchange


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
        state = await initial_agent_state(
            request.message,
            request.conversation_id,
            request.user_id,
            authorization,
        )
        result = await agent_graph.ainvoke(state)
        await save_conversation_exchange(
            request.conversation_id,
            request.user_id,
            request.message,
            result["answer"],
        )
    except DeepSeekConfigurationError as exc:
        raise HTTPException(
            status_code=status.HTTP_503_SERVICE_UNAVAILABLE,
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
            state = await initial_agent_state(
                request.message,
                request.conversation_id,
                request.user_id,
                authorization,
            )
            async for item in stream_agent_events(state):
                event = item["event"]
                if event == "done":
                    await save_conversation_exchange(
                        request.conversation_id,
                        request.user_id,
                        request.message,
                        item["answer"],
                    )
                yield encode_sse_event(event, item)
        except DeepSeekConfigurationError as exc:
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

    return StreamingResponse(
        generate_events(),
        media_type="text/event-stream",
        headers={
            "Cache-Control": "no-cache",
            "X-Accel-Buffering": "no",
        },
    )
