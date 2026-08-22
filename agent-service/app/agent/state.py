from typing import Any, TypedDict


class PendingToolCall(TypedDict):
    id: str
    name: str
    arguments: str


class AgentState(TypedDict):
    """保存一次 Agent 执行过程中的消息和待调用工具。"""

    messages: list[dict[str, Any]]
    answer: str
    pending_tool_calls: list[PendingToolCall]
    tool_rounds: int
    conversation_id: str | None
    user_id: int | None
    authorization: str | None
    appointment_pending_at_start: bool
