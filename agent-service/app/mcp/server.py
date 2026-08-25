from typing import Annotated, Any

from mcp.server import MCPServer
from mcp.types import ToolAnnotations
from pydantic import Field

from app.tools.registry import (
    ToolExecutionContext,
    execute_registered_tool,
    select_tool_specs,
)


MCP_TOOL_NAMES = (
    "search_houses",
    "get_house_detail",
    "compare_houses",
    "calculate_rental_budget",
    "search_rental_knowledge",
)


def exposed_mcp_tool_names() -> tuple[str, ...]:
    specs = select_tool_specs(MCP_TOOL_NAMES)
    unsafe = [
        spec.name
        for spec in specs
        if not spec.read_only or spec.requires_auth or spec.tenant_only
    ]
    if unsafe:
        raise RuntimeError(f"MCP只允许暴露公开只读工具：{unsafe}")
    return tuple(spec.name for spec in specs)


async def execute_mcp_tool(name: str, arguments: dict[str, Any]) -> Any:
    if name not in exposed_mcp_tool_names():
        raise ValueError("该工具未通过MCP公开只读白名单。")
    clean_arguments = {
        key: value
        for key, value in arguments.items()
        if value is not None
    }
    return await execute_registered_tool(
        name,
        clean_arguments,
        ToolExecutionContext(),
    )


READ_ONLY_ANNOTATIONS = ToolAnnotations(
    readOnlyHint=True,
    destructiveHint=False,
    idempotentHint=True,
    openWorldHint=False,
)

mcp = MCPServer(
    name="house-rental",
    title="房屋租赁只读工具服务",
    description="提供公开房源查询、房源对比、预算测算和平台知识检索能力。",
    instructions="仅提供公开只读能力，不提供预约、偏好修改或个人业务查询。",
    version="1.0.0",
)


@mcp.tool(annotations=READ_ONLY_ANNOTATIONS, structured_output=True)
async def search_houses(
    city: str | None = None,
    area: str | None = None,
    min_rent: Annotated[float | None, Field(ge=0)] = None,
    max_rent: Annotated[float | None, Field(ge=0)] = None,
    house_type: str | None = None,
    keyword: str | None = None,
    limit: Annotated[int | None, Field(ge=1, le=10)] = None,
) -> list[dict[str, Any]]:
    """根据城市、区域、租金、户型或关键词查询平台中的真实可租房源。"""
    if min_rent is not None and max_rent is not None and min_rent > max_rent:
        raise ValueError("最低租金不能高于最高租金。")
    return await execute_mcp_tool(
        "search_houses",
        {
            "city": city,
            "area": area,
            "min_rent": min_rent,
            "max_rent": max_rent,
            "house_type": house_type,
            "keyword": keyword,
            "limit": limit,
        },
    )


@mcp.tool(annotations=READ_ONLY_ANNOTATIONS, structured_output=True)
async def get_house_detail(
    house_id: Annotated[int, Field(ge=1)],
) -> dict[str, Any]:
    """根据房源ID查询平台中的真实公开房源详情。"""
    return await execute_mcp_tool(
        "get_house_detail",
        {"house_id": house_id},
    )


@mcp.tool(annotations=READ_ONLY_ANNOTATIONS, structured_output=True)
async def compare_houses(
    house_ids: Annotated[
        list[Annotated[int, Field(ge=1)]],
        Field(min_length=2, max_length=5),
    ],
) -> dict[str, Any]:
    """对比2至5套真实房源的租金、面积、押金等客观指标。"""
    if len(set(house_ids)) != len(house_ids):
        raise ValueError("房源ID不能重复。")
    return await execute_mcp_tool(
        "compare_houses",
        {"house_ids": house_ids},
    )


@mcp.tool(annotations=READ_ONLY_ANNOTATIONS, structured_output=True)
async def calculate_rental_budget(
    monthly_income: Annotated[float, Field(gt=0)],
    monthly_rent: Annotated[float, Field(ge=0)],
    deposit: Annotated[float | None, Field(ge=0)] = None,
    monthly_utilities: Annotated[float | None, Field(ge=0)] = None,
    monthly_transport: Annotated[float | None, Field(ge=0)] = None,
    other_monthly_expenses: Annotated[float | None, Field(ge=0)] = None,
) -> dict[str, Any]:
    """根据收入和租房支出计算租金占比、每月结余及首次准备金额。"""
    return await execute_mcp_tool(
        "calculate_rental_budget",
        {
            "monthly_income": monthly_income,
            "monthly_rent": monthly_rent,
            "deposit": deposit,
            "monthly_utilities": monthly_utilities,
            "monthly_transport": monthly_transport,
            "other_monthly_expenses": other_monthly_expenses,
        },
    )


@mcp.tool(annotations=READ_ONLY_ANNOTATIONS, structured_output=True)
async def search_rental_knowledge(
    query: Annotated[str, Field(min_length=1)],
    top_k: Annotated[int | None, Field(ge=1, le=5)] = None,
) -> dict[str, Any]:
    """检索平台预约、申请、合同、订单、报修和投诉等办理知识。"""
    return await execute_mcp_tool(
        "search_rental_knowledge",
        {"query": query.strip(), "top_k": top_k},
    )


def main() -> None:
    exposed_mcp_tool_names()
    mcp.run(transport="stdio")


if __name__ == "__main__":
    main()
