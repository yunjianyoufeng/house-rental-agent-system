import asyncio
from decimal import Decimal, InvalidOperation, ROUND_HALF_UP
from typing import Any

from app.services.backend_client import post_backend_data
from app.tools.house_tools import compact_house, get_house_detail


DECISION_TOOL_DEFINITIONS: list[dict[str, Any]] = [
    {
        "type": "function",
        "function": {
            "name": "recommend_houses",
            "description": "根据用户的综合租房需求，对平台中的真实房源进行评分和排序推荐。",
            "parameters": {
                "type": "object",
                "properties": {
                    "query": {"type": "string", "description": "用户的完整租房需求"},
                    "city": {"type": "string"},
                    "area": {"type": "string"},
                    "max_rent": {"type": "number", "minimum": 0},
                    "min_square": {"type": "number", "minimum": 0},
                    "house_type": {"type": "string"},
                    "model_type": {
                        "type": "string",
                        "enum": ["RULE", "TFIDF"],
                        "description": "默认使用稳定的规则推荐；只有明确需要文本相似度时才使用 TFIDF。",
                    },
                    "top_k": {"type": "integer", "minimum": 1, "maximum": 10},
                },
                "required": ["query"],
                "additionalProperties": False,
            },
        },
    },
    {
        "type": "function",
        "function": {
            "name": "compare_houses",
            "description": "根据 2 至 5 个房源ID查询真实详情，并比较租金、面积和押金等客观指标。",
            "parameters": {
                "type": "object",
                "properties": {
                    "house_ids": {
                        "type": "array",
                        "items": {"type": "integer", "minimum": 1},
                        "minItems": 2,
                        "maxItems": 5,
                        "uniqueItems": True,
                    }
                },
                "required": ["house_ids"],
                "additionalProperties": False,
            },
        },
    },
    {
        "type": "function",
        "function": {
            "name": "calculate_rental_budget",
            "description": "根据月收入、租金和其他固定开支，计算租金占比、每月余额和首月需准备金额。",
            "parameters": {
                "type": "object",
                "properties": {
                    "monthly_income": {"type": "number", "exclusiveMinimum": 0},
                    "monthly_rent": {"type": "number", "minimum": 0},
                    "deposit": {"type": "number", "minimum": 0},
                    "monthly_utilities": {"type": "number", "minimum": 0},
                    "monthly_transport": {"type": "number", "minimum": 0},
                    "other_monthly_expenses": {"type": "number", "minimum": 0},
                },
                "required": ["monthly_income", "monthly_rent"],
                "additionalProperties": False,
            },
        },
    },
]


def _optional_body(arguments: dict[str, Any]) -> dict[str, Any]:
    field_names = {
        "city": "city",
        "area": "area",
        "max_rent": "maxRent",
        "min_square": "minSquare",
        "house_type": "houseType",
    }
    return {
        backend_name: arguments[name]
        for name, backend_name in field_names.items()
        if arguments.get(name) is not None
    }


async def recommend_houses(
    arguments: dict[str, Any],
    authorization: str | None = None,
) -> list[dict[str, Any]]:
    query = str(arguments.get("query") or "").strip()
    if not query:
        raise ValueError("租房需求不能为空。")

    model_type = arguments.get("model_type", "RULE")
    if model_type not in {"RULE", "TFIDF"}:
        raise ValueError("当前只支持 RULE 或 TFIDF 推荐。")

    body = _optional_body(arguments)
    body.update(
        {
            "query": query,
            "useLlmParse": False,
            "modelType": model_type,
            "topK": arguments.get("top_k", 5),
        }
    )
    headers = {"Authorization": authorization} if authorization else None
    recommendations = await post_backend_data(
        "/recommend/house",
        body,
        headers=headers,
    )
    return [
        {
            "house": compact_house(item.get("house") or {}),
            "score": item.get("score"),
            "reason": item.get("reason"),
            "modelType": item.get("modelType"),
        }
        for item in (recommendations or [])
    ]


