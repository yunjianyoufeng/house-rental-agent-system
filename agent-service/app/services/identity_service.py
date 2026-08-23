from typing import Any

from app.services.backend_client import get_backend_data


async def resolve_authenticated_identity(
    authorization: str | None,
) -> tuple[int, str]:
    if not authorization:
        raise ValueError("登录状态已失效，请重新登录。")
    data: Any = await get_backend_data(
        "/auth/me",
        headers={"Authorization": authorization},
    )
    if not isinstance(data, dict):
        raise ValueError("业务服务未返回有效的登录身份。")

    user_id = data.get("id")
    role_code = data.get("roleCode")
    status = data.get("status")
    if not isinstance(user_id, int) or user_id <= 0:
        raise ValueError("业务服务未返回有效的用户标识。")
    if role_code not in {"TENANT", "LANDLORD", "ADMIN"}:
        raise ValueError("业务服务未返回有效的用户角色。")
    if status != 1:
        raise ValueError("当前账号已被禁用。")
    return user_id, role_code
