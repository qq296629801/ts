# 实施计划：AI 图像生成平台

**分支**：`001-ai-image-platform` | **日期**：2026-06-04 | **规格**：[spec.md](./spec.md)

**输入**：`specs/001-ai-image-platform/spec.md`（含 Clarifications 2026-05-27～2026-06-04）

## 概要

B2C Web 平台：**Vue 2** 前端 + **platform-api**（业务、JPA、双渠道支付、模版、管理、公开图库）+ **gateway-service**（WebFlux、中继生图/对话、配额协调）。默认生图模型 `gpt-image-2`；次数 **先预扣、成功 commit、失败 rollback**；支付 **微信 Native + 支付宝扫码**；订单 **15 分钟**超时；单路生图。

**v1 客群**：仅 C 端个人账号，无企业子账号/多租户。

**当前增量（2026-06-04 澄清，待开发）**：

1. **管理员退款**：已支付订单原路退 + 扣回全部 `quota_granted`；余额不足则拒绝；`REFUNDED` 状态与账单/审计。
2. **公开展示图库对齐**：仅自动取已上架模版封面（热门/最新），移除人工 `featured.public_image_ids` 路径。

其余能力（RBAC、公开图库 API、双渠道下单、管理账单、毛玻璃 UI）**已合入代码**，见 [tasks.md](./tasks.md) 增量表。

## 技术上下文

| 项 | 选型 |
|----|------|
| **语言/版本** | Java 17（运行）/ 规范 Java 21；Spring Boot 3.3.x |
| **主要依赖** | Spring MVC + JPA（platform）、WebFlux（gateway）、Spring AI（对话）、JWT、Vue 2 + Element UI |
| **存储** | MySQL 8、Redis 7、MinIO（开发）/ OSS（生产） |
| **测试** | JUnit 5、MockMvc、集成测试 + `contracts/openapi.yaml` |
| **目标平台** | Docker Compose（`deploy/docker-compose.dev.yml`）；http://localhost |
| **项目类型** | Web 前后端分离 + AI 网关 |
| **性能目标** | 常规 API P95 &lt; 500ms；生图 P95 &lt; 15s（见 spec FR-034–036） |
| **约束** | 宪章：网关无领域逻辑；密钥仅服务端；支付/退款回调 permitAll + 验签 |
| **规模/范围** | 单规格目录 `001`；下一批任务从 T094 起（见 tasks.md） |

## 宪章检查

*门禁：Phase 0 ✅ | Phase 1 设计后 ✅*

| 原则 | 状态 | 说明 |
|------|------|------|
| I. Spring AI 优先 | ✅ | 对话 `ChatModel`；生图 `RelayImageClient` 中继 |
| II. WebFlux 透传网关 | ✅ | AI 仅在 gateway；支付/退款/模版在 platform |
| III. 规格优先增量交付 | ✅ | 澄清已写入 spec；退款为独立可验收增量 |
| IV. 契约与集成验证 | ✅ | openapi v1.1 + 退款路由待补契约测试 |
| V. 密钥与客户端边界 | ✅ | 微信/支付宝密钥仅服务端 |
| VI. 最小范围与可观测 | ✅ | 退款逻辑集中在 `pay` 包；公开图库仅改 `PublicGalleryService` |

**Phase 1 后**：退款不经过 gateway；公开展示不暴露用户私密 `t_image`。

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
└── tasks.md
```

### 源代码（已存在）

```text
backend/platform-api/     # 业务 API :8080
backend/gateway-service/    # AI 网关 :8081
frontend/web/             # Vue :8082 / nginx /
deploy/docker-compose.dev.yml
tests/e2e/smoke-p1.sh, smoke-rbac-pay.sh
```

## 复杂度跟踪

| 违规项 | 为什么需要 | 为什么拒绝更简单方案 |
|--------|------------|----------------------|
| 双服务 | 宪章 II | 单体混 WebFlux + JPA 易阻塞 |
| 渠道退款 API | 澄清要求原路退 + 对账 | 仅改状态不退款不符合 FR-028b |
| `RelayImageClient` 非 Spring AI ImageModel | 中继 b64_json/超时 | 已记录在 research §2 |

## Phase 0：研究与决策

详见 [research.md](./research.md)（§1–9 基线 + §10–13 2026-06-04 增量）。**无未决 NEEDS CLARIFICATION**。

## Phase 1：设计与契约

| 产物 | 路径 | 说明 |
|------|------|------|
| 数据模型 | [data-model.md](./data-model.md) | 订单退款字段、双渠道、公开图库数据源 |
| API 契约 | [contracts/openapi.yaml](./contracts/openapi.yaml) | v1.1 + 待实现 `POST .../refund` |
| 快速验证 | [quickstart.md](./quickstart.md) | 含退款与公开展示走查 |
| UI token | [design-ui-tokens.md](./design-ui-tokens.md) | 顶栏/背景 |

### 退款（待实现）设计要点

```text
Admin POST /api/v1/admin/billing/orders/{orderNo}/refund
  → 校验 ROLE_ADMIN、订单 PAID、balance >= quota_granted
  → 事务：扣减 quota + QuotaLog(REFUND) + status=REFUNDED
  → 调用 WechatPayService/AlipayPayService.refund (dev mock)
  → 写 PayNotifyLog 或 RefundLog 幂等
```

### 公开展示（待对齐）设计要点

- `GET /api/v1/gallery/public?sort=hot|latest`（默认 `hot`）
- 仅 `Template.status=APPROVED` 且 `cover_image_url` 非空
- **删除** `SystemConfig featured.public_image_ids` 注入逻辑

## Phase 2：任务分解

由 `/speckit-tasks` 生成或手工在 [tasks.md](./tasks.md) 追加 **T094+**（本 plan 不生成 tasks.md）。

建议任务包：

| ID 范围 | 内容 |
|---------|------|
| T094–T098 | 退款：Flyway 字段、Service、Admin API、集成测试、管理端按钮 |
| T099–T101 | 公开展示：Service 去精选、sort 参数、契约/冒烟更新 |
| T102 | openapi + quickstart 与 SC-012 走查 |

## 实现状态快照（2026-06-04）

| 能力 | 状态 |
|------|------|
| P1 注册/聊天/生图/配额 | ✅ |
| 模版广场 + 审核 | ✅ |
| 微信/支付宝下单 + 账单 | ✅ |
| RBAC + 游客公开浏览 | ✅ |
| 毛玻璃 + 全站背景 | ✅ |
| 管理员退款 | ❌ 待做 |
| 公开展示纯自动排序 | ⚠️ 需去精选配置 |

## 下一步

1. `/speckit-tasks` 或手工追加 T094+  
2. `/speckit-implement` 执行退款与公开展示对齐  
3. 验收：`quickstart.md` + SC-012
