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
    rag_vector_store: str = "chroma"
    rag_chroma_path: str = "data/rag/chroma"
    rag_chroma_collection: str = "rental_knowledge"
    rag_embedding_provider: str = "openai_compatible"
    rag_embedding_base_url: str = (
        "https://dashscope.aliyuncs.com/compatible-mode/v1"
    )
    rag_embedding_api_key: SecretStr | None = None
    rag_embedding_model: str = "text-embedding-v4"
    rag_embedding_url: str = "http://127.0.0.1:9000/embeddings"
    rag_embedding_timeout_seconds: float = Field(default=30.0, gt=0)
    rag_embedding_dimensions: int = Field(default=768, gt=0)
    rag_embedding_batch_size: int = Field(default=10, ge=1, le=100)
    rag_embedding_max_retries: int = Field(default=2, ge=0, le=5)
    rag_embedding_retry_base_seconds: float = Field(default=0.5, ge=0, le=10)
    rag_top_k: int = 3
    rag_candidate_k: int = Field(default=10, ge=3, le=50)
    rag_vector_weight: float = Field(default=0.7, ge=0, le=1)
    rag_lexical_weight: float = Field(default=0.3, ge=0, le=1)
    rag_rerank_weight: float = Field(default=0.15, ge=0, le=1)
    agent_host: str = "127.0.0.1"
    agent_port: int = 8001
    agent_tool_log_path: str = "logs/tool-calls.jsonl"
    agent_tool_log_max_bytes: int = 2 * 1024 * 1024
    agent_tool_log_backup_count: int = 3
    langsmith_tracing: bool = False
    langsmith_api_key: SecretStr | None = None
    langsmith_endpoint: str = "https://api.smith.langchain.com"
    langsmith_project: str = "house-rental-agent-dev"
    langsmith_hide_inputs: bool = True
    langsmith_hide_outputs: bool = True


@lru_cache
def get_settings() -> Settings:
    return Settings()
