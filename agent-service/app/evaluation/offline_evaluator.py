import argparse
import json
from pathlib import Path
from typing import Any

from app.agent.graph import (
    available_tool_definitions,
    should_prefetch_rental_knowledge,
)
from app.services.llm_service import initial_messages
from app.services.safety_service import UnsafeInputError, validate_user_message


DEFAULT_DATASET_PATH = (
    Path(__file__).resolve().parents[2] / "evals" / "offline_cases.json"
)


def _tool_names(role_code: str | None) -> set[str]:
    return {
        item["function"]["name"]
        for item in available_tool_definitions(role_code)
    }


def evaluate_case(case: dict[str, Any]) -> dict[str, Any]:
    case_id = str(case.get("id") or "unknown")
    case_type = case.get("type")
    passed = False
    detail = ""

    try:
        if case_type == "tool_access":
            names = _tool_names(case.get("roleCode"))
            missing = [name for name in case.get("required", []) if name not in names]
            exposed = [name for name in case.get("forbidden", []) if name in names]
            passed = not missing and not exposed
            detail = f"missing={missing}, exposed={exposed}"
        elif case_type == "sensitive_input":
            blocked = False
            try:
                validate_user_message(str(case.get("message") or ""))
            except UnsafeInputError:
                blocked = True
            expected = bool(case.get("expectedBlocked"))
            passed = blocked == expected
            detail = f"blocked={blocked}, expected={expected}"
        elif case_type == "knowledge_routing":
            routed = should_prefetch_rental_knowledge(
                [{"role": "user", "content": str(case.get("message") or "")}]
            )
            expected = bool(case.get("expected"))
            passed = routed == expected
            detail = f"routed={routed}, expected={expected}"
        elif case_type == "trusted_role_context":
            messages = initial_messages(
                str(case.get("message") or ""),
                role_code=case.get("roleCode"),
            )
            context = messages[1]["content"]
            expected_text = str(case.get("expectedText") or "")
            passed = bool(expected_text) and expected_text in context
            detail = f"expectedText={expected_text}"
        else:
            detail = f"不支持的评测类型：{case_type}"
    except Exception as exc:
        detail = f"评测执行异常：{type(exc).__name__}: {exc}"

    return {
        "id": case_id,
        "type": case_type,
        "passed": passed,
        "detail": detail,
    }


def evaluate_cases(cases: list[dict[str, Any]]) -> dict[str, Any]:
    results = [evaluate_case(case) for case in cases]
    passed = sum(1 for result in results if result["passed"])
    total = len(results)
    return {
        "total": total,
        "passed": passed,
        "failed": total - passed,
        "score": round(passed / total, 4) if total else 0.0,
        "cases": results,
    }


def load_cases(dataset_path: Path = DEFAULT_DATASET_PATH) -> list[dict[str, Any]]:
    payload = json.loads(dataset_path.read_text(encoding="utf-8"))
    if not isinstance(payload, list) or not payload:
        raise ValueError("离线评测数据集必须是非空 JSON 数组。")
    if not all(isinstance(case, dict) for case in payload):
        raise ValueError("离线评测数据集中的每一项都必须是对象。")
    return payload


def run_evaluation(dataset_path: Path = DEFAULT_DATASET_PATH) -> dict[str, Any]:
    return evaluate_cases(load_cases(dataset_path))


def main() -> int:
    parser = argparse.ArgumentParser(description="运行 Agent 离线安全与路由评测")
    parser.add_argument("--dataset", type=Path, default=DEFAULT_DATASET_PATH)
    args = parser.parse_args()
    report = run_evaluation(args.dataset)
    print(json.dumps(report, ensure_ascii=False, indent=2))
    return 0 if report["failed"] == 0 else 1


if __name__ == "__main__":
    raise SystemExit(main())
