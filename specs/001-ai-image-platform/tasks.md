# 任务：AI 图像生成平台

**输入**：`/specs/001-ai-image-platform/`（plan.md、spec.md、research.md、data-model.md、contracts/、quickstart.md）

**前置条件**：plan.md ✅ | spec.md ✅ | 澄清会话 2026-05-27 ✅、2026-06-04 ✅

**测试**：公开 AI 路由、配额边界、支付回调变更时 **必须** 包含契约/集成测试（宪章 IV）。

**组织方式**：按用户故事分组；P1 MVP = US1 + US3 + US2（基础能力完成后）。

**规格**：仅 [spec.md](./spec.md) + [design-ui-tokens.md](./design-ui-tokens.md)。下列增量已合入代码与本仓库，无独立 `002`–`004` 目录。

| 增量 | 交付摘要 | spec 章节 |
|------|----------|-------------|
| 平台 MVP | T001–T093：注册/聊天/生图/模版/管理 | 用户故事 1–7 |
| 顶栏毛玻璃 + 全站背景 | `App.vue`、`app-bg.webp` | §体验增量 / design-ui-tokens |
| RBAC·公开图库·双支付·账单 | `PublicGallery*`、`AlipayPayService`、`AdminBilling*`、路由与冒烟 | 用户故事 5–8、FR-040–044 |

**后续新需求**：改 spec.md，在本文件从 **T094** 起追加任务。

## 格式：`[ID] [P?] [Story] 描述`

---

## Phase 1：准备（共享基础设施）

**目的**：Monorepo 骨架、本地依赖、开发环境可启动

- [x] T001 创建 `backend/pom.xml` 父工程（Java 17、Spring Boot 3.3 BOM、模块 gateway-service/platform-api）
- [x] T002 [P] 初始化 `backend/gateway-service/pom.xml` 与 `GatewayApplication.java`（`backend/gateway-service/src/main/java/com/ts/gateway/GatewayApplication.java`）
- [x] T003 [P] 初始化 `backend/platform-api/pom.xml` 与 `PlatformApplication.java`（`backend/platform-api/src/main/java/com/ts/platform/PlatformApplication.java`）
- [x] T004 [P] 初始化 `frontend/web/`（Vue 2 + Vue Router + Vuex + Element UI，`package.json`、`vue.config.js` 代理 `/api`）
- [x] T005 创建 `deploy/docker-compose.dev.yml`（MySQL 8、Redis 7、MinIO）
- [x] T006 [P] 创建 `deploy/nginx/nginx.conf`（`/api/v1/ai/` → 8081，其余 → 8080）
- [x] T007 [P] 添加根目录 `.gitignore` 与 `backend/**/application-dev.yml.example`（密钥占位，禁止提交真实 Key）
- [x] T008 编写 `README.md` 开发启动说明（链接 `specs/001-ai-image-platform/quickstart.md`）

---

## Phase 2：基础能力（阻塞性前置条件）

**目的**：DB/Redis/JWT/统一响应/配额内部 API/可观测性；**完成前不得开始用户故事**

