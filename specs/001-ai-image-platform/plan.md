# 实施计划：AI 图像生成平台

**分支**：`001-ai-image-platform` | **日期**：2026-06-04 | **规格**：[spec.md](./spec.md)

**输入**：`specs/001-ai-image-platform/spec.md`（含 Clarifications 2026-05-27～2026-06-04）

## 概要

B2C Web 平台：**Vue 2** 前端 + **platform-api**（业务、JPA、双渠道支付/退款、模版、管理、公开图库）+ **gateway-service**（WebFlux、中继生图/对话、配额协调）。默认生图模型 `gpt-image-2`；次数 **先预扣、成功 commit、失败 rollback**；支付 **微信 Native + 支付宝扫码**；订单 **15 分钟**超时；单路生图。

**v1 客群**：仅 C 端个人账号，无企业子账号/多租户。

**交付状态（2026-06-04）**：规格内 **P1–P3 用户故事与 M8 澄清增量均已实现**（`tasks.md` T001–T108 已勾选）。下一批工作为 **生产加固与规格对齐**（见 Phase 2），非功能 MVP 缺口。

## 技术上下文

| 项 | 选型 |
|----|------|
| **语言/版本** | Java 17；Spring Boot 3.3.x |
| **主要依赖** | Spring MVC + JPA（platform）、WebFlux（gateway）、Spring AI（对话）、JWT、Vue 2 + Element UI |
| **存储** | MySQL 8、Redis 7、MinIO（开发）/ OSS（生产） |
| **测试** | JUnit 5、MockMvc、`RefundIntegrationTest`、`PublicGalleryIntegrationTest`、`contracts/openapi.yaml` |
| **目标平台** | Docker Compose（`deploy/docker-compose.dev.yml`）；http://localhost |
| **项目类型** | Web 前后端分离 + AI 网关 |
| **性能目标** | 常规非生图 P95 ≤ 1s（FR-034/SC-003）；生图 P95 ≤ 15s（FR-035/SC-004） |
| **约束** | 宪章：网关无领域逻辑；密钥仅服务端；支付/退款回调 permitAll + 生产验签 |
| **规模/范围** | 单规格目录 `001`；任务全集 T001–T108 已完成 |

## 宪章检查

*门禁：Phase 0 ✅ | Phase 1 设计后 ✅ | M8 实现后 ✅*

对照 [`.specify/memory/constitution.md`](../../.specify/memory/constitution.md)：

| 原则 | 状态 | 说明 |
|------|------|------|
| I. Spring AI 优先 | ✅ | 对话 `ChatModel`；生图 `RelayImageClient` 中继（已记录例外） |
| II. WebFlux 透传网关 | ✅ | AI 仅在 gateway；支付/退款/模版在 platform |
| III. 规格优先增量交付 | ✅ | M8 退款 + 公开展示已按澄清交付 |
| IV. 契约与集成验证 | ✅ | openapi **v1.2.0**；退款/公开图库有集成测试 |
| V. 密钥与客户端边界 | ✅ | 微信/支付宝密钥仅服务端；dev Mock |
| VI. 最小范围与可观测 | ✅ | 退款在 `PayService`；公开图库仅 `PublicGalleryService` |

**已知生产缺口**（不违反宪章，但阻塞上线）：真实支付验签、限流、压测实测值 — 见 `checklists/security-review.md` 与 Phase 2。

## 项目结构

### 文档

```text
specs/001-ai-image-platform/
├── plan.md
├── research.md
├── data-model.md
├── design-ui-tokens.md
├── quickstart.md
├── contracts/openapi.yaml
├── spec.md
├── tasks.md
└── checklists/
```

### 源代码

```text
backend/platform-api/       # 业务 API :8080（含退款、账单、公开图库）
backend/gateway-service/      # AI 网关 :8081
frontend/web/               # Vue :8082 / nginx /
deploy/docker-compose.dev.yml
tests/e2e/smoke-p1.sh, smoke-rbac-pay.sh
```

**结构决策**：双服务拆分满足宪章 II；Nginx 仅 `/api/v1/ai/*` 转发网关，其余 `/api/` → platform-api。

