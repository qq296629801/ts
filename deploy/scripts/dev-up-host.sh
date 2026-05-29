#!/usr/bin/env bash
# 中间件 Docker + 应用在宿机构建（无需拉 maven/node 镜像）
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
cd "$ROOT/deploy"

echo "==> 启动 MySQL / Redis / MinIO"
docker compose -f docker-compose.infra.yml up -d

echo "==> 等待 MySQL 就绪..."
for i in $(seq 1 30); do
  if docker compose -f docker-compose.infra.yml exec -T mysql mysqladmin ping -h 127.0.0.1 -uroot -proot --silent 2>/dev/null; then
    break
  fi
  sleep 2
done

export JWT_SECRET="${JWT_SECRET:-dev-jwt-secret-at-least-32-chars-long!!}"
export INTERNAL_API_KEY="${INTERNAL_API_KEY:-dev-internal-key}"
export OPENAI_API_KEY="${OPENAI_API_KEY:-sk-placeholder}"

echo "==> 启动 platform-api :8080"
(cd "$ROOT/backend/platform-api" && mvn -q spring-boot:run -Dspring-boot.run.profiles=dev) &
PID_PLATFORM=$!

echo "==> 启动 gateway-service :8081"
(cd "$ROOT/backend/gateway-service" && mvn -q spring-boot:run -Dspring-boot.run.profiles=dev) &
PID_GATEWAY=$!

echo "==> 启动 frontend :8082"
(cd "$ROOT/frontend/web" && npm run serve) &
PID_WEB=$!

echo ""
echo "已启动（宿主机模式）"
echo "  前端: http://localhost:8082"
echo "  API:  http://localhost:8080"
echo "  网关: http://localhost:8081"
echo "停止: kill $PID_PLATFORM $PID_GATEWAY $PID_WEB && docker compose -f docker-compose.infra.yml down"
wait
