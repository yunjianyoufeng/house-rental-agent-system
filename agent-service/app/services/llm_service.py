from datetime import datetime
from typing import Any

from openai import AsyncOpenAI

from app.core.config import get_settings


SYSTEM_PROMPT = """你是房屋租赁系统的智能租房助手。
当用户要求查找、推荐、对比或了解房源时，必须调用工具获取平台真实数据，不得编造房源。
用户只想筛选符合条件的房源时使用 search_houses；要求“推荐”、“排序”或给出综合选择时使用 recommend_houses。
对比明确的多个房源ID时使用 compare_houses；计算租房承受能力时使用 calculate_rental_budget。
可先搜索或推荐房源，再根据用户要求查看详情或对比。
如果没有符合条件的房源，应如实说明，并建议用户调整条件，但不得擅自放宽条件再次搜索。
除严格按下述二次确认流程创建看房预约外，当前不具备修改其他业务数据的能力，不得声称已完成其他操作。
回答应简洁，并明确说明房源ID、租金、区域、户型和推荐理由。
押金字段只代表押金金额；除非工具明确返回付款周期，否则不得推断“押一付一”等押付方式。
预算测算只是基于用户输入的算术参考，不得将未提供的开支计入结果。
办理看房预约必须严格分为两轮：首轮只能调用 prepare_appointment，向用户展示房源、时间、地址和备注，并说明尚未创建。
只有在下一轮中，待确认上下文存在且用户明确表达“确认预约”时，才能调用 confirm_appointment。“好的”、“可以”等在有完整待确认摘要时可视为明确确认。
如果用户要求取消尚未创建的预约，调用 cancel_appointment_preparation。用户修改房源、时间或备注时，必须重新调用 prepare_appointment 并再次等待确认。
长期租房偏好只保存结构化信息。只有用户明确说“记住”、“保存为偏好”或明确表达同等意图时，才能调用 save_rental_preference。不得从一次性查房、预算测算或闲聊中自动保存。
只有用户明确说“忘记偏好”、“清除偏好”等时，才能调用 clear_rental_preference。不得保存密码、令牌、身份证号、支付信息或完整对话。
如果当前上下文提供了已保存长期偏好，当用户说“按我的偏好”或要求个性化推荐时可以使用，但用户本轮的明确条件始终优先。
当用户询问本平台的预约、租房申请、合同、订单、报修、投诉或操作流程时，必须调用 search_rental_knowledge 检索项目知识库后再回答，不得凭常识补充未检索到的规则。
知识库只用于平台规则和办理流程，不用于查询实时房源、用户数据或执行操作。回答应以检索片段为依据；资料不足时明确说明，不得编造。
回复必须使用易读的纯文本，不要使用 Markdown 加粗标记、标题符号或分隔线。
"""


class DeepSeekConfigurationError(RuntimeError):
    """DeepSeek 配置缺失。"""


def initial_messages(
    message: str,
    pending_appointment: dict[str, Any] | None = None,
    conversation_messages: list[dict[str, str]] | None = None,
    rental_preference: dict[str, Any] | None = None,
) -> list[dict[str, str]]:
    local_time = datetime.now().astimezone().isoformat(timespec="seconds")
    context = f"当前本地时间：{local_time}。"
    if pending_appointment:
        context += (
            "\n当前会话有一个待确认预约："
            f"{pending_appointment}。"
            "请根据用户本轮表达决定确认、取消或重新准备。"
        )
    else:
        context += "\n当前会话没有待确认预约。"
    if rental_preference:
        visible_preference = {
            key: value
            for key, value in rental_preference.items()
            if key not in {"id", "userId", "createTime", "updateTime"}
            and value is not None
        }
        context += f"\n当前用户已保存的长期租房偏好：{visible_preference}。"
    else:
        context += "\n当前用户没有已保存的长期租房偏好。"
    return [
        {"role": "system", "content": SYSTEM_PROMPT},
        {"role": "system", "content": context},
        *(conversation_messages or []),
        {"role": "user", "content": message},
    ]


async def create_completion(
    messages: list[dict[str, Any]],
    tools: list[dict[str, Any]] | None = None,
):
    settings = get_settings()
    if settings.deepseek_api_key is None:
        raise DeepSeekConfigurationError(
            "尚未配置 DEEPSEEK_API_KEY，当前不能调用模型服务。"
        )

    client = AsyncOpenAI(
        api_key=settings.deepseek_api_key.get_secret_value(),
        base_url=settings.deepseek_base_url,
    )
    request_options: dict[str, Any] = {
        "model": settings.deepseek_model,
        "messages": messages,
        "temperature": 0.2,
    }
    if tools:
        request_options["tools"] = tools
        request_options["tool_choice"] = "auto"
    return await client.chat.completions.create(**request_options)


async def generate_answer(message: str) -> str:
    response = await create_completion(initial_messages(message))
    answer = response.choices[0].message.content
    if not answer:
        raise DeepSeekConfigurationError("模型返回了空内容，请稍后重试。")
    return answer
