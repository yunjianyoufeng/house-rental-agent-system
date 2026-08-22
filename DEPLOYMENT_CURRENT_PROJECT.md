# 房屋租赁管理系统部署文档（当前项目版）

> 历史版本说明：本文生成于 Agent 服务接入前，保留供旧版 EMBEDDING 推荐部署参考。当前启动与配置请以根目录 `README.md` 为准。

> 适用项目：智能推荐版房屋租赁管理系统
> 适用场景：本地运行、毕业设计答辩演示、项目验收、换电脑重新部署
> 文档生成依据：当前项目源码、`application.yml`、`package.json`、Python 推荐服务与数据库脚本

---

## 一、项目组成

当前项目由三个主要服务组成：

| 目录 | 说明 | 默认端口 |
|---|---|---|
| `house-rental-server` | Spring Boot 后端服务，负责业务接口、登录鉴权、数据库读写、文件上传、支付演示、推荐业务调度 | `8080` |
| `house-rental-web` | Vue3 前端服务，负责公共端、租客端、出租者端、管理员端页面展示 | `5173` |
| `ai-recommend-service` | Python 语义推荐服务，负责 EMBEDDING 模型的中文语义相似度计算 | `9000` |

完整运行时的启动顺序为：

```text
1. 启动 MySQL
2. 启动 Redis
3. 启动 Python 语义推荐服务
4. 启动 Spring Boot 后端服务
5. 启动 Vue 前端服务
6. 浏览器访问 http://localhost:5173
```

如果只测试基础租赁功能、RULE 推荐或 TFIDF 推荐，可以暂时不启动 Python 服务；如果要测试 EMBEDDING 推荐或管理员端模型评价中的 EMBEDDING 模型，必须启动 Python 服务。

---

## 二、软件环境要求

请先确认电脑中安装以下环境。

| 软件 | 推荐版本 | 用途 |
|---|---|---|
| JDK | `17` | 运行 Spring Boot 后端 |
| Maven | `3.9+` | 后端依赖管理和命令行启动 |
| Node.js | `20.19+` 或 `22.12+` | 运行 Vue3 / Vite 前端 |
| npm | 随 Node.js 安装，建议 `10+` | 前端依赖安装 |
| MySQL | `8.x` | 项目数据库 |
| Redis | `6.x` 或 `7.x` | 登录 Token 缓存 |
| Python | `3.10` | 运行语义推荐服务 |
| IntelliJ IDEA | 建议安装 | 启动后端和管理项目 |
| Navicat | 可选 | 导入和查看数据库 |
| Apifox | 可选 | 接口测试 |
| Git | 可选 | 版本管理 |

注意：当前前端项目的 `package.json` 中要求 Node.js 版本为 `^20.19.0 || >=22.12.0`，因此不建议继续使用 Node.js 18，否则可能出现 Vite、Rollup 或依赖平台包不兼容的问题。

可以在命令行中检查版本：

```bash
java -version
mvn -v
node -v
npm -v
python --version
```

如果 `mvn -v` 无法识别，也可以直接使用 IntelliJ IDEA 运行后端启动类。

---

## 三、项目交付包整理建议

正式答辩、提交或换电脑部署前，建议项目根目录保留以下内容：

```text
house-rental-server/
house-rental-web/
ai-recommend-service/
house_rental.sql 或 house_rental_ai_final.sql
README.md
DEPLOYMENT.md
```

不建议放入最终交付包的内容：

```text
node_modules/
target/
.idea/
.vscode/
__pycache__/
uploads/
*.iml
*.log
```

说明：

1. `node_modules` 是前端依赖目录，体积大，并且可能包含当前电脑平台相关的二进制依赖，换电脑后容易报错；
2. `target` 是后端编译产物，可以通过 Maven 重新生成；
3. `.idea` 是 IDEA 本地配置，不属于项目核心代码；
4. `uploads` 是运行时上传文件目录，正式交付时可以不打包，除非需要演示已有图片和合同附件。

