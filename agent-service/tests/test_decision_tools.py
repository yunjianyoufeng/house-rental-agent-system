import unittest
from unittest.mock import AsyncMock, patch

from app.tools.decision_tools import recommend_houses


class DecisionToolsTest(unittest.IsolatedAsyncioTestCase):
    async def test_recommendation_forwards_authorization(self):
        backend_mock = AsyncMock(return_value=[])

        with patch(
            "app.tools.decision_tools.post_backend_data",
            new=backend_mock,
        ):
            await recommend_houses(
                {
                    "query": "聊城安静房源",
                    "city": "聊城",
                    "top_k": 3,
                },
                "Bearer verified-token",
            )

        _, body = backend_mock.await_args.args
        self.assertEqual("聊城安静房源", body["query"])
        self.assertEqual(
            {"Authorization": "Bearer verified-token"},
            backend_mock.await_args.kwargs["headers"],
        )


if __name__ == "__main__":
    unittest.main()
