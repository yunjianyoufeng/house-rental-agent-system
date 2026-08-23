import asyncio
import json
from collections.abc import AsyncIterator
from time import perf_counter
from typing import Any, Literal

from langgraph.graph import END, START, StateGraph

from app.agent.state import AgentState, KnowledgeSource, PendingToolCall
from app.core.logging_config import log_tool_call
from app.services.backend_client import BackendServiceError
from app.services.llm_service import (
    DeepSeekConfigurationError,
    create_completion,
    create_completion_stream,
    initial_messages,
)
from app.services.memory_service import load_conversation_messages
from app.rag.vector_store import KnowledgeBaseNotReadyError
from app.tools.appointment_tools import (
    APPOINTMENT_TOOL_DEFINITIONS,
    execute_appointment_tool,
    get_pending_appointment,
)
from app.tools.decision_tools import DECISION_TOOL_DEFINITIONS, execute_decision_tool
from app.tools.house_tools import HOUSE_TOOL_DEFINITIONS, execute_house_tool
from app.tools.knowledge_tools import (
    KNOWLEDGE_TOOL_DEFINITIONS,
    execute_knowledge_tool,
)
from app.tools.preference_tools import (
    PREFERENCE_TOOL_DEFINITIONS,
    execute_preference_tool,
    load_rental_preference,
)
from app.tools.personal_tools import (
    PERSONAL_TOOL_DEFINITIONS,
    execute_personal_tool,
)


MAX_TOOL_ROUNDS = 3
BASE_TOOL_DEFINITIONS = (
    HOUSE_TOOL_DEFINITIONS
    + DECISION_TOOL_DEFINITIONS
    + APPOINTMENT_TOOL_DEFINITIONS
    + PREFERENCE_TOOL_DEFINITIONS
    + KNOWLEDGE_TOOL_DEFINITIONS
)
PERSONAL_TOOL_NAMES = {
    tool["function"]["name"] for tool in PERSONAL_TOOL_DEFINITIONS
}
HOUSE_TOOL_NAMES = {
    tool["function"]["name"] for tool in HOUSE_TOOL_DEFINITIONS
}
APPOINTMENT_TOOL_NAMES = {
    tool["function"]["name"] for tool in APPOINTMENT_TOOL_DEFINITIONS
}
PREFERENCE_TOOL_NAMES = {
    tool["function"]["name"] for tool in PREFERENCE_TOOL_DEFINITIONS
}
KNOWLEDGE_TOOL_NAMES = {
    tool["function"]["name"] for tool in KNOWLEDGE_TOOL_DEFINITIONS
}
KNOWLEDGE_DOMAIN_KEYWORDS = (
    "看房预约",
    "租房申请",
    "申请审核",
    "合同",
    "订单",
    "报修",
    "投诉",
    "平台规则",
)
KNOWLEDGE_QUESTION_KEYWORDS = (
    "如何",
    "怎么",
    "什么",
    "条件",
    "流程",
    "规则",
    "通过后",
    "之后",
    "注意",
    "说明",
    "办理",
    "操作",
)


def available_tool_definitions(role_code: str | None) -> list[dict[str, Any]]:
    tools = list(BASE_TOOL_DEFINITIONS)
    if role_code == "TENANT":
        tools.extend(PERSONAL_TOOL_DEFINITIONS)
    return tools


async def agent_node(state: AgentState) -> dict:
    tools = (
        available_tool_definitions(state["role_code"])
        if state["tool_rounds"] < MAX_TOOL_ROUNDS
        else None
    )
    response = await create_completion(state["messages"], tools=tools)
    message = response.choices[0].message

    assistant_message: dict = {
        "role": "assistant",
        "content": message.content or "",
    }
    pending_tool_calls: list[PendingToolCall] = []
    if message.tool_calls:
        assistant_message["tool_calls"] = []
        for tool_call in message.tool_calls:
            call = {
                "id": tool_call.id,
                "type": "function",
                "function": {
                    "name": tool_call.function.name,
                    "arguments": tool_call.function.arguments,
                },
            }
            assistant_message["tool_calls"].append(call)
            pending_tool_calls.append(
                {
                    "id": tool_call.id,
                    "name": tool_call.function.name,
                    "arguments": tool_call.function.arguments,
                }
            )

    return {
        "messages": [*state["messages"], assistant_message],
        "answer": message.content or "",
        "pending_tool_calls": pending_tool_calls,
    }