def _numeric_house(house: dict[str, Any], field: str) -> Decimal | None:
    value = house.get(field)
    if value is None:
        return None
    try:
        return Decimal(str(value))
    except (InvalidOperation, ValueError):
        return None


def _highlight(
    houses: list[dict[str, Any]], field: str, label: str, use_minimum: bool
) -> dict[str, Any] | None:
    values = [(house, _numeric_house(house, field)) for house in houses]
    valid_values = [(house, value) for house, value in values if value is not None]
    if not valid_values:
        return None
    selected_house, selected_value = (min if use_minimum else max)(
        valid_values, key=lambda item: item[1]
    )
    return {
        "houseId": selected_house.get("id"),
        "title": selected_house.get("title"),
        "value": float(selected_value),
        "label": label,
    }


async def compare_houses(arguments: dict[str, Any]) -> dict[str, Any]:
    house_ids = arguments.get("house_ids")
    if not isinstance(house_ids, list) or not 2 <= len(house_ids) <= 5:
        raise ValueError("请提供 2 至 5 个房源ID进行对比。")
    if len(set(house_ids)) != len(house_ids):
        raise ValueError("对比的房源ID不能重复。")

    houses = await asyncio.gather(
        *(get_house_detail({"house_id": house_id}) for house_id in house_ids)
    )
    highlights = {
        "lowestRent": _highlight(houses, "rentPrice", "月租金最低", True),
        "largestArea": _highlight(houses, "square", "面积最大", False),
        "lowestDeposit": _highlight(houses, "deposit", "押金最低", True),
    }
    return {
        "houses": houses,
        "highlights": {key: value for key, value in highlights.items() if value},
    }


def _money(arguments: dict[str, Any], name: str, required: bool = False) -> Decimal:
    value = arguments.get(name)
    if value is None:
        if required:
            raise ValueError(f"缺少必填参数：{name}。")
        return Decimal("0")
    try:
        amount = Decimal(str(value))
    except (InvalidOperation, ValueError) as exc:
        raise ValueError(f"参数 {name} 必须是数字。") from exc
    if amount < 0 or (name == "monthly_income" and amount == 0):
        raise ValueError(f"参数 {name} 的取值不合法。")
    return amount


def _decimal_number(value: Decimal) -> float:
    return float(value.quantize(Decimal("0.01"), rounding=ROUND_HALF_UP))


async def calculate_rental_budget(arguments: dict[str, Any]) -> dict[str, Any]:
    income = _money(arguments, "monthly_income", required=True)
    rent = _money(arguments, "monthly_rent", required=True)
    deposit = _money(arguments, "deposit")
    utilities = _money(arguments, "monthly_utilities")
    transport = _money(arguments, "monthly_transport")
    other = _money(arguments, "other_monthly_expenses")

    monthly_fixed_cost = rent + utilities + transport + other
    remaining = income - monthly_fixed_cost
    ratio = rent / income * Decimal("100")
    if ratio <= 30:
        pressure = "租金占比较稳妥"
    elif ratio <= 40:
        pressure = "租金占比偏高，需要控制其他开支"
    else:
        pressure = "租金占比较高，建议重新评估预算"

    return {
        "rentToIncomePercent": _decimal_number(ratio),
        "monthlyFixedCost": _decimal_number(monthly_fixed_cost),
        "monthlyRemaining": _decimal_number(remaining),
        "initialRentAndDeposit": _decimal_number(rent + deposit),
        "pressureAssessment": pressure,
        "calculationNote": "结果仅基于用户提供的金额，未包含未填写的生活开支。",
    }


async def execute_decision_tool(
    name: str,
    arguments: dict[str, Any],
    authorization: str | None = None,
) -> Any:
    if name == "recommend_houses":
        return await recommend_houses(arguments, authorization)
    if name == "compare_houses":
        return await compare_houses(arguments)
    if name == "calculate_rental_budget":
        return await calculate_rental_budget(arguments)
    raise ValueError(f"不支持的决策工具：{name}")
