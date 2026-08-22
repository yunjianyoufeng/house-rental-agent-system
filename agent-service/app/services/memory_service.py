import hashlib
import json
from typing import Any

import redis.asyncio as redis

from app.core.config import get_settings


_redis_client: redis.Redis | None = None


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
) -> list[dict[str, str]]:
    if not _has_identity(conversation_id, user_id):
        return []
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