- [x] T009 配置 `backend/platform-api/src/main/resources/application.yml`（数据源、Redis、JWT、Flyway）
- [x] T010 [P] 配置 `backend/gateway-service/src/main/resources/application.yml`（端口 8081、platform-api 基址、Spring AI OpenAI、`gpt-image-2`）
- [x] T011 创建 Flyway `backend/platform-api/src/main/resources/db/migration/V1__user_quota.sql`（t_user、t_user_quota、t_quota_log、t_quota_reservation）
- [x] T012 [P] 实现统一响应 `backend/platform-api/src/main/java/com/ts/platform/common/ApiResponse.java` 与全局异常处理 `GlobalExceptionHandler.java`（业务码 400/401/402/403/409）
- [x] T013 [P] 实现 JWT 工具与过滤器 `backend/platform-api/src/main/java/com/ts/platform/security/JwtTokenProvider.java`、`JwtAuthenticationFilter.java`（7d/30d rememberMe）
- [x] T014 [P] 网关 JWT 验签 `backend/gateway-service/src/main/java/com/ts/gateway/security/JwtWebFilter.java`（与 platform 共享 secret 配置）
- [x] T015 实现 Redis 配置 `backend/platform-api/src/main/java/com/ts/platform/config/RedisConfig.java`
- [x] T016 实现配额领域服务 `backend/platform-api/src/main/java/com/ts/platform/quota/QuotaService.java`（预扣/提交/回滚、Redis `gen:inflight:{userId}` 单路锁）
- [x] T017 实现内部 API `backend/platform-api/src/main/java/com/ts/platform/quota/InternalQuotaController.java`（`/internal/quota/reserve|commit|rollback`）
- [x] T018 [P] 实现网关配额客户端 `backend/gateway-service/src/main/java/com/ts/gateway/client/QuotaClient.java`（WebClient 调 platform internal）
- [x] T019 [P] 集成 Spring AI（`application-dev.yml` 自动配置 ImageModel + ChatModel，`gpt-image-2`）
- [x] T020 实现结构化日志与 `X-Request-Id`（`backend/gateway-service/.../filter/RequestIdWebFilter.java`、`backend/platform-api/.../filter/RequestIdFilter.java`，日志脱敏）
- [x] T021 [P] 实现 OSS 客户端抽象 `backend/gateway-service/src/main/java/com/ts/gateway/storage/OssClient.java`（MinIO 实现，配置 endpoint/bucket）
- [x] T022 [P] 前端 API 封装 `frontend/web/src/api/http.js`（Bearer、统一错误码、402/409 处理）
- [x] T023 [P] 契约测试基类 `backend/platform-api/src/test/java/com/ts/platform/contract/OpenApiContractTestBase.java`（加载 `specs/001-ai-image-platform/contracts/openapi.yaml`）

**检查点**：Flyway 可迁移；platform 8080、gateway 8081 可启动；internal quota reserve 可返回 402/409

---

## Phase 3：用户故事 1 - 注册登录与次数账户（P1）🎯 MVP

**目标**：手机/邮箱注册登录、默认 3 次、登录锁定、前端认证流

**独立测试**：注册→登录→`GET /api/v1/user/quota` 余额为 3；未登录调受保护接口 401

### 用户故事 1 的测试

- [x] T024 [P] [US1] 契约测试 `backend/platform-api/src/test/java/com/ts/platform/contract/AuthContractTest.java`（send-sms、register、login、refresh）
- [x] T025 [P] [US1] 集成测试 `backend/platform-api/src/test/java/com/ts/platform/integration/AuthIntegrationTest.java`（验证码错误、5 次锁定 30min）

### 用户故事 1 的实现

- [x] T026 [P] [US1] Flyway `V2__auth_seed.sql` 无需；实体 `backend/platform-api/src/main/java/com/ts/platform/user/User.java`、`UserQuota.java`
- [x] T027 [P] [US1] Repository `UserRepository.java`、`UserQuotaRepository.java`
- [x] T028 [US1] 短信服务 `backend/platform-api/src/main/java/com/ts/platform/auth/SmsCodeService.java`（Redis TTL 5min、60s 频控）
- [x] T029 [P] [US1] 邮件验证码 `backend/platform-api/src/main/java/com/ts/platform/auth/EmailCodeService.java`（Spring Mail、TTL 10min）
- [x] T030 [US1] 注册服务 `AuthService.java`（校验 FR-003、赠送 3 次写 t_quota_log、生成 invite_code 占位）
- [x] T031 [US1] 控制器 `backend/platform-api/src/main/java/com/ts/platform/auth/AuthController.java`（含 `/auth/refresh`）
- [x] T032 [US1] 用户查询 `backend/platform-api/src/main/java/com/ts/platform/user/UserController.java`（`GET /api/v1/user/profile`、`GET /api/v1/user/quota`）
- [x] T033 [P] [US1] 前端注册页 `frontend/web/src/views/auth/Register.vue`（手机/邮箱 Tab、邀请码 query）
- [x] T034 [P] [US1] 前端登录页 `frontend/web/src/views/auth/Login.vue`（记住我、Token 存 localStorage）
- [x] T035 [US1] 路由守卫 `frontend/web/src/router/index.js`（未登录跳转、invite_code 存 sessionStorage）

