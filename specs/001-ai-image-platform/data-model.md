# 数据模型：AI 图像生成平台

**功能**：`001-ai-image-platform` | **日期**：2026-06-04（更新退款与双渠道）  
**存储**：MySQL 8（权威）、Redis 7（验证码、Token、in-flight 锁、热点缓存）

## 实体关系概览

```text
User 1──1 UserQuota
User 1──* QuotaLog
User 1──* AiSession
User 1──* Image
User 1──* PayOrder
User 1──* InviteReward (as inviter or invitee)
User *──1 User (inviter, optional)
Template *──1 TemplateCategory
Template *──0..1 User (creator)
Template 1──* AuditLog
PayOrder *──1 PayPackage
```

## 表定义

### t_user

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | BIGINT | PK | |
| phone | VARCHAR(20) | UNIQUE, NULL | 手机号 |
| email | VARCHAR(100) | UNIQUE, NULL | 邮箱 |
| nickname | VARCHAR(50) | NOT NULL | 2–20 字符 |
| password_hash | VARCHAR(100) | NOT NULL | BCrypt cost≥12 |
| invite_code | VARCHAR(10) | UNIQUE, NOT NULL | 6 位字母数字 |
| inviter_id | BIGINT | FK→t_user, NULL | 邀请人 |
| role | ENUM | DEFAULT USER | GUEST 不落库；USER / ADMIN |
| status | TINYINT | DEFAULT 1 | 1 正常 0 封禁 |
| login_fail_count | INT | DEFAULT 0 | 连续失败次数 |
| locked_until | DATETIME | NULL | 锁定截止 |
| created_at | DATETIME | | |
| updated_at | DATETIME | | |

**规则**：phone/email 至少其一；注册时写入 `config_snapshot_id`（可选）记录注册时赠送规则版本。

### t_user_quota

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | BIGINT | PK | |
| user_id | BIGINT | UNIQUE, FK | |
| balance | INT | DEFAULT 0 | 当前余额 |
| total_granted | INT | DEFAULT 0 | 累计获得 |
| total_used | INT | DEFAULT 0 | 累计消耗 |
| updated_at | DATETIME | | |

### t_quota_log

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | BIGINT | PK | |
| user_id | BIGINT | FK | |
| change_amount | INT | NOT NULL | 正增负减 |
| reason | ENUM | NOT NULL | REGISTER_GIFT, INVITE_REGISTER, INVITE_PAY, IMAGE_GEN, RECHARGE, REFUND, ADMIN_ADJUST, ROLLBACK |
| ref_type | VARCHAR(32) | NULL | ORDER / SESSION / ADMIN |
| ref_id | VARCHAR(64) | NULL | |
| created_at | DATETIME | | |

### t_quota_reservation（预扣流水，支撑先扣后调）

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | BIGINT | PK | |
| user_id | BIGINT | FK | |
| status | ENUM | PENDING, COMMITTED, ROLLED_BACK | |
| expires_at | DATETIME | | 与 in-flight TTL 对齐 |
| created_at | DATETIME | | |

### t_invite_reward

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | BIGINT | PK | |
| inviter_id | BIGINT | FK | |
| invitee_id | BIGINT | FK | |
| reward_type | ENUM | INVITEE_REGISTER, INVITER_REGISTER, INVITER_FIRST_PAY | |
| reward_quota | INT | | |
| created_at | DATETIME | | |

**唯一约束**：(invitee_id, reward_type) 防止重复发放。

### t_ai_session

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | VARCHAR(36) | PK | UUID |
| user_id | BIGINT | FK | |
| title | VARCHAR(100) | | 首条消息摘要 |
| mode | ENUM | CHAT, IMAGE_GEN | |
| created_at | DATETIME | | |
| updated_at | DATETIME | | |

### t_ai_message

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | BIGINT | PK | |
| session_id | VARCHAR(36) | FK | |
| role | ENUM | user, assistant, system | |
| content_type | ENUM | TEXT, IMAGE_GEN_REQ, IMAGE_RESULT | |
| content | JSON/TEXT | | 文本或结构化 JSON |
| created_at | DATETIME | | |

