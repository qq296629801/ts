# API 契约

**OpenAPI**：[`openapi.yaml`](./openapi.yaml)（OpenAPI 3.1）

## 服务边界

| 前缀 | 服务 | 说明 |
|------|------|------|
| `/api/v1/ai/*` | `gateway-service` (WebFlux) | 对话 SSE、生图；鉴权 + 配额 + Spring AI |
| `/api/v1/*`（除 ai） | `platform-api` (Spring MVC) | 认证、用户、模版、支付、管理 |
| `/internal/*` | `platform-api` | 仅内网；配额 reserve/commit/rollback |

## 统一响应

```json
{
  "code": 200,
  "message": "success",
  "data": {},
  "timestamp": 1716800000
}
```

## 业务状态码

| code | 含义 |
|------|------|
| 200 | 成功 |
| 400 | 参数错误 |
| 401 | 未登录 / Token 失效 |
| 402 | 次数不足 |
| 403 | 权限不足 |
| 404 | 资源不存在 |
| 409 | 生图进行中（单路并发） |
| 429 | 频率限制 |
| 500 | 服务器错误 |

## 契约测试

- `platform-api`：`@WebMvcTest` + OpenAPI 示例请求校验 JSON Schema
- `gateway-service`：`@WebFluxTest` + WireMock OpenAI + WireMock platform internal quota
