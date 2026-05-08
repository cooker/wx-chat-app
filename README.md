# wx-chat-app

一个相册内容展示与后台管理一体化项目（MVP）。

- 后端：Spring Boot 3 + MyBatis + SQLite
- 管理端：Vue 3 + PrimeVue（Sakai 风格）
- 用户端：Vue 3 + 虚拟瀑布流 + Swiper

## 项目结构

- `backend`：Java 后端服务（API、鉴权、静态资源托管）
- `admin`：后台管理前端
- `client`：用户端前端
- `docs/plans`：计划与任务文档

## 环境要求

- Node.js 18+
- JDK 21+
- Maven 3.9+

## 快速开始（开发模式）

### 1) 启动后端

```bash
cd backend
mvn spring-boot:run
```

默认端口：`8080`

### 2) 启动管理端（可选）

```bash
cd admin
npm install
npm run dev
```

### 3) 启动用户端（可选）

```bash
cd client
npm install
npm run dev
```

## 一体化部署（推荐）

本项目已配置前端构建产物输出到后端静态目录：

- 管理端输出到：`backend/src/main/resources/static/admin`
- 用户端输出到：`backend/src/main/resources/static/client`

执行：

```bash
cd admin && npm install && npm run build
cd ../client && npm install && npm run build
cd ../backend && mvn spring-boot:run
```

启动后访问：

- 用户端：`http://localhost:8080/client/`
- 管理端：`http://localhost:8080/admin/`
- 根路径：`http://localhost:8080/`（会重定向到 `/client/`）

## 配置说明

后端核心配置在 `backend/src/main/resources/application.yml`：

- 数据库：`jdbc:sqlite:./wx-chat.db`
- 上传目录：`./uploads`、`./uploads/albums`
- 后台鉴权开关：`app.admin.auth-enabled`
- 后台密码：`app.admin.password`（可用环境变量 `ADMIN_PASSWORD` 覆盖）

## 常用命令

### 前端构建

```bash
cd admin && npm run build
cd client && npm run build
```

### 后端测试

```bash
cd backend
mvn test
```

## 说明

- 静态文件访问与 SPA 路由转发由后端统一处理（`/admin/**`、`/client/**`）。
- API 鉴权仅作用于 `/api/**`，静态资源与前端页面可正常访问。
