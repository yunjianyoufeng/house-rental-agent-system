from typing import Literal

from pydantic import BaseModel, ConfigDict, Field, field_validator


class ChatRequest(BaseModel):
    model_config = ConfigDict(populate_by_name=True)

    message: str = Field(min_length=1, max_length=2000)
    conversation_id: str | None = Field(default=None, alias="conversationId")
    user_id: int | None = Field(default=None, alias="userId", gt=0)
    role_code: Literal["TENANT", "LANDLORD", "ADMIN"] | None = Field(
        default=None,
        alias="roleCode",
    )

    @field_validator("message")
    @classmethod
    def validate_message(cls, value: str) -> str:
        message = value.strip()
        if not message:
            raise ValueError("消息内容不能为空")
        return message


class ChatResponse(BaseModel):
    model_config = ConfigDict(populate_by_name=True)

    answer: str
    conversation_id: str | None = Field(default=None, alias="conversationId")
    sources: list["KnowledgeSource"] = Field(default_factory=list)


class KnowledgeSource(BaseModel):
    source: str
    title: str
    section: str
    relevance: float = Field(ge=0, le=1)
