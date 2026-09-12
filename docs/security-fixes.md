# 安全修复与升级说明

## 本次范围

针对安全报告的 01—07 项修复。保留现有目录结构、Dify 集成和演示数据文件，未增加第三方依赖。代码变更不会自动修改当前运行的数据库、创建账号或部署容器。

| 问题 | 处理结果 | 主要文件 |
| --- | --- | --- |
| 01 模拟支付未受服务端限制 | 默认关闭模拟支付；启动支付和确认支付都检查配置，非演示环境不能伪造付款状态 | `LeaseOrderServiceImpl.java`、`PaymentProperties.java`、`application.yml` |
| 02 默认账号 | Compose 不再自动导入演示 SQL；非演示环境拒绝公开默认密码及明文密码登录，已改密的账号仍可使用 | `compose.yaml`、`UserServiceImpl.java` |
| 03 匿名模型消费 | 游客只使用规则推荐；付费解析须为租客并通过每小时配额；两个解析入口共用配额，候选集上限 200 | 推荐控制器、解析控制器、DTO、`RequestLimitService.java` |
| 04 预约确认 | 待办每次准备生成新版本；执行层要求用户独立确认表达及版本一致 | Agent graph、state、registry、appointment_tools |
| 05 登录防护 | 统一登录失败提示、账号和来源限流，注册限流及密码长度校验 | `AuthController.java`、`UserServiceImpl.java`、`RequestLimitService.java` |
| 06 会话令牌 | 浏览器使用 HttpOnly、SameSite=Strict Cookie；Spring CSRF 校验；1 天空闲期限、7 天绝对期限；旧会话失效 | Token 服务、安全配置、前端 request/agent/csrf/store/main |
| 07 文件上传 | 文件签名、图片解码/尺寸及压缩包展开上限检查；合同附件下载；每日上传配额 | `UploadContentValidator.java`、上传和合同控制器、三个角色页面 |

另收紧 MySQL、Java、Agent 的主机端口到 `127.0.0.1`，新增业务数据库账号，增加 Nginx 请求限流与基础响应头。Agent 非流式和流式接口共用用户配额，直连 Agent 也生效。旧语义服务增加请求大小上限。

## 升级时必须注意

1. **浏览器需要重新登录。** 旧的 Redis Token 不含签发时间，会被拒绝；前端启动时清除旧 localStorage 凭据。localStorage 仅保留展示资料，后端仍是权限判断来源。
2. **支付默认不可用。** 尚未接入真实支付平台；关闭演示模式后，服务端拒绝模拟收款。不得把打开演示开关当作生产收款方案。
3. **演示账号默认不可登录。** `house_rental.sql` 保留用于手工演示。未修改公开默认密码的账号和旧明文密码账号需要重置；本次没有修改存量用户表。
4. **已有 MySQL 数据卷不会因环境变量新增而自动创建业务用户。** 在切换 Compose 之前，数据库所有者须先建立 `rental_app` 并授权；不能删除数据卷解决该问题。
5. **HTTPS 部署设置 `SESSION_COOKIE_SECURE=true`。** 本地 HTTP 使用 `false`；前端经同源 `/api` 代理访问后端。不要把后端、Agent 和 MySQL 端口重新直接公开到公网。
6. 前端 API 登录响应不再提供 Bearer Token；内部 Agent 转发仍使用后端从会话提取的 Bearer 凭据。外部脚本客户端若调用登录，需先获取 `/auth/csrf`，保存 Cookie，再携带 `X-XSRF-TOKEN` 请求头进行写操作。
7. 合同附件现在下载到本地，保留服务器文件扩展名；不自动打开浏览器预览。

## 配置

在现有 `docker.env` 中补充：

```dotenv
# 请自行填写与 root 密码不同的独立强密码，不使用示例文本。
MYSQL_APP_PASSWORD=替换为独立强密码
DEMO_ENABLED=false
PAYMENT_DEMO_MODE=false
# HTTPS 上线时为 true，本地 HTTP 为 false。
SESSION_COOKIE_SECURE=true
```

只有明确隔离的本地演示环境才可设置 `DEMO_ENABLED=true`、`PAYMENT_DEMO_MODE=true`。这允许原演示账号和模拟支付，不会自动导入演示数据。

### 已有数据库的账号准备

由数据库所有者连接 MySQL 后执行以下 SQL。密码替换为与 `MYSQL_APP_PASSWORD` 完全一致的值，妥善处理 SQL 字符串转义；不要把真实密码提交到仓库。

```sql
CREATE USER IF NOT EXISTS 'rental_app'@'%' IDENTIFIED BY '替换为独立强密码';
GRANT ALL PRIVILEGES ON house_rental.* TO 'rental_app'@'%';
```

