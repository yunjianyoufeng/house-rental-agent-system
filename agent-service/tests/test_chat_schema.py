import unittest

from pydantic import ValidationError

from app.schemas.chat import ChatRequest, ChatResponse


class ChatSchemaTest(unittest.TestCase):
    def test_request_accepts_camel_case_fields_and_trims_message(self):
        request = ChatRequest.model_validate(
            {
                "message": "  查询报修规则  ",
                "conversationId": "conversation-1",
                "userId": 8,
            }
        )

        self.assertEqual("查询报修规则", request.message)
        self.assertEqual("conversation-1", request.conversation_id)
        self.assertEqual(8, request.user_id)

    def test_request_rejects_blank_message(self):
        with self.assertRaises(ValidationError):
            ChatRequest(message="   ")

    def test_response_serializes_rag_sources(self):
        response = ChatResponse(
            answer="可以在合同生效期间提交报修。",
            conversation_id="conversation-1",
            sources=[
                {
                    "source": "repair-and-complaint.md",
                    "title": "报修与投诉规则",
                    "section": "报修提交条件",
                    "relevance": 0.82,
                }
            ],
        )

        payload = response.model_dump(by_alias=True)
        self.assertEqual("conversation-1", payload["conversationId"])
        self.assertEqual("repair-and-complaint.md", payload["sources"][0]["source"])
        self.assertEqual("报修提交条件", payload["sources"][0]["section"])


if __name__ == "__main__":
    unittest.main()
