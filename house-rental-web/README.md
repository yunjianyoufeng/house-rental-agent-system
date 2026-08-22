# House Rental Web

房屋租赁与智能决策系统的 Vue 3 前端，包含管理员、出租者、租客三个角色页面，以及租客端独立的智能租房助手页面。

## 环境

- Node.js 20.19+ 或 22.12+
- npm
- Spring Boot 后端默认运行在 `http://127.0.0.1:8080`

## 启动

```cmd
npm install
npm run dev
```

访问 `http://127.0.0.1:5173`。

开发服务器会把 `/api` 和 `/uploads` 请求代理到 Spring Boot。智能助手请求同样先经过 Spring Boot，再转发到 Python Agent。

## 构建与检查

```cmd
npm run build
npm run lint
```

不要提交 `node_modules`、`dist` 或本地环境配置。
