# Quickstart：AI 图像生成平台（开发环境）

**功能**：`001-ai-image-platform` | **日期**：2026-05-27

## 前置条件

- Docker Desktop + Docker Compose v2（**推荐一键启动**）
- 或：JDK 17+、Maven 3.9+、Node 18+（本地分进程启动）
- OpenAI API Key（生图/对话必需）

---

## 方式 A：Docker 一键启动（推荐）

```bash
cd deploy
cp .env.example .env
# 编辑 .env：
#   OPENAI_API_KEY=sk-...        # 中继 Bearer
#   IMAGE_API_URL=https://.../v1/images/generations
#   IMAGE_QUALITY=medium         # 推荐 medium，high 易触发中继 ~60s 断连

docker compose -f docker-compose.dev.yml up -d --build
```

浏览器打开：**http://localhost**（Nginx 转发前端 + API）

| 服务 | 容器内 | 宿主机端口 |
|------|--------|------------|
| 统一入口 | nginx:80 | **80** |
| platform-api | 8080 | 8080 |
| gateway-service | 8081 | 8081 |
| frontend | 8082 | 8082 |
| MySQL / Redis / MinIO | — | 3306 / 6379 / 9000 |

查看验证码与启动日志：

```bash
docker compose -f deploy/docker-compose.dev.yml logs -f platform-api
```

冒烟（经 Nginx）：

```bash
PLATFORM_URL=http://localhost GATEWAY_URL=http://localhost ./tests/e2e/smoke-p1.sh
```

生图中继 + 网关（需有效 Key，可能因中继不稳定失败）：

```bash
chmod +x tests/e2e/smoke-relay-image.sh
./tests/e2e/smoke-relay-image.sh
```

**Nginx 路由**：`http://localhost/api/v1/ai/image/generate` → gateway；`sessions`/`messages` 等 → platform-api。若 gateway 重建后浏览器 502，执行 `docker compose -f deploy/docker-compose.dev.yml restart nginx`。

更多说明见 [deploy/README.md](../../deploy/README.md)。

---

## 方式 B：本地分进程启动

仅启动中间件：

```bash
docker compose -f deploy/docker-compose.dev.yml up -d mysql redis minio minio-init
```

### 环境变量

```bash
export OPENAI_API_KEY=sk-...
export JWT_SECRET=dev-jwt-secret-at-least-32-chars-long
export INTERNAL_API_KEY=dev-internal-key
```

### 后端

```bash
cd backend/platform-api && mvn spring-boot:run -Dspring-boot.run.profiles=dev
cd backend/gateway-service && mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

### 前端

```bash
cd frontend/web && npm install && npm run serve
```

访问 http://localhost:8082（`vue.config.js` 代理到 8080/8081）。

---

## 冒烟验证（P1）

```bash
chmod +x tests/e2e/smoke-p1.sh
./tests/e2e/smoke-p1.sh
```

1. 注册 / 登录 / 配额  
2. 对话 SSE（不扣次）  
3. 生图（需 `OPENAI_API_KEY`）  
4. 充值 Mock 支付  
5. 管理员 `19900000000` / `Admin1234` → `/admin`

## 自动化测试

```bash
cd backend && mvn test
# 可选：真实中继（不纳入 CI）
export RELAY_IT=1 IMAGE_API_URL=... OPENAI_API_KEY=...
mvn -pl gateway-service test -Dtest=RelayImageClientRelayIT
```

## 里程碑对照

| 阶段 | 交付 | 用户故事 |
|------|------|----------|
| M1 | 认证 + 配额 + 网关生图/对话 | P1 US1–3 |
| M2 | 会话/图库 + 邀请 | P1–P2 US2,4 |
| M3 | 模版广场 + 审核 | P2 US5,7 |
| M4 | Native 支付 | P3 US6 |
| M5 | 管理报表 | P3 US7 |
