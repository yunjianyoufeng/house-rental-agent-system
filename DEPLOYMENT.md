# 房屋租赁管理系统部署文档

> 旧版资料：本文主要描述早期 `ai-recommend-service` 语义推荐架构。当前低内存 Agent 版本请以根目录 `README.md` 和 `agent-service/README.md` 为准。

本文档用于指导你在本地电脑或新电脑上完整运行本项目。

当前项目包含三个主要部分：

```text
house-rental-server       Spring Boot 后端服务
house-rental-web          Vue3 前端服务
ai-recommend-service      Python 语义推荐服务
```

完整功能运行时需要依次启动：

```text
1. MySQL
2. Redis
3. Python 语义推荐服务
4. Spring Boot 后端服务
5. Vue 前端服务
```

---

## 一、软件环境准备

请先确认本机已安装以下软件：

| 软件 | 推荐版本 | 用途 |
|---|---|---|
| JDK | 17 | 运行 Spring Boot 后端 |
| Maven | 3.9+ | 后端依赖管理与启动 |
| Node.js | 18+ | 前端运行环境 |
| npm | 9+ | 前端依赖管理 |
| MySQL | 8.x | 数据库存储 |
| Redis | 6.x / 7.x | 登录 Token 缓存 |
| Python | 3.10 | 语义推荐服务 |
| Navicat | 可选 | 数据库管理 |
| Apifox | 可选 | 接口测试 |

可以在命令行检查：

```bash
java -version
mvn -v
node -v
npm -v
python --version
```

如果本机无法识别 `mvn`，也可以直接使用 IntelliJ IDEA 运行后端启动类。

---

## 二、数据库部署

### 1. 创建数据库

```sql
CREATE DATABASE house_rental DEFAULT CHARACTER SET utf8mb4;
```

### 2. 导入 SQL

将项目根目录中的数据库脚本导入到 `house_rental` 数据库。

如果是最终智能推荐版本，建议导入：

```text
house_rental_ai_final.sql
```

如果没有该文件，也可以导入当前最新的：

```text
house_rental.sql
```

### 3. 检查核心业务表

导入成功后，确认以下基础表存在：

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

如果需要使用模型评价功能，还需要确认以下表存在：

```text
recommend_record
recommend_eval_query
recommend_eval_label
```

可以执行：

```sql
SHOW TABLES;
```

也可以执行以下 SQL 检查推荐实验数据：

```sql
SELECT scene_type, COUNT(*) AS 查询数量
FROM recommend_eval_query
GROUP BY scene_type;
```

```sql
SELECT COUNT(*) AS 标注数量
FROM recommend_eval_label;
```

---

## 三、Redis 启动

本项目登录和鉴权依赖 Redis 存储 Token。

默认配置：

```text
host: localhost
port: 6379
db: 0
```

请先启动 Redis，并确认端口 `6379` 未被占用。

如果 Redis 未启动，可能会出现：

- 登录失败；
- 登录后跳回登录页；
- Token 校验失败；
- 接口返回 401。

---

## 四、Python 语义推荐服务部署

EMBEDDING 中文语义向量推荐模型依赖独立的 Python 服务。

### 1. 进入 Python 服务目录

```bash
cd ai-recommend-service
```

### 2. 安装依赖

```bash
python -m pip install -r requirements.txt
```

如果下载速度较慢，可以使用国内镜像源：

```bash
python -m pip install -r requirements.txt -i https://pypi.tuna.tsinghua.edu.cn/simple
```

### 3. 启动 Python 服务

```bash
python -m uvicorn main:app --host 0.0.0.0 --port 9000
```

启动成功后，终端应显示类似：

```text
Application startup complete.
Uvicorn running on http://0.0.0.0:9000
```

注意：服务运行期间不要关闭该终端。

### 4. 验证 Python 服务

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

如果该接口可以访问，说明 Python 语义推荐服务启动成功。

### 5. Python 服务接口说明

语义推荐接口：

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

---

## 五、DeepSeek API Key 配置

系统接入 DeepSeek API，用于解析用户自然语言租房需求。

### 1. 环境变量名称

```text
DEEPSEEK_API_KEY
```

### 2. IntelliJ IDEA 中配置方式

如果使用 IDEA 启动后端：

1. 打开 `Run/Debug Configurations`；
2. 选择 `HouseRentalServerApplication`；
3. 找到 `Environment variables`；
4. 添加：

```text
DEEPSEEK_API_KEY=你的DeepSeek API Key
```

注意：

- 不要加引号；
- 不要写空格；
- 不要写进代码；
- 不要提交到 Git；
- 不要写入 README 或公开文档。

### 3. PowerShell 临时配置方式

如果通过 PowerShell 启动后端，可以执行：