如果前端目录中已经存在从其他电脑复制来的 `node_modules`，并且启动或构建报错，建议先删除该目录，再重新安装依赖：

```bash
cd house-rental-web
rmdir /s /q node_modules
npm install
```

Mac / Linux 可使用：

```bash
cd house-rental-web
rm -rf node_modules
npm install
```

---

## 四、数据库部署

### 1. 创建数据库

打开 MySQL 客户端或 Navicat，创建数据库：

```sql
CREATE DATABASE house_rental DEFAULT CHARACTER SET utf8mb4;
```

### 2. 导入 SQL 脚本

将当前项目中的数据库脚本导入 `house_rental` 数据库。常见文件名可能为：

```text
house_rental.sql
house_rental_ai_final.sql
house_rental(3).sql
```

如果有 `house_rental_ai_final.sql`，优先使用最终 AI 版本；如果没有，就导入当前最新的 `house_rental.sql` 或本次导出的 SQL 文件。

Navicat 导入方式：

```text
右键 house_rental 数据库 → 运行 SQL 文件 → 选择 SQL 脚本 → 开始
```

命令行导入方式示例：

```bash
mysql -u root -p house_rental < house_rental.sql
```

如果 SQL 文件名带括号，例如 `house_rental(3).sql`，命令行中需要加引号：

```bash
mysql -u root -p house_rental < "house_rental(3).sql"
```

### 3. 检查基础业务表

导入完成后执行：

```sql
SHOW TABLES;
```

至少应包含以下基础业务表：

```text
sys_user
house
appointment
rental_application
lease_contract
lease_order
repair_request
complaint
notice
```

### 4. 检查智能推荐相关表

如果要使用智能推荐和模型评价功能，还应包含以下表：

```text
recommend_record
recommend_eval_query
recommend_eval_label
```

可以执行以下 SQL 检查数据是否存在：

```sql
SELECT COUNT(*) AS 房源数量 FROM house;
SELECT COUNT(*) AS 推荐查询数量 FROM recommend_eval_query;
SELECT COUNT(*) AS 人工标注数量 FROM recommend_eval_label;
SELECT COUNT(*) AS 推荐记录数量 FROM recommend_record;
```

### 5. 检查测试账号

可以执行：

```sql
SELECT id, username, nickname, role, status FROM sys_user;
```

当前 SQL 中常用测试账号如下，默认密码一般为 `123456`：

| 角色 | 账号 | 说明 |
|---|---|---|
| 管理员 | `admin` | 管理员账号 |
| 出租者 | `landlord1` | 出租者账号 |
| 租客 | `tenant1` | 租客账号 |
| 租客 | `tenant2` | 租客账号 |
| 租客 | `tenant3` | 租客账号 |

注意：如果某些账号密码已经被 BCrypt 加密，仍然使用默认密码 `123456` 登录；如果 SQL 中存在明文密码，系统登录后可能会自动升级为加密密码，最终提交前建议统一整理为加密密码。

---

## 五、Redis 启动

本项目登录和鉴权依赖 Redis 保存 Token。默认配置为：

```text
host: localhost
port: 6379
database: 0
password: 空
```

如果 Redis 未启动，可能出现以下问题：

```text
登录失败
登录后跳回登录页
Token 校验失败
接口返回 401
```

### 1. Windows 启动示例

进入 Redis 安装目录后执行：

```bash
redis-server.exe redis.windows.conf
```

如果没有配置文件，也可以尝试：

```bash
redis-server.exe
```

### 2. Mac / Linux 启动示例

```bash
redis-server
```

或者使用系统服务：

```bash
sudo systemctl start redis
```

### 3. 验证 Redis

```bash
redis-cli ping
```

正常应返回：

```text
PONG
```

---

## 六、Python 语义推荐服务部署

