import sqlite3
import tempfile
import unittest
from contextlib import closing
from pathlib import Path
from types import SimpleNamespace
from unittest.mock import patch

from app.evaluation.rag_evaluator import evaluate_cases
from app.rag.embedding import EmbeddingBatch
from app.rag.retriever import search_knowledge
from app.rag.vector_store import (
    VectorStoreUnavailableError,
    chroma_vector_search,
    lexical_tokens,
    open_vector_connection,
    rebuild_chroma_index,
)


class RagRetrievalTest(unittest.TestCase):
    def test_chroma_returns_matching_sqlite_chunk(self):
        chunks = [
            {
                "source": "appointment.md",
                "title": "看房预约",
                "section": "二次确认",
                "content": "租客明确确认后才会创建预约。",
            },
            {
                "source": "repair.md",
                "title": "报修流程",
                "section": "提交报修",
                "content": "履约中的租客可以提交报修。",
            },
        ]
        vectors = [[1.0, 0.0, 0.0], [0.0, 1.0, 0.0]]

        # Windows 下 Chroma 可能延迟释放文件句柄，测试清理时允许忽略该问题。
        with tempfile.TemporaryDirectory(ignore_cleanup_errors=True) as temp_dir:
            root = Path(temp_dir)
            database_path = root / "knowledge.db"
            with closing(open_vector_connection(database_path)) as connection:
                connection.execute(
                    """
                    CREATE TABLE knowledge_chunks (
                        id INTEGER PRIMARY KEY,
                        source TEXT NOT NULL,
                        title TEXT NOT NULL,
                        section TEXT NOT NULL,
                        content TEXT NOT NULL
                    )
                    """
                )
                for chunk_id, chunk in enumerate(chunks, start=1):
                    connection.execute(
                        """
                        INSERT INTO knowledge_chunks
                            (id, source, title, section, content)
                        VALUES (?, ?, ?, ?, ?)
                        """,
                        (
                            chunk_id,
                            chunk["source"],
                            chunk["title"],
                            chunk["section"],
                            chunk["content"],
                        ),
                    )
                connection.commit()

                rebuild_chroma_index(
                    root / "chroma",
                    "test_rental_knowledge",
                    chunks,
                    vectors,
                )
                results = chroma_vector_search(
                    connection,
                    [1.0, 0.0, 0.0],
                    2,
                    root / "chroma",
                    "test_rental_knowledge",
                )

        self.assertEqual("appointment.md", results[0]["source"])
        self.assertGreater(results[0]["vectorScore"], results[1]["vectorScore"])

    def test_chinese_lexical_tokens_include_characters_and_bigrams(self):
        tokens = lexical_tokens("如何预约看房？")

        self.assertIn("预", tokens)
        self.assertIn("预约", tokens)

    def test_rag_evaluator_calculates_hit_rate_and_mrr(self):
        cases = [
            {"id": "hit-first", "query": "预约", "expectedSource": "a.md"},
            {"id": "miss", "query": "报修", "expectedSource": "b.md"},
        ]

        def fake_search(query: str, top_k: int):
            del query, top_k
            return [{"source": "a.md", "section": "流程"}]

        report = evaluate_cases(cases, search_fn=fake_search, top_k=3)

        self.assertEqual(0.5, report["hitAtK"])
        self.assertEqual(0.5, report["mrr"])

    def test_retriever_falls_back_to_sqlite_vec_when_chroma_fails(self):
        with tempfile.TemporaryDirectory(ignore_cleanup_errors=True) as temp_dir:
            database_path = Path(temp_dir) / "knowledge.db"
            database_path.touch()
            settings = SimpleNamespace(
                rag_database_path=str(database_path),
                rag_top_k=3,
                rag_candidate_k=10,
                rag_vector_store="chroma",
                rag_chroma_path=str(Path(temp_dir) / "chroma"),
                rag_chroma_collection="rental_knowledge",
                rag_vector_weight=0.7,
                rag_lexical_weight=0.3,
                rag_rerank_weight=0.15,
            )
            embedding = EmbeddingBatch(
                vectors=[[1.0, 0.0]],
                dimensions=2,
                model="test-model",
                provider="http",
            )
            fallback_result = {
                "id": 1,
                "source": "appointment.md",
                "title": "看房预约",
                "section": "二次确认",
                "content": "明确确认后创建预约。",
                "vectorScore": 1.0,
            }

            with (
                patch("app.rag.retriever.get_settings", return_value=settings),
                patch(
                    "app.rag.retriever.open_vector_connection",
                    return_value=sqlite3.connect(":memory:"),
                ),
                patch("app.rag.retriever.ensure_index_ready", return_value={}),
                patch("app.rag.retriever.lexical_search", return_value=[]),
                patch("app.rag.retriever.embed_texts", return_value=embedding),
                patch("app.rag.retriever.validate_embedding_metadata"),
                patch(
                    "app.rag.retriever.chroma_vector_search",
                    side_effect=VectorStoreUnavailableError("test failure"),
                ),
                patch(
                    "app.rag.retriever.sqlite_vector_search",
                    return_value=[fallback_result],
                ) as sqlite_search,
            ):
                results = search_knowledge("如何预约", top_k=1)

        sqlite_search.assert_called_once()
        self.assertEqual("appointment.md", results[0]["source"])
        self.assertEqual("vector", results[0]["retrievalMode"])


if __name__ == "__main__":
    unittest.main()
