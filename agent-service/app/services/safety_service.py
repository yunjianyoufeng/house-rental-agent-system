import re


class UnsafeInputError(ValueError):
    """用户消息包含不应发送给模型的敏感信息。"""


SENSITIVE_PATTERNS = (
    ("API Key", re.compile(r"\bsk-[A-Za-z0-9_-]{16,}\b")),
    (
        "访问令牌",
        re.compile(r"\bBearer\s+[A-Za-z0-9._~+/=-]{20,}", re.IGNORECASE),
    ),
    (
        "JWT",
        re.compile(
            r"\beyJ[A-Za-z0-9_-]{8,}\.[A-Za-z0-9_-]{8,}\."
            r"[A-Za-z0-9_-]{8,}\b"
        ),
    ),
    ("身份证号", re.compile(r"(?<!\d)\d{17}[\dXx](?!\d)")),
    ("银行卡号", re.compile(r"(?<!\d)(?:\d[ -]?){15,18}\d(?!\d)")),
    (
        "明文密码",
        re.compile(
            r"(?:密码|password|passwd|pwd)\s*(?:是|为|[:：=])\s*\S{4,}",
            re.IGNORECASE,
        ),
    ),
)


def validate_user_message(message: str) -> None:
    for label, pattern in SENSITIVE_PATTERNS:
        if pattern.search(message):
            raise UnsafeInputError(
                f"消息中疑似包含{label}，为保护账户安全，请删除敏感内容后重试。"
            )