**检查点**：quickstart 步骤 1–2 通过（SC-001）

---

## Phase 4：用户故事 3 - 安全中转与用量控制（P1）🎯 MVP

**目标**：网关生图/对话；先扣后调；单路并发；契约覆盖 401/402/409/rollback

**独立测试**：Mock 上游失败余额回滚；并发第二次生图 409；日志无 API Key

### 用户故事 3 的测试

- [x] T036 [P] [US3] 契约测试 `backend/gateway-service/src/test/java/com/ts/gateway/contract/ImageGatewayContractTest.java`（generate 200/402/409）
- [x] T037 [P] [US3] 集成测试 `backend/gateway-service/src/test/java/com/ts/gateway/integration/ImageGenerateIntegrationTest.java`（WireMock OpenAI 失败→rollback、成功→commit）
- [x] T038 [P] [US3] 集成测试 `backend/gateway-service/src/test/java/com/ts/gateway/integration/ChatSseIntegrationTest.java`（SSE 流式、不扣 quota）

### 用户故事 3 的实现

- [x] T039 [US3] 生图编排 `ImageGenerateService`（reserve→`RelayImageClient`→Oss→commit/rollback）
- [x] T040 [US3] 控制器 `backend/gateway-service/src/main/java/com/ts/gateway/api/ImageGatewayController.java`（`POST /api/v1/ai/image/generate`）
- [x] T041 [US3] 对话 SSE `backend/gateway-service/src/main/java/com/ts/gateway/api/ChatGatewayController.java`（`POST /api/v1/ai/chat`，Flux SSE）
- [x] T042 [US3] 平台写图 API `backend/platform-api/src/main/java/com/ts/platform/image/InternalImageController.java`（供网关回调：保存 t_image、quota commit）
- [x] T043 [P] [US3] 网关错误映射 `backend/gateway-service/src/main/java/com/ts/gateway/config/GatewayExceptionHandler.java`（上游超时友好提示）
- [x] T044 [P] [US3] 验证日志脱敏单测 `backend/gateway-service/src/test/java/com/ts/gateway/security/LogSanitizerTest.java`

**检查点**：已登录生图成功扣 1 次；失败余额不变（SC-002 生图部分）

---

## Phase 5：用户故事 2 - AI 对话与图像生成 UI（P1）🎯 MVP

**目标**：会话/消息/图库；聊天 UI；顶部剩余次数；与生图网关联调

**独立测试**：单会话内文字 SSE + 生图 1 次；历史消息分页 20 条；图库可见

### 用户故事 2 的测试

- [x] T045 [P] [US2] 契约测试 `backend/platform-api/src/test/java/com/ts/platform/contract/AiSessionContractTest.java`（sessions、messages 分页）
- [x] T046 [P] [US2] 集成测试 `backend/platform-api/src/test/java/com/ts/platform/integration/AiSessionIntegrationTest.java`

### 用户故事 2 的实现

- [x] T047 [P] [US2] Flyway `V3__ai_session_message_image.sql`（t_ai_session、t_ai_message、t_image）
- [x] T048 [P] [US2] 实体与仓库 `backend/platform-api/src/main/java/com/ts/platform/ai/AiSession.java`、`AiMessage.java`、`ImageAsset.java` 及 Repository
- [x] T049 [US2] 会话服务 `backend/platform-api/src/main/java/com/ts/platform/ai/AiSessionService.java`（新建/重命名/删除、消息分页）
- [x] T050 [US2] 控制器 `backend/platform-api/src/main/java/com/ts/platform/ai/AiSessionController.java`（`GET /api/v1/ai/sessions`、`GET .../messages`）
- [x] T051 [US2] 图库 `backend/platform-api/src/main/java/com/ts/platform/image/UserImageController.java`（`GET /api/v1/user/images`）
- [x] T052 [US2] 网关会话关联：生图/对话请求携带 `sessionId` 写入消息（`PlatformInternalClient` + 网关编排）
- [x] T053 [P] [US2] 前端聊天页 `frontend/web/src/views/chat/ChatRoom.vue`（消息列表、SSE、Shift+Enter；骨架屏待完善）
- [x] T054 [P] [US2] 前端会话侧栏 `frontend/web/src/views/chat/SessionList.vue`
- [x] T055 [P] [US2] 前端图库 `frontend/web/src/views/user/ImageGallery.vue`
- [x] T056 [US2] 顶部配额展示 `frontend/web/src/components/QuotaBadge.vue`（调 `/api/v1/user/quota`）

