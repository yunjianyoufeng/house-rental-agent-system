# 自动化测试说明

本项目优先使用已有依赖完成测试，避免为了测试额外占用磁盘和内存。

## Agent 服务测试

在项目根目录执行：

```powershell
cd agent-service
.\.venv\Scripts\python.exe -B -m unittest discover -s tests -v
```

当前覆盖：

- 聊天请求字段校验与驼峰字段映射
- 空消息拦截
- RAG 来源响应序列化
- RAG 来源合并、去重与相关度边界

## Spring Boot 测试

在 IDEA 的 Maven 工具窗口中运行 `Lifecycle > test`，或在配置了 Maven 命令行的环境中执行：

```powershell
cd house-rental-server
mvn test
```

当前覆盖 Agent 回答及 RAG 来源的 JSON 反序列化。

## Vue 构建校验

```powershell
cd house-rental-web
npm run build
```

该命令会检查 Vue 单文件组件能否完成生产构建，不会安装新依赖。
