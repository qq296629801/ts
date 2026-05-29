# 开发环境 Docker 一键启动

## 快速开始

```bash
cd deploy
cp .env.example .env
# 编辑 .env，填入 OPENAI_API_KEY

docker compose -f docker-compose.dev.yml up -d --build
```

## 访问地址

| 入口 | URL |
|------|-----|
| **推荐**（Nginx 统一入口） | http://localhost |
| 前端直连 | http://localhost:8082 |
| 业务 API | http://localhost:8080 |
| AI 网关 | http://localhost:8081 |
| MinIO 控制台 | http://localhost:9001（minioadmin / minioadmin） |

## 常用命令

```bash
# 查看日志（验证码在 platform-api 日志里）
docker compose -f docker-compose.dev.yml logs -f platform-api

# API 502（重建过 platform 后）：重载 nginx 解析上游
docker compose -f docker-compose.dev.yml restart nginx

# 停止
docker compose -f docker-compose.dev.yml down

# 重建单个服务
docker compose -f docker-compose.dev.yml up -d --build platform-api
```

## 说明

- 首次 `--build` 会下载 Maven/npm 依赖，耗时较长。
- 开发管理员（dev profile）：手机 `19900000000` / 密码 `Admin1234`（platform-api 启动后自动创建）。
- 生图、对话依赖有效的 `OPENAI_API_KEY`（Bearer）及 `IMAGE_API_URL`（OpenAI 兼容 `/v1/images/generations`）。
- 推荐 `IMAGE_QUALITY=medium`；`high` 可能导致中继约 60s 断连（`EOF` / `upstream did not return any image output`）。
- 生图冒烟：`./tests/e2e/smoke-relay-image.sh`（栈已 `up` 后执行）。
- **502 排查**：先看 `gateway-service` 是否 `Started GatewayApplication`；若 `NoSuchMethodException: RelayImageClient` 需重建 gateway；若日志为中继错误则属上游，次数会自动回滚。

## 拉镜像超时（`auth.docker.io` / `i/o timeout`）

构建时要拉 `maven`、`node` 镜像，国内访问 Docker Hub 常超时。已默认改为 **DaoCloud 镜像**（`docker.m.daocloud.io/...`）。

请重新构建：

```bash
cd deploy
docker compose -f docker-compose.dev.yml build --no-cache
docker compose -f docker-compose.dev.yml up -d
```

仍失败时，任选其一：

1. **Docker Desktop → Settings → Docker Engine** 配置 registry mirrors，例如：
   ```json
   "registry-mirrors": ["https://docker.m.daocloud.io"]
   ```
2. **宿主机跑应用**（不构建 maven/node 镜像）：
   ```bash
   chmod +x deploy/scripts/dev-up-host.sh
   ./deploy/scripts/dev-up-host.sh
   ```
   或手动：`docker compose -f docker-compose.infra.yml up -d`，再在本机 `mvn` / `npm`。

## 仅基础设施（本地跑 Java/Vue）

若要在宿主机用 `mvn` / `npm` 热更新，可只启动中间件：

```bash
docker compose -f docker-compose.dev.yml up -d mysql redis minio minio-init
```
