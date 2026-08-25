import json
import unittest
from unittest.mock import AsyncMock, patch

from app.agent.graph import (
    available_tool_definitions,
    tool_node,
)
from app.services.safety_service import UnsafeInputError, validate_user_message


class AgentSecurityTest(unittest.IsolatedAsyncioTestCase):
    def test_rejects_messages_containing_secret_values(self):
        unsafe_messages = (
            "我的 API Key 是 sk-1234567890abcdefghij",
            "Authorization: Bearer abcdefghijklmnopqrstuvwxyz123456",
            "JWT 是 eyJabcdefgh.eyJijklmnop.abcdefghijk",
            "身份证号是 370102199001011234",
            "银行卡号 6222 0212 3456 7890 123",
            "我的密码是 abc123456",
        )

        for message in unsafe_messages:
            with self.subTest(message=message):
                with self.assertRaises(UnsafeInputError):
                    validate_user_message(message)

    def test_allows_normal_password_help_question(self):
        validate_user_message("忘记登录密码后应该怎么办？")

    def test_only_tenant_receives_mutating_and_personal_tools(self):
        tenant_tools = {
            item["function"]["name"]
            for item in available_tool_definitions("TENANT")
        }
        landlord_tools = {
            item["function"]["name"]
            for item in available_tool_definitions("LANDLORD")
        }

        self.assertIn("prepare_appointment", tenant_tools)
        self.assertIn("save_rental_preference", tenant_tools)
        self.assertIn("get_my_contracts", tenant_tools)
        self.assertNotIn("prepare_appointment", landlord_tools)
        self.assertNotIn("save_rental_preference", landlord_tools)
        self.assertNotIn("get_my_contracts", landlord_tools)

    async def test_rejects_forged_tenant_tool_call_for_landlord(self):
        state = {
            "messages": [],
            "answer": "",
            "pending_tool_calls": [
                {
                    "id": "forged-call",
                    "name": "save_rental_preference",
                    "arguments": json.dumps({"preferred_city": "济南"}),
                }
            ],
            "tool_rounds": 0,
            "conversation_id": "conversation-1",
            "user_id": 9,
            "role_code": "LANDLORD",
            "authorization": "Bearer trusted-token",
            "appointment_pending_at_start": False,
            "knowledge_sources": [],
        }

        with patch(
            "app.tools.registry.execute_preference_tool",
            new=AsyncMock(),
        ) as execute_preference:
            result = await tool_node(state)

        execute_preference.assert_not_awaited()
        tool_result = json.loads(result["messages"][-1]["content"])
        self.assertIn("无权调用", tool_result["error"])


if __name__ == "__main__":
    unittest.main()
