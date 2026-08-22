from fastapi import APIRouter, Header, HTTPException, status
from openai import OpenAIError
from redis.exceptions import RedisError

from app.agent.graph import agent_graph, initial_agent_state
from app.schemas.chat import ChatRequest, ChatResponse
from app.services.llm_service import DeepSeekConfigurationError
from app.services.memory_service import save_conversation_exchange


router = APIRouter(prefix="/api/agent", tags=["agent"])


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
    )
