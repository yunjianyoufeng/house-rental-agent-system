import hashlib
import json
import logging
from typing import Any
from urllib.parse import quote

import redis.asyncio as redis

from app.core.config import get_settings
from app.services.backend_client import (
    BackendServiceError,
    get_backend_data,
    post_backend_data,
    put_backend_data,
)
from app.services.llm_service import create_history_summary


_redis_client: redis.Redis | None = None
logger = logging.getLogger(__name__)


def get_redis_client() -> redis.Redis:
    global _redis_client
    if _redis_client is None:
        settings = get_settings()
        _redis_client = redis.Redis.from_url(
            settings.redis_url,
            decode_responses=True,
            max_connections=10,
        )
    return _redis_client


async def close_redis_client() -> None:
    global _redis_client
    if _redis_client is not None:
        await _redis_client.aclose()
        _redis_client = None


def _memory_key(namespace: str, conversation_id: str, user_id: int) -> str:
    conversation_hash = hashlib.sha256(conversation_id.encode("utf-8")).hexdigest()
    return f"agent:{namespace}:{user_id}:{conversation_hash}"


def _has_identity(conversation_id: str | None, user_id: int | None) -> bool:
    return bool(conversation_id) and user_id is not None


async def load_conversation_messages(
    conversation_id: str | None,
    user_id: int | None,
    authorization: str | None = None,
) -> list[dict[str, str]]:
    if not _has_identity(conversation_id, user_id):
        return []
    if authorization:
        try:
            return await _load_persistent_context(conversation_id, authorization)
        except BackendServiceError as exc:
            logger.warning("MySQL对话上下文读取失败，降级使用Redis：%s", exc)

    settings = get_settings()
    key = _memory_key("conversation", conversation_id, user_id)
    values = await get_redis_client().lrange(key, 0, -1)
    messages: list[dict[str, str]] = []
    for value in values:
        try:
            message = json.loads(value)
        except (TypeError, json.JSONDecodeError):
            continue
        if (
            isinstance(message, dict)
            and message.get("role") in {"user", "assistant"}
            and isinstance(message.get("content"), str)
        ):
            messages.append(
                {"role": message["role"], "content": message["content"]}
            )
    return messages[-settings.conversation_max_messages :]


async def save_conversation_exchange(
    conversation_id: str | None,
    user_id: int | None,
    user_message: str,
    assistant_message: str,
    authorization: str | None = None,
    sources: list[dict[str, Any]] | None = None,
) -> None:
    if not _has_identity(conversation_id, user_id):
        return
    settings = get_settings()
    key = _memory_key("conversation", conversation_id, user_id)
    user_value = json.dumps(
        {"role": "user", "content": user_message}, ensure_ascii=False
    )
    assistant_value = json.dumps(
        {"role": "assistant", "content": assistant_message}, ensure_ascii=False
    )
    async with get_redis_client().pipeline(transaction=True) as pipeline:
        pipeline.rpush(key, user_value, assistant_value)
        pipeline.ltrim(key, -settings.conversation_max_messages, -1)
        pipeline.expire(key, settings.conversation_ttl_seconds)
        await pipeline.execute()

    if authorization:
        try:
            await post_backend_data(
                "/tenant/agent-history/exchanges",
                {
                    "conversationId": conversation_id,
                    "userMessage": user_message,
                    "assistantMessage": assistant_message,
                    "sourcesJson": json.dumps(sources or [], ensure_ascii=False),
                },
                headers={"Authorization": authorization},
            )
        except BackendServiceError as exc:
            logger.warning("MySQL对话历史保存失败，当前仅保留Redis副本：%s", exc)


async def _load_persistent_context(
    conversation_id: str,
    authorization: str,
) -> list[dict[str, str]]:
    settings = get_settings()
    encoded_id = quote(conversation_id, safe="")
    data = await get_backend_data(
        f"/tenant/agent-history/{encoded_id}/context",
        headers={"Authorization": authorization},
    )
    if not isinstance(data, dict):
        return []

    summary = str(data.get("summary") or "").strip()
    raw_messages = data.get("messages")
    messages: list[dict[str, Any]] = []
    if isinstance(raw_messages, list):
        for item in raw_messages:
            if (
                isinstance(item, dict)
                and item.get("role") in {"user", "assistant"}
                and isinstance(item.get("content"), str)
                and isinstance(item.get("id"), int)
            ):
                messages.append(item)

    recent_count = max(2, min(
        settings.conversation_recent_messages,
        settings.conversation_max_messages,
    ))
    if len(messages) > settings.conversation_max_messages:
        compact_messages = messages[:-recent_count]
        try:
            summary = await create_history_summary(
                summary or None,
                [
                    {"role": item["role"], "content": item["content"]}
                    for item in compact_messages
                ],
            )
            through_message_id = compact_messages[-1]["id"]
            await put_backend_data(
                f"/tenant/agent-history/{encoded_id}/summary",
                {
                    "summary": summary,
                    "throughMessageId": through_message_id,
                },
                headers={"Authorization": authorization},
            )
            messages = messages[-recent_count:]
        except Exception as exc:
            logger.warning("对话摘要生成失败，降级使用最近消息：%s", exc)
            messages = messages[-settings.conversation_max_messages :]

    context: list[dict[str, str]] = []
    if summary:
        context.append({"role": "system", "content": f"此前对话摘要：{summary}"})
    context.extend(
        {"role": item["role"], "content": item["content"]}
        for item in messages[-settings.conversation_max_messages :]
    )
    return context


async def get_json_state(
    namespace: str,
    conversation_id: str | None,
    user_id: int | None,
) -> dict[str, Any] | None:
    if not _has_identity(conversation_id, user_id):
        return None
    key = _memory_key(namespace, conversation_id, user_id)
    value = await get_redis_client().get(key)
    if value is None:
        return None
    try:
        state = json.loads(value)
    except json.JSONDecodeError:
        await get_redis_client().delete(key)
        return None
    return state if isinstance(state, dict) else None


async def set_json_state(
    namespace: str,
    conversation_id: str,
    user_id: int,
    state: dict[str, Any],
    ttl_seconds: int,
) -> None:
    key = _memory_key(namespace, conversation_id, user_id)
    await get_redis_client().set(
        key,
        json.dumps(state, ensure_ascii=False),
        ex=ttl_seconds,
    )


async def delete_state(
    namespace: str,
    conversation_id: str | None,
    user_id: int | None,
) -> bool:
    if not _has_identity(conversation_id, user_id):
        return False
    key = _memory_key(namespace, conversation_id, user_id)
    return bool(await get_redis_client().delete(key))
