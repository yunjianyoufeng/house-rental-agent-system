import unittest
from unittest.mock import AsyncMock, patch

from mcp import Client

from app.mcp.server import (
    MCP_TOOL_NAMES,
    execute_mcp_tool,
    exposed_mcp_tool_names,
    mcp,
)


class McpServerTest(unittest.IsolatedAsyncioTestCase):
    def test_exposes_only_expected_public_read_only_tools(self):
        self.assertEqual(MCP_TOOL_NAMES, exposed_mcp_tool_names())
        self.assertNotIn("confirm_appointment", exposed_mcp_tool_names())
        self.assertNotIn("get_my_orders", exposed_mcp_tool_names())

    async def test_rejects_non_whitelisted_tool(self):
        with self.assertRaisesRegex(ValueError, "公开只读白名单"):
            await execute_mcp_tool("confirm_appointment", {})

    async def test_lists_tools_through_in_memory_mcp_client(self):
        async with Client(mcp) as client:
            result = await client.list_tools()

        names = {tool.name for tool in result.tools}
        self.assertEqual(set(MCP_TOOL_NAMES), names)
        search_tool = next(
            tool for tool in result.tools if tool.name == "search_houses"
        )
        self.assertTrue(search_tool.annotations.read_only_hint)
        self.assertFalse(search_tool.annotations.destructive_hint)
        self.assertIn("city", search_tool.input_schema["properties"])

    async def test_calls_registered_tool_through_mcp_client(self):
        expected = [{"id": 19, "title": "近医院电梯两室"}]
        with patch(
            "app.mcp.server.execute_registered_tool",
            new=AsyncMock(return_value=expected),
        ) as execute_tool:
            async with Client(mcp) as client:
                result = await client.call_tool(
                    "search_houses",
                    {"city": "济南", "limit": 3},
                )

        self.assertFalse(result.is_error)
        self.assertEqual(expected, result.structured_content["result"])
        execute_tool.assert_awaited_once()
        name, arguments, context = execute_tool.await_args.args
        self.assertEqual("search_houses", name)
        self.assertEqual({"city": "济南", "limit": 3}, arguments)
        self.assertIsNone(context.authorization)


if __name__ == "__main__":
    unittest.main()
