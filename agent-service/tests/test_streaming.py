import json
import unittest
from types import SimpleNamespace
from unittest.mock import AsyncMock, patch

from app.agent.graph import should_prefetch_rental_knowledge, stream_agent_events
from app.api.chat import encode_sse_event


class FakeStream:
    def __init__(self, contents):
        self.contents = contents

    def __aiter__(self):
        async def iterate():
            for content in self.contents:
                delta = SimpleNamespace(content=content, tool_calls=[])
                yield SimpleNamespace(choices=[SimpleNamespace(delta=delta)])

        return iterate()


class StreamingAgentTest(unittest.IsolatedAsyncioTestCase):
    async def test_streams_text_delta_and_done_event(self):
        state = {
            "messages": [{"role": "user", "content": "你好"}],
            "answer": "",
            "pending_tool_calls": [],
            "tool_rounds": 0,
            "conversation_id": "conversation-1",
            "user_id": 8,
            "authorization": None,
            "appointment_pending_at_start": False,
            "knowledge_sources": [],
        }

        with patch(
            "app.agent.graph.create_completion_stream",
            new=AsyncMock(return_value=FakeStream(["你", "好"])),
        ):
            events = [event async for event in stream_agent_events(state)]

        self.assertEqual(
            ["delta", "delta", "done"],
            [item["event"] for item in events],
        )
        self.assertEqual("你好", events[-1]["answer"])

    async def test_prefetches_rag_for_platform_process_question(self):
        state = {
            "messages": [
                {
                    "role": "user",
                    "content": "请说明租房申请审核通过后合同和订单如何处理",
                }
            ],
            "answer": "",
            "pending_tool_calls": [],
            "tool_rounds": 0,
            "conversation_id": "conversation-2",
            "user_id": 8,
            "authorization": None,
            "appointment_pending_at_start": False,
            "knowledge_sources": [],
        }
        knowledge_result = {
            "query": "申请审核",
            "results": [
                {
                    "source": "rental-application-contract-order.md",
                    "title": "租房申请、合同与订单",
                    "section": "申请审核通过后",
                    "content": "审核通过后生成合同和订单。",
                    "relevance": 0.9,
                }
            ],
        }

        with (
            patch(
                "app.agent.graph.execute_knowledge_tool",
                new=AsyncMock(return_value=knowledge_result),
            ),
            patch(
                "app.agent.graph.create_completion_stream",
                new=AsyncMock(return_value=FakeStream(["已", "处理"])),
            ),
        ):
            events = [event async for event in stream_agent_events(state)]

        self.assertEqual("status", events[0]["event"])
        self.assertEqual(
            "rental-application-contract-order.md",
            events[-1]["sources"][0]["source"],
        )

    def test_does_not_treat_appointment_action_as_knowledge_question(self):
        messages = [{"role": "user", "content": "帮我预约19号房明天上午看房"}]

        self.assertFalse(should_prefetch_rental_knowledge(messages))

    def test_encodes_valid_sse_event(self):
        encoded = encode_sse_event("delta", {"content": "济南"})
        event_line, data_line = encoded.splitlines()[:2]

        self.assertEqual("event: delta", event_line)
        self.assertEqual(
            "济南",
            json.loads(data_line.removeprefix("data: "))["content"],
        )


if __name__ == "__main__":
    unittest.main()
