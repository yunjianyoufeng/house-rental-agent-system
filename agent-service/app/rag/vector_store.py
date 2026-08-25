import re
import sqlite3
from pathlib import Path
from typing import Any

import chromadb
import sqlite_vec

from app.rag.embedding import EmbeddingBatch


LEXICAL_SEGMENT_PATTERN = re.compile(r"[\u4e00-\u9fff]+|[a-z0-9]+")


class KnowledgeBaseNotReadyError(RuntimeError):
    """知识库尚未构建、结构不完整或Embedding配置不匹配。"""


class VectorStoreUnavailableError(RuntimeError):
    """配置的向量库暂时不可用，可由检索层执行降级。"""


def open_vector_connection(database_path: Path) -> sqlite3.Connection:
    connection = sqlite3.connect(database_path)
    connection.row_factory = sqlite3.Row
    connection.enable_load_extension(True)
    sqlite_vec.load(connection)
    connection.enable_load_extension(False)
    return connection


def rebuild_chroma_index(
    chroma_path: Path,
    collection_name: str,
    chunks: list[dict[str, str]],
    vectors: list[list[float]],
) -> None:
    """重建 Chroma 集合，ID 与 SQLite 分片主键保持一致。"""
    chroma_path.mkdir(parents=True, exist_ok=True)
    try:
        client = chromadb.PersistentClient(path=str(chroma_path))
        existing = {collection.name for collection in client.list_collections()}
        if collection_name in existing:
            client.delete_collection(collection_name)
        collection = client.create_collection(
            name=collection_name,
            metadata={"hnsw:space": "cosine"},
        )
        # Chroma 负责语义召回；完整正文仍以 SQLite 分片表为准，避免双份业务数据漂移。
        collection.add(
            ids=[str(index) for index in range(1, len(chunks) + 1)],
            embeddings=vectors,
            documents=[chunk["content"] for chunk in chunks],
            metadatas=[
                {
                    "source": chunk["source"],
                    "title": chunk["title"],
                    "section": chunk["section"],
                }
                for chunk in chunks
            ],
        )
    except Exception as exc:
        # 隔离 Chroma/Rust/SQLite 的实现异常，交给上层决定是否降级。
        raise VectorStoreUnavailableError(f"Chroma 索引构建失败：{exc}") from exc


def lexical_tokens(text: str) -> list[str]:
    tokens: list[str] = []
    for segment in LEXICAL_SEGMENT_PATTERN.findall((text or "").lower()):
        if re.fullmatch(r"[\u4e00-\u9fff]+", segment):
            tokens.extend(segment)
            tokens.extend(
                segment[index:index + 2]
                for index in range(max(0, len(segment) - 1))
            )
        else:
            tokens.append(segment)
    return list(dict.fromkeys(token for token in tokens if token))


def lexical_document(*parts: str) -> str:
    return " ".join(lexical_tokens(" ".join(parts)))


def lexical_query(text: str) -> str:
    tokens = lexical_tokens(text)[:32]
    return " OR ".join(f'"{token}"' for token in tokens)


def get_index_metadata(connection: sqlite3.Connection) -> dict[str, str]:
    try:
        rows = connection.execute(
            "SELECT key, value FROM knowledge_metadata"
        ).fetchall()
    except sqlite3.OperationalError as exc:
        raise KnowledgeBaseNotReadyError(
            "知识库缺少RAG 2.0元数据，请重新构建索引"
        ) from exc
    return {str(row["key"]): str(row["value"]) for row in rows}


def ensure_index_ready(connection: sqlite3.Connection) -> dict[str, str]:
    required_tables = {
        "knowledge_chunks",
        "knowledge_vectors",
        "knowledge_fts",
        "knowledge_metadata",
    }
    rows = connection.execute(
        "SELECT name FROM sqlite_master WHERE type IN ('table', 'view')"
    ).fetchall()
    existing = {str(row["name"]) for row in rows}
    missing = required_tables - existing
    if missing:
        raise KnowledgeBaseNotReadyError(
            "知识库索引结构不完整，请重新构建索引："
            + ", ".join(sorted(missing))
        )
    return get_index_metadata(connection)


