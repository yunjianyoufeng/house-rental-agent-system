import unittest
from unittest.mock import Mock, patch

import httpx

from app.core.config import Settings
from app.rag.embedding import (
    EmbeddingConfigurationError,
    embed_texts,
)


DIMENSIONS = 768


def embedding_response(
    items: list[dict],
    status_code: int = 200,
) -> httpx.Response:
    request = httpx.Request("POST", "https://example.com/v1/embeddings")
    return httpx.Response(
        status_code,
        json={"data": items, "model": "text-embedding-v4"},
        request=request,
    )


def test_settings(**overrides) -> Settings:
    values = {
        "rag_embedding_provider": "openai_compatible",
        "rag_embedding_base_url": "https://example.com/v1",
        "rag_embedding_api_key": "test-secret",
        "rag_embedding_model": "text-embedding-v4",
        "rag_embedding_dimensions": DIMENSIONS,
        "rag_embedding_batch_size": 10,
        "rag_embedding_max_retries": 2,
        "rag_embedding_retry_base_seconds": 0,
    }
    values.update(overrides)
    return Settings(_env_file=None, **values)


class EmbeddingProviderTest(unittest.TestCase):
    def test_openai_compatible_response_is_sorted_and_normalized(self):
        first = [3.0] + [0.0] * (DIMENSIONS - 1)
        second = [0.0, 4.0] + [0.0] * (DIMENSIONS - 2)
        response = embedding_response(
            [
                {"index": 1, "embedding": second},
                {"index": 0, "embedding": first},
            ]
        )

        with patch("app.rag.embedding.httpx.post", return_value=response) as post:
            batch = embed_texts(["预约流程", "合同流程"], test_settings())

        self.assertEqual(1.0, batch.vectors[0][0])
        self.assertEqual(1.0, batch.vectors[1][1])
        self.assertEqual("openai_compatible", batch.provider)
        request = post.call_args.kwargs
        self.assertEqual(
            "Bearer test-secret",
            request["headers"]["Authorization"],
        )
        self.assertEqual(DIMENSIONS, request["json"]["dimensions"])

    def test_retries_rate_limit_without_exposing_response_body(self):
        rate_limited = embedding_response([], status_code=429)
        success = embedding_response(
            [{"index": 0, "embedding": [1.0] + [0.0] * (DIMENSIONS - 1)}]
        )

        with (
            patch(
                "app.rag.embedding.httpx.post",
                side_effect=[rate_limited, success],
            ) as post,
            patch("app.rag.embedding.time.sleep") as sleep,
        ):
            batch = embed_texts(["报修流程"], test_settings())

        self.assertEqual(2, post.call_count)
        sleep.assert_not_called()
        self.assertEqual(1, len(batch.vectors))

    def test_batches_requests_using_configured_limit(self):
        def respond(*args, **kwargs):
            del args
            texts = kwargs["json"]["input"]
            vector = [1.0] + [0.0] * (DIMENSIONS - 1)
            return embedding_response(
                [
                    {"index": index, "embedding": vector}
                    for index, _ in enumerate(texts)
                ]
            )

        post = Mock(side_effect=respond)
        with patch("app.rag.embedding.httpx.post", post):
            batch = embed_texts(
                [f"知识片段{index}" for index in range(11)],
                test_settings(),
            )

        self.assertEqual(2, post.call_count)
        self.assertEqual(11, len(batch.vectors))

    def test_rejects_missing_api_key_without_network_call(self):
        settings = test_settings(rag_embedding_api_key=None)

        with patch("app.rag.embedding.httpx.post") as post:
            with self.assertRaises(EmbeddingConfigurationError):
                embed_texts(["看房预约"], settings)

        post.assert_not_called()


if __name__ == "__main__":
    unittest.main()
