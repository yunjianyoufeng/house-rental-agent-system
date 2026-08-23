from contextvars import ContextVar, Token
from copy import deepcopy
from threading import Lock
from time import monotonic
from typing import Any


_TRACE_ID: ContextVar[str | None] = ContextVar("agent_trace_id", default=None)
_STARTED_AT = monotonic()
_LOCK = Lock()
_METRICS: dict[str, Any] = {}


def _empty_metrics() -> dict[str, Any]:
    return {
        "requests": {
            "total": 0,
            "success": 0,
            "error": 0,
            "durationMsTotal": 0,
            "durationMsMax": 0,
        },
        "models": {
            "calls": 0,
            "success": 0,
            "error": 0,
            "retries": 0,
            "durationMsTotal": 0,
            "promptTokens": 0,
            "promptCacheHitTokens": 0,
            "promptCacheMissTokens": 0,
            "completionTokens": 0,
            "totalTokens": 0,
            "estimatedCostCny": 0.0,
        },
        "tools": {
            "calls": 0,
            "success": 0,
            "error": 0,
            "durationMsTotal": 0,
        },
    }


_METRICS.update(_empty_metrics())


def set_trace_id(trace_id: str) -> Token:
    return _TRACE_ID.set(trace_id)


def reset_trace_id(token: Token) -> None:
    _TRACE_ID.reset(token)


def get_trace_id() -> str | None:
    return _TRACE_ID.get()


def record_request(status: str, duration_ms: int) -> None:
    with _LOCK:
        metrics = _METRICS["requests"]
        metrics["total"] += 1
        metrics["success" if status == "success" else "error"] += 1
        metrics["durationMsTotal"] += duration_ms
        metrics["durationMsMax"] = max(metrics["durationMsMax"], duration_ms)


def record_model_call(
    status: str,
    duration_ms: int,
    retries: int,
    usage: dict[str, int],
    estimated_cost_cny: float,
) -> None:
    with _LOCK:
        metrics = _METRICS["models"]
        metrics["calls"] += 1
        metrics["success" if status == "success" else "error"] += 1
        metrics["retries"] += retries
        metrics["durationMsTotal"] += duration_ms
        metrics["promptTokens"] += usage.get("promptTokens", 0)
        metrics["promptCacheHitTokens"] += usage.get(
            "promptCacheHitTokens", 0
        )
        metrics["promptCacheMissTokens"] += usage.get(
            "promptCacheMissTokens", 0
        )
        metrics["completionTokens"] += usage.get("completionTokens", 0)
        metrics["totalTokens"] += usage.get("totalTokens", 0)
        metrics["estimatedCostCny"] = round(
            metrics["estimatedCostCny"] + estimated_cost_cny,
            8,
        )


def record_tool_call(status: str, duration_ms: int) -> None:
    with _LOCK:
        metrics = _METRICS["tools"]
        metrics["calls"] += 1
        metrics["success" if status == "success" else "error"] += 1
        metrics["durationMsTotal"] += duration_ms


def metrics_snapshot() -> dict[str, Any]:
    with _LOCK:
        snapshot = deepcopy(_METRICS)
    request_total = snapshot["requests"]["total"]
    model_total = snapshot["models"]["calls"]
    tool_total = snapshot["tools"]["calls"]
    snapshot["requests"]["averageDurationMs"] = round(
        snapshot["requests"]["durationMsTotal"] / request_total,
        2,
    ) if request_total else 0
    snapshot["models"]["averageDurationMs"] = round(
        snapshot["models"]["durationMsTotal"] / model_total,
        2,
    ) if model_total else 0
    snapshot["tools"]["averageDurationMs"] = round(
        snapshot["tools"]["durationMsTotal"] / tool_total,
        2,
    ) if tool_total else 0
    return {
        "uptimeSeconds": round(monotonic() - _STARTED_AT, 2),
        **snapshot,
    }


def reset_metrics() -> None:
    """仅供自动化测试重置进程内指标。"""

    with _LOCK:
        _METRICS.clear()
        _METRICS.update(_empty_metrics())
