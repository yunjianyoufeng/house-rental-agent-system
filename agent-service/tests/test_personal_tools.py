import unittest
from unittest.mock import AsyncMock, patch

from app.agent.graph import available_tool_definitions
from app.tools.personal_tools import execute_personal_tool


class PersonalToolsTest(unittest.IsolatedAsyncioTestCase):
    async def test_queries_current_user_endpoint_and_removes_sensitive_fields(self):
        backend_records = [
            {
                "id": 6,
                "tenantId": 8,
                "houseId": 19,
                "houseTitle": "近医院电梯两室",
                "status": 1,
                "contractUrl": "private-contract.pdf",
            }
        ]
        with patch(
            "app.tools.personal_tools.get_backend_data",
            new=AsyncMock(return_value=backend_records),
        ) as backend:
            result = await execute_personal_tool(
                "get_my_contracts",
                "Bearer valid-token",
                "TENANT",
            )

        backend.assert_awaited_once_with(
            "/tenant/agent-tools/me/contracts",
            headers={"Authorization": "Bearer valid-token"},
        )
        self.assertEqual(1, result["count"])
        self.assertEqual("生效中", result["items"][0]["statusText"])
        self.assertNotIn("tenantId", result["items"][0])
        self.assertNotIn("contractUrl", result["items"][0])

    async def test_rejects_non_tenant_role(self):
        with self.assertRaisesRegex(ValueError, "没有租客"):
            await execute_personal_tool(
                "get_my_orders",
                "Bearer valid-token",
                "LANDLORD",
            )

    async def test_rejects_missing_authorization(self):
        with self.assertRaisesRegex(ValueError, "登录状态"):
            await execute_personal_tool("get_my_repairs", None, "TENANT")

    def test_only_tenant_receives_personal_tool_definitions(self):
        tenant_names = {
            tool["function"]["name"]
            for tool in available_tool_definitions("TENANT")
        }
        landlord_names = {
            tool["function"]["name"]
            for tool in available_tool_definitions("LANDLORD")
        }

        self.assertIn("get_my_contracts", tenant_names)
        self.assertNotIn("get_my_contracts", landlord_names)


if __name__ == "__main__":
    unittest.main()