EMBEDDING 中文语义向量推荐模型依赖独立的 Python 服务。该服务只负责语义相似度计算，不直接访问 MySQL，也不保存推荐记录。

### 1. 进入目录

```bash
cd ai-recommend-service
```

### 2. 确认 Python 版本

```bash
python --version
```

建议使用：

```text
Python 3.10
```

### 3. 安装依赖

```bash
python -m pip install -r requirements.txt
```

如果下载较慢，可以使用清华镜像源：

```bash
python -m pip install -r requirements.txt -i https://pypi.tuna.tsinghua.edu.cn/simple
```

当前 `requirements.txt` 包含：

```text
fastapi
uvicorn
sentence-transformers
pydantic
```

### 4. 模型离线加载说明

当前 Python 服务默认使用模型：

```text
shibing624/text2vec-base-chinese
```

`main.py` 会优先从本地 Hugging Face 缓存中加载模型，并启用离线加载。这样可以避免每次启动时联网检查，但也意味着新电脑第一次运行时，如果本地没有模型缓存，可能会启动失败。

如果启动时报模型找不到，可以手动指定本地模型目录。Windows PowerShell 示例：

```powershell
$env:SENTENCE_TRANSFORMER_MODEL_PATH="C:\Users\你的用户名\.cache\huggingface\hub\models--shibing624--text2vec-base-chinese\snapshots\具体目录"
```

然后重新启动 Python 服务。

### 5. 启动 Python 服务

```bash
python -m uvicorn main:app --host 0.0.0.0 --port 9000
```

启动成功后，控制台一般会出现：

```text
Application startup complete.
Uvicorn running on http://0.0.0.0:9000
```

注意：`0.0.0.0` 是服务监听地址，不是浏览器访问地址。浏览器测试时应访问 `localhost` 或 `127.0.0.1`。

### 6. 验证 Python 服务

浏览器访问：

```text
http://localhost:9000/health
```

正常返回类似：

```json
{
  "status": "ok",
  "model": "shibing624/text2vec-base-chinese",
  "offline": true
}
```

### 7. 语义推荐接口

接口地址：

```text
POST http://localhost:9000/semantic-recommend
```

请求示例：

```json
{
  "query": "我想找安静一点，适合考研复习的房子",
  "topK": 3,
  "houses": [
    {
      "id": 1,
      "text": "AI测试-考研安静一室一厅 聊城 东昌府区 学府路 一室一厅 1500元 小区环境安静 适合考研 学习 休息"
    },
    {
      "id": 2,
      "text": "AI测试-交通便利两室 聊城 东昌府区 两室一厅 1900元 靠近公交站 交通方便 通勤便利"
    }
  ]
}
```

返回示例：

```json
[
  {
    "houseId": 1,
    "score": 0.662887
  },
  {
    "houseId": 2,
    "score": 0.347725
  }
]
```

### 8. 端口变更

如果 9000 端口被占用，可以改用 9001：

```bash
python -m uvicorn main:app --host 0.0.0.0 --port 9001
```

同时需要修改后端 `application.yml`：

```yaml
ai:
  recommend:
    embedding-url: http://localhost:9001/semantic-recommend
```

---

## 七、DeepSeek API Key 配置

系统接入 DeepSeek API，用于解析用户自然语言租房需求。该配置不是必须项，如果未配置 API Key，系统会自动回退到本地规则解析。

### 1. 环境变量名称

```text
DEEPSEEK_API_KEY
```

### 2. IntelliJ IDEA 配置方式

如果使用 IDEA 启动后端：

```text
Run/Debug Configurations
→ 选择 HouseRentalServerApplication
→ Environment variables
→ 添加 DEEPSEEK_API_KEY=你的Key
```

注意：

```text
不要加引号
不要写空格
不要写进代码
不要提交到 Git
不要写入公开文档
不要在截图中展示完整 Key
```

