# 实施计划：[FEATURE]

**分支**：`[###-feature-name]` | **日期**：[DATE] | **规格**：[link]

**输入**：来自 `/specs/[###-feature-name]/spec.md` 的功能规格

**说明**：本模板由 `/speckit-plan` 命令填充。执行流程参考 `.specify/templates/plan-template.md`。

## 概要

[从功能规格中提取：核心需求 + 研究阶段确定的技术方案]

## 技术上下文

<!--
  待填写：将本节替换为项目真实技术细节。
  下面结构用于引导规划过程，不代表必须全部照抄。
-->

**语言/版本**：[例如 Java 21、Spring Boot 3.x，或 需要澄清]

**主要依赖**：[例如 Spring AI、Spring WebFlux、spring-boot-starter-webflux，或 需要澄清]

**存储**：[如适用，例如 PostgreSQL、文件，或 不适用]

**测试**：[例如 JUnit、pytest、XCTest、cargo test，或 需要澄清]

**目标平台**：[例如 Linux 服务端、iOS 15+、WASM，或 需要澄清]

**项目类型**：[例如 库/CLI/Web 服务/移动应用/编译器/桌面应用，或 需要澄清]

**性能目标**：[领域相关，例如 1000 req/s、1 秒内返回搜索结果，或 需要澄清]

**约束**：[领域相关，例如 p95 < 200ms、内存 < 100MB、支持离线，或 需要澄清]

**规模/范围**：[领域相关，例如 1 万用户、50 个页面，或 需要澄清]

## 宪章检查

*门禁：必须在 Phase 0 研究前通过；Phase 1 设计后再次检查。*

对照 `.specify/memory/constitution.md`（AI 图片中转网关）验证：

| 原则 | 继续推进前必须成立的门禁 |
|------|--------------------------|
| I. Spring AI 优先 | 图片/上游调用使用 Spring AI；默认图片模型可配置且从 `gpt-image-2` 开始；原始 HTTP 仅允许在复杂度跟踪中记录例外 |
| II. WebFlux 透传网关 | 新代理路由保持响应式透传；不得引入阻塞调用；没有理由不得在网关层加入领域逻辑 |
| III. 规格优先的增量交付 | `spec.md` 存在，并包含优先级用户故事、验收场景、功能需求、成功标准和可独立测试的 P1 |
| IV. 契约与集成验证 | 公开路由、上游中转行为、认证边界、错误映射和流式行为已有契约/集成验证计划 |
| V. 密钥与客户端边界 | API Key 与上游凭证仅保留在服务端；客户端永远不能收到服务商密钥 |
| VI. 最小范围与可观测运行 | 无未说明的重构或依赖；额外复杂度记录在复杂度跟踪；新增表面具备结构化日志、关联 ID 和可操作错误 |

**Phase 0 前**：所有行均满足，或在复杂度跟踪中记录例外。

**Phase 1 后**：设计产物（`research.md`、`data-model.md`、`contracts/`）不违反原则；设计扩展后重新检查最小范围与可观测运行。

## 项目结构

### 文档（当前功能）

```text
specs/[###-feature]/
├── plan.md              # 本文件（/speckit-plan 命令输出）
├── research.md          # Phase 0 输出（/speckit-plan 命令）
├── data-model.md        # Phase 1 输出（/speckit-plan 命令）
├── quickstart.md        # Phase 1 输出（/speckit-plan 命令）
├── contracts/           # Phase 1 输出（/speckit-plan 命令）
└── tasks.md             # Phase 2 输出（/speckit-tasks 命令创建，不由 /speckit-plan 创建）
```

### 源代码（仓库根目录）

<!--
  待填写：将下面的占位结构替换为本功能真实目录结构。
  删除未使用的选项，并用真实路径展开选中的结构。
  最终计划中不应保留“选项”标签。
-->

```text
# [未使用则删除] 选项 1：单项目（默认）
src/
├── models/
├── services/
├── cli/
└── lib/

tests/
├── contract/
├── integration/
└── unit/

# [未使用则删除] 选项 2：Web 应用（检测到前端 + 后端时）
backend/
├── src/
│   ├── models/
│   ├── services/
│   └── api/
└── tests/

frontend/
├── src/
│   ├── components/
│   ├── pages/
│   └── services/
└── tests/

# [未使用则删除] 选项 3：移动端 + API（检测到 iOS/Android 时）
api/
└── [同后端结构]

ios/ 或 android/
└── [平台相关结构：功能模块、UI 流程、平台测试]
```

**结构决策**：[说明选择的结构，并引用上方真实目录]

## 复杂度跟踪

> **仅当宪章检查存在必须说明的违规项时填写**

| 违规项 | 为什么需要 | 为什么拒绝更简单方案 |
|--------|------------|----------------------|
| [例如第 4 个项目] | [当前需要] | [为什么 3 个项目不够] |
| [例如 Repository 模式] | [具体问题] | [为什么直接数据访问不够] |
