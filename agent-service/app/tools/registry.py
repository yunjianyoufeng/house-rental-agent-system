from dataclasses import dataclass
from typing import Any

from app.tools.appointment_tools import (
    APPOINTMENT_TOOL_DEFINITIONS,
    execute_appointment_tool,
)
from app.tools.decision_tools import DECISION_TOOL_DEFINITIONS, execute_decision_tool
from app.tools.house_tools import HOUSE_TOOL_DEFINITIONS, execute_house_tool
from app.tools.knowledge_tools import (
    KNOWLEDGE_TOOL_DEFINITIONS,
    execute_knowledge_tool,
)
from app.tools.personal_tools import PERSONAL_TOOL_DEFINITIONS, execute_personal_tool
from app.tools.preference_tools import (
    PREFERENCE_TOOL_DEFINITIONS,
    execute_preference_tool,
)


@dataclass(frozen=True)
class ToolSpec:
    definition: dict[str, Any]
    category: str
    tenant_only: bool = False
    requires_auth: bool = False
    read_only: bool = True

    @property
    def name(self) -> str:
        return str(self.definition["function"]["name"])


@dataclass(frozen=True)
class ToolExecutionContext:
    conversation_id: str | None = None
    user_id: int | None = None
    role_code: str | None = None
    authorization: str | None = None
    appointment_pending_at_start: bool = False
    appointment_confirmation_version: str | None = None
    appointment_explicit_confirmation: bool = False


def _build_specs(
    definitions: list[dict[str, Any]],
    category: str,
    *,
    tenant_only: bool = False,
    requires_auth: bool = False,
    read_only: bool = True,
) -> tuple[ToolSpec, ...]:
    return tuple(
        ToolSpec(
            definition=definition,
            category=category,
            tenant_only=tenant_only,
            requires_auth=requires_auth,
            read_only=read_only,
        )
        for definition in definitions
    )


TOOL_SPECS = (
    *_build_specs(HOUSE_TOOL_DEFINITIONS, "house"),
    *_build_specs(DECISION_TOOL_DEFINITIONS, "decision"),
    *_build_specs(KNOWLEDGE_TOOL_DEFINITIONS, "knowledge"),
    *_build_specs(
        APPOINTMENT_TOOL_DEFINITIONS,
        "appointment",
        tenant_only=True,
        requires_auth=True,
        read_only=False,
    ),
    *_build_specs(
        PREFERENCE_TOOL_DEFINITIONS,
        "preference",
        tenant_only=True,
        requires_auth=True,
        read_only=False,
    ),
    *_build_specs(
        PERSONAL_TOOL_DEFINITIONS,
        "personal",
        tenant_only=True,
        requires_auth=True,
    ),
)
TOOL_REGISTRY = {spec.name: spec for spec in TOOL_SPECS}

if len(TOOL_REGISTRY) != len(TOOL_SPECS):
    raise RuntimeError("工具名称必须保持唯一。")


def get_tool_spec(name: str) -> ToolSpec | None:
    return TOOL_REGISTRY.get(name)


def select_tool_specs(names: tuple[str, ...]) -> list[ToolSpec]:
    specs = []
    for name in names:
        spec = get_tool_spec(name)
        if spec is None:
            raise ValueError(f"工具未注册：{name}")
        specs.append(spec)
    return specs


def available_tool_specs(role_code: str | None) -> list[ToolSpec]:
    return [
        spec
        for spec in TOOL_SPECS
        if not spec.tenant_only or role_code == "TENANT"
    ]


def available_tool_definitions(role_code: str | None) -> list[dict[str, Any]]:
    return [spec.definition for spec in available_tool_specs(role_code)]


def validate_tool_access(
    tool_name: str,
    role_code: str | None,
    authorization: str | None,
) -> ToolSpec:
    spec = get_tool_spec(tool_name)
    if spec is None or (spec.tenant_only and role_code != "TENANT"):
        raise ValueError("当前登录角色无权调用该工具。")
    if spec.requires_auth and not authorization:
        raise ValueError("登录状态已失效，无法执行租客专属操作。")
    return spec


async def execute_registered_tool(
    name: str,
    arguments: dict[str, Any],
    context: ToolExecutionContext,
) -> Any:
    spec = validate_tool_access(
        name,
        context.role_code,
        context.authorization,
    )

    if spec.category == "house":
        return await execute_house_tool(name, arguments)
    if spec.category == "appointment":
        return await execute_appointment_tool(
            name,
            arguments,
            context.conversation_id,
            context.user_id,
            context.authorization,
            context.appointment_pending_at_start,
            context.appointment_confirmation_version,
            context.appointment_explicit_confirmation,
        )
    if spec.category == "preference":
        return await execute_preference_tool(
            name,
            arguments,
            context.authorization,
        )
    if spec.category == "knowledge":
        return await execute_knowledge_tool(name, arguments)
    if spec.category == "personal":
        return await execute_personal_tool(
            name,
            context.authorization,
            context.role_code,
        )
    if spec.category == "decision":
        return await execute_decision_tool(
            name,
            arguments,
            context.authorization,
        )
    raise ValueError(f"不支持的工具类别：{spec.category}")
