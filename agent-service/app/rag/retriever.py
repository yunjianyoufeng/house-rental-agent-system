import logging
from contextlib import closing
from pathlib import Path
from typing import Any

from app.core.config import get_settings
from app.core.langsmith_observability import child_trace
from app.rag.embedding import EmbeddingServiceError, embed_texts
from app.rag.vector_store import (
    KnowledgeBaseNotReadyError,
    VectorStoreUnavailableError,
    chroma_vector_search,
    ensure_index_ready,
    lexical_search,
    lexical_tokens,
    open_vector_connection,
    validate_embedding_metadata,
    sqlite_vector_search,
)


logger = logging.getLogger(__name__)


def _fuse_results(
    query: str,
    vector_results: list[dict[str, Any]],
    lexical_results: list[dict[str, Any]],
    vector_weight: float,
    lexical_weight: float,
    rerank_weight: float,
) -> list[dict[str, Any]]:
    candidates: dict[int, dict[str, Any]] = {}
    for item in vector_results:
        candidates[item["id"]] = dict(item)
    for item in lexical_results:
        existing = candidates.setdefault(item["id"], dict(item))
        existing["lexicalScore"] = item["lexicalScore"]

    weight_total = vector_weight + lexical_weight
    if weight_total <= 0:
        vector_weight, lexical_weight, weight_total = 0.7, 0.3, 1.0
    vector_weight /= weight_total
    lexical_weight /= weight_total

    query_tokens = set(lexical_tokens(query))
    for item in candidates.values():
        document_tokens = set(
            lexical_tokens(
                f"{item['title']} {item['section']} {item['content']}"
            )
        )
        coverage = (
            len(query_tokens & document_tokens) / len(query_tokens)
            if query_tokens else 0.0
        )
        base_score = (
            vector_weight * float(item.get("vectorScore", 0.0))
            + lexical_weight * float(item.get("lexicalScore", 0.0))
        )
        item["relevance"] = (
            (1.0 - rerank_weight) * base_score
            + rerank_weight * coverage
        )
        item["retrievalMode"] = (
            "hybrid"
            if "vectorScore" in item and "lexicalScore" in item
            else "vector" if "vectorScore" in item else "lexical"
        )

    return sorted(
        candidates.values(),
        key=lambda item: (-item["relevance"], item["id"]),
    )


def _search_knowledge(query: str, top_k: int | None = None) -> list[dict[str, Any]]:
    settings = get_settings()
    database_path = Path(settings.rag_database_path)
    if not database_path.is_file():
        raise KnowledgeBaseNotReadyError(
            "知识库索引尚未生成，请先运行 python -m app.rag.ingest"
        )

    result_count = max(1, min(top_k or settings.rag_top_k, 5))
    candidate_k = max(result_count, settings.rag_candidate_k)
    # 显式释放连接，避免 Windows 下索引重建时文件被查询进程长期占用。
    with closing(open_vector_connection(database_path)) as connection:
        metadata = ensure_index_ready(connection)
        lexical_results = lexical_search(connection, query, candidate_k)
        vector_results: list[dict[str, Any]] = []
        try:
            embedding = embed_texts([query], settings)
            validate_embedding_metadata(metadata, embedding)
            vector_store = settings.rag_vector_store.strip().lower()
            if vector_store == "chroma":
                try:
                    vector_results = chroma_vector_search(
                        connection,
                        embedding.vectors[0],
                        candidate_k,
                        Path(settings.rag_chroma_path),
                        settings.rag_chroma_collection,
                    )
                except VectorStoreUnavailableError as exc:
                    # Chroma 不可用时复用入库时同步生成的 sqlite-vec 索引。
                    logger.warning("Chroma不可用，降级为sqlite-vec：%s", exc)
                    vector_results = sqlite_vector_search(
                        connection,
                        embedding.vectors[0],
                        candidate_k,
                    )
            elif vector_store == "sqlite-vec":
                vector_results = sqlite_vector_search(
                    connection,
                    embedding.vectors[0],
                    candidate_k,
                )
            else:
                raise KnowledgeBaseNotReadyError(
                    f"不支持的向量库：{settings.rag_vector_store}"
                )
        except EmbeddingServiceError as exc:
            logger.warning("Embedding服务不可用，降级为关键词检索：%s", exc)

    fused = _fuse_results(
        query,
        vector_results,
        lexical_results,
        settings.rag_vector_weight,
        settings.rag_lexical_weight,
        settings.rag_rerank_weight,
    )
    if not fused:
        raise KnowledgeBaseNotReadyError("知识库中没有匹配内容")
    return [
        {
            "source": item["source"],
            "title": item["title"],
            "section": item["section"],
            "content": item["content"],
            "relevance": round(max(0.0, min(1.0, item["relevance"])), 4),
            "retrievalMode": item["retrievalMode"],
        }
        for item in fused[:result_count]
    ]


def search_knowledge(query: str, top_k: int | None = None) -> list[dict[str, Any]]:
    """检索知识库，并仅向观测平台记录非敏感检索摘要。"""

    with child_trace(
        "rental-knowledge-retrieval",
        "retriever",
        inputs={"queryLength": len(query), "topK": top_k},
    ) as retrieval_trace:
        results = _search_knowledge(query, top_k)
        if retrieval_trace is not None:
            retrieval_trace.end(
                outputs={
                    "itemCount": len(results),
                    "sources": [item["source"] for item in results],
                    "retrievalModes": sorted(
                        {item["retrievalMode"] for item in results}
                    ),
                }
            )
        return results
