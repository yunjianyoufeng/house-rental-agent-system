import re
import sqlite3
from pathlib import Path

import sqlite_vec

from app.core.config import get_settings
from app.rag.embedding import embed_text
from app.rag.vector_store import open_vector_connection


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
        content_end = matches[index + 1].start() if index + 1 < len(matches) else len(text)
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
    knowledge_dir = Path(settings.rag_knowledge_dir)
    documents = sorted(knowledge_dir.glob("*.md"))
    if not documents:
        raise FileNotFoundError(f"知识文档目录为空：{knowledge_dir}")

    database_path = Path(settings.rag_database_path)
    database_path.parent.mkdir(parents=True, exist_ok=True)
    with open_vector_connection(database_path) as connection:
        connection.execute(
            """
            CREATE TABLE IF NOT EXISTS knowledge_chunks (
                id INTEGER PRIMARY KEY,
                source TEXT NOT NULL,
                title TEXT NOT NULL,
                section TEXT NOT NULL,
                content TEXT NOT NULL
            )
            """
        )
        connection.execute(
            f"CREATE VIRTUAL TABLE IF NOT EXISTS knowledge_vectors "
            f"USING vec0(embedding float[{settings.rag_embedding_dimensions}])"
        )
        connection.execute("DELETE FROM knowledge_vectors")
        connection.execute("DELETE FROM knowledge_chunks")

        chunk_id = 0
        for document in documents:
            for chunk in split_markdown(document):
                chunk_id += 1
                searchable_text = (
                    f"{chunk['title']} {chunk['section']} {chunk['content']}"
                )
                vector = embed_text(searchable_text, settings.rag_embedding_dimensions)
                connection.execute(
                    """
                    INSERT INTO knowledge_chunks(id, source, title, section, content)
                    VALUES (?, ?, ?, ?, ?)
                    """,
                    (
                        chunk_id,
                        document.name,
                        chunk["title"],
                        chunk["section"],
                        chunk["content"],
                    ),
                )
                connection.execute(
                    "INSERT INTO knowledge_vectors(rowid, embedding) VALUES (?, ?)",
                    (chunk_id, sqlite_vec.serialize_float32(vector)),
                )
        connection.commit()
    return chunk_id


if __name__ == "__main__":
    count = build_index()
    print(f"知识库索引构建完成，共写入 {count} 个知识片段。")
