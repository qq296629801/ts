#!/usr/bin/env bash
# 中继 + 网关生图功能冒烟（需 deploy/.env 中 OPENAI_API_KEY、栈已启动）
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
DEPLOY="${DEPLOY_DIR:-$ROOT/deploy}"
PLATFORM="${PLATFORM_URL:-http://127.0.0.1:8080}"
GATEWAY="${GATEWAY_URL:-http://127.0.0.1:8081}"

if [[ -f "$DEPLOY/.env" ]]; then
  set -a
  # shellcheck disable=SC1090
  source "$DEPLOY/.env"
  set +a
fi

echo "==> 1. 中继直连（容器内 curl，约 30s～3min）"
docker compose -f "$DEPLOY/docker-compose.dev.yml" exec -T gateway-service sh -c '
  curl -sS -o /tmp/relay.json -w "http=%{http_code} time=%{time_total}s bytes=%{size_download}\n" --max-time 300 \
    -X POST "$IMAGE_API_URL" -H "Authorization: Bearer $OPENAI_API_KEY" -H "Content-Type: application/json" \
    -d "{\"model\":\"${IMAGE_MODEL:-gpt-image-2}\",\"prompt\":\"smoke relay red dot\",\"size\":\"1024x1024\",\"quality\":\"${IMAGE_QUALITY:-medium}\",\"n\":1}"
  python3 -c "
import json, sys
d = json.load(open(\"/tmp/relay.json\"))
if \"error\" in d:
    print(\"RELAY_ERROR\", d[\"error\"].get(\"message\", d[\"error\"]))
    sys.exit(2)
if not d.get(\"data\"):
    print(\"RELAY_BAD\", d)
    sys.exit(2)
item = d[\"data\"][0]
print(\"RELAY_OK\", \"url\" if \"url\" in item else \"b64_json\")
"'

echo "==> 2. 网关生图 E2E（管理员 19900000000）"
LOGIN=$(curl -sf -m 15 -X POST "$PLATFORM/api/v1/auth/login" \
  -H 'Content-Type: application/json' \
  -d '{"account":"19900000000","password":"Admin1234","rememberMe":false}')
TOKEN=$(echo "$LOGIN" | python3 -c "import sys,json; print(json.load(sys.stdin)['data']['accessToken'])")
SESS=$(curl -sf -m 15 -X POST "$PLATFORM/api/v1/ai/sessions" \
  -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' -d '{"mode":"CHAT"}')
SESSION_ID=$(echo "$SESS" | python3 -c "import sys,json; print(json.load(sys.stdin)['data']['id'])")

GEN=$(curl -sS -m 360 -X POST "$GATEWAY/api/v1/ai/image/generate" \
  -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' \
  -d "{\"prompt\":\"smoke gateway red dot\",\"size\":\"1024x1024\",\"sessionId\":\"$SESSION_ID\"}")

echo "$GEN" | python3 -c "
import sys, json
raw = sys.stdin.read()
try:
    d = json.loads(raw)
except json.JSONDecodeError:
    print('GATEWAY_BAD_JSON', raw[:500])
    sys.exit(1)
if 'imageUrl' in d:
    print('GATEWAY_OK', d['imageUrl'])
    sys.exit(0)
if d.get('code') == 502 and 'upstream' in d.get('message', '').lower():
    print('GATEWAY_502_RELAY', d['message'])
    sys.exit(3)
print('GATEWAY_FAIL', d)
sys.exit(1)
"
