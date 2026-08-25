import hashlib
import math
import re
import time
from dataclasses import dataclass

import httpx

from app.core.config import Settings, get_settings


TOKEN_PATTERN = re.compile(r"[\u4e00-\u9fff]|[a-z0-9]+")


class EmbeddingServiceError(RuntimeError):
    """Embedding服务不可用或返回了无效向量。"""


class EmbeddingConfigurationError(EmbeddingServiceError):
    """Embedding配置缺失，调用方不得回显密钥等敏感信息。"""


@dataclass(frozen=True)
class EmbeddingBatch:
    vectors: list[list[float]]
    dimensions: int
    model: str
    provider: str


def _stable_bucket(token: str, dimensions: int) -> tuple[int, float]:
    digest = hashlib.blake2b(token.encode("utf-8"), digest_size=8).digest()
    raw = int.from_bytes(digest, byteorder="little", signed=False)
    index = raw % dimensions
    sign = 1.0 if (raw >> 63) == 0 else -1.0
    return index, sign


def embed_text(text: str, dimensions: int = 384) -> list[float]:
    """使用稳定字符特征生成兼容后备向量。"""
    if dimensions <= 0:
        raise ValueError("向量维度必须大于0")

    normalized = "".join(TOKEN_PATTERN.findall((text or "").lower()))
    if not normalized:
        return [0.0] * dimensions

    features: list[str] = []
    for size in (1, 2, 3):
        features.extend(
            normalized[index:index + size]
            for index in range(max(0, len(normalized) - size + 1))
        )

    vector = [0.0] * dimensions
    for feature in features:
        index, sign = _stable_bucket(feature, dimensions)
        vector[index] += sign

    norm = math.sqrt(sum(value * value for value in vector))
    if norm == 0:
        return vector
    return [value / norm for value in vector]


def _normalize_vector(vector: list[float]) -> list[float]:
    norm = math.sqrt(sum(value * value for value in vector))
    if norm == 0:
        return vector
    return [value / norm for value in vector]


def _validate_vectors(
    raw_vectors: object,
    expected_count: int,
    dimensions: int,
) -> list[list[float]]:
    if not isinstance(raw_vectors, list) or len(raw_vectors) != expected_count:
        raise EmbeddingServiceError("Embedding服务返回的向量数量不正确")

    vectors: list[list[float]] = []
    for raw_vector in raw_vectors:
        if not isinstance(raw_vector, list) or len(raw_vector) != dimensions:
            raise EmbeddingServiceError("Embedding服务返回了长度不一致的向量")
        try:
            vector = [float(value) for value in raw_vector]
        except (TypeError, ValueError) as exc:
            raise EmbeddingServiceError("Embedding向量包含非数值内容") from exc
        vectors.append(_normalize_vector(vector))
    return vectors


