import hashlib
import math
import re


TOKEN_PATTERN = re.compile(r"[\u4e00-\u9fff]|[a-z0-9]+")


def _stable_bucket(token: str, dimensions: int) -> tuple[int, float]:
    digest = hashlib.blake2b(token.encode("utf-8"), digest_size=8).digest()
    raw = int.from_bytes(digest, byteorder="little", signed=False)
    index = raw % dimensions
    sign = 1.0 if (raw >> 63) == 0 else -1.0
    return index, sign


def embed_text(text: str, dimensions: int = 384) -> list[float]:
    """使用稳定的字符特征生成低内存向量，不加载本地神经网络模型。"""
    if dimensions <= 0:
        raise ValueError("向量维度必须大于 0")

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