### 3. PowerShell 临时配置方式

```powershell
$env:DEEPSEEK_API_KEY="你的DeepSeek API Key"
```

然后在同一个 PowerShell 终端中启动后端。

### 4. CMD 临时配置方式

```cmd
set DEEPSEEK_API_KEY=你的DeepSeek API Key
```

然后在同一个 CMD 窗口中启动后端。

### 5. 未配置 API Key 的表现

调用：

```text
POST http://localhost:8080/recommend/parse-demand
```

如果 DeepSeek 调用成功，返回中的 `parseType` 通常为：

```text
LLM_PARSE
```

如果未配置 API Key、Key 无效、余额不足、网络异常或接口超时，系统会回退为：

```text
RULE_PARSE
```

这是正常兜底逻辑，不代表推荐功能完全不可用。

---

## 八、Spring Boot 后端部署

### 1. 进入后端目录

```bash
cd house-rental-server
```

### 2. 检查配置文件

打开：

```text
src/main/resources/application.yml
```

当前项目主要配置如下：

```yaml
server:
  port: ${SERVER_PORT:8080}

spring:
  datasource:
    url: ${DB_URL:jdbc:mysql://localhost:3306/house_rental?useUnicode=true&characterEncoding=utf-8&serverTimezone=Asia/Shanghai}
    username: ${DB_USERNAME:root}
    password: ${DB_PASSWORD:root}

  data:
    redis:
      host: ${REDIS_HOST:localhost}
      port: ${REDIS_PORT:6379}
      password: ${REDIS_PASSWORD:}
      database: ${REDIS_DATABASE:0}

app:
  upload:
    base-dir: ${UPLOAD_BASE_DIR:./uploads}

ai:
  deepseek:
    base-url: https://api.deepseek.com
    model: deepseek-v4-flash
    api-key: ${DEEPSEEK_API_KEY:}
  recommend:
    embedding-url: http://localhost:9000/semantic-recommend
```

如果本机 MySQL 用户名或密码不是 `root/root`，需要修改 `application.yml` 或配置环境变量。

### 3. 常用环境变量

| 环境变量 | 说明 | 默认值 |
|---|---|---|
| `SERVER_PORT` | 后端端口 | `8080` |
| `DB_URL` | MySQL 连接地址 | `jdbc:mysql://localhost:3306/house_rental...` |
| `DB_USERNAME` | MySQL 用户名 | `root` |
| `DB_PASSWORD` | MySQL 密码 | `root` |
| `REDIS_HOST` | Redis 地址 | `localhost` |
| `REDIS_PORT` | Redis 端口 | `6379` |
| `REDIS_PASSWORD` | Redis 密码 | 空 |
| `REDIS_DATABASE` | Redis 数据库编号 | `0` |
| `UPLOAD_BASE_DIR` | 文件上传保存目录 | `./uploads` |
| `DEEPSEEK_API_KEY` | DeepSeek API Key | 空 |

### 4. 命令行启动后端

```bash
mvn spring-boot:run
```

启动成功后，控制台应出现类似：

```text
Tomcat started on port 8080
Started HouseRentalServerApplication
```

后端地址：

```text
http://localhost:8080
```

### 5. IDEA 启动后端

如果使用 IntelliJ IDEA：

```text
打开 house-rental-server
等待 Maven 依赖加载完成
找到 HouseRentalServerApplication
点击 Run
```

如果 Maven 下载慢，可以在 IDEA 或 Maven 配置中使用国内镜像。

### 6. 后端 Swagger 页面

项目引入了 `springdoc-openapi-starter-webmvc-ui`，后端启动后可以尝试访问：

```text
http://localhost:8080/swagger-ui/index.html
```

如果页面能打开，可以辅助查看和测试接口。

---

## 九、Vue 前端部署

### 1. 进入前端目录

```bash
cd house-rental-web
```

### 2. 安装依赖