## 复杂度跟踪

| 违规项 | 为什么需要 | 为什么拒绝更简单方案 |
|--------|------------|----------------------|
| 双服务 | 宪章 II | 单体混 WebFlux + JPA 易阻塞 |
| 渠道退款 API | FR-028b 原路退 + 对账 | 仅改状态不退款不符合规格 |
| `RelayImageClient` 非 Spring AI ImageModel | 中继 b64_json/超时 | 已记录在 research §2 |
| 退款幂等复用 `t_pay_notify_log` | 与支付回调同一幂等模式 | 独立 RefundLog 表增加迁移成本（v1 接受） |

## Phase 0：研究与决策

详见 [research.md](./research.md)（§1–13）。**无未决 NEEDS CLARIFICATION**。

## Phase 1：设计与契约

| 产物 | 路径 | 说明 |
|------|------|------|
| 数据模型 | [data-model.md](./data-model.md) | V9 退款字段、双渠道、`REFUND` QuotaLog |
| API 契约 | [contracts/openapi.yaml](./contracts/openapi.yaml) | **v1.2.0**（退款、`sort`、账单视图） |
| 快速验证 | [quickstart.md](./quickstart.md) | M7/M8 走查、SC-012 |
| UI token | [design-ui-tokens.md](./design-ui-tokens.md) | 顶栏/背景 |

### 退款（已交付）

```text
POST /api/v1/admin/billing/orders/{orderNo}/refund
  → 校验 ADMIN、订单 PAID、balance >= quota_granted（不足则 400）
  → 渠道 WechatPayService/AlipayPayService.refund（dev Mock）
  → 事务：扣配额 + QuotaLog(REFUND) + status=REFUNDED + PayNotifyLog(refund-{orderNo})
```

代码：`PayService.refundPaidOrder`、`AdminBillingController`、`V9__pay_refund.sql`、`RefundIntegrationTest`。

### 公开展示（已交付）

- `GET /api/v1/gallery/public?sort=hot|latest`（默认 `hot`）
- 仅 `APPROVED` 模版且 `cover_image_url` 非空；**已移除** `featured.public_image_ids`

代码：`PublicGalleryService`、`PublicGallery.vue` 排序切换。

## Phase 2：任务与后续

| 阶段 | 任务 ID | 状态 |
|------|---------|------|
| 平台 MVP + 体验 + RBAC/支付 | T001–T093 | ✅ |
| M8 退款 + 公开展示 + 契约走查 | T094–T108 | ✅ |

**建议下一批（未写入 tasks，待 `/speckit-tasks` 追加 T109+）**：

| 主题 | 内容 |
|------|------|
| 规格对齐 | ~~统一 FR-034 与 SC-003~~（2026-05-30 已写入 spec） |
| 测试 | 邮箱注册集成测试（SC-001 双通道） |
| 生产加固 | 微信/支付宝真实验签、`security-review.md` 限流与审计告警 |
| 性能 | 执行 `checklists/perf-baseline.md` 压测并填实测值 |
| 债务 | ~~修复 tasks 重复编号~~（T109–T112 已重编号） |

本 plan **不生成** `tasks.md`；由 `/speckit-tasks` 维护。

## 实现状态快照（2026-06-04）

| 能力 | 状态 |
|------|------|
| P1 注册/聊天/生图/配额 | ✅ |
| 模版广场 + 审核 | ✅ |
| 微信/支付宝下单 + 账单 | ✅ |
| RBAC + 游客公开浏览 | ✅ |
| 毛玻璃 + 全站背景 | ✅ |
| 管理员原路退款（M8） | ✅ |
| 公开展示纯自动排序（M8） | ✅ |
| 生产验签 / 限流 / 压测实测 | ⏳ Phase 2 建议 |

## 下一步

1. 可选：`/speckit-specify` 或手工修订 spec（性能口径、退款场景 7）
2. `/speckit-tasks` 追加 T109+（生产加固）
3. 验收：`quickstart.md` M8 + `./tests/e2e/smoke-rbac-pay.sh`
4. 合并分支前：完成 `security-review.md` 与 perf-baseline 实测
