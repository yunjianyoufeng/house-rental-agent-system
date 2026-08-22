from functools import lru_cache

from pydantic import SecretStr
from pydantic_settings import BaseSettings, SettingsConfigDict


class Settings(BaseSettings):
    """从本地环境变量读取 Agent 服务配置。"""

    model_config = SettingsConfigDict(
        env_file=".env",
        env_file_encoding="utf-8",
        extra="ignore",
    )

    deepseek_api_key: SecretStr | None = None
    deepseek_base_url: str = "https://api.deepseek.com"
    deepseek_model: str = "deepseek-chat"
    spring_backend_url: str = "http://127.0.0.1:8080"
    redis_url: str = "redis://127.0.0.1:6379/0"
    conversation_ttl_seconds: int = 3600
    conversation_max_messages: int = 12
    appointment_pending_ttl_seconds: int = 900
    rag_database_path: str = "data/rag/knowledge.db"
    rag_knowledge_dir: str = "knowledge"
    rag_embedding_dimensions: int = 384
    rag_top_k: int = 3
    agent_host: str = "127.0.0.1"
    agent_port: int = 8001


@lru_cache
def get_settings() -> Settings:
    return Settings()
