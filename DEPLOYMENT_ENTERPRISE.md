# 房屋租赁 Agent 系统部署与运维指南

本文对应当前 `Vue 3 + Spring Boot + FastAPI/LangGraph + MySQL + Redis` 架构。
旧版 `DEPLOYMENT.md` 仅作为历史资料保留。

## 1. 推荐运行方式

对于内存和磁盘空间较少的 Windows 开发电脑，推荐继续使用本地分服务方式：

1. 启动本机 MySQL。
2. 只在 Docker 中启动 Redis。
3. 使用 IntelliJ IDEA 启动 Spring Boot。
4. 使用 Python 虚拟环境启动 Agent。
5. 使用 `npm run dev` 启动 Vue。

这种方式可以复用已经下载的 Maven、npm 和 Python 依赖，不需要额外保存多层 Docker 镜像。

## 2. 本地启动检查

启动顺序：

```text
MySQL → Redis → Spring Boot → Agent → Vue
```

检查地址：

| 服务 | 地址 |
|---|---|
| Spring Boot | `http://127.0.0.1:8080` |
| Agent 健康检查 | `http://127.0.0.1:8001/health` |
| Agent 指标 | `http://127.0.0.1:8001/metrics` |
| Vue | `http://127.0.0.1:5173` |

Agent `.env` 应使用：

```text
DEEPSEEK_MODEL=deepseek-v4-flash
```

不要在终端截图、日志或 Git 中暴露 `DEEPSEEK_API_KEY`。

## 3. 轻量可观测性

每次同步或流式问答都会生成 `traceId`。它会出现在：

- 同步响应的 `traceId` 字段与 `X-Trace-Id` 响应头；
- 流式 SSE 事件与 `X-Trace-Id` 响应头；
- `agent_request`、`agent_model_call`、`agent_tool_call` 结构化日志。

查看最近日志（CMD）：

```cmd
cd /d D:\javaprogramsssss\agent-service
powershell -NoProfile -Command "Get-Content .\logs\tool-calls.jsonl -Tail 20"
```

实时跟踪：

```cmd
powershell -NoProfile -Command "Get-Content .\logs\tool-calls.jsonl -Tail 10 -Wait"
```

指标接口只返回聚合数字，不返回用户问题、回答、令牌或密码。指标保存在内存中，Agent 重启后清零；滚动日志默认单文件 2 MB，保留 3 个备份。

估算成本根据 DeepSeek 返回的实际 Token 和环境变量中的单价计算。该数字用于开发监控，不替代 DeepSeek 控制台账单。

## 4. Docker Compose 部署（可选）

Docker 方式会下载并保存基础镜像。磁盘紧张时不要执行本节命令。

首次准备：

```cmd
copy docker.env.example docker.env
```

编辑 `docker.env`，至少修改数据库密码和 DeepSeek Key。`docker.env` 已加入 `.gitignore`。

只校验 Compose 配置，不下载镜像：

```cmd
docker compose --env-file docker.env config
```

构建并启动默认轻量栈：

```cmd
docker compose --env-file docker.env up -d --build mysql redis backend agent web
```

访问：

```text
http://127.0.0.1:5173
```

默认栈为各容器设置了约 1.6 GB 的总内存上限，不包含 Docker Desktop 自身开销。首次构建会占用数 GB 磁盘空间。

## 5. 可选旧 Embedding 服务

`ai-recommend-service` 会加载 `sentence-transformers`，内存和镜像体积明显更大，默认不会启动。只有确实需要演示旧版 EMBEDDING 推荐时才使用：

```cmd
set SENTENCE_TRANSFORMER_MODEL_PATH=D:\你的模型目录
docker compose --env-file docker.env --profile embedding up -d --build embedding
```

该模式额外允许最多约 1.5 GB 内存。普通 Agent、规则推荐和 TF-IDF 推荐不依赖它。

## 6. Docker 日志与指标

```cmd
docker compose logs --tail 100 backend
docker compose logs --tail 100 agent
```

Agent 指标：

```text
http://127.0.0.1:8001/metrics
```

结构化 Agent 日志保存在 Docker 卷 `agent-logs` 中。查看卷：

```cmd
docker volume ls
```

## 7. 数据持久化与备份

Compose 使用以下命名卷：

- `mysql-data`：业务数据库；
- `redis-data`：登录和短期状态；
- `uploads-data`：房源图片与合同附件；
- `agent-logs`：Agent 滚动日志。

执行 `docker compose down` 不会删除这些卷。不要执行 `docker compose down -v`，除非明确要永久删除全部容器数据。

生产或正式演示前应备份 MySQL 和 `uploads-data`。数据库迁移由 Flyway 自动执行，禁止在生产环境使用 Flyway clean。

## 8. 停止与资源清理

停止但保留数据：

```cmd
docker compose --env-file docker.env stop
```

移除容器但保留数据卷：

```cmd
docker compose --env-file docker.env down
```

清理镜像和数据属于破坏性操作，不应在未确认镜像、容器和卷用途前执行。

## 9. 发布前检查

- GitHub CI 为绿色。
- `.env`、`docker.env` 和真实 Key 未被 Git 跟踪。
- 演示账号密码已修改，或明确限制为本地环境。
- `/health`、`/metrics`、登录、RAG、房源查询和预约二次确认正常。
- 日志中未出现 Authorization、密码或 API Key 明文。
- MySQL、上传文件和必要配置已有备份。
- 公网部署时在 Nginx 前增加 HTTPS、访问控制和防火墙规则。
