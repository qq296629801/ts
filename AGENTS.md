<!-- SPECKIT START -->
**当前功能**：`001-ai-image-platform` — [plan.md](specs/001-ai-image-platform/plan.md) | [tasks.md](specs/001-ai-image-platform/tasks.md) | [quickstart.md](specs/001-ai-image-platform/quickstart.md)

**代码路径**：
| 模块 | 路径 | 端口 |
|------|------|------|
| 业务 API | `backend/platform-api` | 8080 |
| AI 网关 | `backend/gateway-service` | 8081 |
| 前端 | `frontend/web` | 8082（dev proxy） |
| 一键开发 | `deploy/docker-compose.dev.yml` | 全栈 + http://localhost |
| 中间件 | 同上（仅 mysql/redis/minio） | 本地 mvn/npm 时用 |
| 契约 | `specs/001-ai-image-platform/contracts/openapi.yaml` | — |
| 冒烟 | `tests/e2e/smoke-p1.sh` | — |

**技术栈**（见 `.specify/memory/constitution.md`）：Java 17、Spring Boot 3、WebFlux 网关 + Spring AI、JPA/H2(测)/MySQL(开)、Redis、Vue 2。
<!-- SPECKIT END -->


## 交互规则

- **语言**: 始终使用中文回复
- **思考逻辑**: 在给出代码前，先用两句话简述实现思路
- **精简模式**: 除非我要求，否则不需要解释基础语法
- **修正**: 如果我提出的需求违反了现有的架构，请直接指出并建议更好的方案
- **代码注释**: 代码注释和日志输出统一使用中文