```powershell
$env:DEEPSEEK_API_KEY="你的DeepSeek API Key"
```

然后在同一个终端启动后端。

### 4. 未配置 API Key 的情况

如果未配置 `DEEPSEEK_API_KEY`，系统不会报错，而是自动回退到本地规则解析。

此时 `/recommend/parse-demand` 返回结果中的：

```text
parseType
```

通常为：

```text
RULE_PARSE
```

如果 DeepSeek 调用成功，则为：

```text
LLM_PARSE
```

---

## 六、后端部署

### 1. 进入后端目录

```bash
cd house-rental-server
```

### 2. 检查配置文件

打开：

```text
src/main/resources/application.yml
```

关键配置包括：

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/house_rental
    username: root
    password: root

  data:
    redis:
      host: localhost
      port: 6379
      database: 0

ai:
  deepseek:
    base-url: https://api.deepseek.com
    model: deepseek-v4-flash
    api-key: ${DEEPSEEK_API_KEY:}
  recommend:
    embedding-url: http://localhost:9000/semantic-recommend
```

如果本机 MySQL 密码不是 `root`，请修改数据库密码。

### 3. 启动后端

方式一：命令行启动。

```bash
mvn spring-boot:run
```

方式二：使用 IntelliJ IDEA 启动。

运行启动类：

```text
HouseRentalServerApplication
```

### 4. 后端启动成功标志

控制台出现类似内容：

```text
Tomcat started on port 8080
Started HouseRentalServerApplication
```

后端默认地址：

```text
http://localhost:8080
```

---

## 七、前端部署

### 1. 进入前端目录

```bash
cd house-rental-web
```

### 2. 安装依赖

```bash
npm install
```

### 3. 启动前端

```bash
npm run dev
```

### 4. 前端访问地址

通常为：

```text
http://localhost:5173
```

Vite 已配置代理：

```text
/api      → http://localhost:8080
/uploads  → http://localhost:8080
```

所以开发环境下接口、图片和合同附件可以正常访问。

---

## 八、默认测试账号

默认密码统一为：

```text
123456
```

测试账号如下：

| 角色 | 账号 |
|---|---|
| 管理员 | `admin` |
| 出租者 | `landlord1` |
| 出租者 | `landlord2` |
| 租客 | `tenant1` |
| 租客 | `tenant2` |
| 租客 | `tenant3` |

---

## 九、核心功能验证顺序

### 1. Python 服务验证

浏览器访问：

```text
http://localhost:9000/health
```

正常返回 `status = ok`。

---

### 2. DeepSeek 解析接口验证

接口：

```text
POST http://localhost:8080/recommend/parse-demand
```

请求体：

```json
{
  "query": "我想找一套1500元以内，靠近学校，适合考研复习的一室一厅"
}
```

如果 DeepSeek 调用成功，应看到：

```json
"parseType": "LLM_PARSE"
```

如果未配置 API Key 或调用失败，会看到：

```json
"parseType": "RULE_PARSE"
```

这是正常兜底逻辑。

---

### 3. 智能推荐接口验证

接口：

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

正常情况下返回推荐房源、推荐分数和推荐原因。

如果使用 `EMBEDDING` 模型，需要保证 Python 服务已启动。

---

### 4. 管理员模型评价验证

登录管理员账号，进入：

```text
管理员首页 → 模型评价
```

分别选择：

```text
RULE
TFIDF
EMBEDDING
```

点击“计算评价指标”。

正常应显示：

- 查询数量；
- Precision@K；
- Recall@K；
- F1@K；
- NDCG@K；
- 平均响应时间；
- 评价结果明细。

注意：模型评价时默认关闭 DeepSeek 解析，使用本地规则解析，避免大量调用 DeepSeek API 导致超时或产生额外费用。

---

## 十、业务功能验证顺序

### 1. 管理员验证

- 登录管理员；
- 查看数据总览；
- 打开用户管理，尝试启用 / 禁用用户；
- 打开房源审核，审核待审房源；
- 打开投诉管理，处理投诉；
- 打开公告管理，发布公告；
- 打开模型评价，测试三种推荐模型。

### 2. 出租者验证

- 登录出租者；
- 发布房源；
- 上传房源图片；
- 编辑房源；
- 上架 / 下架房源；
- 查看预约管理；
- 审批租房申请；
- 在合同管理上传附件或结束合同；
- 在报修处理中处理租客报修。

### 3. 租客验证

- 登录租客；
- 浏览房源列表；
- 查看房源详情；
- 使用智能找房；
- 提交预约；
- 提交租房申请；
- 打开我的订单进行支付演示；
- 查看我的合同；
- 提交报修；
- 提交投诉。

---

## 十一、文件上传说明

### 1. 上传目录

后端默认上传目录为：

```text
./uploads
```

会按类型和日期生成子目录，例如：

```text
uploads/images/20260418/
uploads/contracts/20260418/
```

### 2. 图片访问

上传后的图片地址类似：

```text
/uploads/images/20260418/xxxxxx.png
```

### 3. 合同附件访问

上传后的合同附件地址类似：

```text
/uploads/contracts/20260418/xxxxxx.pdf
```

### 4. 上传失败排查

检查：

- 当前角色是否具备上传权限；
- 文件大小是否超限；
- 文件格式是否支持；
- 项目目录是否有写入权限；
- 后端是否正常启动。

---

## 十二、支付功能说明

当前支付为演示模式，不是真实微信 / 支付宝商户接入。

当前流程：

1. 租客进入订单支付页面；
2. 选择微信或支付宝；
3. 系统展示演示二维码或演示收银台；
4. 点击“模拟支付成功”；
5. 系统更新订单、合同和房源状态。

如果后续接入真实支付，需要补充：

- 商户参数；
- 统一下单；
- 支付回调；
- 签名验签；
- 幂等处理；
- 取消和失败处理。

---

## 十三、常见故障排查

### 1. 数据库连接失败

检查：

- MySQL 是否启动；
- 数据库名是否为 `house_rental`；
- 用户名密码是否正确；
- 3306 端口是否可用；
- 数据库字符集是否为 `utf8mb4`。

### 2. Redis 连接失败

检查：

- Redis 是否启动；
- host / port 是否正确；
- 是否设置了密码；
- 6379 端口是否可用。

### 3. 前端无法请求后端

检查：

- 后端是否在 8080 端口运行；
- `vite.config.js` 代理是否存在；
- 浏览器控制台是否有 401、403、500 报错；
- Axios baseURL 是否正确。

### 4. 登录后跳回登录页

检查：

- Redis 是否正常；
- token 是否已失效；
- `localStorage` 中的 `userInfo` 是否被清空；
- 后端鉴权接口是否返回 401。

### 5. Python 服务无法启动

检查：

- Python 是否为 3.10；
- 是否已执行 `pip install -r requirements.txt`；
- 模型文件是否已下载；
- 9000 端口是否被占用；
- 是否使用了离线加载版本的 `main.py`。

### 6. EMBEDDING 推荐报错

先访问：

```text
http://localhost:9000/health
```

如果无法访问，说明 Python 服务未启动。

重新启动：

```bash
python -m uvicorn main:app --host 0.0.0.0 --port 9000
```

### 7. DeepSeek 接口未生效

如果 `/recommend/parse-demand` 返回：

```text
RULE_PARSE
```

可能原因：

- 未配置 `DEEPSEEK_API_KEY`；
- API Key 无效；
- API Key 余额不足；
- DeepSeek 网络连接异常；
- DeepSeek 接口超时。

系统会自动回退到本地规则解析。

### 8. 模型评价页面超时

检查：

- 前端 Axios 超时时间是否已调大；
- Python 服务是否正常；
- 后端是否正常；
- 是否正在测试 EMBEDDING 模型；
- 网络是否稳定。

### 9. 浏览器控制台出现插件 404

如果控制台出现类似浏览器插件相关的 404，例如某些 `perplexity-results` 或插件脚本报错，一般不是项目本身接口问题。应以项目接口请求是否成功为准。

---

## 十四、交付建议

毕设提交或项目验收时建议打包内容包括：

```text
house-rental-server/
house-rental-web/
ai-recommend-service/
house_rental_ai_final.sql
README.md
DEPLOYMENT.md
```

如果没有 `house_rental_ai_final.sql`，请导出当前最新数据库 SQL，并确保其中包含：

- 基础业务表；
- 测试账号；
- 房源数据；
- AI 测试房源；
- 推荐记录表；
- 推荐实验查询表；
- 人工标注表。

---

## 十五、最终启动顺序总结

完整演示时建议按以下顺序启动：

```text
1. 启动 MySQL
2. 启动 Redis
3. 启动 Python 语义推荐服务
4. 访问 http://localhost:9000/health 确认正常
5. 启动 Spring Boot 后端
6. 启动 Vue 前端
7. 访问 http://localhost:5173
8. 登录管理员 / 出租者 / 租客账号进行测试
```

如果只测试基础租赁功能，可以不启动 Python 服务。

如果要测试 EMBEDDING 推荐模型或管理员模型评价中的 EMBEDDING 模型，必须启动 Python 服务。

---

## 十六、API Key 安全提醒

DeepSeek API Key 属于敏感信息。

请注意：

- 不要在截图中展示完整 Key；
- 不要把 Key 写入代码；
- 不要把 Key 写入文档；
- 不要提交到 Git；
- 如果 Key 曾经暴露，应立即删除并重新生成；
- 推荐只通过本地环境变量配置。
