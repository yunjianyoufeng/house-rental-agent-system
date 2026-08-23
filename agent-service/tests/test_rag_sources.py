import unittest

from app.agent.graph import merge_knowledge_sources


class RagSourceCollectionTest(unittest.TestCase):
    def test_merges_and_deduplicates_sources(self):
        sources = merge_knowledge_sources(
            [],
            [
                {
                    "source": "repair-and-complaint.md",
                    "title": "报修与投诉规则",
                    "section": "报修提交条件",
                    "relevance": 0.82,
                },
                {
                    "source": "repair-and-complaint.md",
                    "title": "报修与投诉规则",
                    "section": "报修提交条件",
                    "relevance": 0.75,
                },
            ],
        )

        self.assertEqual(1, len(sources))
        self.assertEqual("报修与投诉规则", sources[0]["title"])

    def test_ignores_incomplete_source_and_limits_relevance(self):
        sources = merge_knowledge_sources(
            [],
            [
                {"source": "", "title": "无效来源", "section": "章节"},
                {
                    "source": "lease-process.md",
                    "title": "租赁流程",
                    "section": "申请审核",
                    "relevance": 2.5,
                },
            ],
        )

        self.assertEqual(1, len(sources))
        self.assertEqual(1.0, sources[0]["relevance"])


if __name__ == "__main__":
    unittest.main()