def _openai_compatible_embeddings(
    texts: list[str],
    settings: Settings,
) -> EmbeddingBatch:
    if settings.rag_embedding_api_key is None:
        raise EmbeddingConfigurationError(
            "缺少RAG_EMBEDDING_API_KEY，无法调用云端Embedding服务"
        )

    api_key = settings.rag_embedding_api_key.get_secret_value().strip()
    if not api_key:
        raise EmbeddingConfigurationError(
            "RAG_EMBEDDING_API_KEY不能为空"
        )
    base_url = settings.rag_embedding_base_url.rstrip("/")
    if not base_url.startswith(("https://", "http://")):
        raise EmbeddingConfigurationError("RAG_EMBEDDING_BASE_URL格式无效")

    endpoint = f"{base_url}/embeddings"
    request_body = {
        "model": settings.rag_embedding_model,
        "input": texts,
        "dimensions": settings.rag_embedding_dimensions,
        "encoding_format": "float",
    }
    response: httpx.Response | None = None
    last_error: Exception | None = None
    for attempt in range(settings.rag_embedding_max_retries + 1):
        try:
            response = httpx.post(
                endpoint,
                headers={"Authorization": f"Bearer {api_key}"},
                json=request_body,
                timeout=settings.rag_embedding_timeout_seconds,
            )
            response.raise_for_status()
            break
        except httpx.HTTPStatusError as exc:
            last_error = exc
            status_code = exc.response.status_code
            retryable = status_code in {408, 409, 429} or status_code >= 500
            if not retryable or attempt >= settings.rag_embedding_max_retries:
                raise EmbeddingServiceError(
                    f"云端Embedding调用失败：HTTP {status_code}"
                ) from exc
        except httpx.RequestError as exc:
            last_error = exc
            if attempt >= settings.rag_embedding_max_retries:
                raise EmbeddingServiceError(
                    f"云端Embedding调用失败：{type(exc).__name__}"
                ) from exc

        # 只对限流、服务端错误和网络异常退避，避免无效Key重复消耗请求。
        delay = settings.rag_embedding_retry_base_seconds * (2 ** attempt)
        if delay > 0:
            time.sleep(delay)

    if response is None or response.is_error:
        raise EmbeddingServiceError(
            f"云端Embedding调用失败：{type(last_error).__name__}"
        )

    try:
        payload = response.json()
    except ValueError as exc:
        raise EmbeddingServiceError("云端Embedding返回了无效JSON") from exc
    items = payload.get("data") if isinstance(payload, dict) else None
    if not isinstance(items, list) or len(items) != len(texts):
        raise EmbeddingServiceError("云端Embedding返回的向量数量不正确")

    # OpenAI兼容接口允许响应顺序按index标识，先排序再恢复输入顺序。
    try:
        indexes = [int(item["index"]) for item in items]
        if sorted(indexes) != list(range(len(texts))):
            raise ValueError("index不连续")
        ordered_items = sorted(items, key=lambda item: int(item["index"]))
        raw_vectors = [item["embedding"] for item in ordered_items]
    except (KeyError, TypeError, ValueError) as exc:
        raise EmbeddingServiceError("云端Embedding响应结构不正确") from exc
    vectors = _validate_vectors(
        raw_vectors,
        expected_count=len(texts),
        dimensions=settings.rag_embedding_dimensions,
    )
    return EmbeddingBatch(
        vectors=vectors,
        dimensions=settings.rag_embedding_dimensions,
        model=settings.rag_embedding_model,
        provider="openai_compatible",
    )


def _http_embeddings(texts: list[str], settings: Settings) -> EmbeddingBatch:
    try:
        response = httpx.post(
            settings.rag_embedding_url,
            json={"texts": texts, "normalize": True},
            timeout=settings.rag_embedding_timeout_seconds,
        )
        response.raise_for_status()
        payload = response.json()
    except (httpx.HTTPError, ValueError) as exc:
        raise EmbeddingServiceError(
            f"Embedding服务调用失败：{type(exc).__name__}"
        ) from exc

    vectors = payload.get("vectors")
    dimensions = payload.get("dimensions")
    model = str(payload.get("model") or "unknown")
    if not isinstance(vectors, list) or len(vectors) != len(texts):
        raise EmbeddingServiceError("Embedding服务返回的向量数量不正确")
    if not isinstance(dimensions, int) or dimensions <= 0:
        raise EmbeddingServiceError("Embedding服务返回的向量维度无效")
    if dimensions != settings.rag_embedding_dimensions:
        raise EmbeddingServiceError(
            "Embedding维度与配置不一致："
            f"service={dimensions}, configured={settings.rag_embedding_dimensions}"
        )

    normalized_vectors = _validate_vectors(
        vectors,
        expected_count=len(texts),
        dimensions=dimensions,
    )

    return EmbeddingBatch(
        vectors=normalized_vectors,
        dimensions=dimensions,
        model=model,
        provider="http",
    )


def embed_texts(
    texts: list[str],
    settings: Settings | None = None,
) -> EmbeddingBatch:
    active_settings = settings or get_settings()
    cleaned = [str(text or "").strip() for text in texts]
    if not cleaned or any(not text for text in cleaned):
        raise ValueError("待向量化文本不能为空")

    provider = active_settings.rag_embedding_provider.strip().lower()
    if provider == "openai_compatible":
        vectors: list[list[float]] = []
        batch_size = active_settings.rag_embedding_batch_size
        for start in range(0, len(cleaned), batch_size):
            batch = _openai_compatible_embeddings(
                cleaned[start:start + batch_size],
                active_settings,
            )
            vectors.extend(batch.vectors)
        return EmbeddingBatch(
            vectors=vectors,
            dimensions=active_settings.rag_embedding_dimensions,
            model=active_settings.rag_embedding_model,
            provider="openai_compatible",
        )
    if provider == "http":
        return _http_embeddings(cleaned, active_settings)
    if provider == "hash":
        dimensions = active_settings.rag_embedding_dimensions
        return EmbeddingBatch(
            vectors=[embed_text(text, dimensions) for text in cleaned],
            dimensions=dimensions,
            model="stable-character-hash",
            provider="hash",
        )
    raise ValueError(f"不支持的Embedding提供方式：{provider}")
