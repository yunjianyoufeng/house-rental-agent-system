from typing import Any

from app.services.backend_client import (
    delete_backend_data,
    get_backend_data,
    put_backend_data,
)


PREFERENCE_PATH = "/tenant/agent/preferences"

PREFERENCE_TOOL_DEFINITIONS: list[dict[str, Any]] = [
    {
        "type": "function",
        "function": {
            "name": "save_rental_preference",
            "description": "只在用户明确要求记住或保存长期租房偏好时，结构化保存其明确提供的字段。",
            "parameters": {
                "type": "object",
                "properties": {
                    "budget_min": {"type": "number", "minimum": 0},
                    "budget_max": {"type": "number", "minimum": 0},
                    "preferred_city": {"type": "string", "maxLength": 50},
                    "preferred_area": {"type": "string", "maxLength": 100},
                    "preferred_house_type": {"type": "string", "maxLength": 50},
                    "workplace": {"type": "string", "maxLength": 150},
                    "max_commute_minutes": {
                        "type": "integer",
                        "minimum": 1,
                        "maximum": 300,
                    },
                    "preference_tags": {"type": "string", "maxLength": 500},
                },
                "minProperties": 1,
                "additionalProperties": False,
            },
        },
    },
    {
        "type": "function",
        "function": {
            "name": "clear_rental_preference",
            "description": "只在用户明确要求忘记或清除所有长期租房偏好时使用。",
            "parameters": {
                "type": "object",
                "properties": {},
                "additionalProperties": False,
            },
        },
    },
]


def _authorization_headers(authorization: str | None) -> dict[str, str]:
    if not authorization:
        raise ValueError("登录状态已失效，无法读写长期租房偏好。")
    return {"Authorization": authorization}


async def load_rental_preference(
    authorization: str | None,
) -> dict[str, Any] | None:
    if not authorization:
        return None
    return await get_backend_data(
        PREFERENCE_PATH,
        headers=_authorization_headers(authorization),
    )


async def save_rental_preference(
    arguments: dict[str, Any], authorization: str | None
) -> dict[str, Any]:
    field_names = {
        "budget_min": "budgetMin",
        "budget_max": "budgetMax",
        "preferred_city": "preferredCity",
        "preferred_area": "preferredArea",
        "preferred_house_type": "preferredHouseType",
        "workplace": "workplace",
        "max_commute_minutes": "maxCommuteMinutes",
        "preference_tags": "preferenceTags",
    }
    body = {
        backend_name: arguments[name]
        for name, backend_name in field_names.items()
        if arguments.get(name) is not None
    }
    if not body:
        raise ValueError("请至少提供一项要保存的长期租房偏好。")
    return await put_backend_data(
        PREFERENCE_PATH,
        body,
        headers=_authorization_headers(authorization),
    )


async def clear_rental_preference(authorization: str | None) -> dict[str, str]:
    result = await delete_backend_data(
        PREFERENCE_PATH,
        headers=_authorization_headers(authorization),
    )
    return {"message": result or "已清除长期租房偏好"}


async def execute_preference_tool(
    name: str,
    arguments: dict[str, Any],
    authorization: str | None,
) -> Any:
    if name == "save_rental_preference":
        return await save_rental_preference(arguments, authorization)
    if name == "clear_rental_preference":
        return await clear_rental_preference(authorization)
    raise ValueError(f"不支持的长期偏好工具：{name}")
