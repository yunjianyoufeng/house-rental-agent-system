# AI Recommend Service

> 旧版可选服务：该服务加载本地神经网络模型，内存较小的电脑无需启动。当前智能助手位于 `agent-service`，请参阅根目录 `README.md`。

本目录是房屋租赁管理系统的 Python 语义推荐服务，用于支持后端的 EMBEDDING 中文语义向量推荐模型。

Spring Boot 后端在进行 EMBEDDING 推荐时，会把用户租房需求和候选房源文本发送到本服务，本服务计算语义相似度后返回房源 ID 和相似度分数。

---

## 一、服务作用

本服务主要完成以下工作：

```text
用户租房需求
        ↓
中文语义向量模型编码
        ↓
房源文本向量化
        ↓
计算余弦相似度
        ↓
返回 Top-K 推荐房源 ID 和分数
```

该服务不直接访问数据库，只负责语义相似度计算。数据库查询、房源筛选、推荐记录保存等逻辑仍由 Spring Boot 后端完成。

---

## 二、运行环境

推荐环境：

| 环境 | 版本 |
|---|---|
| Python | 3.10 |
| FastAPI | requirements.txt 中指定 |
| Uvicorn | requirements.txt 中指定 |
| sentence-transformers | requirements.txt 中指定 |

确认 Python 版本：

```bash
python --version
```

---

## 三、目录结构

```text
ai-recommend-service/
├── main.py              FastAPI 服务入口
├── requirements.txt     Python 依赖文件
└── README.md            当前说明文档
```

---

## 四、安装依赖

进入当前目录：

```bash
cd ai-recommend-service
```

安装依赖：

```bash
python -m pip install -r requirements.txt
```

如果下载较慢，可以使用国内镜像源：

```bash
python -m pip install -r requirements.txt -i https://pypi.tuna.tsinghua.edu.cn/simple
```

---

## 五、启动服务

启动命令：

```bash
python -m uvicorn main:app --host 0.0.0.0 --port 9000
```

启动成功后，控制台会显示类似：

```text
Application startup complete.
Uvicorn running on http://0.0.0.0:9000
```

注意：服务运行期间不要关闭该终端。

---

## 六、健康检查接口

浏览器访问：

```text
http://localhost:9000/health
```

正常返回示例：

```json
{
  "status": "ok",
  "model": "shibing624/text2vec-base-chinese",
  "offline": true
}
```

如果可以正常返回，说明 Python 语义推荐服务已经启动成功。

---

## 七、语义推荐接口

接口地址：

```text
POST http://localhost:9000/semantic-recommend
```

请求体示例：

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
    },
    {
      "id": 3,
      "text": "AI测试-家庭两室一厅 聊城 东昌府区 两室一厅 2200元 适合一家人居住 生活配套齐全"
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
    "houseId": 3,
    "score": 0.46985
  },
  {
    "houseId": 2,
    "score": 0.347725
  }
]
```

字段说明：

| 字段 | 说明 |
|---|---|
| `houseId` | 房源 ID |
| `score` | 语义相似度分数，越高表示越匹配 |

---

## 八、模型说明

当前默认使用模型：

```text
shibing624/text2vec-base-chinese
```

该模型用于将中文租房需求和房源文本转换为向量，再通过余弦相似度计算两者之间的语义匹配程度。

在本项目中，Spring Boot 后端会拼接房源文本，主要包括：

```text
房源标题 + 城市 + 区域 + 地址 + 户型 + 面积 + 租金 + 楼层 + 房源描述
```

然后传递给 Python 服务进行语义相似度计算。

---

## 九、离线加载说明

为了避免每次启动服务都连接 Hugging Face，本服务建议使用离线加载方式。

当前 `main.py` 会优先从本地模型缓存中读取模型。如果模型已经下载过，通常可以直接启动，不需要重新联网下载。

如果启动时提示找不到模型，可以手动指定本地模型目录。

Windows 下模型缓存目录通常类似：

```text
C:\Users\你的用户名\.cache\huggingface\hub\models--shibing624--text2vec-base-chinese\snapshots\xxxx
```

可以通过环境变量指定：

```powershell
$env:SENTENCE_TRANSFORMER_MODEL_PATH="你的本地模型 snapshots 目录"
```

然后重新启动服务：

```bash
python -m uvicorn main:app --host 0.0.0.0 --port 9000
```

---

## 十、与 Spring Boot 后端的关系

Spring Boot 后端通过以下配置调用本服务：

```yaml
ai:
  recommend:
    embedding-url: http://localhost:9000/semantic-recommend