**检查点**：quickstart 步骤 3–4；SC-002 全路径通过

---

## Phase 6：用户故事 4 - 邀请裂变（P2）

**目标**：邀请码/链接、注册奖励、首充奖励 10 次

**独立测试**：带 invite 注册双方次数正确；无效 invite 仅 3 次

### 用户故事 4 的测试

- [x] T057 [P] [US4] 集成测试 `backend/platform-api/src/test/java/com/ts/platform/integration/InviteIntegrationTest.java`

### 用户故事 4 的实现

- [x] T058 [P] [US4] Flyway `V4__invite_reward.sql`（t_invite_reward）
- [x] T059 [US4] `InviteService.java`（FR-008/009、防自邀请、幂等奖励）
- [x] T060 [US4] 扩展 `AuthService.register` 处理 invite_code
- [x] T061 [US4] `InviteController.java`（`GET /api/v1/user/invite`）
- [x] T062 [P] [US4] 前端个人中心邀请卡片 `frontend/web/src/views/user/InviteCard.vue`

**检查点**：邀请注册被邀请人 6 次、邀请人 +5 次

---

## Phase 7：用户故事 5 - 模版广场（P2）

**目标**：广场浏览、发布、审核流、使用模版、点赞收藏

**独立测试**：游客只读；发布后 PENDING；APPROVED 可一键使用

### 用户故事 5 的测试

- [x] T063 [P] [US5] 契约测试 `backend/platform-api/src/test/java/com/ts/platform/contract/TemplateContractTest.java`

### 用户故事 5 的实现

- [x] T064 [P] [US5] Flyway `V5__template.sql`（t_template、t_template_category、t_template_favorite）
- [x] T065 [P] [US5] 实体与 `TemplateService.java`、`TemplateController.java`（列表/搜索/发布/use/like）
- [x] T066 [US5] AI 审核 `TemplateAuditService.java`（阿里云内容安全、自动 REJECTED）
- [x] T067 [P] [US5] 前端广场 `frontend/web/src/views/template/TemplatePlaza.vue`（瀑布流、筛选）
- [x] T068 [P] [US5] 前端发布模版 `frontend/web/src/views/template/PublishTemplate.vue`
- [x] T069 [US5] 「使用模版」跳转聊天并填充 prompt（`ChatRoom.vue` 接 query）

**检查点**：仅 APPROVED 在广场展示；违规 AI 自动拒绝

---

## Phase 8：用户故事 6 - 充值与订单（P3）

**目标**：Native 扫码、15 分钟取消、回调验签幂等、次数到账

**独立测试**：创建订单得 code_url；模拟回调 PAID 到账；超时 CANCELLED 不入账

### 用户故事 6 的测试

- [x] T070 [P] [US6] 集成测试 `backend/platform-api/src/test/java/com/ts/platform/integration/PayIntegrationTest.java`（验签、幂等、超时）
- [x] T071 [P] [US6] 契约测试 `backend/platform-api/src/test/java/com/ts/platform/contract/PayContractTest.java`

### 用户故事 6 的实现

- [x] T072 [P] [US6] Flyway `V6__pay.sql`（t_pay_package、t_pay_order、t_pay_notify_log）及套餐种子数据
- [x] T073 [US6] `WechatPayService.java`（Native 下单、回调验签）
- [x] T074 [US6] `PayController.java`（packages、create-order、wx-notify、order 查询）
- [x] T075 [US6] 订单超时调度 `OrderExpireScheduler.java`（15min PENDING→CANCELLED）
- [x] T076 [US6] 支付成功发配额并触发邀请首充奖励（扩展 `InviteService`）
- [x] T077 [P] [US6] 前端充值页 `frontend/web/src/views/pay/Recharge.vue`（二维码、轮询订单状态）

**检查点**：SC-006；超时订单显示已取消

---

## Phase 9：用户故事 7 - 后台运营与管理（P3）

