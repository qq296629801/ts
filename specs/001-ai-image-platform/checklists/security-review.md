# 安全审查清单（001-ai-image-platform）

**日期**：2026-05-27

## 已落实

| 项 | 说明 |
|----|------|
| API Key 服务端 | `OPENAI_API_KEY` 仅 gateway 环境变量，不下发前端 |
| JWT | 网关与 platform 共用 secret；日志脱敏 `LogSanitizer` |
| 支付回调 | `wx-notify` 无 JWT；生产需微信支付 v3 验签（当前 dev Mock） |
| 密码存储 | BCrypt(12) |
| 内部 API | `X-Internal-Api-Key` 保护 `/internal/**` |
| 管理员 | `ROLE_ADMIN` + `/api/v1/admin/**` |

## 待生产强化

- [ ] 微信支付真实验签与证书轮换
- [ ] 阿里云内容安全替换 dev 敏感词列表
- [ ] 限流（登录、发短信、生图）
- [ ] 审计日志留存与告警

## 代码扫描（自动化）

- `LogSanitizerTest`：JWT / sk- 模式脱敏
- `WebFluxBlockGuardTest`：网关主代码无 `.block(`

## 人工抽查建议

1. 开启 DEBUG 后生图一次，确认日志无完整 JWT 与 prompt 批量导出
2. 检查 API 响应体不含 `OPENAI_API_KEY`
3. 确认 `.env` 未提交 Git
