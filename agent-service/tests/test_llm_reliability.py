import unittest
from types import SimpleNamespace
from unittest.mock import AsyncMock, patch

from openai import OpenAIError
from pydantic import SecretStr

from app.services.llm_service import (
    ModelServiceUnavailableError,
    create_completion,
)


def _settings(max_retries: int = 2):
    return SimpleNamespace(
        deepseek_api_key=SecretStr("test-key"),
        deepseek_base_url="https://example.invalid",
        deepseek_model="test-model",
        deepseek_timeout_seconds=5.0,
        deepseek_max_retries=max_retries,
        deepseek_retry_base_seconds=0,
        deepseek_input_cache_hit_cny_per_million=0.02,
        deepseek_input_cache_miss_cny_per_million=1.0,
        deepseek_output_cny_per_million=2.0,
    )


def _client_with_side_effect(side_effect):
    create = AsyncMock(side_effect=side_effect)
    client = SimpleNamespace(
        chat=SimpleNamespace(completions=SimpleNamespace(create=create))
    )
    return client, create


class LlmReliabilityTest(unittest.IsolatedAsyncioTestCase):
    async def test_retries_transient_error_and_returns_success(self):
        response = SimpleNamespace(choices=[])
        client, create = _client_with_side_effect(
            [OpenAIError("temporary"), response]
        )

        with (
            patch("app.services.llm_service.get_settings", return_value=_settings()),
            patch("app.services.llm_service.AsyncOpenAI", return_value=client),
            patch(
                "app.services.llm_service.get_langsmith_client",
                return_value=None,
            ),
            patch("app.services.llm_service._is_retryable_error", return_value=True),
        ):
            result = await create_completion([{"role": "user", "content": "你好"}])

        self.assertIs(response, result)
        self.assertEqual(2, create.await_count)

    async def test_does_not_retry_permanent_error(self):
        client, create = _client_with_side_effect(OpenAIError("bad request"))

        with (
            patch("app.services.llm_service.get_settings", return_value=_settings()),
            patch("app.services.llm_service.AsyncOpenAI", return_value=client),
            patch(
                "app.services.llm_service.get_langsmith_client",
                return_value=None,
            ),
            patch("app.services.llm_service._is_retryable_error", return_value=False),
        ):
            with self.assertRaises(OpenAIError):
                await create_completion([{"role": "user", "content": "你好"}])

        self.assertEqual(1, create.await_count)

    async def test_raises_stable_error_after_retry_exhaustion(self):
        client, create = _client_with_side_effect(OpenAIError("temporary"))

        with (
            patch(
                "app.services.llm_service.get_settings",
                return_value=_settings(max_retries=2),
            ),
            patch("app.services.llm_service.AsyncOpenAI", return_value=client),
            patch(
                "app.services.llm_service.get_langsmith_client",
                return_value=None,
            ),
            patch("app.services.llm_service._is_retryable_error", return_value=True),
        ):
            with self.assertRaises(ModelServiceUnavailableError):
                await create_completion([{"role": "user", "content": "你好"}])

        self.assertEqual(3, create.await_count)


if __name__ == "__main__":
    unittest.main()
