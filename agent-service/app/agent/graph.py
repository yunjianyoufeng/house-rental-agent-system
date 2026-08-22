import asyncio
import json
from typing import Literal

from langgraph.graph import END, START, StateGraph

from app.agent.state import AgentState, PendingToolCall
from app.services.backend_client import BackendServiceError
from app.services.llm_service import create_completion, initial_messages
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


MAX_TOOL_ROUNDS = 3
ALL_TOOL_DEFINITIONS = (
    HOUSE_TOOL_DEFINITIONS
    + DECISION_TOOL_DEFINITIONS
    + APPOINTMENT_TOOL_DEFINITIONS
    + PREFERENCE_TOOL_DEFINITIONS
    + KNOWLEDGE_TOOL_DEFINITIONS
)
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


async def agent_node(state: AgentState) -> dict:
    tools = ALL_TOOL_DEFINITIONS if state["tool_rounds"] < MAX_TOOL_ROUNDS else None
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
    for tool_call in state["pending_tool_calls"]:
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
            else:
                result = await execute_decision_tool(tool_call["name"], arguments)
            content = json.dumps(result, ensure_ascii=False)
        except (
            json.JSONDecodeError,
            KeyError,
            ValueError,
            BackendServiceError,
            KnowledgeBaseNotReadyError,
        ) as exc:
            content = json.dumps({"error": str(exc)}, ensure_ascii=False)

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
    }


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
    authorization: str | None = None,
) -> AgentState:
    pending_appointment, conversation_messages, rental_preference = await asyncio.gather(
        get_pending_appointment(conversation_id, user_id),
        load_conversation_messages(conversation_id, user_id),
        load_rental_preference(authorization),
    )
    return {
        "messages": initial_messages(
            message,
            pending_appointment,
            conversation_messages,
            rental_preference,
        ),
        "answer": "",
        "pending_tool_calls": [],
        "tool_rounds": 0,
        "conversation_id": conversation_id,
        "user_id": user_id,
        "authorization": authorization,
        "appointment_pending_at_start": pending_appointment is not None,
    }