async def tool_node(state: AgentState) -> dict:
    tool_messages = []
    knowledge_sources = list(state["knowledge_sources"])
    for tool_call in state["pending_tool_calls"]:
        started_at = perf_counter()
        arguments = {}
        result = None
        status = "error"
        error_message = None
        try:
            arguments = json.loads(tool_call["arguments"] or "{}")
            if tool_call["name"] in HOUSE_TOOL_NAMES:
                result = await execute_house_tool(tool_call["name"], arguments)
            elif tool_call["name"] in APPOINTMENT_TOOL_NAMES:
                result = await execute_appointment_tool(
                    tool_call["name"],
                    arguments,
                    state["conversation_id"],
                    state["user_id"],
                    state["authorization"],
                    state["appointment_pending_at_start"],
                )
            elif tool_call["name"] in PREFERENCE_TOOL_NAMES:
                result = await execute_preference_tool(
                    tool_call["name"],
                    arguments,
                    state["authorization"],
                )
            elif tool_call["name"] in KNOWLEDGE_TOOL_NAMES:
                result = await execute_knowledge_tool(tool_call["name"], arguments)
                knowledge_sources = merge_knowledge_sources(
                    knowledge_sources,
                    result.get("results", []),
                )
            elif tool_call["name"] in PERSONAL_TOOL_NAMES:
                result = await execute_personal_tool(
                    tool_call["name"],
                    state["authorization"],
                    state["role_code"],
                )
            else:
                result = await execute_decision_tool(tool_call["name"], arguments)
            content = json.dumps(result, ensure_ascii=False)
            status = "success"
        except (
            json.JSONDecodeError,
            KeyError,
            ValueError,
            BackendServiceError,
            KnowledgeBaseNotReadyError,
        ) as exc:
            error_message = str(exc)
            content = json.dumps({"error": str(exc)}, ensure_ascii=False)
        except Exception as exc:
            error_message = str(exc)
            raise
        finally:
            log_tool_call(
                conversation_id=state["conversation_id"],
                user_id=state["user_id"],
                tool_name=tool_call["name"],
                arguments=arguments,
                status=status,
                duration_ms=round((perf_counter() - started_at) * 1000),
                result=result,
                error=error_message,
            )

        tool_messages.append(
            {
                "role": "tool",
                "tool_call_id": tool_call["id"],
                "content": content,
            }
        )

    return {
        "messages": [*state["messages"], *tool_messages],
        "pending_tool_calls": [],
        "tool_rounds": state["tool_rounds"] + 1,
        "knowledge_sources": knowledge_sources,
    }


def merge_knowledge_sources(
    existing: list[KnowledgeSource],
    results: list[dict],
) -> list[KnowledgeSource]:
    merged = list(existing)
    seen = {(item["source"], item["section"]) for item in merged}

    for item in results:
        source = str(item.get("source") or "").strip()
        title = str(item.get("title") or "").strip()
        section = str(item.get("section") or "").strip()
        key = (source, section)
        if not source or not title or key in seen:
            continue

        try:
            relevance = float(item.get("relevance", 0))
        except (TypeError, ValueError):
            relevance = 0.0

        merged.append(
            {
                "source": source,
                "title": title,
                "section": section,
                "relevance": max(0.0, min(1.0, relevance)),
            }
        )
        seen.add(key)

    return merged


