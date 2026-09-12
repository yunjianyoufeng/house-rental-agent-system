from typing import Any, TypedDict


class PendingToolCall(TypedDict):
    id: str
    name: str
    arguments: str


class KnowledgeSource(TypedDict):
    source: str
    title: str
    section: str
    relevance: float


class AgentState(TypedDict):
    """保存一次 Agent 执行过程中的消息和待调用工具。"""

    messages: list[dict[str, Any]]
    answer: str
    pending_tool_calls: list[PendingToolCall]
    tool_rounds: int
    conversation_id: str | None
    user_id: int | None
    role_code: str | None
    authorization: str | None
    appointment_pending_at_start: bool
    appointment_confirmation_version: str | None
    appointment_explicit_confirmation: bool
    knowledge_sources: list[KnowledgeSource]
