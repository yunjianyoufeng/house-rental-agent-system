# House Rental Agent Service

房屋租赁系统的智能助手编排服务，使用 FastAPI、LangGraph 和 DeepSeek API。Spring Boot 负责业务执行，Agent 通过工具调用 Spring Boot 接口，不直接操作业务数据库。

## 功能

- 真实房源搜索、详情、推荐、对比和预算测算
- 看房预约准备与二次确认
- Redis 短期对话记忆和待确认状态
- MySQL 长期租房偏好（经 Spring Boot 接口访问）
- `sqlite-vec` 轻量级 RAG 平台知识问答
- Agent 工具调用结构化滚动日志
- 请求级 `traceId`、模型 Token/成本与轻量运行指标
- 模型超时重试、安全边界和自动化 Agent 评测
- DeepSeek、Spring Boot、Vue 端到端 SSE 流式回答

## 配置

```cmd
copy .env.example .env
```

在 `.env` 中填写 `DEEPSEEK_API_KEY`。其余配置已有本地开发默认值。`.env` 不应提交到 Git。

默认模型为 `deepseek-v4-flash`。估算成本使用 `.env.example` 中的人民币单价，
价格变化时只需调整环境变量，不需要修改代码。

## 安装与启动

```cmd
py -3.11 -m venv .venv
.venv\Scripts\activate
python -m pip install -r requirements.txt
python -m app.rag.ingest
python -m uvicorn app.main:app --host 127.0.0.1 --port 8001
```

健康检查：`http://127.0.0.1:8001/health`

## RAG

知识文档存放在 `knowledge/`。执行 `python -m app.rag.ingest` 后，索引生成在 `data/rag/knowledge.db`，该文件不会提交到 Git。

当前向量由稳定的中文字符特征生成，不依赖本地神经网络模型。这是真实的 `sqlite-vec` 向量检索，但语义能力弱于大型 Embedding 模型，优点是内存和磁盘占用很低。

## Agent 调用日志与指标

结构化事件默认写入 `logs/tool-calls.jsonl`，每行是一条 JSON 记录，覆盖请求、模型和工具调用。记录包含 `traceId`、状态、耗时、重试次数、Token、估算成本、会话、脱敏参数和精简结果摘要。

单个文件默认最多 2 MB，并保留 3 个备份，避免长期运行占用过多磁盘。授权信息、令牌、密码和 API Key 会被自动遮盖。日志目录不会提交到 Git。

运行时访问 `http://127.0.0.1:8001/metrics` 可查看当前进程内的请求量、错误数、平均耗时、模型 Token、估算成本和工具调用量。服务重启后指标会清零，结构化滚动日志仍保留。

## 运行依赖

- Spring Boot：`http://127.0.0.1:8080`
- Redis：`redis://127.0.0.1:6379/0`
- DeepSeek API：需要有效 API Key 和网络连接

## 验证

```cmd
.venv\Scripts\python.exe -m pip check
.venv\Scripts\python.exe -m app.rag.ingest
.venv\Scripts\python.exe -c "import app.agent.graph; print('Agent graph import OK')"
.venv\Scripts\python.exe -B -m unittest discover -s tests -v
.venv\Scripts\python.exe -m app.evaluation.offline_evaluator
```

## Agent 评测

离线评测覆盖角色工具权限、敏感信息拦截、RAG 路由和可信角色上下文，
不访问 DeepSeek，不消耗模型额度。GitHub CI 会将它作为合并门禁执行：

```cmd
.venv\Scripts\python.exe -m app.evaluation.offline_evaluator
```

在线评测会调用正在运行的 Agent，检查真实回答、关键词和 RAG 来源。
它只在手动提供租客登录令牌后运行，不会由 CI 自动执行：

```cmd
set AGENT_EVAL_TOKEN=你的租客登录令牌
.venv\Scripts\python.exe -m app.evaluation.live_evaluator
set AGENT_EVAL_TOKEN=
```

评测数据分别位于 `evals/offline_cases.json` 和 `evals/live_cases.json`。
