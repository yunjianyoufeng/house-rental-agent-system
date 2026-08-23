import unittest

from app.evaluation.live_evaluator import score_live_payload
from app.evaluation.offline_evaluator import evaluate_cases, run_evaluation


class AgentEvaluationTest(unittest.TestCase):
    def test_offline_dataset_passes(self):
        report = run_evaluation()

        self.assertEqual(0, report["failed"])
        self.assertEqual(1.0, report["score"])

    def test_offline_evaluator_reports_failed_case(self):
        report = evaluate_cases(
            [
                {
                    "id": "intentional-failure",
                    "type": "tool_access",
                    "roleCode": "LANDLORD",
                    "required": ["get_my_contracts"],
                    "forbidden": [],
                }
            ]
        )

        self.assertEqual(1, report["failed"])
        self.assertEqual(0.0, report["score"])

    def test_live_payload_requires_answer_keyword_and_sources(self):
        case = {
            "id": "rag-case",
            "anyKeywords": ["合同", "订单"],
            "forbiddenPhrases": ["编造"],
            "requireSources": True,
        }
        result = score_live_payload(
            case,
            200,
            {
                "answer": "审核通过后将处理合同。",
                "sources": [{"source": "knowledge.md"}],
            },
        )

        self.assertTrue(result["passed"])


if __name__ == "__main__":
    unittest.main()
