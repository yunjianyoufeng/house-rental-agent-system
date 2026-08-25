import unittest
from unittest.mock import AsyncMock, patch

from app.tools.registry import (
    TOOL_REGISTRY,
    TOOL_SPECS,
    ToolExecutionContext,
    available_tool_definitions,
    execute_registered_tool,
    get_tool_spec,
    validate_tool_access,
)


class ToolRegistryTest(unittest.IsolatedAsyncioTestCase):
    def test_registered_tool_names_are_unique(self):
        self.assertEqual(len(TOOL_SPECS), len(TOOL_REGISTRY))

    def test_only_tenant_receives_tenant_tools(self):
        public_names = {
            item["function"]["name"]
            for item in available_tool_definitions(None)
        }
        tenant_names = {
            item["function"]["name"]
            for item in available_tool_definitions("TENANT")
        }

        self.assertIn("search_houses", public_names)
        self.assertNotIn("get_my_orders", public_names)
        self.assertIn("get_my_orders", tenant_names)

    def test_exposes_metadata_for_future_mcp_filtering(self):
        house_spec = get_tool_spec("search_houses")
        appointment_spec = get_tool_spec("confirm_appointment")

        self.assertIsNotNone(house_spec)
        self.assertTrue(house_spec.read_only)
        self.assertFalse(house_spec.requires_auth)
        self.assertIsNotNone(appointment_spec)
        self.assertFalse(appointment_spec.read_only)
        self.assertTrue(appointment_spec.requires_auth)

    def test_rejects_tenant_tool_without_authorization(self):
        with self.assertRaisesRegex(ValueError, "登录状态已失效"):
            validate_tool_access("get_my_orders", "TENANT", None)

    async def test_dispatches_registered_house_tool(self):
        expected = [{"id": 19, "title": "近医院电梯两室"}]
        with patch(
            "app.tools.registry.execute_house_tool",
            new=AsyncMock(return_value=expected),
        ) as execute_house:
            result = await execute_registered_tool(
                "search_houses",
                {"city": "济南"},
                ToolExecutionContext(),
            )

        self.assertEqual(expected, result)
        execute_house.assert_awaited_once_with(
            "search_houses",
            {"city": "济南"},
        )


if __name__ == "__main__":
    unittest.main()
