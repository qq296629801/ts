# 问题修复清单（AI 图像平台）

按 **现象** 查表 → 对照 **原因** → 执行 **处理步骤**。适用于本地 Docker、CentOS 服务器部署。

**通用诊断命令**（在 `deploy/` 目录）：

```bash
docker compose -f docker-compose.dev.yml ps
docker compose -f docker-compose.dev.yml logs --tail=80 gateway-service
docker compose -f docker-compose.dev.yml logs --tail=80 platform-api
docker compose -f docker-compose.dev.yml logs --tail=30 nginx
```

---

## 快速索引

| 现象 | 跳转 |
|------|------|
| 浏览器 502，`gateway` 起不来 | [§1](#1-nginx-502--gateway-未启动) |
| `sessions` / 消息 404 | [§2](#2-api-404-sessions-或消息) |
| 生图 502，日志有中继英文错误 | [§3](#3-生图-502--上游中继问题) |
| `EOF` / `prematurely closed` | [§3](#3-生图-502--上游中继问题) |
| 前端一直转圈 ~60s 失败 | [§4](#4-前端超时-60-秒) |
| 有图但裂图/打不开 | [§5](#5-图片-url-无法访问) |
| 402 次数不足 | [§6](#6-402-次数不足) |
| 409 已有进行中任务 | [§7](#7-409-生图并发锁) |
| `npm EAI_AGAIN` 构建失败 | [§8](#8-docker-构建-npm-安装失败) |
| 构建 OOM / 进程被杀 | [§9](#9-内存不足--oom) |
| 拉镜像超时 | [§10](#10-拉取-docker-镜像超时) |
| 验证码收不到 | [§11](#11-短信验证码) |
| 支付不成功 | [§12](#12-微信支付) |

---

## 1. Nginx 502 / Gateway 未启动

### 现象

- 访问 `http://域名或IP/api/v1/ai/image/generate` → **502 Bad Gateway**
- `docker compose ps` 中 **无** `gateway-service`，或反复重启
- 日志：`NoSuchMethodException: RelayImageClient.<init>()`

### 原因

- Gateway 进程未起来，Nginx 连不上 `gateway-service:8081`
- `RelayImageClient` 多构造函数未标 `@Autowired`，Spring 启动失败（已修复，需 **重建镜像**）

### 处理

```bash
cd deploy
docker compose -f docker-compose.dev.yml logs --tail=50 gateway-service
# 必须看到：Started GatewayApplication

docker compose -f docker-compose.dev.yml up -d --build gateway-service
docker compose -f docker-compose.dev.yml restart nginx
```

重建 **platform-api** 后若仍 502，务必 `restart nginx`（动态 DNS 解析上游）。

---

## 2. API 404（sessions 或消息）

### 现象

- `POST /api/v1/ai/sessions` → **404**
- 聊天页无法创建会话

### 原因

Nginx 把 **整段** `/api/v1/ai/` 都转给了 gateway，而 `sessions` 属于 **platform-api**。

### 正确路由（`deploy/nginx/nginx.conf`）

| 路径 | 上游 |
|------|------|
| `/api/v1/ai/chat` | gateway:8081 |
| `/api/v1/ai/image/` | gateway:8081 |
| 其余 `/api/` | platform-api:8080 |

### 处理

```bash
# 核对 nginx.conf 后
docker compose -f docker-compose.dev.yml restart nginx
```

---

## 3. 生图 502 / 上游中继问题

### 现象

| 日志关键词 | 含义 |
|------------|------|
| `upstream did not return any image output` | 中继返回业务错误（HTTP 200 + JSON error） |
| `EOF reached while reading` | 读响应体时连接被掐断（常见 ~50–60s） |
| `Connection prematurely closed BEFORE response` | 上游在返回头之前断连 |
| `中继生图重试第 N 次` | 正在自动重试（最多 3 次） |
| `图像生成失败，次数已回滚` | 网关已回滚配额，属预期行为 |

### 原因

- **中继侧**：Key 无效、余额不足、服务不稳定、`quality=high` 耗时超过中继/CDN ~60s 空闲超时
- **非网关逻辑错误**时，次数会自动回滚

### 处理

**① 在 gateway 容器内直连中继**

```bash
docker compose -f docker-compose.dev.yml exec -T gateway-service sh -c '
curl -sS --max-time 120 -X POST "$IMAGE_API_URL" \
  -H "Authorization: Bearer $OPENAI_API_KEY" \
  -H "Content-Type: application/json" \
  -d "{\"model\":\"gpt-image-2\",\"prompt\":\"red dot\",\"size\":\"1024x1024\",\"quality\":\"medium\",\"n\":1}"' | head -c 200
'
```

- 若此处也失败 → 换 Key、联系中继商、保持 `IMAGE_QUALITY=medium`
- 若此处成功、浏览器仍失败 → 查 [§4](#4-前端超时-60-秒)、Nginx 超时

**② 检查 `deploy/.env`**

```env
OPENAI_API_KEY=有效Key
IMAGE_API_URL=https://.../v1/images/generations
IMAGE_QUALITY=medium
```

**③ 重建 gateway**

```bash
docker compose -f docker-compose.dev.yml up -d --build gateway-service
```

**④ 冒烟脚本**（栈已启动）

```bash
./tests/e2e/smoke-relay-image.sh
```

### 历史代码问题（已修复，供对照）

| 错误 | 处理 |
|------|------|
| `restricted header name: "Connection"` | 勿在 JDK HttpClient 手动设 `Connection` 头 |
| Reactor `PrematureCloseException` | 已改用 JDK HttpClient + HTTP/1.1 |

---

## 4. 前端超时（~60 秒）

### 现象

- 页面提示「生图失败」，Network 里请求 **canceled** 或约 60s 失败
- 网关日志仍在跑、甚至稍后成功

### 原因

`frontend/web/src/api/http.js` 默认 **axios timeout 60s**；生图常需 30s～3min。

### 处理

- 生图接口应单独设置 `timeout: 360000`（`ChatRoom.vue` 已改）
- 重建 **frontend** 镜像后部署
- 用户侧：**只点一次**，等待完成

Nginx 生图路由需 `proxy_read_timeout 360s`（`nginx.conf` 已配）。

---

## 5. 图片 URL 无法访问

### 现象

- 生图返回 `imageUrl`，浏览器 **裂图**
- URL 形如 `http://localhost:9000/ai-images/...`

### 原因

`app.minio.public-endpoint` 指向用户浏览器访问不到的地址。

### 处理

在 `docker-compose.dev.yml` → `gateway-service.environment` 增加（换成公网可访问地址）：

```yaml
APP_MINIO_PUBLIC_ENDPOINT: http://你的公网IP或域名:9000
```

或使用 **阿里云 OSS** + CDN 域名。

- 防火墙/安全组需放行 **9000**（或改 Nginx 反代 MinIO，只开 80/443）
- 修改后：`docker compose -f docker-compose.dev.yml up -d --build gateway-service`

---

## 6. 402 次数不足

### 现象

- 响应 `402` / `次数不足，请充值`

### 原因

用户配额余额为 0（**不是** API Key 问题）。

### 处理

- 开发环境：管理员 `19900000000` / `Admin1234`（dev 自动补至 100 次）
- 或充值流程增加次数
- 查库：`t_user_quota` / 管理后台调次数

---

## 7. 409 生图并发锁

### 现象

- `409` / `已有进行中的生图任务`

### 原因

单用户 **单路生图**（Redis `gen:inflight:{userId}`），上次预扣未完结。

### 处理

- 等待上一笔结束或失败回滚（约 2 分钟 TTL）
- 勿连续多点生图
- 仍卡住：查 Redis、platform-api 日志，必要时清 `gen:inflight:*`（仅测试环境）

---

## 8. Docker 构建 npm 安装失败

### 现象

```
npm error code EAI_AGAIN
npm error getaddrinfo EAI_AGAIN registry.npmmirror.com
```

构建 **frontend** 镜像失败，整栈起不来。

### 原因

Docker 构建容器内 **DNS 不稳定**（CentOS 7 常见）。

### 处理

**① 配置 Docker DNS**（`/etc/docker/daemon.json`）

```json
{
  "dns": ["223.5.5.5", "114.114.114.114", "8.8.8.8"]
}
```

```bash
systemctl restart docker
```

**② 宿主机 resolv.conf 补充 nameserver**（可选）

**③ 仅重建 frontend**

```bash
cd deploy
docker compose -f docker-compose.dev.yml build frontend
docker compose -f docker-compose.dev.yml up -d
```

**④ 内存不足时** 见 [§9](#9-内存不足--oom)

---

## 9. 内存不足 / OOM

### 现象

- Maven/npm 构建中途退出 **137**
- 容器反复重启

### 原因

首次 `--build` 同时拉镜像、编 Java、跑 npm，内存 < 4G 易 OOM。

### 处理

```bash
# 增加 2G swap（示例）
fallocate -l 2G /swapfile || dd if=/dev/zero of=/swapfile bs=1M count=2048
chmod 600 /swapfile && mkswap /swapfile && swapon /swapfile
echo '/swapfile none swap sw 0 0' >> /etc/fstab
```

分步构建：

```bash
docker compose -f docker-compose.dev.yml up -d mysql redis minio minio-init
docker compose -f docker-compose.dev.yml build platform-api gateway-service
docker compose -f docker-compose.dev.yml build frontend
docker compose -f docker-compose.dev.yml up -d
```

---

## 10. 拉取 Docker 镜像超时

### 现象

`auth.docker.io` / `i/o timeout`

### 处理

见 [README.md](./README.md#拉镜像超时authdockerio--i-o-timeout)：DaoCloud 镜像前缀、`registry-mirrors`、或仅起中间件 + 本机 `mvn`/`npm`。

---

## 11. 短信验证码

### 现象

- 注册收不到短信

### 原因

开发环境验证码打在 **platform-api 日志**，未接真实短信网关。

### 处理

```bash
docker compose -f docker-compose.dev.yml logs -f platform-api | grep 验证码
```

生产需配置阿里云 SMS（见 `SmsCodeService`）。

---

## 12. 微信支付

### 现象

- 下单成功但无法真实付款；或回调不到账

### 原因

默认 **`app.wechat.mock-enabled:true`**（Mock 支付）。

### 处理

生产需：关闭 Mock、配置微信商户号、公网 HTTPS 回调 `POST /api/v1/pay/wx-notify`、验签。见 `specs/001-ai-image-platform/checklists/security-review.md`。

---

## 13. 环境变量与密钥

| 变量 | 说明 |
|------|------|
| `OPENAI_API_KEY` | 中继 Bearer，仅 gateway |
| `IMAGE_API_URL` | 生图完整 URL |
| `IMAGE_QUALITY` | 推荐 `medium` |
| `JWT_SECRET` | ≥32 字符，gateway 与 platform 一致 |
| `INTERNAL_API_KEY` | platform `/internal/**` |

- **勿提交** `deploy/.env` 到 Git
- 修改 `.env` 后需 **重建** 对应服务容器

---

## 14. 发布前检查（服务器）

```bash
# 1. 全部 Running
docker compose -f docker-compose.dev.yml ps

# 2. 网关已启动
docker compose -f docker-compose.dev.yml logs gateway-service | grep Started

# 3. 首页
curl -sS -o /dev/null -w "%{http_code}\n" http://127.0.0.1/

# 4. 中继（可选）
docker compose -f docker-compose.dev.yml exec -T gateway-service sh -c \
  'curl -sS --max-time 120 -X POST "$IMAGE_API_URL" -H "Authorization: Bearer $OPENAI_API_KEY" \
   -H "Content-Type: application/json" \
   -d "{\"model\":\"gpt-image-2\",\"prompt\":\"test\",\"size\":\"1024x1024\",\"quality\":\"medium\",\"n\":1}"' | head -c 150

# 5. 单元测试（本机或 CI）
cd backend && mvn -pl gateway-service test
```

---

## 15. 相关文档

| 文档 | 用途 |
|------|------|
| [deploy/README.md](./README.md) | 一键启动与常用命令 |
| [quickstart.md](../specs/001-ai-image-platform/quickstart.md) | 本地开发流程 |
| [security-review.md](../specs/001-ai-image-platform/checklists/security-review.md) | 生产安全加固 |
| [perf-baseline.md](../specs/001-ai-image-platform/checklists/perf-baseline.md) | 性能目标 |
| `tests/e2e/smoke-p1.sh` | 注册→登录→生图冒烟 |
| `tests/e2e/smoke-relay-image.sh` | 中继 + 网关生图冒烟 |

---

*最后更新：根据 2026-05 部署与中继联调经验整理。*
