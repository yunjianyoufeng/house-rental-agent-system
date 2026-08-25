import json
from pathlib import Path
from typing import Any, Callable

from app.rag.retriever import search_knowledge


PROJECT_ROOT = Path(__file__).resolve().parents[2]
DEFAULT_CASES_PATH = PROJECT_ROOT / "evals" / "rag_cases.json"
SearchFunction = Callable[[str, int], list[dict[str, Any]]]


def evaluate_cases(
    cases: list[dict[str, Any]],
    search_fn: SearchFunction = search_knowledge,
    top_k: int = 3,
) -> dict[str, Any]:
    """计算检索 Hit@K 与 MRR，避免只凭主观问答判断模型效果。"""
    details: list[dict[str, Any]] = []
    reciprocal_rank_total = 0.0
    hit_count = 0

    for case in cases:
        results = search_fn(str(case["query"]), top_k)
        expected_source = str(case["expectedSource"])
        expected_sections = set(case.get("expectedSections") or [])
        matched_rank = 0
        for rank, result in enumerate(results, start=1):
            source_matches = result.get("source") == expected_source
            section_matches = (
                not expected_sections or result.get("section") in expected_sections
            )
            if source_matches and section_matches:
                matched_rank = rank
                break

        if matched_rank:
            hit_count += 1
            reciprocal_rank_total += 1.0 / matched_rank
        details.append(
            {
                "id": case["id"],
                "passed": bool(matched_rank),
                "rank": matched_rank or None,
                "returnedSources": [result.get("source") for result in results],
            }
        )

    total = len(cases)
    return {
        "total": total,
        "passed": hit_count,
        "failed": total - hit_count,
        "topK": top_k,
        "hitAtK": round(hit_count / total, 4) if total else 0.0,
        "mrr": round(reciprocal_rank_total / total, 4) if total else 0.0,
        "details": details,
    }


def run_evaluation(
    cases_path: Path = DEFAULT_CASES_PATH,
    top_k: int = 3,
) -> dict[str, Any]:
    cases = json.loads(cases_path.read_text(encoding="utf-8"))
    return evaluate_cases(cases, top_k=top_k)


if __name__ == "__main__":
    report = run_evaluation()
    print(json.dumps(report, ensure_ascii=False, indent=2))
    raise SystemExit(0 if report["hitAtK"] >= 0.8 else 1)
