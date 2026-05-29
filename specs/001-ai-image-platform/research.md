# 研究记录：AI 图像生成平台

**功能**：`001-ai-image-platform` | **日期**：2026-05-27

## 1. 总体架构形态

**Decision**：采用 **双服务 + 单前端** 仓库结构：`gateway-service`（Spring WebFlux + Spring AI）与 `platform-api`（Spring Boot Web + JPA），前端 `web`（Vue 2）。

**Rationale**：宪章要求网关保持透传、非阻塞且不承载完整领域工作流；注册、支付、模版审核、报表等强持久化逻辑放在 `platform-api`。AI 路由统一经网关，业务 API 经 `platform-api`，Nginx 按路径分流。

**Alternatives considered**：
- 单体 MVC：实现简单但违反 WebFlux 透传与非阻塞网关约束。
- 网关承担全部 API：网关内引入 JPA/支付，违反「最小网关」原则。

## 2. 上游模型集成（Spring AI）

**Decision**：图像生成使用 Spring AI `ImageModel`（默认 `gpt-image-2`）；文字对话使用 Spring AI `ChatModel` + 流式 `Flux<ChatResponse>` 输出 SSE。

**Rationale**：符合宪章 Spring AI 优先；配置通过 `spring.ai.openai.*` 外部化 API Key 与模型名。

**Alternatives considered**：
- 原始 WebClient 直调 OpenAI：仅当流式字段映射不足时作为 **复杂度例外** 记录在 `plan.md`，并限定在 `ReactiveOpenAiClient` 适配层。

## 3. 次数预扣与单路并发

**Decision**：`platform-api` 暴露内部 REST `POST /internal/quota/reserve`（预扣）与 `POST /internal/quota/commit|rollback`；`gateway-service` 生图前调用 reserve，成功后 commit，失败 rollback。Redis 键 `gen:inflight:{userId}` 实现 **FR-017a** 单路锁，TTL 略大于上游超时（如 120s）。

**Rationale**：澄清会话约定「先扣后调」；配额权威数据在 MySQL，Redis 做并发锁与验证码缓存。

**Alternatives considered**：
- 网关直连 DB：耦合过高，违反模块边界。
- 仅 DB 乐观锁：难以表达 in-flight 语义，易超扣。

## 4. 认证与会话

**Decision**：JWT（Access Token，默认 7 天；记住我 30 天）+ Refresh Token（单次轮换，存 Redis 黑名单/版本号）。网关与 `platform-api` 共享 JWT 公钥验签（或网关调用 `platform-api` introspection，优先 **本地验签** 降低延迟）。

**Rationale**：满足 FR-004/005；网关无需访问用户表即可鉴权。

**Alternatives considered**：
- Session Cookie：不利于前后端分离 SPA。
- 网关每次调用户服务验权：延迟高，仅作降级方案。

## 5. 对象存储

**Decision**：生产阿里云 OSS；本地/CI 使用 MinIO（S3 兼容 API）。`gateway-service` 生图成功后流式或临时 URL 拉取写入 OSS，元数据回写 `platform-api`。

**Rationale**：规格要求图库可访问；MinIO 降低开发成本。

## 6. 微信支付 v1

**Decision**：仅 **Native 扫码**（`/v3/pay/transactions/native`），订单 15 分钟超时由 **调度任务 + `expires_at` 字段** 关闭；回调 `/api/v1/pay/wx-notify` 验签 + 幂等表 `t_pay_notify_log`。

**Rationale**：与澄清结论一致；JSAPI/H5 明确排除在 v1。

## 7. 短信 / 邮件 / 审核

**Decision**：
- 短信：阿里云 SMS（6 位验证码，Redis `sms:{phone}` TTL 5min，发送间隔 60s）。
- 邮件：Spring Mail + SMTP。
- 模版封面审核：阿里云内容安全（ImageModeration）；超阈值自动 `REJECTED`，否则进人工队列。

**Rationale**：与原需求书一致；国内可用性与合规成熟度高。

## 8. 订单超时与配置生效

**Decision**：`t_pay_order.expires_at = created_at + 15min`；`@Scheduled` 每分钟扫描 `PENDING` 过期改 `CANCELLED`。系统配置表 `t_system_config` 键值存储，**读取当前值仅应用于保存后新事件**（注册、邀请、充值奖励各自挂钩时间戳或 config_version）。

**Rationale**：落实澄清「全部仅未来生效」与 15 分钟关单。

## 9. 测试策略

**Decision**：
- 契约：`contracts/openapi.yaml` + Spring MockMvc/WebTestClient 契约测试。
- 网关集成：WireMock 上游 OpenAI；配额 reserve/rollback 集成测试。
- 支付：微信沙箱回调签名 fixture。

**Rationale**：宪章 IV 要求公开路由具备契约/集成验证。