该用户只有业务库权限，不具有全局 root 权限；当前 Flyway 同进程运行，需要业务库内建表和迁移权限。若用户已经存在，应由所有者确认其密码与权限，`CREATE USER IF NOT EXISTS` 不会重置密码。

新空库由 Flyway 建表，不会创建默认管理员。可先通过注册页注册一个独立强密码账号，再由数据库所有者核对后晋升角色：

```sql
SELECT id, username, role_code FROM sys_user WHERE username = '实际注册的用户名';
-- 确认查询结果为本人预期的唯一账号后执行，不要批量更新。
UPDATE sys_user SET role_code = 'ADMIN' WHERE id = 实际核对的用户ID;
```

存量明文/默认密码账号需使用可信的密码重置流程。本项目目前没有自助密码重置界面；可由所有者安排独立新账号并保留原数据，或在核实身份后写入新生成的 BCrypt 密码哈希。不要对所有默认 ID 无条件禁用或覆盖密码。

## 限额与实现边界

- 登录：每连接来源 30 次/15 分钟、每规范化账号 10 次/15 分钟；注册每来源 5 次/小时。
- 推荐：每来源 30 次/分钟；两个付费需求解析入口共用每租客 20 次/小时额度。
- Agent：每用户 6 次/分钟、60 次/小时，非流式与流式共用额度。
- 上传：每用户 100 次/天；图片 5 MiB，附件 10 MiB；图片累计像素上限 1600 万，GIF 最多 200 帧；DOCX 展开上限 30 MiB、最多 1000 个条目。
- Redis 计数与 TTL 原子写入；不可用时不放行付费路径。应用按 `remoteAddr` 计来源，不信任客户端的转发头；位于同一反向代理后时来源额度会共享。Nginx 另按连接来源限流，多级代理部署应结合实际拓扑配置可信来源及额度。
- 计数采用固定窗口，并非精确滑动窗口。Redis 配置、实例容量及配额阈值应按上线规模验证。
- 文件格式校验不是杀毒。JDK 缺少 WebP 解码器，所以 WebP 使用容器结构与尺寸校验；旧 DOC 使用 OLE 签名检查；PDF 检查文件头尾，DOCX 检查包结构与宏项目。生产附件的恶意内容扫描仍需要单独评估，不能把这些校验当作“文件绝对安全”。
- HttpOnly 减少凭据被脚本直接读取的风险，不消除同源脚本代用户操作的风险。Dify 外部脚本仍须来自可信来源；本次未下载或审查远程脚本内容。
- 本次不包含支付平台接入、线上渗透测试或依赖漏洞数据库审计。

## 验证方法

所有本次执行的构建/测试都使用已有工具和依赖，未下载资源。

本次结果：Java/JUnit 41 项、Python 64 项、前端 CSRF 3 项测试通过；Vue 生产构建、Compose 配置校验及 `git diff --check` 通过。没有启动生产服务、执行数据库变更或调用真实付费模型。标准 Maven Surefire 流程的缓存缺失见下文。

```bat
cd /d D:\javaprogramsssss\agent-service
.venv\Scripts\python.exe -B -m unittest discover -s tests -v

cd /d D:\javaprogramsssss\house-rental-web
node --test tests/csrf.test.js
npm run build

cd /d D:\javaprogramsssss\house-rental-server
set "JAVA_HOME=D:\java\IntelliJ IDEA Community Edition 2024.1.1\jbr"
"D:\java\IntelliJ IDEA Community Edition 2024.1.1\plugins\maven\lib\maven3\bin\mvn.cmd" -o -Dmaven.repo.local="C:\Users\谢\.m2\repository" test-compile
```

本机 Maven Surefire 缺少 `surefire-junit-platform:3.2.5`。本次使用临时 `target/OfflineSecurityTests.java` 直接调用已缓存的 JUnit Jupiter 引擎执行测试，保留生命周期与参数化测试；没有把“编译成功”当成“测试通过”。完整 Maven Surefire 流程尚需补齐该组件。

如果后续希望补齐标准 Maven 测试流程，以下命令由你在 CMD 自行执行：用途是解析现有 POM 的测试运行组件，主要缺失版本为 `org.apache.maven.surefire:surefire-junit-platform:3.2.5` 及其传递依赖，不新增项目依赖。

```bat
cd /d D:\javaprogramsssss\house-rental-server
set "JAVA_HOME=D:\java\IntelliJ IDEA Community Edition 2024.1.1\jbr"
"D:\java\IntelliJ IDEA Community Edition 2024.1.1\plugins\maven\lib\maven3\bin\mvn.cmd" -Dmaven.repo.local="C:\Users\谢\.m2\repository" test
```

上面的最后一条命令会联网；本次没有执行，执行完成前不会由助手下载缺失组件。
