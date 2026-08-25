import unittest
from types import SimpleNamespace
from unittest.mock import patch

from pydantic import SecretStr

from app.core.langsmith_observability import (
    REDACTED_VALUE,
    agent_trace_context,
    get_langsmith_client,
    sanitize_trace_value,
    summarize_trace_result,
)


def _settings(*, tracing: bool, api_key: str | None = None):
    return SimpleNamespace(
        langsmith_tracing=tracing,
        langsmith_api_key=SecretStr(api_key) if api_key else None,
        langsmith_endpoint="https://api.smith.langchain.com",
        langsmith_project="test-project",
        langsmith_hide_inputs=True,
        langsmith_hide_outputs=True,
    )


class LangSmithObservabilityTest(unittest.TestCase):
    def setUp(self):
        get_langsmith_client.cache_clear()

    def tearDown(self):
        get_langsmith_client.cache_clear()

    def test_sanitizes_credentials_and_personal_information(self):
        value = {
            "authorization": "Bearer abc.def.ghi",
            "nested": {
                "api_key": "sk-test-secret-value",
                "message": "联系 13812345678 或 user@example.com，身份证 11010119900101123X",
            },
        }

        sanitized = sanitize_trace_value(value)

        self.assertEqual(REDACTED_VALUE, sanitized["authorization"])
        self.assertEqual(REDACTED_VALUE, sanitized["nested"]["api_key"])
        self.assertNotIn("13812345678", sanitized["nested"]["message"])
        self.assertNotIn("user@example.com", sanitized["nested"]["message"])
        self.assertNotIn("11010119900101123X", sanitized["nested"]["message"])

    def test_disabled_tracing_does_not_create_client_or_trace(self):
        with (
            patch(
                "app.core.langsmith_observability.get_settings",
                return_value=_settings(tracing=False),
            ),
            patch("app.core.langsmith_observability.Client") as client_class,
        ):
            self.assertIsNone(get_langsmith_client())
            with agent_trace_context(
                trace_id="trace-1",
                conversation_id="conversation-1",
                role_code="TENANT",
                streaming=False,
            ) as run_tree:
                self.assertIsNone(run_tree)

        client_class.assert_not_called()

    def test_enabled_client_uses_privacy_defaults(self):
        settings = _settings(tracing=True, api_key="lsv2_test-secret-value")
        with (
            patch(
                "app.core.langsmith_observability.get_settings",
                return_value=settings,
            ),
            patch("app.core.langsmith_observability.Client") as client_class,
        ):
            client = get_langsmith_client()

        self.assertIs(client_class.return_value, client)
        client_class.assert_called_once_with(
            api_url=settings.langsmith_endpoint,
            api_key="lsv2_test-secret-value",
            anonymizer=sanitize_trace_value,
            hide_inputs=True,
            hide_outputs=True,
        )

    def test_tool_result_summary_does_not_include_values(self):
        summary = summarize_trace_result(
            {"houses": [{"address": "敏感地址"}], "token": "secret"}
        )

        self.assertEqual("object", summary["type"])
        self.assertEqual(1, summary["itemCount"])
        self.assertEqual(["houses", "token"], summary["keys"])
        self.assertNotIn("敏感地址", str(summary))
        self.assertNotIn("secret", str(summary))


if __name__ == "__main__":
    unittest.main()