**目标**：模版人工审核、用户管理、系统配置（仅未来生效）、报表

**独立测试**：管理员批量审核；改注册赠送仅新用户生效

### 用户故事 7 的测试

- [x] T078 [P] [US7] 集成测试 `backend/platform-api/src/test/java/com/ts/platform/integration/AdminConfigIntegrationTest.java`（配置仅未来生效）

### 用户故事 7 的实现

- [x] T079 [P] [US7] Flyway `V7__admin_config_audit.sql`（t_system_config、t_audit_log）
- [x] T080 [US7] 管理员鉴权 `AdminSecurityConfig.java`（ROLE_ADMIN）
- [x] T081 [US7] `AdminTemplateController.java`、`AdminUserController.java`、`AdminConfigController.java`
- [x] T082 [US7] `ReportService.java`（日/周/月新增用户、生图量、收入、模版排行）
- [x] T083 [P] [US7] 前端管理布局 `frontend/web/src/views/admin/AdminLayout.vue`
- [x] T084 [P] [US7] 前端审核列表 `frontend/web/src/views/admin/TemplateAudit.vue`
- [x] T085 [P] [US7] 前端用户管理 `frontend/web/src/views/admin/UserManage.vue`
- [x] T086 [P] [US7] 前端系统配置 `frontend/web/src/views/admin/SystemConfig.vue`

**检查点**：SC-007；FR-031 配置变更不追溯旧用户余额

---

## Phase 10：打磨与横切关注点

**目的**：联调、文档、安全与性能验证

- [x] T087 [P] 完善 `deploy/docker-compose.dev.yml` 与 `quickstart.md` 一致性
- [x] T088 [P] [US3] `RelayImageClient` + JDK HttpClient 中继生图（`b64_json`/`url`、重试、错误映射）
- [x] T089 [P] [US3] `RelayImageClientTest`（MockWebServer）与可选 `RelayImageClientRelayIT`（`RELAY_IT=1`）
- [x] T090 [P] Nginx 拆分 `/api/v1/ai/chat|image` → gateway，其余 `/api/` → platform-api；生图 `proxy_read_timeout` 360s
- [x] T091 [P] 前端生图 axios 超时 360s；`tests/e2e/smoke-relay-image.sh` 中继+网关冒烟
- [x] T088 端到端冒烟脚本 `tests/e2e/smoke-p1.sh`（覆盖 quickstart §6）
- [x] T089 [P] 安全审查：确认响应/日志无 `OPENAI_API_KEY`、完整 JWT、用户 prompt 批量明文
- [x] T090 [P] 网关阻塞审查：WebFlux 路径无 `block()`、无界 buffer（代码扫描或 ArchUnit）
- [x] T091 性能基线文档 `specs/001-ai-image-platform/checklists/perf-baseline.md`（P95 目标对照 FR-034–036）
- [x] T092 [P] 前端移动端适配检查主要页面（注册、聊天、广场、充值）
- [x] T093 更新 `AGENTS.md` SPECKIT 段落实装路径（若与 plan 有偏差则同步）

---

## 依赖与执行顺序

### 阶段依赖

```text
Phase 1 → Phase 2 → Phase 3 (US1) → Phase 4 (US3) → Phase 5 (US2)
                              ↘ Phase 6 (US4) — 依赖 US1
Phase 5 → Phase 7 (US5) — 依赖图库/生图
Phase 3 → Phase 8 (US6) — 依赖配额账户
Phase 7 → Phase 9 (US7) — 依赖模版与支付
Phase * → Phase 10
```

### 用户故事依赖

| 故事 | 依赖 | 说明 |
|------|------|------|
| US1 | Phase 2 | MVP 认证 |
| US3 | US1 + Phase 2 | 需 JWT 用户与 quota internal |
| US2 | US3 | 生图/对话写会话 |
| US4 | US1 | 注册流 |
| US5 | US2 | 图库发模版 |
| US6 | US1 | 配额充值 |
| US7 | US5、US6 | 审核与报表 |

### 并行机会