```bash
npm install
```

如果之前复制过旧的 `node_modules`，建议先删除再安装。

### 3. 启动前端

```bash
npm run dev
```

启动成功后，一般会显示：

```text
Local: http://localhost:5173/
```

浏览器访问：

```text
http://localhost:5173
```

### 4. 前端代理说明

当前 `vite.config.js` 已配置代理：

```text
/api      → http://localhost:8080
/uploads  → http://localhost:8080
```

因此开发环境中，前端通过 `/api` 调用后端接口，通过 `/uploads` 访问后端上传的图片和合同附件。

### 5. 前端构建

如果需要生成前端静态文件：

```bash
npm run build
```

生成目录一般为：

```text
dist/
```

答辩本地演示通常使用 `npm run dev` 即可，不一定需要构建。

---

## 十、文件上传说明

后端默认上传目录为：

```text
house-rental-server/uploads
```

实际配置项为：

```yaml
app:
  upload:
    base-dir: ${UPLOAD_BASE_DIR:./uploads}
```

常见上传路径：

```text
uploads/images/日期/图片文件
uploads/contracts/日期/合同附件
```

浏览器访问地址通常为：

```text
http://localhost:8080/uploads/images/日期/文件名
http://localhost:8080/uploads/contracts/日期/文件名
```

前端开发环境中可以直接使用：

```text
/uploads/images/日期/文件名
/uploads/contracts/日期/文件名
```

因为 Vite 已经把 `/uploads` 代理到后端。

如果上传成功但浏览器不显示，优先检查：

```text
后端是否正常启动
数据库中保存的 URL 是否以 /uploads 开头
后端是否配置了静态资源映射
文件是否真实存在于 uploads 目录
前端是否通过 /uploads 路径访问
```

---

## 十一、支付功能说明

当前支付功能为演示模式，不是真实微信或支付宝商户支付。

默认配置：

```yaml
app:
  payment:
    demo-mode: ${PAYMENT_DEMO_MODE:true}
    app-name: ${PAYMENT_APP_NAME:house-rental-system}
```

当前流程：

```text
租客进入订单支付页面
→ 选择微信或支付宝
→ 系统展示演示二维码或演示收银台提示
→ 点击模拟支付成功
→ 系统更新订单状态、合同状态和房源状态
```

如果后续接入真实支付，需要补充：

```text
商户参数配置
统一下单接口
支付回调通知
签名验签
幂等处理
支付取消和失败处理
```

答辩时建议说明：支付模块主要用于验证租赁业务状态流转，不涉及真实资金结算。

---

## 十二、核心功能验证顺序

### 1. 基础启动验证

按顺序完成：

```text
1. MySQL 启动成功
2. Redis 返回 PONG
3. Python 服务 /health 返回 status=ok
4. 后端启动成功，端口 8080 正常
5. 前端启动成功，端口 5173 正常
```

### 2. 登录验证

打开：

```text
http://localhost:5173
```

分别登录：

```text
管理员：admin / 123456
出租者：landlord1 / 123456
租客：tenant1 / 123456
```

如果登录后立即跳回登录页，优先检查 Redis 是否启动。

### 3. 管理员端验证

```text
登录管理员
→ 查看数据总览
→ 用户管理启用/禁用
→ 房源审核
→ 投诉处理
→ 公告管理
→ 模型评价
```

模型评价建议分别测试：

```text
RULE
TFIDF
EMBEDDING
```

其中 EMBEDDING 需要 Python 服务正常运行。

### 4. 出租者端验证

```text
登录 landlord1
→ 发布房源
→ 上传房源图片
→ 编辑房源
→ 上架 / 下架房源
→ 查看预约
→ 审批租房申请
→ 查看合同
→ 上传合同附件
→ 处理报修
```

### 5. 租客端验证

