# House Rental Agent Service

房屋租赁系统的智能助手编排服务，使用 FastAPI、LangGraph 和 DeepSeek API。Spring Boot 负责业务执行，Agent 通过工具调用 Spring Boot 接口，不直接操作业务数据库。

## 功能

- 真实房源搜索、详情、推荐、对比和预算测算
- 看房预约准备与二次确认
- Redis 短期对话记忆和待确认状态
- MySQL 长期租房偏好（经 Spring Boot 接口访问）
- Chroma + SQLite FTS5 混合检索，`sqlite-vec` 可自动降级
- Agent 工具调用结构化滚动日志
- 请求级 `traceId`、模型 Token/成本与轻量运行指标
- 模型超时重试、安全边界和自动化 Agent 评测
- DeepSeek、Spring Boot、Vue 端到端 SSE 流式回答
- MCP 2.x 标准只读工具服务

## 配置

```cmd
copy .env.example .env
```

在 `.env` 中至少填写：

```text
DEEPSEEK_API_KEY=你的DeepSeek密钥
RAG_EMBEDDING_API_KEY=你的百炼密钥
```

默认使用百炼 OpenAI 兼容地址和 `text-embedding-v4`（768 维）。如果使用业务空间专属域名，修改 `RAG_EMBEDDING_BASE_URL`。`.env` 不应提交到 Git。

百炼官方参考：[Base URL 总览](https://help.aliyun.com/zh/model-studio/base-url)、[OpenAI Embedding 兼容接口](https://help.aliyun.com/zh/model-studio/embedding-interfaces-compatible-with-openai/)。

默认模型为 `deepseek-v4-flash`。估算成本使用 `.env.example` 中的人民币单价，
价格变化时只需调整环境变量，不需要修改代码。

## LangSmith 链路观测

项目默认关闭 LangSmith。启用前先在 LangSmith 控制台创建 API Key，然后在
`agent-service/.env` 中填写：

```text
LANGSMITH_TRACING=true
LANGSMITH_API_KEY=你的LangSmith密钥
LANGSMITH_PROJECT=house-rental-agent-dev
LANGSMITH_HIDE_INPUTS=true
LANGSMITH_HIDE_OUTPUTS=true
```

重启 Agent 服务后，一次聊天请求会形成请求、LangGraph、DeepSeek、工具调用和
RAG 检索链路。默认不上传模型输入和输出；工具只记录参数名和结果摘要，RAG 只
记录查询长度、命中数量与知识来源。排查结束后可将 `LANGSMITH_TRACING` 改回
`false`。`LANGSMITH_API_KEY` 只写入本地 `.env` 或部署环境变量，不要提交到 Git。

## 安装与启动

```cmd
py -3.11 -m venv .venv
.venv\Scripts\activate
python -m pip install -r requirements.txt
python -m app.rag.ingest
python -m uvicorn app.main:app --host 127.0.0.1 --port 8001
```

健康检查：`http://127.0.0.1:8001/health`

## MCP 只读工具服务

MCP 服务通过统一工具注册层复用现有业务能力，不直接访问 MySQL。目前仅开放：

- `search_houses`
- `get_house_detail`
- `compare_houses`
- `calculate_rental_budget`
- `search_rental_knowledge`

预约、长期偏好和个人业务工具不会通过 MCP 暴露。启动本地 stdio 服务：

```cmd
cd agent-service
.venv\Scripts\python.exe -m app.mcp.server
```

MCP Host 配置示例，`command` 和 `cwd` 需要替换为本机绝对路径：

```json
{
  "mcpServers": {
    "house-rental": {
      "command": "<项目路径>\\agent-service\\.venv\\Scripts\\python.exe",
      "args": ["-m", "app.mcp.server"],
      "cwd": "<项目路径>\\agent-service"
    }
  }
}
```

MCP 使用 stdio 传输，标准输出属于协议通道，运行时日志应写入标准错误或现有结构化日志文件。

## RAG

知识文档存放在 `knowledge/`。生产和完整演示默认使用：

- 云端 `text-embedding-v4` 生成 768 维语义向量
- Chroma 执行向量召回
- SQLite FTS5/BM25 执行关键词召回
- 轻量 Token 覆盖率执行结果重排
- Chroma 异常时自动降级到同步构建的 `sqlite-vec` 索引

配置 `RAG_EMBEDDING_API_KEY` 后，在 `agent-service` 目录构建索引：

```cmd
.venv\Scripts\python.exe -m app.rag.ingest
```

SQLite 分片、FTS5 和后备向量索引位于 `data/rag/knowledge.db`，Chroma 数据位于
`data/rag/chroma/`，两者都不会提交到 Git。切换向量库可设置
`RAG_VECTOR_STORE=chroma` 或 `RAG_VECTOR_STORE=sqlite-vec`；更换 Embedding 模型或维度后必须重新构建索引。

运行检索评测：

```cmd
.venv\Scripts\python.exe -B -m app.evaluation.rag_evaluator
```

上述命令使用当前 `.env` 配置的 Embedding 服务，会访问网络并可能产生少量 API 费用。只验证工程链路、不评估真实语义质量时，请使用下面的 `hash` 配置；该模式不会调用云端 Embedding。

评测数据位于 `evals/rag_cases.json`，输出 Hit@3 和 MRR，Hit@3 低于 0.8 时返回失败状态。

CI 不使用真实 Key。它通过以下确定性配置验证完整入库和检索链路：

```cmd
set RAG_EMBEDDING_PROVIDER=hash
.venv\Scripts\python.exe -m app.rag.ingest
.venv\Scripts\python.exe -m app.evaluation.rag_evaluator
set RAG_EMBEDDING_PROVIDER=
```

`hash` 仅用于 CI 和故障诊断，不作为生产语义模型。旧 `ai-recommend-service` 仍保留用于历史推荐功能，但不属于当前 Agent RAG 主流程。

## Agent 调用日志与指标

结构化事件默认写入 `logs/tool-calls.jsonl`，每行是一条 JSON 记录，覆盖请求、模型和工具调用。记录包含 `traceId`、状态、耗时、重试次数、Token、估算成本、会话、脱敏参数和精简结果摘要。

单个文件默认最多 2 MB，并保留 3 个备份，避免长期运行占用过多磁盘。授权信息、令牌、密码和 API Key 会被自动遮盖。日志目录不会提交到 Git。

运行时访问 `http://127.0.0.1:8001/metrics` 可查看当前进程内的请求量、错误数、平均耗时、模型 Token、估算成本和工具调用量。服务重启后指标会清零，结构化滚动日志仍保留。

## 运行依赖

- Spring Boot：`http://127.0.0.1:8080`
- Redis：`redis://127.0.0.1:6379/0`
- DeepSeek API：需要有效 API Key 和网络连接
- 云端 Embedding API：需要独立的百炼 API Key 和网络连接

## 验证

```cmd
.venv\Scripts\python.exe -m pip check
.venv\Scripts\python.exe -m app.rag.ingest
.venv\Scripts\python.exe -c "import app.agent.graph; print('Agent graph import OK')"
.venv\Scripts\python.exe -B -m unittest discover -s tests -v
.venv\Scripts\python.exe -m app.evaluation.offline_evaluator
.venv\Scripts\python.exe -B -m app.evaluation.rag_evaluator
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
