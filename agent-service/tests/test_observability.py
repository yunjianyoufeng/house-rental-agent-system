import unittest

from app.core.observability import (
    metrics_snapshot,
    record_model_call,
    record_request,
    record_tool_call,
    reset_metrics,
)
from app.services.llm_service import estimate_cost_cny, summarize_usage


class ObservabilityTest(unittest.TestCase):
    def setUp(self):
        reset_metrics()

    def tearDown(self):
        reset_metrics()

    def test_aggregates_request_model_and_tool_metrics(self):
        record_request("success", 120)
        record_request("error", 80)
        record_model_call(
            "success",
            200,
            1,
            {
                "promptTokens": 100,
                "promptCacheHitTokens": 40,
                "promptCacheMissTokens": 60,
                "completionTokens": 20,
                "totalTokens": 120,
            },
            0.0001,
        )
        record_tool_call("success", 10)

        snapshot = metrics_snapshot()

        self.assertEqual(2, snapshot["requests"]["total"])
        self.assertEqual(100, snapshot["requests"]["averageDurationMs"])
        self.assertEqual(1, snapshot["models"]["retries"])
        self.assertEqual(120, snapshot["models"]["totalTokens"])
        self.assertEqual(1, snapshot["tools"]["calls"])

    def test_summarizes_usage_and_estimates_cost(self):
        usage = summarize_usage(
            {
                "prompt_tokens": 1000,
                "prompt_cache_hit_tokens": 400,
                "prompt_cache_miss_tokens": 600,
                "completion_tokens": 200,
                "total_tokens": 1200,
            }
        )

        cost = estimate_cost_cny(usage)

        self.assertEqual(400, usage["promptCacheHitTokens"])
        self.assertEqual(600, usage["promptCacheMissTokens"])
        self.assertAlmostEqual(0.001008, cost)


if __name__ == "__main__":
    unittest.main()
