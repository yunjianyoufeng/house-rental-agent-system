import sqlite3
from pathlib import Path
from typing import Any

import sqlite_vec

from app.core.config import get_settings
from app.rag.embedding import embed_text


class KnowledgeBaseNotReadyError(RuntimeError):
    """知识库尚未构建或内容为空。"""


def open_vector_connection(database_path: Path) -> sqlite3.Connection:
    connection = sqlite3.connect(database_path)
    connection.row_factory = sqlite3.Row
    connection.enable_load_extension(True)
    sqlite_vec.load(connection)
    connection.enable_load_extension(False)
    return connection


def search_knowledge(query: str, top_k: int | None = None) -> list[dict[str, Any]]:
    settings = get_settings()
    database_path = Path(settings.rag_database_path)
    if not database_path.is_file():
        raise KnowledgeBaseNotReadyError(
            "知识库索引尚未生成，请先运行 python -m app.rag.ingest"
        )

    result_count = max(1, min(top_k or settings.rag_top_k, 5))
    query_vector = embed_text(query, settings.rag_embedding_dimensions)
    with open_vector_connection(database_path) as connection:
        table_exists = connection.execute(
            "SELECT 1 FROM sqlite_master WHERE name = 'knowledge_vectors'"
        ).fetchone()
        if table_exists is None:
            raise KnowledgeBaseNotReadyError("知识库索引结构不存在，请重新构建索引")

        rows = connection.execute(
            """
            SELECT chunks.source, chunks.title, chunks.section, chunks.content,
                   matches.distance
            FROM (
                SELECT rowid, distance
                FROM knowledge_vectors
                WHERE embedding MATCH ? AND k = ?
                ORDER BY distance
            ) AS matches
            JOIN knowledge_chunks AS chunks ON chunks.id = matches.rowid
            ORDER BY matches.distance
            """,
            (sqlite_vec.serialize_float32(query_vector), result_count),
        ).fetchall()

    if not rows:
        raise KnowledgeBaseNotReadyError("知识库中没有可检索内容，请重新构建索引")
    return [
        {
            "source": row["source"],
            "title": row["title"],
            "section": row["section"],
            "content": row["content"],
            "relevance": round(max(0.0, 1.0 - float(row["distance"]) / 2.0), 4),
        }
        for row in rows
    ]
