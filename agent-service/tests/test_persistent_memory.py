import unittest
from unittest.mock import AsyncMock, patch

from app.services.memory_service import _load_persistent_context


class PersistentMemoryTest(unittest.IsolatedAsyncioTestCase):
    async def test_compacts_old_messages_and_keeps_recent_window(self):
        messages = [
            {
                "id": index,
                "role": "user" if index % 2 else "assistant",
                "content": f"消息{index}",
            }
            for index in range(1, 15)
        ]
        with (
            patch(
                "app.services.memory_service.get_backend_data",
                new=AsyncMock(
                    return_value={
                        "summary": "已有摘要",
                        "summaryMessageId": 0,
                        "messages": messages,
                    }
                ),
            ),
            patch(
                "app.services.memory_service.create_history_summary",
                new=AsyncMock(return_value="压缩后的摘要"),
            ) as summarize,
            patch(
                "app.services.memory_service.put_backend_data",
                new=AsyncMock(return_value="ok"),
            ) as update_summary,
        ):
            context = await _load_persistent_context(
                "conversation-1",
                "Bearer token",
            )

        self.assertEqual("此前对话摘要：压缩后的摘要", context[0]["content"])
        self.assertEqual(8, len(context) - 1)
        self.assertEqual("消息7", context[1]["content"])
        summarize.assert_awaited_once()
        self.assertEqual(
            6,
            update_summary.await_args.args[1]["throughMessageId"],
        )

    async def test_returns_summary_and_messages_without_recompression(self):
        with (
            patch(
                "app.services.memory_service.get_backend_data",
                new=AsyncMock(
                    return_value={
                        "summary": "用户预算1800元",
                        "messages": [
                            {"id": 11, "role": "user", "content": "继续找房"},
                            {"id": 12, "role": "assistant", "content": "好的"},
                        ],
                    }
                ),
            ),
            patch(
                "app.services.memory_service.create_history_summary",
                new=AsyncMock(),
            ) as summarize,
        ):
            context = await _load_persistent_context(
                "conversation-2",
                "Bearer token",
            )

        self.assertEqual("system", context[0]["role"])
        self.assertEqual("继续找房", context[1]["content"])
        summarize.assert_not_awaited()


if __name__ == "__main__":
    unittest.main()
