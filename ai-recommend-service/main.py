import os
from pathlib import Path
from typing import List, Optional

# 让 Hugging Face / Transformers 优先使用本地缓存，避免每次启动联网检查
os.environ.setdefault("HF_HUB_OFFLINE", "1")
os.environ.setdefault("TRANSFORMERS_OFFLINE", "1")
os.environ.setdefault("HF_HUB_DISABLE_SYMLINKS_WARNING", "1")

from fastapi import FastAPI
from pydantic import BaseModel, Field, field_validator
from sentence_transformers import SentenceTransformer, util


app = FastAPI(title="House Rental Semantic Recommend Service")


# 默认模型名称
DEFAULT_MODEL_NAME = "shibing624/text2vec-base-chinese"

# 也支持以后手动指定本地模型路径，例如：
# $env:SENTENCE_TRANSFORMER_MODEL_PATH="C:\Users\你的用户名\.cache\huggingface\hub\models--shibing624--text2vec-base-chinese\snapshots\xxxx"
MODEL_NAME = os.getenv("SENTENCE_TRANSFORMER_MODEL_PATH", DEFAULT_MODEL_NAME)


def find_local_model_snapshot() -> Optional[str]:
    """
    自动查找本地 Hugging Face 缓存中的 text2vec-base-chinese 模型目录。
    常见路径：
    C:\\Users\\用户名\\.cache\\huggingface\\hub\\models--shibing624--text2vec-base-chinese\\snapshots\\xxxx
    """
    snapshot_root = (
            Path.home()
            / ".cache"
            / "huggingface"
            / "hub"
            / "models--shibing624--text2vec-base-chinese"
            / "snapshots"
    )

    if not snapshot_root.exists():
        return None

    snapshot_dirs = [item for item in snapshot_root.iterdir() if item.is_dir()]
    if not snapshot_dirs:
        return None

    # 优先选择包含模型配置文件的目录
    for snapshot_dir in snapshot_dirs:
        if (snapshot_dir / "config.json").exists():
            return str(snapshot_dir)

    # 如果没有找到 config.json，就返回第一个目录
    return str(snapshot_dirs[0])


def load_sentence_model() -> SentenceTransformer:
    """
    加载语义向量模型：
    1. 如果设置了 SENTENCE_TRANSFORMER_MODEL_PATH，则优先使用该路径；
    2. 否则自动查找本地缓存；
    3. 如果找不到本地缓存，则尝试用模型名从本地缓存加载；
    4. 全程 local_files_only=True，避免启动时联网。
    """
    model_path = MODEL_NAME

    if MODEL_NAME == DEFAULT_MODEL_NAME:
        local_snapshot = find_local_model_snapshot()
        if local_snapshot:
            model_path = local_snapshot

    try:
        return SentenceTransformer(model_path, local_files_only=True)
    except Exception as e:
        raise RuntimeError(
            "语义向量模型加载失败。请确认模型已经下载完成，或者手动设置 "
            "SENTENCE_TRANSFORMER_MODEL_PATH 为本地模型 snapshots 目录。"
            f" 当前模型路径：{model_path}。原始错误：{e}"
        )


model = load_sentence_model()


class HouseItem(BaseModel):
    id: int = Field(..., description="房源ID")
    text: str = Field(..., description="房源文本")


class RecommendRequest(BaseModel):
    query: str = Field(..., description="用户自然语言租房需求")
    houses: List[HouseItem] = Field(default_factory=list, description="候选房源列表")
    topK: int = Field(default=5, description="返回推荐数量")


class RecommendResult(BaseModel):
    houseId: int
    score: float


class EmbeddingRequest(BaseModel):
    texts: List[str] = Field(min_length=1, max_length=128)
    normalize: bool = True

    @field_validator("texts")
    @classmethod
    def validate_texts(cls, texts: List[str]) -> List[str]:
        cleaned = [text.strip() for text in texts]
        if any(not text for text in cleaned):
            raise ValueError("待向量化文本不能为空")
        return cleaned


class EmbeddingResponse(BaseModel):
    model: str
    dimensions: int
    vectors: List[List[float]]


@app.get("/health")
def health():
    return {
        "status": "ok",
        "model": MODEL_NAME,
        "offline": True
    }


@app.post("/embeddings", response_model=EmbeddingResponse)
def create_embeddings(req: EmbeddingRequest):
    vectors = model.encode(
        req.texts,
        convert_to_numpy=True,
        normalize_embeddings=req.normalize,
    )
    return {
        "model": MODEL_NAME,
        "dimensions": int(vectors.shape[1]),
        "vectors": vectors.tolist(),
    }


@app.post("/semantic-recommend", response_model=List[RecommendResult])
def semantic_recommend(req: RecommendRequest):
    if not req.query.strip() or not req.houses:
        return []

    top_k = req.topK
    if top_k <= 0:
        top_k = 5
    top_k = min(top_k, 20)

    house_texts = [item.text for item in req.houses]

    query_embedding = model.encode(
        req.query,
        convert_to_tensor=True,
        normalize_embeddings=True
    )

    house_embeddings = model.encode(
        house_texts,
        convert_to_tensor=True,
        normalize_embeddings=True
    )

    scores = util.cos_sim(query_embedding, house_embeddings)[0]

    results = []
    for index, score in enumerate(scores):
        results.append({
            "houseId": req.houses[index].id,
            "score": round(float(score), 6)
        })

    results.sort(key=lambda item: item["score"], reverse=True)
    return results[:top_k]
