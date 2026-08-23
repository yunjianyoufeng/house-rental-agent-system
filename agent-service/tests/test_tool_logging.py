import json
import tempfile
import unittest
from pathlib import Path

from app.core.logging_config import (
    REDACTED_VALUE,
    configure_tool_logging,
    log_tool_call,
    sanitize_log_value,
    shutdown_tool_logging,
)
from app.core.observability import reset_trace_id, set_trace_id


class ToolLoggingTest(unittest.TestCase):
    def tearDown(self):
        shutdown_tool_logging()

    def test_sanitizes_sensitive_fields_recursively(self):
        sanitized = sanitize_log_value(
            {
                "authorization": "Bearer secret",
                "nested": {"api_key": "sk-test", "city": "济南"},
            }
        )

        self.assertEqual(REDACTED_VALUE, sanitized["authorization"])
        self.assertEqual(REDACTED_VALUE, sanitized["nested"]["api_key"])
        self.assertEqual("济南", sanitized["nested"]["city"])

    def test_writes_structured_tool_call_event(self):
        with tempfile.TemporaryDirectory() as temp_dir:
            log_path = Path(temp_dir) / "tool-calls.jsonl"
            configure_tool_logging(log_path, max_bytes=2048, backup_count=1)

            trace_token = set_trace_id("trace-tool-123456")
            try:
                log_tool_call(
                    conversation_id="conversation-1",
                    user_id=8,
                    tool_name="search_rental_knowledge",
                    arguments={"query": "报修条件"},
                    status="success",
                    duration_ms=12,
                    result={"results": [{"source": "repair-and-complaint.md"}]},
                )
            finally:
                reset_trace_id(trace_token)
            shutdown_tool_logging()

            event = json.loads(log_path.read_text(encoding="utf-8").strip())
            self.assertEqual("agent_tool_call", event["event"])
            self.assertEqual("search_rental_knowledge", event["tool"])
            self.assertEqual("trace-tool-123456", event["traceId"])
            self.assertEqual("success", event["status"])
            self.assertEqual(1, event["result"]["itemCount"])


if __name__ == "__main__":
    unittest.main()
