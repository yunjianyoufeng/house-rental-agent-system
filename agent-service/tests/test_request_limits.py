import unittest
from unittest.mock import AsyncMock, patch

from fastapi import HTTPException

from app.api.chat import chat_stream
from app.schemas.chat import ChatRequest
from app.services.request_limit_service import check_chat_quota


class RequestLimitTest(unittest.IsolatedAsyncioTestCase):
    async def test_user_quota_rejects_over_limit(self):
        redis = AsyncMock()
        redis.eval.return_value = 7
        with patch("app.services.request_limit_service.get_redis_client", return_value=redis):
            with self.assertRaises(HTTPException) as result:
                await check_chat_quota(8)
        self.assertEqual(429, result.exception.status_code)
        self.assertIn(":8", redis.eval.await_args.args[2])

    async def test_stream_rate_limit_is_http_429_before_model_call(self):
        with (
            patch("app.api.chat.resolve_authenticated_identity", new=AsyncMock(return_value=(8, "TENANT"))),
            patch("app.api.chat.check_chat_quota", new=AsyncMock(side_effect=HTTPException(429, "limited"))),
            patch("app.api.chat.initial_agent_state", new=AsyncMock()) as initial_state,
        ):
            with self.assertRaises(HTTPException) as result:
                await chat_stream(ChatRequest(message="你好"), "Bearer token")
        self.assertEqual(429, result.exception.status_code)
        initial_state.assert_not_awaited()
