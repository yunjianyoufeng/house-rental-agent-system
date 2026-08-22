from typing import Any

from app.services.backend_client import get_backend_data


HOUSE_TOOL_DEFINITIONS: list[dict[str, Any]] = [
    {
        "type": "function",
        "function": {
            "name": "search_houses",
            "description": "根据城市、区域、租金、户型或关键词查询平台中真实可租的房源。",
            "parameters": {
                "type": "object",
                "properties": {
                    "city": {"type": "string", "description": "城市，例如济南"},
                    "area": {"type": "string", "description": "区县，例如历下区"},
                    "min_rent": {"type": "number", "minimum": 0},
                    "max_rent": {"type": "number", "minimum": 0},
                    "house_type": {"type": "string", "description": "户型，例如一室一厅"},
                    "keyword": {"type": "string", "description": "安静、学校、交通等描述关键词"},
                    "limit": {"type": "integer", "minimum": 1, "maximum": 10},
                },
                "additionalProperties": False,
            },
        },
    },
    {
        "type": "function",
        "function": {
            "name": "get_house_detail",
            "description": "根据房源ID查询平台中的真实公开房源详情。",
            "parameters": {
                "type": "object",
                "properties": {
                    "house_id": {"type": "integer", "minimum": 1},
                },
                "required": ["house_id"],
                "additionalProperties": False,
            },
        },
    },
]


def compact_house(house: dict[str, Any], include_detail: bool = False) -> dict[str, Any]:
    fields = [
        "id",
        "title",
        "city",
        "area",
        "rentPrice",
        "houseType",
        "square",
        "address",
        "description",
    ]
    if include_detail:
        fields.extend(["deposit", "floor", "longitude", "latitude", "imageUrls"])
    return {field: house.get(field) for field in fields}


async def search_houses(arguments: dict[str, Any]) -> list[dict[str, Any]]:
    parameter_names = {
        "city": "city",
        "area": "area",
        "min_rent": "minRent",
        "max_rent": "maxRent",
        "house_type": "houseType",
        "keyword": "keyword",
        "limit": "limit",
    }
    params = {
        backend_name: arguments[name]
        for name, backend_name in parameter_names.items()
        if arguments.get(name) is not None
    }
    houses = await get_backend_data("/agent-tools/houses/search", params=params)
    return [compact_house(house) for house in houses]


async def get_house_detail(arguments: dict[str, Any]) -> dict[str, Any]:
    house_id = arguments["house_id"]
    house = await get_backend_data(f"/agent-tools/houses/{house_id}")
    return compact_house(house, include_detail=True)


async def execute_house_tool(name: str, arguments: dict[str, Any]) -> Any:
    if name == "search_houses":
        return await search_houses(arguments)
    if name == "get_house_detail":
        return await get_house_detail(arguments)
    raise ValueError(f"不支持的工具：{name}")
