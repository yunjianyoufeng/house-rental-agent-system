import asyncio
from typing import Any

from app.rag.retriever import search_knowledge


KNOWLEDGE_TOOL_DEFINITIONS = [
    {
        "type": "function",
        "function": {
            "name": "search_rental_knowledge",
            "description": "检索本平台的预约、租房申请、合同订单、报修、投诉和使用流程知识。",
            "parameters": {
                "type": "object",
                "properties": {
                    "query": {
                        "type": "string",
                        "description": "用户想了解的平台规则或办理流程",
                    },
                    "top_k": {
                        "type": "integer",
                        "description": "返回知识片段数量，范围 1 到 5",
                        "minimum": 1,
                        "maximum": 5,
                    },
                },
                "required": ["query"],
            },
        },
    }
]


async def execute_knowledge_tool(name: str, arguments: dict[str, Any]) -> dict[str, Any]:
    if name != "search_rental_knowledge":
        raise ValueError(f"不支持的知识库工具：{name}")
    query = str(arguments.get("query") or "").strip()
    if not query:
        raise ValueError("知识库检索问题不能为空")
    top_k = arguments.get("top_k")
    results = await asyncio.to_thread(search_knowledge, query, top_k)
    return {"query": query, "results": results}