- **Phase 1**：T002/T003/T004/T006/T007 可并行
- **Phase 2**：T010/T012/T013/T014/T018/T019/T021/T022/T023 可并行（T016→T017 串行）
- **US1**：T026/T027/T028/T029/T033/T034 可并行
- **US3**：T036/T037/T038 可并行；T039 阻塞 T040
- **US5 前端**：T067/T068 可与后端 T065 并行（API 稳定后联调）

### 并行示例：Phase 2

```bash
# 平台与网关配置并行
T010 gateway application.yml | T009 platform application.yml
# 安全与客户端并行
T013 JwtTokenProvider | T014 JwtWebFilter | T018 QuotaClient | T019 SpringAiConfig
```

### 并行示例：P1 完成后

```bash
# US4 与 US5 后端可不同人并行（US4 不依赖 US5）
开发者 A: Phase 6 US4
开发者 B: Phase 7 US5（需 US2 图库已完成）
```

---

## 实施策略

### MVP 优先（推荐）

1. Phase 1 + Phase 2（阻塞）
2. Phase 3 US1 + Phase 4 US3 + Phase 5 US2
3. **停止并验证**：`quickstart.md` §6、SC-001/SC-002
4. 再按 P2→P3 推进 US4–US7

### 增量交付

| 增量 | 包含故事 | 可演示能力 |
|------|----------|------------|
| M1 MVP | US1+US3+US2 | 注册、聊天、生图、配额 |
| M2 增长 | US4 | 邀请裂变 |
| M3 内容 | US5+US7 审核 | 模版广场 UGC |
| M4 变现 | US6 | Native 充值 |
| M5 运营 | US7 全量 | 报表与配置 |
| M6 体验 | — | 毛玻璃顶栏 + 全站背景（已交付） |
| M7 权限与支付 | — | 公开图库、支付宝、管理账单（已交付） |
| M8 澄清增量 | T094–T108 | 管理员原路退款、公开展示纯自动排序 |

---

## Phase 11：US6/US7 增量 — 管理员线上退款（P2）

**目标**：满足 FR-028b/c、SC-012；已支付订单可原路退款、扣回 `quota_granted`；余额不足时拒绝退款。

**独立验收**：管理员 `19900000000` 对 PAID 订单退款成功 → 用户余额减少、订单 `REFUNDED`、账单汇总不含该笔；余额小于赠送次数时 API 4xx 且订单仍为 PAID。

- [x] T094 [P] 新增 Flyway `backend/platform-api/src/main/resources/db/migration/V9__pay_refund.sql`：`t_pay_order.refunded_at`、`refund_notify_id`（与 data-model.md 一致）
- [x] T095 [P] [US6] 扩展 `backend/platform-api/src/main/java/com/ts/platform/pay/PayOrder.java` 与 JPA 映射 REFUNDED 状态字段
- [x] T096 [P] [US6] 在 `backend/platform-api/src/main/java/com/ts/platform/pay/WechatPayService.java` 与 `AlipayPayService.java` 增加 `refund(orderNo, amount)`（dev profile 返回 mock 成功）
- [x] T097 [US7] 在 `backend/platform-api/src/main/java/com/ts/platform/admin/AdminBillingService.java` 实现 `refundOrder`：校验 ADMIN、PAID、`balance >= quota_granted`、事务内扣配额 + `QuotaLog` reason=`REFUND` + 调渠道退款 + 幂等写 `PayNotifyLog`
- [x] T098 [US7] 在 `backend/platform-api/src/main/java/com/ts/platform/admin/AdminBillingController.java` 暴露 `POST /api/v1/admin/billing/orders/{orderNo}/refund`（对齐 `contracts/openapi.yaml`）
- [x] T099 [US7] 新增 `backend/platform-api/src/test/java/com/ts/platform/integration/RefundIntegrationTest.java`：成功退款、余额不足拒绝、非 ADMIN 403
- [x] T100 [US7] 在 `frontend/web/src/views/admin/BillingOrders.vue` 增加退款确认与 REFUNDED 状态展示
- [x] T101 [US7] 更新 `AdminBillingService` 汇总逻辑：已退款订单不计入收入/笔数（`backend/platform-api/src/main/java/com/ts/platform/admin/AdminBillingService.java`）

---

## Phase 12：US5/US8 增量 — 公开展示纯自动排序（P2）