```

因此，如果本服务端口不是 `9000`，需要同步修改后端 `application.yml` 中的配置。

例如 Python 服务改为 `9001`：

```bash
python -m uvicorn main:app --host 0.0.0.0 --port 9001
```

则后端配置也要改为：

```yaml
ai:
  recommend:
    embedding-url: http://localhost:9001/semantic-recommend
```

---

## 十一、常见问题

### 1. 启动时报 Hugging Face 连接超时

说明模型仍在尝试联网检查或下载。

解决方式：

1. 确认模型已经下载完成；
2. 使用离线加载版本的 `main.py`；
3. 必要时设置 `SENTENCE_TRANSFORMER_MODEL_PATH` 指向本地模型目录。

---

### 2. 浏览器访问 `0.0.0.0:9000` 失败

`0.0.0.0` 是服务监听地址，不是浏览器访问地址。

浏览器应访问：

```text
http://localhost:9000/health
```

或：

```text
http://127.0.0.1:9000/health
```

---

### 3. 端口 9000 被占用

可以换端口启动：

```bash
python -m uvicorn main:app --host 0.0.0.0 --port 9001
```

同时修改 Spring Boot 后端配置：

```yaml
ai:
  recommend:
    embedding-url: http://localhost:9001/semantic-recommend
```

---

### 4. Spring Boot 调用 EMBEDDING 推荐失败

先确认本服务是否正常：

```text
http://localhost:9000/health
```

如果该地址无法访问，说明 Python 服务未启动或端口不可用。

---

### 5. 第一次启动很慢

第一次启动可能需要加载模型文件，耗时较长是正常现象。

如果模型已经下载完成，后续启动会明显更快。

---

### 6. IDEA 中 Python 文件有红线

如果终端可以正常运行服务，但 IDEA 中显示依赖红线，通常是 IDEA 没有配置 Python 解释器。

解决方式：

1. 打开 IDEA 的 Python Interpreter 配置；
2. 选择本机 Python 3.10；
3. 确认依赖安装在该 Python 环境中。

这类红线不一定影响服务运行。

---

## 十二、启动顺序提醒

完整项目运行顺序建议为：

```text
1. 启动 MySQL
2. 启动 Redis
3. 启动 Python 语义推荐服务
4. 访问 http://localhost:9000/health 确认服务正常
5. 启动 Spring Boot 后端
6. 启动 Vue 前端
```

如果只测试 RULE 或 TFIDF 模型，可以不启动 Python 服务。

如果要测试 EMBEDDING 模型，必须启动 Python 服务。

---

## 十三、测试建议

启动服务后，建议依次测试：

1. 浏览器访问 `/health`；
2. Apifox 测试 `/semantic-recommend`；
3. Spring Boot 测试 `/recommend/house`，`modelType` 使用 `EMBEDDING`；
4. 管理员端模型评价页面选择 `EMBEDDING` 进行测试。

---

## 十四、注意事项

- 本服务不直接连接 MySQL；
- 本服务不保存推荐记录；
- 本服务只负责语义相似度计算；
- 推荐记录和业务数据仍由 Spring Boot 后端管理；
- 不要关闭运行 Python 服务的终端；
- 如果更换模型或端口，需要同步修改后端配置。
