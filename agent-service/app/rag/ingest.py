import os
import re
import tempfile
from contextlib import closing
from pathlib import Path

import sqlite_vec

from app.core.config import get_settings
from app.rag.embedding import embed_texts
from app.rag.vector_store import (
    lexical_document,
    open_vector_connection,
    rebuild_chroma_index,
)


HEADING_PATTERN = re.compile(r"^(#{1,3})\s+(.+)$", re.MULTILINE)


def split_markdown(source: Path) -> list[dict[str, str]]:
    text = source.read_text(encoding="utf-8").strip()
    matches = list(HEADING_PATTERN.finditer(text))
    if not matches:
        return [{"title": source.stem, "section": "正文", "content": text}]

    title = source.stem
    chunks: list[dict[str, str]] = []
    for index, match in enumerate(matches):
        heading_level = len(match.group(1))
        heading = match.group(2).strip()
        if heading_level == 1:
            title = heading
        content_start = match.end()
        content_end = (
            matches[index + 1].start()
            if index + 1 < len(matches)
            else len(text)
        )
        body = text[content_start:content_end].strip()
        if body:
            chunks.append(
                {
                    "title": title,
                    "section": heading if heading_level > 1 else "概述",
                    "content": f"{heading}\n{body}",
                }
            )
    return chunks


def build_index() -> int:
    settings = get_settings()
    vector_store = settings.rag_vector_store.strip().lower()
    if vector_store not in {"chroma", "sqlite-vec"}:
        raise ValueError(f"不支持的向量库：{settings.rag_vector_store}")

    knowledge_dir = Path(settings.rag_knowledge_dir)
    documents = sorted(knowledge_dir.glob("*.md"))
    if not documents:
        raise FileNotFoundError(f"知识文档目录为空：{knowledge_dir}")

    chunks: list[dict[str, str]] = []
    for document in documents:
        for chunk in split_markdown(document):
            chunks.append({"source": document.name, **chunk})
    searchable_texts = [
        f"{chunk['title']} {chunk['section']} {chunk['content']}"
        for chunk in chunks
    ]
    embedding_batch = embed_texts(searchable_texts, settings)

    database_path = Path(settings.rag_database_path)
    database_path.parent.mkdir(parents=True, exist_ok=True)
    # 在同目录构建临时索引，全部写入成功后再替换，避免中断时破坏可用索引。
    temporary_file = tempfile.NamedTemporaryFile(
        prefix="knowledge-build-",
        suffix=".db",
        dir=database_path.parent,
        delete=False,
    )
    temporary_path = Path(temporary_file.name)
    temporary_file.close()
    try:
        # sqlite3.Connection 的上下文管理器不会自动 close，Windows 替换文件前必须显式关闭。
        with closing(open_vector_connection(temporary_path)) as connection:
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
            connection.execute(
                f"CREATE VIRTUAL TABLE knowledge_vectors "
                f"USING vec0(embedding float[{embedding_batch.dimensions}])"
            )
            connection.execute(
                "CREATE VIRTUAL TABLE knowledge_fts USING fts5(tokens)"
            )
            connection.execute(
                """
                CREATE TABLE knowledge_metadata (
                    key TEXT PRIMARY KEY,
                    value TEXT NOT NULL
                )
                """
            )

            for chunk_id, (chunk, vector) in enumerate(
                zip(chunks, embedding_batch.vectors, strict=True),
                start=1,
            ):
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
                connection.execute(
                    "INSERT INTO knowledge_vectors(rowid, embedding) VALUES (?, ?)",
                    (chunk_id, sqlite_vec.serialize_float32(vector)),
                )
                connection.execute(
                    "INSERT INTO knowledge_fts(rowid, tokens) VALUES (?, ?)",
                    (
                        chunk_id,
                        lexical_document(
                            chunk["title"],
                            chunk["section"],
                            chunk["content"],
                        ),
                    ),
                )

            metadata = {
                "provider": embedding_batch.provider,
                "model": embedding_batch.model,
                "dimensions": str(embedding_batch.dimensions),
                "chunk_count": str(len(chunks)),
                # 两份索引使用同一批向量，Chroma 故障时可直接回退 sqlite-vec。
                "vector_store": (
                    "chroma+sqlite-vec"
                    if vector_store == "chroma"
                    else "sqlite-vec"
                ),
                "retrieval": "hybrid-vector-bm25-rerank",
            }
            connection.executemany(
                "INSERT INTO knowledge_metadata(key, value) VALUES (?, ?)",
                metadata.items(),
            )
            connection.commit()
        os.replace(temporary_path, database_path)
    finally:
        # 替换成功后临时路径已不存在；失败时只清理由本次构建创建的文件。
        temporary_path.unlink(missing_ok=True)

    if vector_store == "chroma":
        rebuild_chroma_index(
            Path(settings.rag_chroma_path),
            settings.rag_chroma_collection,
            chunks,
            embedding_batch.vectors,
        )
    return len(chunks)


if __name__ == "__main__":
    count = build_index()
    print(f"RAG 2.0知识库索引构建完成，共写入{count}个知识片段。")
