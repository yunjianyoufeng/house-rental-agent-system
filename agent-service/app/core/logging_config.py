import json
import logging
from datetime import UTC, datetime
from logging.handlers import RotatingFileHandler
from pathlib import Path
from typing import Any

from app.core.observability import (
    get_trace_id,
    record_model_call,
    record_request,
    record_tool_call as record_tool_metric,
)


TOOL_LOGGER_NAME = "house_rental_agent.tool_calls"
REDACTED_VALUE = "[REDACTED]"
SENSITIVE_KEY_PARTS = ("authorization", "token", "password", "secret", "apikey")


def configure_tool_logging(
    log_path: str | Path,
    max_bytes: int,
    backup_count: int,
) -> None:
    """配置轻量滚动工具调用日志，避免日志无限占用磁盘。"""

    path = Path(log_path)
    path.parent.mkdir(parents=True, exist_ok=True)

    logger = logging.getLogger(TOOL_LOGGER_NAME)
    logger.setLevel(logging.INFO)
    logger.propagate = False
    shutdown_tool_logging()

    handler = RotatingFileHandler(
        path,
        maxBytes=max_bytes,
        backupCount=backup_count,
        encoding="utf-8",
    )
    handler.setFormatter(logging.Formatter("%(message)s"))
    logger.addHandler(handler)


def shutdown_tool_logging() -> None:
    logger = logging.getLogger(TOOL_LOGGER_NAME)
    for handler in list(logger.handlers):
        logger.removeHandler(handler)
        handler.close()


def sanitize_log_value(value: Any) -> Any:
    if isinstance(value, dict):
        sanitized = {}
        for key, item in value.items():
            normalized_key = str(key).lower().replace("_", "").replace("-", "")
            if any(part in normalized_key for part in SENSITIVE_KEY_PARTS):
                sanitized[key] = REDACTED_VALUE
            else:
                sanitized[key] = sanitize_log_value(item)
        return sanitized
    if isinstance(value, list):
        return [sanitize_log_value(item) for item in value]
    return value


def summarize_tool_result(result: Any) -> dict[str, Any]:
    if isinstance(result, dict):
        summary: dict[str, Any] = {"type": "object", "keys": sorted(result.keys())}
        for key in ("houses", "results", "items"):
            items = result.get(key)
            if isinstance(items, list):
                summary["itemCount"] = len(items)
                break
        return summary
    if isinstance(result, list):
        return {"type": "array", "itemCount": len(result)}
    return {"type": type(result).__name__}


def log_tool_call(
    *,
    conversation_id: str | None,
    user_id: int | None,
    tool_name: str,
    arguments: dict[str, Any],
    status: str,
    duration_ms: int,
    result: Any = None,
    error: str | None = None,
) -> None:
    event = {
        "timestamp": datetime.now(UTC).isoformat(),
        "event": "agent_tool_call",
        "traceId": get_trace_id(),
        "conversationId": conversation_id,
        "userId": user_id,
        "tool": tool_name,
        "status": status,
        "durationMs": duration_ms,
        "arguments": sanitize_log_value(arguments),
        "result": summarize_tool_result(result),
    }
    if error:
        event["error"] = error

    logging.getLogger(TOOL_LOGGER_NAME).info(
        json.dumps(event, ensure_ascii=False, separators=(",", ":"))
    )
    record_tool_metric(status, duration_ms)


def log_model_call(
    *,
    model: str,
    status: str,
    duration_ms: int,
    retries: int,
    usage: dict[str, int] | None = None,
    estimated_cost_cny: float = 0.0,
    error_type: str | None = None,
) -> None:
    safe_usage = usage or {}
    event = {
        "timestamp": datetime.now(UTC).isoformat(),
        "event": "agent_model_call",
        "traceId": get_trace_id(),
        "model": model,
        "status": status,
        "durationMs": duration_ms,
        "retries": retries,
        "usage": safe_usage,
        "estimatedCostCny": round(estimated_cost_cny, 8),
    }
    if error_type:
        event["errorType"] = error_type
    logging.getLogger(TOOL_LOGGER_NAME).info(
        json.dumps(event, ensure_ascii=False, separators=(",", ":"))
    )
    record_model_call(
        status,
        duration_ms,
        retries,
        safe_usage,
        estimated_cost_cny,
    )


def log_agent_request(
    *,
    status: str,
    duration_ms: int,
    conversation_id: str | None,
    user_id: int | None,
    error_type: str | None = None,
) -> None:
    event = {
        "timestamp": datetime.now(UTC).isoformat(),
        "event": "agent_request",
        "traceId": get_trace_id(),
        "conversationId": conversation_id,
        "userId": user_id,
        "status": status,
        "durationMs": duration_ms,
    }
    if error_type:
        event["errorType"] = error_type
    logging.getLogger(TOOL_LOGGER_NAME).info(
        json.dumps(event, ensure_ascii=False, separators=(",", ":"))
    )
    record_request(status, duration_ms)
