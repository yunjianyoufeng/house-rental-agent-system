from typing import Any

from app.services.backend_client import get_backend_data


PERSONAL_TOOL_DEFINITIONS: list[dict[str, Any]] = [
    {
        "type": "function",
        "function": {
            "name": name,
            "description": description,
            "parameters": {
                "type": "object",
                "properties": {},
                "additionalProperties": False,
            },
        },
    }
    for name, description in (
        ("get_my_appointments", "查询当前登录租客自己的看房预约记录。"),
        ("get_my_applications", "查询当前登录租客自己的租房申请记录。"),
        ("get_my_contracts", "查询当前登录租客自己的租赁合同记录。"),
        ("get_my_orders", "查询当前登录租客自己的租赁订单和支付状态。"),
        ("get_my_repairs", "查询当前登录租客自己提交的报修记录。"),
        ("get_my_complaints", "查询当前登录租客自己提交的投诉记录。"),
    )
]

PERSONAL_TOOL_PATHS = {
    "get_my_appointments": ("appointments", "appointment"),
    "get_my_applications": ("applications", "application"),
    "get_my_contracts": ("contracts", "contract"),
    "get_my_orders": ("orders", "order"),
    "get_my_repairs": ("repairs", "repair"),
    "get_my_complaints": ("complaints", "complaint"),
}

VISIBLE_FIELDS = {
    "appointment": (
        "id", "houseId", "houseTitle", "appointmentTime", "status", "remark", "createTime",
    ),
    "application": (
        "id", "houseId", "houseTitle", "status", "remark", "createTime",
    ),
    "contract": (
        "id", "houseId", "houseTitle", "startDate", "endDate", "monthlyRent", "deposit", "status", "createTime",
    ),
    "order": (
        "id", "contractId", "houseId", "houseTitle", "amount", "payType", "payStatus", "payTime", "createTime",
    ),
    "repair": (
        "id", "houseId", "houseTitle", "content", "status", "result", "createTime",
    ),
    "complaint": (
        "id", "targetId", "targetName", "content", "status", "result", "createTime",
    ),
}


def _status_text(record_type: str, item: dict[str, Any]) -> str | None:
    if record_type == "order":
        return {0: "未支付", 1: "已支付", 2: "已取消", 3: "已过期"}.get(
            item.get("payStatus")
        )
    if record_type in {"appointment", "application"}:
        return {0: "待处理", 1: "已同意", 2: "已拒绝"}.get(item.get("status"))
    if record_type in {"repair", "complaint"}:
        if record_type == "repair":
            return {0: "待处理", 1: "处理中", 2: "已完成"}.get(item.get("status"))
        return {0: "待处理", 1: "已处理"}.get(item.get("status"))
    if record_type == "contract":
        return {0: "待生效", 1: "生效中", 2: "已结束", 3: "已取消"}.get(
            item.get("status")
        )
    return None


def _compact_item(record_type: str, item: Any) -> dict[str, Any]:
    if not isinstance(item, dict):
        return {"value": str(item)}
    compact = {
        field: item[field]
        for field in VISIBLE_FIELDS[record_type]
        if item.get(field) is not None
    }
    status_text = _status_text(record_type, item)
    if status_text:
        compact["statusText"] = status_text
    return compact


async def execute_personal_tool(
    name: str,
    authorization: str | None,
    role_code: str | None,
) -> dict[str, Any]:
    if role_code != "TENANT":
        raise ValueError("当前登录角色没有租客个人业务数据查询权限。")
    if not authorization:
        raise ValueError("登录状态已失效，无法查询个人业务数据。")
    if name not in PERSONAL_TOOL_PATHS:
        raise ValueError(f"不支持的个人业务查询工具：{name}")

    path_name, record_type = PERSONAL_TOOL_PATHS[name]
    data = await get_backend_data(
        f"/tenant/agent-tools/me/{path_name}",
        headers={"Authorization": authorization},
    )
    records = data if isinstance(data, list) else []
    return {
        "recordType": record_type,
        "count": len(records),
        "items": [_compact_item(record_type, item) for item in records[:20]],
        "truncated": len(records) > 20,
    }
