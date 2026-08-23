from contextlib import asynccontextmanager

from fastapi import FastAPI

from app.api.chat import router as chat_router
from app.core.config import get_settings
from app.core.logging_config import configure_tool_logging, shutdown_tool_logging
from app.services.memory_service import close_redis_client


settings = get_settings()
configure_tool_logging(
    settings.agent_tool_log_path,
    settings.agent_tool_log_max_bytes,
    settings.agent_tool_log_backup_count,
)


@asynccontextmanager
async def lifespan(_: FastAPI):
    yield
    await close_redis_client()
    shutdown_tool_logging()


app = FastAPI(
    title="House Rental Agent Service",
    version="0.1.0",
    lifespan=lifespan,
)
app.include_router(chat_router)


@app.get("/health")
def health() -> dict[str, str]:
    """用于确认 Agent 服务及其基础配置能够正常加载。"""

    return {
        "status": "ok",
        "service": "house-rental-agent",
        "model": settings.deepseek_model,
    }
