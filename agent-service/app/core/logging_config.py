import json
import logging
from datetime import UTC, datetime
from logging.handlers import RotatingFileHandler
from pathlib import Path
from typing import Any


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
