from datetime import datetime
import hashlib
from typing import Any

from app.core.config import get_settings
from app.services.backend_client import post_backend_data
from app.services.memory_service import delete_state, get_json_state, set_json_state
from app.tools.house_tools import get_house_detail


APPOINTMENT_TOOL_DEFINITIONS: list[dict[str, Any]] = [
    {
        "type": "function",
        "function": {
            "name": "prepare_appointment",
            "description": "准备一个看房预约并返回待确认摘要。该工具不会创建预约。",
            "parameters": {
                "type": "object",
                "properties": {
                    "house_id": {"type": "integer", "minimum": 1},
                    "appointment_time": {
                        "type": "string",
                        "description": "ISO 8601 本地时间，格式 YYYY-MM-DDTHH:mm:ss",
                    },
                    "remark": {"type": "string", "maxLength": 200},
                },
                "required": ["house_id", "appointment_time"],
                "additionalProperties": False,
            },
        },
    },
    {
        "type": "function",
        "function": {
            "name": "confirm_appointment",
            "description": "在用户于新的一轮对话中明确确认待办预约后，创建该预约。",
            "parameters": {
                "type": "object",
                "properties": {},
                "additionalProperties": False,
            },
        },
    },
    {
        "type": "function",
        "function": {
            "name": "cancel_appointment_preparation",
            "description": "用户明确取消或放弃当前待确认的看房预约时，清除待办信息。",
            "parameters": {
                "type": "object",
                "properties": {},
                "additionalProperties": False,
            },
        },
    },
]


PENDING_NAMESPACE = "appointment"


def _check_conversation_identity(
    conversation_id: str | None, user_id: int | None
) -> tuple[str, int]:
    if not conversation_id or user_id is None:
        raise ValueError("当前会话缺少登录身份或会话标识，无法办理预约。")
    return conversation_id, user_id


async def get_pending_appointment(
    conversation_id: str | None, user_id: int | None
) -> dict[str, Any] | None:
    return await get_json_state(PENDING_NAMESPACE, conversation_id, user_id)


def _parse_appointment_time(value: Any) -> datetime:
    if not isinstance(value, str) or not value.strip():
        raise ValueError("预约时间不能为空。")
    try:
        appointment_time = datetime.fromisoformat(value.strip())
    except ValueError as exc:
        raise ValueError("预约时间格式不正确。") from exc

    local_now = datetime.now().astimezone()
    if appointment_time.tzinfo is None:
        appointment_time = appointment_time.replace(tzinfo=local_now.tzinfo)
    else:
        appointment_time = appointment_time.astimezone(local_now.tzinfo)
    if appointment_time <= local_now:
        raise ValueError("预约时间必须晚于当前时间。")
    return appointment_time


def _build_request_key(
    user_id: int,
    conversation_id: str,
    house_id: Any,
    appointment_time: str,
    remark: str,
) -> str:
    request_source = (
        f"{user_id}:{conversation_id}:{house_id}:{appointment_time}:{remark}"
    )
    return hashlib.sha256(request_source.encode("utf-8")).hexdigest()


async def prepare_appointment(
    arguments: dict[str, Any],
    conversation_id: str | None,
    user_id: int | None,
) -> dict[str, Any]:
    conversation_id, user_id = _check_conversation_identity(
        conversation_id, user_id
    )
    appointment_time = _parse_appointment_time(arguments.get("appointment_time"))
    house = await get_house_detail({"house_id": arguments["house_id"]})
    remark = str(arguments.get("remark") or "").strip()
    normalized_time = appointment_time.isoformat(timespec="seconds")

    pending = {
        "houseId": house.get("id"),
        "houseTitle": house.get("title"),
        "address": house.get("address"),
        "rentPrice": house.get("rentPrice"),
        "appointmentTime": normalized_time,
        "remark": remark,
        "requestKey": _build_request_key(
            user_id,
            conversation_id,
            house.get("id"),
            normalized_time,
            remark,
        ),
    }
    await set_json_state(
        PENDING_NAMESPACE,
        conversation_id,
        user_id,
        pending,
        get_settings().appointment_pending_ttl_seconds,
    )
    return {
        **pending,
        "status": "PENDING_CONFIRMATION",
        "requiresExplicitConfirmation": True,
        "message": "仅已准备预约，尚未创建。请向用户展示完整信息并等待下一轮明确确认。",
    }


async def confirm_appointment(
    conversation_id: str | None,
    user_id: int | None,
    authorization: str | None,
    pending_at_start: bool,
) -> dict[str, Any]:
    conversation_id, user_id = _check_conversation_identity(
        conversation_id, user_id
    )
    if not pending_at_start:
        raise ValueError("不能在准备预约的同一轮直接创建，必须等待用户下一轮明确确认。")
    pending = await get_pending_appointment(conversation_id, user_id)
    if pending is None:
        raise ValueError("当前没有有效的待确认预约，请重新提供预约信息。")
    if not authorization:
        raise ValueError("登录状态已失效，无法创建预约。")

    appointment_time = datetime.fromisoformat(pending["appointmentTime"])
    request_key = pending.get("requestKey") or _build_request_key(
        user_id,
        conversation_id,
        pending["houseId"],
        pending["appointmentTime"],
        str(pending.get("remark") or "").strip(),
    )
    result = await post_backend_data(
        "/tenant/appointment/add",
        {
            "houseId": pending["houseId"],
            "appointmentTime": appointment_time.replace(tzinfo=None).isoformat(
                timespec="seconds"
            ),
            "remark": pending["remark"],
            "requestKey": request_key,
        },
        headers={"Authorization": authorization},
    )
    await delete_state(PENDING_NAMESPACE, conversation_id, user_id)
    return {
        "status": "CREATED",
        "message": result or "预约成功",
        "houseId": pending["houseId"],
        "houseTitle": pending["houseTitle"],
        "appointmentTime": appointment_time.isoformat(timespec="seconds"),
    }


async def cancel_appointment_preparation(
    conversation_id: str | None, user_id: int | None
) -> dict[str, Any]:
    _check_conversation_identity(conversation_id, user_id)
    removed = await delete_state(PENDING_NAMESPACE, conversation_id, user_id)
    return {
        "status": "CANCELLED" if removed else "NO_PENDING_APPOINTMENT",
        "message": "已取消待确认预约。" if removed else "当前没有待确认预约。",
    }


async def execute_appointment_tool(
    name: str,
    arguments: dict[str, Any],
    conversation_id: str | None,
    user_id: int | None,
    authorization: str | None,
    pending_at_start: bool,
) -> Any:
    if name == "prepare_appointment":
        return await prepare_appointment(arguments, conversation_id, user_id)
    if name == "confirm_appointment":
        return await confirm_appointment(
            conversation_id,
            user_id,
            authorization,
            pending_at_start,
        )
    if name == "cancel_appointment_preparation":
        return await cancel_appointment_preparation(conversation_id, user_id)
    raise ValueError(f"不支持的预约工具：{name}")
