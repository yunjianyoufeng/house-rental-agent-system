from fastapi import HTTPException

from app.services.memory_service import get_redis_client


COUNTER_SCRIPT = """
local count = redis.call('INCR', KEYS[1])
if count == 1 then redis.call('EXPIRE', KEYS[1], ARGV[1]) end
return count
"""


async def check_chat_quota(user_id: int) -> None:
    """在 Agent 本身限额，直连端口和流式入口也不能绕过付费调用配额。"""
    for window, limit in ((60, 6), (3600, 60)):
        count = await get_redis_client().eval(
            COUNTER_SCRIPT, 1, f"request-limit:agent:{window}:{user_id}", window
        )
        if count is None or int(count) > limit:
            raise HTTPException(status_code=429, detail="智能助手调用过于频繁，请稍后重试。",
                                headers={"Retry-After": str(window)})