**目标**：满足澄清 D / FR-044a；`GET /api/v1/gallery/public` 仅已上架模版封面，支持 `sort=hot|latest`，**移除** `featured.public_image_ids`。

**独立验收**：游客 `GET /gallery/public?sort=latest` 200，响应无 `FEATURED` 类型项；切换 `sort` 顺序与 `useCount`/`createdAt` 一致。

- [x] T102 [US5] 重写 `backend/platform-api/src/main/java/com/ts/platform/gallery/PublicGalleryService.java`：删除 `SystemConfigRepository` 精选逻辑，仅 `Template.status=APPROVED` 且封面非空
- [x] T103 [US5] 在 `backend/platform-api/src/main/java/com/ts/platform/gallery/PublicGalleryController.java` 增加查询参数 `sort`（默认 `hot`：`useCount` DESC；`latest`：`createdAt` DESC）
- [x] T104 [US5] 更新 `backend/platform-api/src/test/java/com/ts/platform/integration/PublicGalleryIntegrationTest.java`：覆盖 `sort`、断言无人工精选 URL
- [x] T105 [P] [US5] 更新 `frontend/web/src/views/gallery/PublicGallery.vue`：传递 `sort` 查询参数并展示切换控件

---

## Phase 13：打磨 — 契约与走查（澄清增量）

**目标**：OpenAPI、quickstart、冒烟与 SC-012 一致。

- [x] T106 [P] 核对并必要时修订 `specs/001-ai-image-platform/contracts/openapi.yaml`（`sort` 参数、`refund` 响应与实现一致）
- [x] T107 [P] 更新 `specs/001-ai-image-platform/quickstart.md`：M8 退款走查 + 公开展示 `sort` 验收步骤
- [x] T108 扩展 `tests/e2e/smoke-rbac-pay.sh`：公开展示 `sort` 与管理员退款路径（dev mock 可跳过真实渠道）

---

### 澄清增量依赖（T094+）

```text
T094、T095 ─┬─► T097 ─► T098 ─► T099
T096 ───────┘              └─► T100、T101

T102 ─► T103 ─► T104
T105 可与 T103 并行（前端）

T106–T108 依赖 Phase 11–12 实现完成
```

### 澄清增量并行示例

```bash
# 退款后端与渠道 mock 可并行起步
开发者 A: T094 + T095 + T097
开发者 B: T096

# 公开展示后端与前端
开发者 A: T102 + T103
开发者 B: T105（待 T103 契约稳定）
```

---

## 任务统计

| 阶段 | 任务 ID 范围 | 数量 |
|------|----------------|------|
| Phase 1 准备 | T001–T008 | 8 |
| Phase 2 基础 | T009–T023 | 15 |
| US1 | T024–T035 | 12 |
| US3 | T036–T044 | 9 |
| US2 | T045–T056 | 12 |
| US4 | T057–T062 | 6 |
| US5 | T063–T069 | 7 |
| US6 | T070–T077 | 8 |
| US7 | T078–T086 | 9 |
| Phase 10 打磨 | T087–T093 | 7 |
| Phase 11 退款 | T094–T101 | 8 |
| Phase 12 公开展示 | T102–T105 | 4 |
| Phase 13 澄清打磨 | T106–T108 | 3 |
| **合计** | **T001–T108** | **108** |

**MVP 任务**：T001–T056（Phase 1–5，共 56 项）

**下一增量（M8）**：T094–T108（共 15 项，建议顺序执行 Phase 11 → 12 → 13）

---

## 备注

- 所有任务描述已含仓库内路径，实施时包名前缀 `com.ts` 可依团队规范调整，但须同步更新任务路径
- 支付/短信/审核 SDK 密钥仅环境变量注入
- 邀请首充奖励（+10）在 US6 T076 挂钩，US4 仅实现注册奖励
- OpenAPI v1.2.0 含退款与 `sort`；T106 与实现对齐
- 冒烟：`tests/e2e/smoke-p1.sh`、`tests/e2e/smoke-rbac-pay.sh`（T108 扩展退款与 sort）
- T094+ 对应 plan.md「待实现」：管理员退款、公开展示去精选
