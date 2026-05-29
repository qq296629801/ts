# AI 图像生成平台 需求规格说明书

> **Software Requirements Specification**
> 版本：v1.0 | 编制日期：2026年5月 | 文档状态：评审中


| 属性    | 内容                              |
| ----- | ------------------------------- |
| 前端技术栈 | Vue 2 + Element UI              |
| 后端技术栈 | Spring Boot 3 + Spring AI + JPA |
| 数据库   | MySQL 8 + Redis                 |
| 目标平台  | Web（PC + 移动端自适应）                |


---

## 目录

1. [项目概述](#1-项目概述)
2. [用户角色与权限](#2-用户角色与权限)
3. [用户注册与登录模块](#3-用户注册与登录模块)
4. [AI 图像生成网关（Spring AI 中转）](#4-ai-图像生成网关spring-ai-中转)
5. [聊天窗口模块](#5-聊天窗口模块)
6. [模版广场模块](#6-模版广场模块)
7. [后台管理模块](#7-后台管理模块)
8. [支付与充值模块](#8-支付与充值模块)
9. [数据库核心表设计](#9-数据库核心表设计)
10. [接口规范](#10-接口规范)
11. [非功能性需求](#11-非功能性需求)
12. [项目开发里程碑](#12-项目开发里程碑)

- [附录 A：术语表](#附录-a术语表)
- [附录 B：参考资料](#附录-b参考资料)

---

## 1. 项目概述

### 1.1 项目背景

本项目旨在建设一款基于大模型的 AI 图像生成平台，提供文字对话、图像生成、模版广场等核心能力。平台面向个人用户及企业用户，通过积分/次数体系进行商业化变现，同时通过邀请裂变机制实现用户增长。

### 1.2 项目目标

- 提供流畅的 AI 对话与图像生成体验（GPT-Image-2 模型接入）
- 支持手机验证码与邮箱两种注册方式，并提供邀请码注册奖励机制
- 搭建模版广场，支持用户生成图发布与运营后台管理
- 集成微信支付，支持用户在线充值购买生成次数
- 构建 Spring Boot + Spring AI 中转网关，屏蔽直接对外暴露 OpenAI Key 的风险

### 1.3 技术选型总览


| 层级    | 技术                                     | 说明                          |
| ----- | -------------------------------------- | --------------------------- |
| 前端    | Vue 2 + Vuex + Vue Router + Element UI | SPA 单页应用，PC/移动自适应           |
| 后端框架  | Spring Boot 3 + Spring Data JPA        | RESTful API，Hibernate ORM   |
| AI 网关 | Spring AI + Spring WebFlux             | 透传转发 OpenAI GPT-Image-2 API |
| 数据库   | MySQL 8                                | 主业务数据持久化                    |
| 缓存    | Redis                                  | 短信验证码、Token、邀请码缓存           |
| 消息    | Spring Event / RabbitMQ（可选）            | 异步审核通知                      |
| 对象存储  | 阿里云 OSS / MinIO                        | 用户生成图片存储                    |
| 支付    | 微信支付 v3                                | 充值购买次数                      |
| 短信    | 阿里云短信 / 腾讯云短信                          | 手机验证码                       |
| 邮件    | Spring Mail + SMTP                     | 邮箱验证码                       |
| 部署    | Docker + Nginx                         | 容器化部署                       |


---

## 2. 用户角色与权限


| 角色    | 标识      | 主要权限描述                                 |
| ----- | ------- | -------------------------------------- |
| 游客    | `GUEST` | 浏览模版广场（只读），无法生成图片，可注册/登录               |
| 普通用户  | `USER`  | 注册后默认赠送 3 次生成机会；可对话、生成图、管理个人图库、发布模版、充值 |
| 超级管理员 | `ADMIN` | 管理用户、管理模版、审核违规内容、配置系统参数、查看统计报表         |


---

## 3. 用户注册与登录模块

### 3.1 功能概述

本模块涵盖用户注册、登录、账户管理及邀请体系相关功能。注册方式支持手机号+短信验证码、邮箱+邮件验证码两种途径，并支持通过邀请链接/邀请码注册享受额外奖励。

### 3.2 手机号注册

#### 3.2.1 业务流程

1. 用户输入手机号，点击「获取验证码」
2. 系统校验手机号格式合法性，并检查是否已注册
3. 调用短信服务发送 6 位数字验证码（有效期 5 分钟，同一手机号 60 秒内限 1 次发送）
4. 用户填写验证码、密码、昵称（必填）
5. 系统校验验证码正确性，创建用户账户
6. 默认赠送 3 次图像生成机会，写入用户积分账户
7. 若 URL 参数中携带 `invite_code`，触发邀请奖励逻辑（见 3.5 节）

#### 3.2.2 验证规则


| 字段  | 规则                        |
| --- | ------------------------- |
| 手机号 | 11位纯数字，正则：`^1[3-9]\d{9}$` |
| 验证码 | 6位数字，Redis缓存，5分钟过期        |
| 密码  | 8-20位，需含字母与数字             |
| 昵称  | 2-20个字符，不可含特殊符号           |


### 3.3 邮箱注册

流程与手机号注册类似，区别在于发送邮件验证码。邮件包含 6 位数字验证码，有效期 10 分钟。邮箱格式需符合标准 RFC 5322。

### 3.4 登录

- 支持手机号 + 密码登录
- 支持邮箱 + 密码登录
- 登录成功后返回 JWT Token（有效期 7 天），存于前端 localStorage
- 连续登录失败 5 次，锁定账号 30 分钟
- 支持「记住我」选项，延长 Token 有效期至 30 天

### 3.5 邀请码机制

#### 3.5.1 邀请链接生成

每个注册用户都拥有唯一邀请码（6位字母数字组合），可在个人中心查看。系统自动生成邀请链接，格式为：

```
https://yourdomain.com/register?invite_code=XXXXXX
```

#### 3.5.2 邀请奖励规则


| 角色   | 奖励时机           | 奖励内容                |
| ---- | -------------- | ------------------- |
| 被邀请人 | 通过邀请链接/邀请码完成注册 | 额外获得 3 次生成机会（共 6 次） |
| 邀请人  | 被邀请人首次完成注册     | 获得 5 次生成机会          |
| 邀请人  | 被邀请人首次完成付费充值   | 额外获得 10 次生成机会       |


#### 3.5.3 URL 邀请码识别

前端在路由守卫（`router.beforeEach`）中解析 URL 中的 `invite_code` 参数，存入 Vuex Store 及 sessionStorage；注册时随表单数据一并提交后端校验。

---

## 4. AI 图像生成网关（Spring AI 中转）

### 4.1 设计目标

- 隐藏 OpenAI API Key，前端请求打到自有网关而非直接访问 OpenAI
- 统一鉴权、次数扣减、日志记录
- 支持 GPT-Image-2 图像生成接口透传
- 使用 Spring WebFlux 实现非阻塞流式转发

### 4.2 网关架构


| 组件                            | 职责                                        |
| ----------------------------- | ----------------------------------------- |
| `GatewayController` (WebFlux) | 接收前端请求，验证JWT，扣减次数，转发至 OpenAI              |
| `ReactiveOpenAiClient`        | 封装 Spring AI WebClient，构造 OpenAI 请求，处理响应流 |
| `QuotaService`                | 查询并原子性扣减用户生成次数（Redis + DB 双写）             |
| `ImageStoreService`           | 将 OpenAI 返回的图片 URL/Base64 异步存储至 OSS       |
| `AuditFilter`                 | 异步调用大模型图片审核，不阻塞主流程                        |


### 4.3 关键接口设计

#### 4.3.1 图像生成接口


| 属性   | 值                                             |
| ---- | --------------------------------------------- |
| 接口路径 | `POST /api/v1/ai/image/generate`              |
| 认证方式 | Bearer JWT                                    |
| 请求体  | `{ prompt: string, size: string, n: number }` |
| 响应   | `{ taskId, imageUrl, remainingQuota }`        |
| 次数消耗 | 每次调用扣减 1 次，次数不足返回 402                         |


#### 4.3.2 对话接口（文字）


| 属性   | 值                                            |
| ---- | -------------------------------------------- |
| 接口路径 | `POST /api/v1/ai/chat`                       |
| 认证方式 | Bearer JWT                                   |
| 请求体  | `{ messages: [{role, content}], sessionId }` |
| 响应   | Server-Sent Events (SSE) 流式返回                |
| 次数消耗 | 纯文字对话不消耗生成次数                                 |


### 4.4 Spring AI + WebFlux 核心代码思路

```java
// ImageGatewayController.java (Spring WebFlux)
@PostMapping("/image/generate")
public Mono<ResponseEntity<ImageResult>> generate(
    @RequestBody ImageRequest req,
    @AuthenticationPrincipal UserDetails user) {
  return quotaService.deductQuota(user.getUsername())
    .then(openAiClient.generateImage(req))
    .flatMap(imageStoreService::saveAndReturn)
    .map(ResponseEntity::ok);
}
```

```java
// ReactiveOpenAiClient.java
@Component
public class ReactiveOpenAiClient {
  private final WebClient webClient;

  public Mono<String> generateImage(ImageRequest req) {
    return webClient.post()
      .uri("https://api.openai.com/v1/images/generations")
      .header("Authorization", "Bearer " + apiKey)
      .bodyValue(req)
      .retrieve()
      .bodyToMono(OpenAiImageResponse.class)
      .map(r -> r.getData().get(0).getUrl());
  }
}
```

---

## 5. 聊天窗口模块

### 5.1 功能说明

聊天窗口是用户与 AI 交互的核心界面，支持多种交互模式：

- **纯文字对话**：用户输入文字，AI 流式返回文字回答
- **文字生成图**：用户描述图片内容，AI 调用 GPT-Image-2 生成并展示图片
- **图生图**（可选扩展）：上传参考图+提示词，生成变体图

### 5.2 会话管理


| 字段           | 类型           | 说明                   |
| ------------ | ------------ | -------------------- |
| `session_id` | VARCHAR(36)  | UUID，每个对话会话唯一标识      |
| `user_id`    | BIGINT       | 所属用户                 |
| `title`      | VARCHAR(100) | 会话标题（取首条消息前20字）      |
| `mode`       | ENUM         | `CHAT` / `IMAGE_GEN` |
| `created_at` | DATETIME     | 创建时间                 |
| `updated_at` | DATETIME     | 最后活跃时间               |


### 5.3 消息类型


| 消息类型    | role值       | content格式                                           |
| ------- | ----------- | --------------------------------------------------- |
| 用户文字消息  | `user`      | 纯文本字符串                                              |
| AI 文字回复 | `assistant` | Markdown 文本（前端渲染）                                   |
| 用户生图请求  | `user`      | `{ type:'image_gen', prompt:'...' }`                |
| AI 生图结果 | `assistant` | `{ type:'image', url:'https://...', prompt:'...' }` |


### 5.4 UI 交互要求

- 消息列表支持无限滚动加载历史记录（分页，每页 20 条）
- AI 回复支持 Markdown 渲染（marked.js）
- 图像生成时显示骨架屏加载动画，生成完成后平滑展示图片
- 输入框支持 `Shift+Enter` 换行，`Enter` 发送
- 侧边栏展示历史会话列表，支持新建会话、重命名、删除
- 生成次数实时显示在顶部导航栏

---

## 6. 模版广场模块

### 6.1 模版概念

模版是可复用的图像生成提示词（Prompt）组合，用户可一键使用模版快速生成图片。模版来源：

- 管理员在后台手动新增
- 用户将自己生成的满意图片发布为模版，经管理员审核后上架

### 6.2 模版数据模型


| 字段                | 类型           | 说明                                  |
| ----------------- | ------------ | ----------------------------------- |
| `id`              | BIGINT PK    | 主键                                  |
| `title`           | VARCHAR(100) | 模版名称                                |
| `description`     | VARCHAR(500) | 模版描述                                |
| `prompt`          | TEXT         | 核心提示词                               |
| `cover_image_url` | VARCHAR(500) | 封面图片 URL（OSS）                       |
| `category_id`     | BIGINT FK    | 所属分类                                |
| `source_type`     | ENUM         | `ADMIN` / `USER`                    |
| `creator_id`      | BIGINT FK    | 创作者用户ID（管理员创建为null）                 |
| `status`          | ENUM         | `PENDING` / `APPROVED` / `REJECTED` |
| `use_count`       | INT          | 被使用次数                               |
| `like_count`      | INT          | 点赞数                                 |
| `created_at`      | DATETIME     | 创建时间                                |


### 6.3 用户发布模版流程

1. 用户在个人图库中选择一张已生成的图片
2. 点击「发布为模版」，填写模版标题、描述、分类
3. 提交后状态为 `PENDING`（待审核）
4. 管理员在后台审核（含 AI 自动审核，见第 7 节）
5. 审核通过后状态变为 `APPROVED`，模版上架展示；审核不通过则通知用户原因

### 6.4 模版广场前端功能

- 首页展示热门/最新/分类模版，瀑布流布局
- 支持按分类、关键词搜索筛选
- 模版卡片展示封面图、标题、使用次数、点赞数
- 一键使用模版：自动填充提示词至聊天窗口并跳转
- 支持收藏模版功能

---

## 7. 后台管理模块

### 7.1 模版审核（AI 自动 + 人工）

#### 7.1.1 AI 自动审核

当用户提交模版后，系统自动触发 AI 图像审核流程：

1. 调用内容审核 API（如阿里云内容安全 / OpenAI Vision 识别接口）
2. AI 对封面图进行违规检测（涉黄、涉政、涉暴等）
3. 若 AI 判定违规（置信度 > 阈值），自动将状态置为 `REJECTED`，并记录原因
4. 若 AI 判定合规，流转至人工审核队列（状态保持 `PENDING`）
5. 人工审核员可查看 AI 建议，最终做出审批决定

```java
// AuditService.java
@Async
public void autoAudit(Long templateId, String imageUrl) {
  AuditResult result = visionAuditClient.check(imageUrl);
  if (result.getScore() > auditThreshold) {
    templateRepository.updateStatus(templateId, TemplateStatus.REJECTED, result.getReason());
    notificationService.notifyUser(templateId, "您的模版因涉及违规内容未通过审核");
  } else {
    // 转人工审核队列，附带AI建议分数
    auditQueueService.enqueue(templateId, result);
  }
}
```

#### 7.1.2 人工审核界面

- 列表展示待审核模版，含 AI 审核结论与置信度评分
- 支持批量审核（批量通过 / 批量拒绝）
- 拒绝时需填写拒绝原因，系统自动通知用户

### 7.2 用户管理

- 查看用户列表，支持按手机号/邮箱/昵称搜索
- 查看用户详情：注册时间、生成次数、充值记录、邀请关系树
- 支持封禁/解封用户账号
- 手动调整用户生成次数余额

### 7.3 系统配置

- 配置新用户注册赠送次数（默认 3 次）
- 配置邀请奖励次数规则
- 配置 AI 审核置信度阈值
- 配置支付套餐（次数/价格）

### 7.4 数据报表

- 日/周/月 新增用户趋势
- 图像生成量统计
- 充值收入统计
- 模版使用排行榜

---

## 8. 支付与充值模块

### 8.1 充值套餐


| 套餐名称 | 次数    | 价格（元）   | 备注               |
| ---- | ----- | ------- | ---------------- |
| 体验包  | 10 次  | ¥6.00   | 适合新用户试用          |
| 基础包  | 50 次  | ¥25.00  | 性价比首选，折合 ¥0.5/次  |
| 标准包  | 150 次 | ¥60.00  | 折合 ¥0.4/次        |
| 专业包  | 500 次 | ¥150.00 | 折合 ¥0.3/次，适合重度用户 |


### 8.2 微信支付流程

```
用户选择套餐
    │
    ▼
POST /api/v1/pay/create-order  ──▶  创建本地订单（PENDING）
    │
    ▼
调用微信支付 v3 下单接口
    │
    ▼
返回 prepay_id / code_url
    │
    ▼
前端唤起微信支付（JSAPI / Native 扫码）
    │
    ▼
用户完成支付
    │
    ▼
微信回调 POST /api/v1/pay/wx-notify
    │
    ▼
校验签名 → 更新订单 PAID → 增加用户次数余额
    │
    ▼
前端轮询订单状态 / WebSocket 推送 → 展示支付成功
```

1. 用户选择套餐，点击「立即购买」
2. 前端调用后端 `POST /api/v1/pay/create-order` 创建订单
3. 后端调用微信支付 v3 JSAPI/Native 下单，获取 `prepay_id`
4. 前端唤起微信支付收银台（H5 或扫码）
5. 用户完成支付，微信回调后端 Webhook 接口
6. 后端校验签名，更新订单状态为 `PAID`，增加用户次数余额
7. 前端轮询订单状态（或 WebSocket 推送），展示支付成功提示

### 8.3 订单数据模型


| 字段                  | 类型            | 说明                                            |
| ------------------- | ------------- | --------------------------------------------- |
| `id`                | BIGINT PK     | 主键                                            |
| `order_no`          | VARCHAR(64)   | 业务订单号（唯一）                                     |
| `user_id`           | BIGINT FK     | 用户ID                                          |
| `package_id`        | BIGINT FK     | 套餐ID                                          |
| `amount`            | DECIMAL(10,2) | 支付金额（元）                                       |
| `quota_granted`     | INT           | 购买的次数                                         |
| `pay_type`          | ENUM          | `WECHAT`                                      |
| `status`            | ENUM          | `PENDING` / `PAID` / `REFUNDED` / `CANCELLED` |
| `wx_transaction_id` | VARCHAR(64)   | 微信交易号                                         |
| `paid_at`           | DATETIME      | 支付时间                                          |
| `created_at`        | DATETIME      | 创建时间                                          |


---

## 9. 数据库核心表设计


| 表名                    | 用途                                                                                 |
| --------------------- | ---------------------------------------------------------------------------------- |
| `t_user`              | 用户基本信息（id, phone, email, nickname, password_hash, invite_code, inviter_id, status） |
| `t_user_quota`        | 用户生成次数账户（user_id, balance, total_granted, total_used）                              |
| `t_quota_log`         | 次数变动明细（user_id, change_amount, reason, created_at）                                 |
| `t_invite_reward`     | 邀请奖励记录（inviter_id, invitee_id, reward_type, reward_quota）                          |
| `t_ai_session`        | AI 对话会话（id, user_id, title, mode, created_at）                                      |
| `t_ai_message`        | 消息记录（id, session_id, role, content_type, content, created_at）                      |
| `t_image`             | 用户生成图片库（id, user_id, session_id, image_url, prompt, created_at）                    |
| `t_template`          | 模版（id, title, prompt, cover_url, status, source_type, creator_id）                  |
| `t_template_category` | 模版分类（id, name, sort_order）                                                         |
| `t_pay_package`       | 充值套餐配置（id, name, quota, price, status）                                             |
| `t_pay_order`         | 支付订单（id, order_no, user_id, amount, status, wx_transaction_id）                     |
| `t_audit_log`         | AI 审核日志（id, target_type, target_id, result, score, reason）                         |


### 9.1 核心建表 SQL（部分示例）

```sql
-- 用户表
CREATE TABLE t_user (
  id           BIGINT AUTO_INCREMENT PRIMARY KEY,
  phone        VARCHAR(20)  UNIQUE,
  email        VARCHAR(100) UNIQUE,
  nickname     VARCHAR(50)  NOT NULL,
  password_hash VARCHAR(100) NOT NULL,
  invite_code  VARCHAR(10)  UNIQUE NOT NULL,
  inviter_id   BIGINT,
  status       TINYINT DEFAULT 1 COMMENT '1正常 0封禁',
  created_at   DATETIME DEFAULT CURRENT_TIMESTAMP,
  updated_at   DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- 次数账户表
CREATE TABLE t_user_quota (
  id            BIGINT AUTO_INCREMENT PRIMARY KEY,
  user_id       BIGINT UNIQUE NOT NULL,
  balance       INT DEFAULT 0  COMMENT '当前余额',
  total_granted INT DEFAULT 0  COMMENT '累计获得',
  total_used    INT DEFAULT 0  COMMENT '累计消耗',
  updated_at    DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- 模版表
CREATE TABLE t_template (
  id             BIGINT AUTO_INCREMENT PRIMARY KEY,
  title          VARCHAR(100) NOT NULL,
  description    VARCHAR(500),
  prompt         TEXT         NOT NULL,
  cover_image_url VARCHAR(500),
  category_id    BIGINT,
  source_type    ENUM('ADMIN','USER') NOT NULL,
  creator_id     BIGINT,
  status         ENUM('PENDING','APPROVED','REJECTED') DEFAULT 'PENDING',
  use_count      INT DEFAULT 0,
  like_count     INT DEFAULT 0,
  created_at     DATETIME DEFAULT CURRENT_TIMESTAMP
);
```

---

## 10. 接口规范

### 10.1 通用响应格式

```json
{
  "code": 200,
  "message": "success",
  "data": { },
  "timestamp": 1716800000
}
```

### 10.2 业务状态码


| 状态码   | 含义            |
| ----- | ------------- |
| `200` | 操作成功          |
| `400` | 请求参数错误        |
| `401` | 未登录或 Token 过期 |
| `402` | 次数不足，无法生成     |
| `403` | 权限不足          |
| `404` | 资源不存在         |
| `429` | 请求频率过高        |
| `500` | 服务器内部错误       |


### 10.3 主要接口列表


| 模块  | 方法     | 路径                                   | 说明              |
| --- | ------ | ------------------------------------ | --------------- |
| 认证  | `POST` | `/api/v1/auth/send-sms`              | 发送手机验证码         |
| 认证  | `POST` | `/api/v1/auth/send-email`            | 发送邮件验证码         |
| 认证  | `POST` | `/api/v1/auth/register`              | 用户注册（含邀请码）      |
| 认证  | `POST` | `/api/v1/auth/login`                 | 用户登录            |
| 认证  | `POST` | `/api/v1/auth/refresh`               | 刷新 Token        |
| AI  | `POST` | `/api/v1/ai/chat`                    | 文字对话（SSE）       |
| AI  | `POST` | `/api/v1/ai/image/generate`          | 生成图片            |
| AI  | `GET`  | `/api/v1/ai/sessions`                | 获取会话列表          |
| AI  | `GET`  | `/api/v1/ai/sessions/{id}/messages`  | 获取会话消息          |
| 模版  | `GET`  | `/api/v1/templates`                  | 获取模版广场列表        |
| 模版  | `POST` | `/api/v1/templates/publish`          | 用户发布模版          |
| 模版  | `POST` | `/api/v1/templates/{id}/use`         | 使用模版            |
| 模版  | `POST` | `/api/v1/templates/{id}/like`        | 点赞模版            |
| 支付  | `POST` | `/api/v1/pay/create-order`           | 创建充值订单          |
| 支付  | `POST` | `/api/v1/pay/wx-notify`              | 微信支付回调（Webhook） |
| 支付  | `GET`  | `/api/v1/pay/order/{orderNo}`        | 查询订单状态          |
| 支付  | `GET`  | `/api/v1/pay/packages`               | 获取充值套餐列表        |
| 用户  | `GET`  | `/api/v1/user/profile`               | 获取个人信息          |
| 用户  | `GET`  | `/api/v1/user/quota`                 | 查询剩余次数          |
| 用户  | `GET`  | `/api/v1/user/images`                | 个人图库列表          |
| 用户  | `GET`  | `/api/v1/user/invite`                | 获取我的邀请码/链接      |
| 管理  | `GET`  | `/api/v1/admin/templates/pending`    | 待审核模版列表         |
| 管理  | `POST` | `/api/v1/admin/templates/{id}/audit` | 审核模版            |
| 管理  | `GET`  | `/api/v1/admin/users`                | 用户列表            |
| 管理  | `POST` | `/api/v1/admin/users/{id}/ban`       | 封禁用户            |


---

## 11. 非功能性需求

### 11.1 性能需求

- API 接口平均响应时间 < 500ms（AI 生成类接口除外）
- 图像生成接口 P95 响应时间 < 15s（依赖 OpenAI 服务）
- 系统支持并发用户数 ≥ 1000
- 数据库查询优化：核心查询均建立索引，慢查询 < 100ms

### 11.2 安全需求

- 所有 API 通信使用 HTTPS/TLS 1.2+
- 密码使用 BCrypt 加密存储，盐值强度 ≥ 12
- JWT Token 需设置合理过期时间，Refresh Token 单次有效
- OpenAI API Key 仅存于服务端配置，不下发至客户端
- 输入内容进行 XSS/SQL 注入过滤
- 微信支付回调签名必须验证
- 图片审核防止违规内容上传

### 11.3 可用性需求

- 核心服务 SLA ≥ 99.5%
- 支持 Docker 容器化部署，支持水平扩展
- 数据库每日凌晨自动备份

### 11.4 前端兼容性

- Chrome 80+，Safari 13+，Firefox 75+，Edge 80+
- 移动端 iOS 13+ / Android 9+
- 分辨率兼容：1280px ~ 2560px（PC），375px ~ 414px（手机）

---

## 12. 项目开发里程碑


| 阶段             | 周期        | 主要交付物                                                    |
| -------------- | --------- | -------------------------------------------------------- |
| Phase 1 - 基础架构 | 第 1-2 周   | Spring Boot 项目骨架、数据库建表、Spring AI 网关基础版、Vue2 项目初始化、用户注册登录 |
| Phase 2 - 核心功能 | 第 3-5 周   | 邀请码体系、聊天窗口（文字+图像生成）、个人图库、次数账户体系                          |
| Phase 3 - 模版广场 | 第 6-7 周   | 模版广场展示、用户发布模版、后台审核（AI + 人工）                              |
| Phase 4 - 支付   | 第 8 周     | 微信支付集成、充值套餐管理、订单中心                                       |
| Phase 5 - 后台管理 | 第 9-10 周  | 管理员控制台完整功能、数据报表、系统配置                                     |
| Phase 6 - 测试上线 | 第 11-12 周 | 联调测试、性能压测、Bug 修复、生产部署                                    |


---

## 附录 A：术语表


| 术语             | 解释                                      |
| -------------- | --------------------------------------- |
| GPT-Image-2    | OpenAI 推出的图像生成模型，支持文字描述生成高质量图片          |
| Spring AI      | Spring 官方 AI 框架，封装 OpenAI 等大模型 API 调用   |
| Spring WebFlux | Spring 响应式 Web 框架，基于 Reactor，适合异步非阻塞场景  |
| JWT            | JSON Web Token，用于无状态身份认证的令牌标准           |
| OSS            | 对象存储服务（Object Storage Service），用于存储用户图片 |
| SSE            | Server-Sent Events，服务端推送事件，用于 AI 流式回复   |
| 次数/Quota       | 用户可使用 AI 生成图片的次数，可通过注册赠送或充值获得           |
| Prompt         | 提示词，用于指导 AI 生成特定风格或内容的图片描述文字            |


## 附录 B：参考资料

- OpenAI Images API 文档：[https://platform.openai.com/docs/api-reference/images](https://platform.openai.com/docs/api-reference/images)
- Spring AI 官方文档：[https://docs.spring.io/spring-ai/reference/](https://docs.spring.io/spring-ai/reference/)
- 微信支付 v3 API 文档：[https://pay.weixin.qq.com/docs/merchant/](https://pay.weixin.qq.com/docs/merchant/)
- Vue 2 官方文档：[https://v2.vuejs.org/](https://v2.vuejs.org/)
- Spring WebFlux 文档：[https://docs.spring.io/spring-framework/reference/web/webflux.html](https://docs.spring.io/spring-framework/reference/web/webflux.html)

---

*文档结束 — AI 图像生成平台需求规格说明书 v1.0*