def validate_embedding_metadata(
    metadata: dict[str, str],
    embedding: EmbeddingBatch,
) -> None:
    expected_dimensions = int(metadata.get("dimensions") or 0)
    expected_model = metadata.get("model") or ""
    expected_provider = metadata.get("provider") or ""
    if expected_dimensions != embedding.dimensions:
        raise KnowledgeBaseNotReadyError("查询向量维度与知识库索引不一致，请重新构建索引")
    if expected_model != embedding.model or expected_provider != embedding.provider:
        raise KnowledgeBaseNotReadyError(
            "Embedding模型或提供方式已变化，请重新构建知识库索引"
        )


def sqlite_vector_search(
    connection: sqlite3.Connection,
    query_vector: list[float],
    candidate_k: int,
) -> list[dict[str, Any]]:
    rows = connection.execute(
        """
        SELECT chunks.id, chunks.source, chunks.title, chunks.section,
               chunks.content, matches.distance
        FROM (
            SELECT rowid, distance
            FROM knowledge_vectors
            WHERE embedding MATCH ? AND k = ?
            ORDER BY distance
        ) AS matches
        JOIN knowledge_chunks AS chunks ON chunks.id = matches.rowid
        ORDER BY matches.distance
        """,
        (sqlite_vec.serialize_float32(query_vector), candidate_k),
    ).fetchall()
    return [
        {
            "id": int(row["id"]),
            "source": row["source"],
            "title": row["title"],
            "section": row["section"],
            "content": row["content"],
            "vectorScore": max(0.0, 1.0 - float(row["distance"]) / 2.0),
        }
        for row in rows
    ]


def chroma_vector_search(
    connection: sqlite3.Connection,
    query_vector: list[float],
    candidate_k: int,
    chroma_path: Path,
    collection_name: str,
) -> list[dict[str, Any]]:
    """从 Chroma 召回分片 ID，再从 SQLite 读取权威分片内容。"""
    try:
        client = chromadb.PersistentClient(path=str(chroma_path))
        collection = client.get_collection(collection_name)
        result_count = min(candidate_k, collection.count())
        if result_count <= 0:
            return []
        query_result = collection.query(
            query_embeddings=[query_vector],
            n_results=result_count,
            include=["distances"],
        )
    except Exception as exc:
        # Chroma 版本内部也可能抛出 AttributeError 等异常，统一转换后才能可靠降级。
        raise VectorStoreUnavailableError(f"Chroma 查询失败：{exc}") from exc

    raw_ids = (query_result.get("ids") or [[]])[0]
    raw_distances = (query_result.get("distances") or [[]])[0]
    try:
        chunk_ids = [int(item) for item in raw_ids]
    except (TypeError, ValueError) as exc:
        raise VectorStoreUnavailableError("Chroma 返回了无效分片 ID") from exc
    if not chunk_ids:
        return []

    placeholders = ",".join("?" for _ in chunk_ids)
    rows = connection.execute(
        f"""
        SELECT id, source, title, section, content
        FROM knowledge_chunks
        WHERE id IN ({placeholders})
        """,
        chunk_ids,
    ).fetchall()
    chunks_by_id = {int(row["id"]): row for row in rows}

    ordered_results: list[dict[str, Any]] = []
    for chunk_id, distance in zip(chunk_ids, raw_distances, strict=False):
        row = chunks_by_id.get(chunk_id)
        if row is None:
            continue
        ordered_results.append(
            {
                "id": chunk_id,
                "source": row["source"],
                "title": row["title"],
                "section": row["section"],
                "content": row["content"],
                # cosine distance 越小越相关，转换为统一的 0~1 相似度。
                "vectorScore": max(0.0, min(1.0, 1.0 - float(distance))),
            }
        )
    return ordered_results


def lexical_search(
    connection: sqlite3.Connection,
    query: str,
    candidate_k: int,
) -> list[dict[str, Any]]:
    match_query = lexical_query(query)
    if not match_query:
        return []
    rows = connection.execute(
        """
        SELECT chunks.id, chunks.source, chunks.title, chunks.section,
               chunks.content, bm25(knowledge_fts) AS lexical_rank
        FROM knowledge_fts
        JOIN knowledge_chunks AS chunks ON chunks.id = knowledge_fts.rowid
        WHERE knowledge_fts MATCH ?
        ORDER BY lexical_rank
        LIMIT ?
        """,
        (match_query, candidate_k),
    ).fetchall()
    return [
        {
            "id": int(row["id"]),
            "source": row["source"],
            "title": row["title"],
            "section": row["section"],
            "content": row["content"],
            "lexicalScore": 1.0 / rank,
        }
        for rank, row in enumerate(rows, start=1)
    ]
