from functools import lru_cache

from pydantic import Field, SecretStr
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
    deepseek_model: str = "deepseek-v4-flash"
    deepseek_timeout_seconds: float = Field(default=30.0, gt=0)
    deepseek_max_retries: int = Field(default=2, ge=0, le=5)
    deepseek_retry_base_seconds: float = Field(default=0.5, ge=0, le=10)
    deepseek_input_cache_hit_cny_per_million: float = Field(default=0.02, ge=0)
    deepseek_input_cache_miss_cny_per_million: float = Field(default=1.0, ge=0)
    deepseek_output_cny_per_million: float = Field(default=2.0, ge=0)
    spring_backend_url: str = "http://127.0.0.1:8080"
    redis_url: str = "redis://127.0.0.1:6379/0"
    conversation_ttl_seconds: int = 3600
    conversation_max_messages: int = 12
    conversation_recent_messages: int = 8
    appointment_pending_ttl_seconds: int = 900
    rag_database_path: str = "data/rag/knowledge.db"
    rag_knowledge_dir: str = "knowledge"
    rag_embedding_dimensions: int = 384
    rag_top_k: int = 3
    agent_host: str = "127.0.0.1"
    agent_port: int = 8001
    agent_tool_log_path: str = "logs/tool-calls.jsonl"
    agent_tool_log_max_bytes: int = 2 * 1024 * 1024
    agent_tool_log_backup_count: int = 3


@lru_cache
def get_settings() -> Settings:
    return Settings()