### t_image

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | BIGINT | PK | |
| user_id | BIGINT | FK | |
| session_id | VARCHAR(36) | NULL | |
| image_url | VARCHAR(500) | NOT NULL | OSS URL |
| prompt | TEXT | | |
| created_at | DATETIME | | |

### t_template

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | BIGINT | PK | |
| title | VARCHAR(100) | | |
| description | VARCHAR(500) | | |
| prompt | TEXT | | |
| cover_image_url | VARCHAR(500) | | |
| category_id | BIGINT | FK | |
| source_type | ENUM | ADMIN, USER | |
| creator_id | BIGINT | NULL | |
| status | ENUM | PENDING, APPROVED, REJECTED | |
| reject_reason | VARCHAR(500) | NULL | |
| use_count | INT | DEFAULT 0 | |
| like_count | INT | DEFAULT 0 | |
| created_at | DATETIME | | |

### t_template_category

| id, name, sort_order, status |

### t_template_favorite

| user_id, template_id | UNIQUE |

### t_pay_package

| id, name, quota, price DECIMAL(10,2), status, sort_order |

### t_pay_order

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | PK |
| order_no | VARCHAR(64) | UNIQUE |
| user_id | BIGINT | FK |
| package_id | BIGINT | FK |
| amount | DECIMAL(10,2) | |
| quota_granted | INT | |
| pay_type | ENUM | WECHAT, ALIPAY |
| status | ENUM | PENDING, PAID, CANCELLED, REFUNDED |
| wx_transaction_id | VARCHAR(64) | NULL | 微信渠道可复用 |
| channel_trade_no | VARCHAR(64) | NULL | 渠道交易号（通用） |
| code_url | VARCHAR(512) | 扫码链接 |
| expires_at | DATETIME | created_at + 15min |
| paid_at | DATETIME | NULL |
| refunded_at | DATETIME | NULL | V9 迁移，退款完成时间 |
| refund_notify_id | VARCHAR(128) | NULL | 退款幂等键（如 `refund-{orderNo}`，写入 `t_pay_notify_log`） |
| created_at | DATETIME | |

### t_pay_notify_log

| notify_id, order_no, processed_at | 幂等 |

### t_audit_log

| id, target_type, target_id, result, score, reason, operator_id, created_at |

### t_system_config

| config_key, config_value JSON, updated_at | 注册赠送、邀请奖励、审核阈值等 |

## 状态机

### 支付订单

```text
PENDING ──(支付成功+验签)──► PAID
PENDING ──(15min超时/扫描)──► CANCELLED
PAID ──(管理员退款：余额足够+渠道成功)──► REFUNDED
```

**退款规则**：仅 PAID；`balance >= quota_granted` 方可发起；扣回全部发放次数后调渠道退款；失败则保持 PAID（可重试）。

### 模版

```text
PENDING ──(AI拒绝)──► REJECTED
PENDING ──(AI通过)──► 待人工 ──(通过)──► APPROVED
                              └──(拒绝)──► REJECTED
```

### 配额预扣

```text
PENDING ──(生图成功)──► COMMITTED
PENDING ──(失败/超时)──► ROLLED_BACK
```

## Redis 键

| 键模式 | TTL | 用途 |
|--------|-----|------|
| `sms:code:{phone}` | 5min | 短信验证码 |
| `email:code:{email}` | 10min | 邮件验证码 |
| `sms:limit:{phone}` | 60s | 发送频控 |
| `gen:inflight:{userId}` | 120s | 单路生图锁 |
| `refresh:token:{jti}` | 按策略 | Refresh 轮换 |
| `invite:pending:{sessionId}` | 会话级 | 可选，注册页 invite_code |

## 索引建议

- `t_user(phone)`, `t_user(email)`, `t_user(invite_code)`
- `t_ai_message(session_id, created_at)`
- `t_template(status, category_id)`, FULLTEXT(title, description) 可选
- `t_pay_order(user_id, status)`, `t_pay_order(expires_at)` 供超时扫描
- `t_quota_log(user_id, created_at)`
