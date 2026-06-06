# AI 图像生成平台 — 项目宪章

**版本**：1.0.0 | **批准日期**：2026-05-27 | **最后修订**：2026-06-04

## 核心原则

### I. Spring AI 优先

对话与多模态能力优先使用 Spring AI；默认图片模型从 `gpt-image-2` 起可配置。绕过 Spring AI 的原始 HTTP 仅允许在 `plan.md` 复杂度跟踪中记录例外（如中继 `RelayImageClient`）。

### II. WebFlux 透传网关

AI 路由由 `gateway-service` 以响应式透传处理；禁止在网关层引入阻塞调用或无界缓冲；不得在网关承载支付、模版审核、报表等领域逻辑。

### III. 规格优先的增量交付

功能以 `specs/001-ai-image-platform/spec.md` 为唯一规格真理源；按用户故事优先级增量交付；每项增量须可独立验收。

### IV. 契约与集成验证

公开路由、认证边界、支付回调、配额与上游错误映射须有 OpenAPI 契约或集成测试；变更契约须同步 `contracts/openapi.yaml` 与相关测试。

### V. 密钥与客户端边界

OpenAI、微信、支付宝等上游凭证仅存服务端；客户端不得接收服务商密钥或商户私钥。

### VI. 最小范围与可观测运行

避免未说明的重构与依赖；额外复杂度记入 `plan.md` 复杂度跟踪；新增路径须具备结构化日志、关联 ID 与可操作错误信息。

## 技术栈约束

- Java 17、Spring Boot 3.x
- `platform-api`：Spring MVC + JPA + MySQL/Redis
- `gateway-service`：Spring WebFlux + Spring AI（对话）+ 中继生图
- 前端：Vue 2 + Element UI
- 部署：`deploy/docker-compose.dev.yml`

## 治理

- 宪章优先于临时实现决策；修订须更新本文件与 `plan.md` 宪章检查表。
- 实施路径见 `specs/001-ai-image-platform/plan.md`；任务分解见 `tasks.md`。