```text
登录 tenant1
→ 浏览房源
→ 查看房源详情
→ 使用智能找房
→ 提交预约
→ 提交租房申请
→ 查看订单
→ 模拟支付
→ 查看合同
→ 提交报修
→ 提交投诉
```

### 6. 智能推荐接口验证

#### DeepSeek / 本地规则解析接口

```text
POST http://localhost:8080/recommend/parse-demand
```

请求体：

```json
{
  "query": "我想找一套1500元以内，靠近学校，适合考研复习的一室一厅"
}
```

观察返回中的：

```text
parseType
```

如果为 `LLM_PARSE`，说明 DeepSeek 调用成功；如果为 `RULE_PARSE`，说明回退到了本地规则解析。

#### 智能推荐接口

```text
POST http://localhost:8080/recommend/house
```

请求体：

```json
{
  "query": "我想找一套1500元以内，靠近学校，适合考研复习的一室一厅",
  "modelType": "EMBEDDING",
  "topK": 5
}
```

`modelType` 可选：

```text
RULE
TFIDF
EMBEDDING
```

---

## 十三、常见问题排查

### 1. 数据库连接失败

检查：

```text
MySQL 是否启动
数据库名是否为 house_rental
用户名和密码是否正确
3306 端口是否被占用
数据库字符集是否为 utf8mb4
application.yml 或环境变量是否配置正确
```

### 2. Redis 连接失败

检查：

```text
Redis 是否启动
6379 端口是否可用
是否设置了密码
application.yml 中 Redis 配置是否正确
redis-cli ping 是否返回 PONG
```

### 3. 登录后跳回登录页

通常与 Token 或 Redis 有关，检查：

```text
Redis 是否正常运行
浏览器 localStorage 是否保存 token / userInfo
后端接口是否返回 401
当前账号 status 是否为 1
```

### 4. 前端请求后端失败

检查：

```text
后端是否启动在 8080 端口
前端是否启动在 5173 端口
vite.config.js 代理是否存在
浏览器控制台 Network 中接口是否为 401 / 403 / 404 / 500
Axios baseURL 是否为 /api
```

### 5. 前端依赖安装或构建失败

如果出现 Vite、Rollup、esbuild、oxlint 等依赖错误，优先检查 Node.js 版本。当前项目建议使用：

```text
Node.js 20.19+ 或 22.12+
```

然后删除旧依赖重新安装：

```bash
cd house-rental-web
rmdir /s /q node_modules
npm install
```

Mac / Linux：

```bash
cd house-rental-web
rm -rf node_modules
npm install
```

### 6. Python 服务无法启动

检查：

```text
Python 是否为 3.10
是否安装 requirements.txt
9000 端口是否被占用
本地是否存在 text2vec-base-chinese 模型缓存
是否需要设置 SENTENCE_TRANSFORMER_MODEL_PATH
```

### 7. EMBEDDING 推荐报错

先访问：

```text
http://localhost:9000/health
```

如果无法访问，说明 Python 服务未启动或模型加载失败。

如果 Python 服务改了端口，需要同步修改后端：

```yaml
ai:
  recommend:
    embedding-url: http://localhost:新端口/semantic-recommend
```

### 8. DeepSeek 未生效

如果返回 `RULE_PARSE`，可能原因是：

```text
未配置 DEEPSEEK_API_KEY
API Key 无效
账户余额不足
网络异常
接口超时
```

这是正常兜底逻辑，系统仍然可以使用本地规则解析完成推荐。

### 9. 模型评价页面超时

检查：

```text
是否选择了 EMBEDDING 模型
Python 服务是否正常
后端是否正常
评价数据是否较多
前端 Axios 超时时间是否足够
电脑性能是否较低
```

### 10. 上传图片或合同附件失败

检查：

```text
当前账号角色是否正确
文件大小是否超过限制
文件格式是否支持
后端 uploads 目录是否有写入权限
数据库是否保存了正确的文件 URL
```

### 11. 浏览器控制台出现插件 404

