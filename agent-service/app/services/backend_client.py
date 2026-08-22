from typing import Any

import httpx

from app.core.config import get_settings


class BackendServiceError(RuntimeError):
    """Spring Boot 业务接口调用失败。"""


def _extract_data(response: httpx.Response) -> Any:
    try:
        payload = response.json()
    except ValueError as exc:
        raise BackendServiceError("业务服务返回了无法解析的数据。") from exc

    if not isinstance(payload, dict) or payload.get("code") != 200:
        message = payload.get("message") if isinstance(payload, dict) else None
        raise BackendServiceError(message or "业务接口返回失败。")
    return payload.get("data")


async def get_backend_data(
    path: str,
    params: dict[str, Any] | None = None,
    headers: dict[str, str] | None = None,
) -> Any:
    settings = get_settings()
    try:
        async with httpx.AsyncClient(
            base_url=settings.spring_backend_url,
            timeout=10.0,
        ) as client:
            response = await client.get(path, params=params, headers=headers)
            response.raise_for_status()
    except httpx.HTTPError as exc:
        raise BackendServiceError("房屋租赁业务服务当前不可用。") from exc

    return _extract_data(response)


async def post_backend_data(
    path: str,
    body: dict[str, Any],
    headers: dict[str, str] | None = None,
) -> Any:
    settings = get_settings()
    try:
        async with httpx.AsyncClient(
            base_url=settings.spring_backend_url,
            timeout=15.0,
        ) as client:
            response = await client.post(path, json=body, headers=headers)
            response.raise_for_status()
    except httpx.HTTPError as exc:
        raise BackendServiceError("房屋租赁业务服务当前不可用。") from exc

    return _extract_data(response)


async def put_backend_data(
    path: str,
    body: dict[str, Any],
    headers: dict[str, str] | None = None,
) -> Any:
    settings = get_settings()
    try:
        async with httpx.AsyncClient(
            base_url=settings.spring_backend_url,
            timeout=15.0,
        ) as client:
            response = await client.put(path, json=body, headers=headers)
            response.raise_for_status()
    except httpx.HTTPError as exc:
        raise BackendServiceError("房屋租赁业务服务当前不可用。") from exc

    return _extract_data(response)


async def delete_backend_data(
    path: str,
    headers: dict[str, str] | None = None,
) -> Any:
    settings = get_settings()
    try:
        async with httpx.AsyncClient(
            base_url=settings.spring_backend_url,
            timeout=15.0,
        ) as client:
            response = await client.delete(path, headers=headers)
            response.raise_for_status()
    except httpx.HTTPError as exc:
        raise BackendServiceError("房屋租赁业务服务当前不可用。") from exc

    return _extract_data(response)
