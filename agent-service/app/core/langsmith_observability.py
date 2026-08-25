import re
from contextlib import contextmanager
from functools import lru_cache
from typing import Any, Iterator

from langsmith import Client, tracing_context
from langsmith.run_helpers import trace

from app.core.config import get_settings


REDACTED_VALUE = "[REDACTED]"
SENSITIVE_KEY_PARTS = (
    "authorization",
    "token",
    "password",
    "secret",
    "apikey",
    "api_key",
)
SENSITIVE_TEXT_PATTERNS = (
    re.compile(r"Bearer\s+[A-Za-z0-9._~+/=-]+", re.IGNORECASE),
    re.compile(r"\b(?:sk|lsv2|ls)__?[-A-Za-z0-9_]{12,}\b", re.IGNORECASE),
    re.compile(r"\b1[3-9]\d{9}\b"),
    re.compile(r"\b[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}\b"),
    re.compile(r"\b\d{17}[0-9Xx]\b"),
)


def sanitize_trace_value(value: Any) -> Any:
    """在数据离开本服务前移除密钥、令牌和常见个人敏感信息。"""

    if isinstance(value, dict):
        sanitized = {}
        for key, item in value.items():
            normalized_key = str(key).lower().replace("-", "").replace("_", "")
            if any(
                part.replace("_", "") in normalized_key
                for part in SENSITIVE_KEY_PARTS
            ):
                sanitized[key] = REDACTED_VALUE
            else:
                sanitized[key] = sanitize_trace_value(item)
        return sanitized
    if isinstance(value, (list, tuple)):
        return [sanitize_trace_value(item) for item in value]
    if isinstance(value, str):
        sanitized_text = value
        for pattern in SENSITIVE_TEXT_PATTERNS:
            sanitized_text = pattern.sub(REDACTED_VALUE, sanitized_text)
        return sanitized_text
    return value


def langsmith_is_enabled() -> bool:
    settings = get_settings()
    return settings.langsmith_tracing and settings.langsmith_api_key is not None


@lru_cache
def get_langsmith_client() -> Client | None:
    settings = get_settings()
    if not langsmith_is_enabled():
        return None
    return Client(
        api_url=settings.langsmith_endpoint,
        api_key=settings.langsmith_api_key.get_secret_value(),
        anonymizer=sanitize_trace_value,
        hide_inputs=settings.langsmith_hide_inputs,
        hide_outputs=settings.langsmith_hide_outputs,
    )


@contextmanager
def agent_trace_context(
    *,
    trace_id: str,
    conversation_id: str | None,
    role_code: str | None,
    streaming: bool,
) -> Iterator[Any | None]:
    """创建请求级根 Trace；关闭或缺少密钥时保持无副作用。"""

    client = get_langsmith_client()
    if client is None:
        yield None
        return

    settings = get_settings()
    metadata = sanitize_trace_value(
        {
            "traceId": trace_id,
            "conversationId": conversation_id,
            "roleCode": role_code,
            "streaming": streaming,
        }
    )
    tags = ["house-rental-agent", "stream" if streaming else "non-stream"]
    with tracing_context(
        enabled=True,
        client=client,
        project_name=settings.langsmith_project,
        metadata=metadata,
        tags=tags,
    ):
        with trace(
            "rental-agent-chat",
            run_type="chain",
            inputs={"conversationId": conversation_id},
            metadata=metadata,
            tags=tags,
            client=client,
            project_name=settings.langsmith_project,
        ) as run_tree:
            yield run_tree


@contextmanager
def child_trace(
    name: str,
    run_type: str,
    *,
    inputs: dict[str, Any] | None = None,
    metadata: dict[str, Any] | None = None,
) -> Iterator[Any | None]:
    """在当前 Agent Trace 下创建工具或检索子节点。"""

    client = get_langsmith_client()
    if client is None:
        yield None
        return

    settings = get_settings()
    with trace(
        name,
        run_type=run_type,
        inputs=sanitize_trace_value(inputs or {}),
        metadata=sanitize_trace_value(metadata or {}),
        client=client,
        project_name=settings.langsmith_project,
    ) as run_tree:
        yield run_tree


def summarize_trace_result(result: Any) -> dict[str, Any]:
    if isinstance(result, dict):
        summary: dict[str, Any] = {"type": "object", "keys": sorted(result)}
        for key in ("houses", "results", "items"):
            if isinstance(result.get(key), list):
                summary["itemCount"] = len(result[key])
                break
        return summary
    if isinstance(result, list):
        return {"type": "array", "itemCount": len(result)}
    return {"type": type(result).__name__}


def close_langsmith_client() -> None:
    client = get_langsmith_client()
    if client is not None:
        client.close(timeout=2.0)
    get_langsmith_client.cache_clear()
