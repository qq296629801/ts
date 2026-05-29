# AI 图像生成平台

基于 Spring Boot 3 + WebFlux 网关 + Vue 2 的 AI 图像生成 Web 平台。

## 文档

- 功能规格：`specs/001-ai-image-platform/spec.md`
- 实施计划：`specs/001-ai-image-platform/plan.md`
- 本地启动：`specs/001-ai-image-platform/quickstart.md`
- 任务清单：`specs/001-ai-image-platform/tasks.md`

## 快速启动

```bash
# 基础设施
docker compose -f deploy/docker-compose.dev.yml up -d

# 业务 API（8080）
cd backend/platform-api && mvn spring-boot:run -Dspring-boot.run.profiles=dev

# AI 网关（8081）
cd backend/gateway-service && mvn spring-boot:run -Dspring-boot.run.profiles=dev

# 前端（8082）
cd frontend/web && npm install && npm run serve
```

## 模块

| 模块 | 端口 | 职责 |
|------|------|------|
| `platform-api` | 8080 | 认证、配额、会话、支付、模版 |
| `gateway-service` | 8081 | AI 对话 SSE、生图透传 |
| `frontend/web` | 8082 | Vue 2 SPA |
