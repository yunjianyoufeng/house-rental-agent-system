import argparse
import json
import os
from pathlib import Path
from typing import Any
from uuid import uuid4

import httpx


DEFAULT_DATASET_PATH = (
    Path(__file__).resolve().parents[2] / "evals" / "live_cases.json"
)


def score_live_payload(
    case: dict[str, Any],
    status_code: int,
    payload: dict[str, Any],
) -> dict[str, Any]:
    answer = str(payload.get("answer") or "").strip()
    sources = payload.get("sources") if isinstance(payload.get("sources"), list) else []
    keywords = [str(item) for item in case.get("anyKeywords", [])]
    forbidden = [str(item) for item in case.get("forbiddenPhrases", [])]
    checks = {
        "httpSuccess": status_code == 200,
        "answerPresent": bool(answer),
        "keywordMatched": not keywords or any(item in answer for item in keywords),
        "forbiddenAbsent": not any(item in answer for item in forbidden),
        "sourcesPresent": not case.get("requireSources") or bool(sources),
    }
    return {
        "id": str(case.get("id") or "unknown"),
        "passed": all(checks.values()),
        "checks": checks,
    }


def run_live_evaluation(
    base_url: str,
    token: str,
    dataset_path: Path = DEFAULT_DATASET_PATH,
) -> dict[str, Any]:
    cases = json.loads(dataset_path.read_text(encoding="utf-8"))
    authorization = token if token.lower().startswith("bearer ") else f"Bearer {token}"
    results = []
    with httpx.Client(base_url=base_url, timeout=90.0) as client:
        for case in cases:
            try:
                response = client.post(
                    "/api/agent/chat",
                    headers={"Authorization": authorization},
                    json={
                        "message": case["message"],
                        "conversationId": f"eval-{uuid4()}",
                    },
                )
                try:
                    payload = response.json()
                except ValueError:
                    payload = {}
                result = score_live_payload(case, response.status_code, payload)
            except httpx.HTTPError as exc:
                result = {
                    "id": str(case.get("id") or "unknown"),
                    "passed": False,
                    "checks": {"request": False},
                    "error": str(exc),
                }
            results.append(result)

    passed = sum(1 for result in results if result["passed"])
    total = len(results)
    return {
        "total": total,
        "passed": passed,
        "failed": total - passed,
        "score": round(passed / total, 4) if total else 0.0,
        "cases": results,
    }


def main() -> int:
    parser = argparse.ArgumentParser(description="运行 Agent 在线问答质量评测")
    parser.add_argument(
        "--base-url",
        default=os.getenv("AGENT_EVAL_BASE_URL", "http://127.0.0.1:8001"),
    )
    parser.add_argument("--dataset", type=Path, default=DEFAULT_DATASET_PATH)
    args = parser.parse_args()
    token = os.getenv("AGENT_EVAL_TOKEN", "").strip()
    if not token:
        print("缺少 AGENT_EVAL_TOKEN，未运行会产生模型调用的在线评测。")
        return 2

    report = run_live_evaluation(args.base_url, token, args.dataset)
    print(json.dumps(report, ensure_ascii=False, indent=2))
    return 0 if report["failed"] == 0 else 1


if __name__ == "__main__":
    raise SystemExit(main())