async def stream_agent_events(state: AgentState) -> AsyncIterator[dict[str, Any]]:
    """执行 Agent 循环，并将模型文本片段和最终来源逐步交给接口层。"""

    current_state = dict(state)
    if should_prefetch_rental_knowledge(current_state["messages"]):
        latest_question = next(
            str(message.get("content") or "")
            for message in reversed(current_state["messages"])
            if message.get("role") == "user"
        )
        forced_tool_call: PendingToolCall = {
            "id": "forced-knowledge-search",
            "name": "search_rental_knowledge",
            "arguments": json.dumps(
                {"query": latest_question},
                ensure_ascii=False,
            ),
        }
        current_state["messages"] = [
            *current_state["messages"],
            {
                "role": "assistant",
                "content": "",
                "tool_calls": [
                    {
                        "id": forced_tool_call["id"],
                        "type": "function",
                        "function": {
                            "name": forced_tool_call["name"],
                            "arguments": forced_tool_call["arguments"],
                        },
                    }
                ],
            },
        ]
        current_state["pending_tool_calls"] = [forced_tool_call]
        yield {
            "event": "status",
            "phase": "tool",
            "tools": [forced_tool_call["name"]],
        }
        tool_update = await tool_node(current_state)
        current_state.update(tool_update)

    while True:
        tools = (
            available_tool_definitions(current_state["role_code"])
            if current_state["tool_rounds"] < MAX_TOOL_ROUNDS
            else None
        )
        stream = await create_completion_stream(current_state["messages"], tools=tools)
        content_parts: list[str] = []
        streamed_tool_calls: dict[int, dict[str, str]] = {}

        async for chunk in stream:
            if not chunk.choices:
                continue
            delta = chunk.choices[0].delta
            if delta.content:
                content_parts.append(delta.content)
                yield {"event": "delta", "content": delta.content}

            for tool_call in delta.tool_calls or []:
                call = streamed_tool_calls.setdefault(
                    tool_call.index,
                    {"id": "", "name": "", "arguments": ""},
                )
                if tool_call.id:
                    call["id"] = tool_call.id
                if tool_call.function:
                    if tool_call.function.name:
                        call["name"] += tool_call.function.name
                    if tool_call.function.arguments:
                        call["arguments"] += tool_call.function.arguments

        answer = "".join(content_parts)
        pending_tool_calls: list[PendingToolCall] = [
            {
                "id": call["id"],
                "name": call["name"],
                "arguments": call["arguments"],
            }
            for _, call in sorted(streamed_tool_calls.items())
            if call["id"] and call["name"]
        ]
        assistant_message: dict[str, Any] = {
            "role": "assistant",
            "content": answer,
        }
        if pending_tool_calls:
            assistant_message["tool_calls"] = [
                {
                    "id": call["id"],
                    "type": "function",
                    "function": {
                        "name": call["name"],
                        "arguments": call["arguments"],
                    },
                }
                for call in pending_tool_calls
            ]

        current_state["messages"] = [
            *current_state["messages"],
            assistant_message,
        ]
        current_state["answer"] = answer
        current_state["pending_tool_calls"] = pending_tool_calls

        if not pending_tool_calls:
            if not answer:
                raise DeepSeekConfigurationError("模型返回了空内容，请稍后重试。")
            yield {
                "event": "done",
                "answer": answer,
                "sources": current_state["knowledge_sources"],
            }
            return

        yield {
            "event": "status",
            "phase": "tool",
            "tools": [call["name"] for call in pending_tool_calls],
        }
        tool_update = await tool_node(current_state)
        current_state.update(tool_update)


def should_prefetch_rental_knowledge(messages: list[dict[str, Any]]) -> bool:
    latest_question = next(
        (
            str(message.get("content") or "")
            for message in reversed(messages)
            if message.get("role") == "user"
        ),
        "",
    )
    return any(
        keyword in latest_question for keyword in KNOWLEDGE_DOMAIN_KEYWORDS
    ) and any(keyword in latest_question for keyword in KNOWLEDGE_QUESTION_KEYWORDS)


def route_after_agent(state: AgentState) -> Literal["tools", "end"]:
    return "tools" if state["pending_tool_calls"] else "end"


def build_agent_graph():
    builder = StateGraph(AgentState)
    builder.add_node("agent", agent_node)
    builder.add_node("tools", tool_node)
    builder.add_edge(START, "agent")
    builder.add_conditional_edges(
        "agent",
        route_after_agent,
        {
            "tools": "tools",
            "end": END,
        },
    )
    builder.add_edge("tools", "agent")
    return builder.compile()


agent_graph = build_agent_graph()


async def initial_agent_state(
    message: str,
    conversation_id: str | None = None,
    user_id: int | None = None,
    role_code: str | None = None,
    authorization: str | None = None,
) -> AgentState:
    pending_appointment, conversation_messages, rental_preference = await asyncio.gather(
        get_pending_appointment(conversation_id, user_id),
        load_conversation_messages(conversation_id, user_id, authorization),
        load_rental_preference(authorization),
    )
    return {
        "messages": initial_messages(
            message,
            pending_appointment,
            conversation_messages,
            rental_preference,
            role_code,
        ),
        "answer": "",
        "pending_tool_calls": [],
        "tool_rounds": 0,
        "conversation_id": conversation_id,
        "user_id": user_id,
        "role_code": role_code,
        "authorization": authorization,
        "appointment_pending_at_start": pending_appointment is not None,
        "knowledge_sources": [],
    }