如果控制台出现浏览器插件相关 404，例如某些插件脚本请求失败，一般不是项目本身问题。应以项目接口请求是否成功、页面功能是否正常为准。

---

## 十四、答辩演示建议顺序

建议按以下顺序演示：

```text
1. 介绍系统背景、三类角色和核心业务流程
2. 管理员登录，展示数据总览、用户管理、房源审核、投诉处理、公告管理
3. 出租者登录，展示房源发布、图片上传、申请审批、合同管理、报修处理
4. 租客登录，展示房源浏览、智能找房、预约、申请、支付演示、合同查看、报修和投诉
5. 展示 DeepSeek 需求解析，如果未配置 Key，则说明系统会自动回退到本地规则解析
6. 展示 RULE、TFIDF、EMBEDDING 三种推荐模型
7. 展示管理员端模型评价，说明 Precision@K、Recall@K、F1@K、NDCG@K 和平均响应时间
8. 总结系统创新点和后续优化方向
```

答辩时可以重点强调：

```text
系统完成了从房源发布、审核、申请、合同、订单、支付演示到售后处理的完整租赁闭环；
智能推荐模块支持三种推荐模型，并通过模型评价页面对推荐效果进行量化分析；
Python 语义推荐服务与 Spring Boot 后端解耦，便于后续扩展更复杂的推荐模型。
```

---

## 十五、最终提交前检查清单

提交或答辩前建议逐项确认：

| 检查项 | 是否完成 |
|---|---|
| MySQL 可正常启动 |  |
| Redis 可正常启动 |  |
| 数据库脚本可重新导入 |  |
| 后端可正常启动 |  |
| 前端可正常启动 |  |
| Python 推荐服务 `/health` 正常 |  |
| 管理员账号可登录 |  |
| 出租者账号可登录 |  |
| 租客账号可登录 |  |
| 房源图片可上传并显示 |  |
| 合同附件可上传并访问 |  |
| 租房申请通过后可生成合同和订单 |  |
| 模拟支付后订单、合同、房源状态正确变化 |  |
| RULE 推荐可用 |  |
| TFIDF 推荐可用 |  |
| EMBEDDING 推荐可用 |  |
| 模型评价页面可计算指标 |  |
| README 和部署文档已更新 |  |
| 交付包已删除 `node_modules`、`target`、`.idea`、`uploads` 等非必要目录 |  |
| DeepSeek API Key 未写入代码、文档或截图 |  |

---

## 十六、最终启动命令汇总

### 1. 启动 MySQL

根据本机安装方式启动 MySQL 服务。

### 2. 启动 Redis

```bash
redis-server
```

验证：

```bash
redis-cli ping
```

### 3. 启动 Python 推荐服务

```bash
cd ai-recommend-service
python -m uvicorn main:app --host 0.0.0.0 --port 9000
```

验证：

```text
http://localhost:9000/health
```

### 4. 启动后端

```bash
cd house-rental-server
mvn spring-boot:run
```

或使用 IDEA 运行：

```text
HouseRentalServerApplication
```

### 5. 启动前端

```bash
cd house-rental-web
npm install
npm run dev
```

访问：

```text
http://localhost:5173
```

---

## 十七、补充说明

1. 本项目当前更适合本地答辩演示和课程设计验收；如果部署到云服务器，还需要额外配置 Nginx、域名、防火墙、安全组、后端生产配置、前端静态资源部署和 HTTPS。
2. 当前支付为演示模式，不涉及真实资金交易。
3. 当前 DeepSeek API Key 建议只通过环境变量配置，不要写入任何代码或文档。
4. 当前 EMBEDDING 推荐依赖本地 Python 服务和本地模型缓存，新电脑部署时需要特别检查模型是否已经下载。
5. 当前前端建议使用 Node.js 20.19+ 或 22.12+，不要继续按旧文档使用 Node.js 18。
