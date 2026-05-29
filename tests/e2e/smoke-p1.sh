#!/usr/bin/env bash
# P1 冒烟：依赖 platform-api(8080)、gateway-service(8081) 已启动
set -euo pipefail

# Docker 一键启动时用 PLATFORM_URL=http://localhost GATEWAY_URL=http://localhost
PLATFORM="${PLATFORM_URL:-http://127.0.0.1:8080}"
GATEWAY="${GATEWAY_URL:-http://127.0.0.1:8081}"
PHONE="138$(date +%s | tail -c 8)"

echo "==> 1. 发送验证码"
curl -sf -X POST "$PLATFORM/api/v1/auth/send-sms" \
  -H 'Content-Type: application/json' \
  -d "{\"phone\":\"$PHONE\"}" | grep -q '"code":200' || { echo "send-sms 失败"; exit 1; }

echo "==> 2. 注册（验证码见 platform 日志，开发环境可用 123456 若已 Mock）"
CODE="${SMS_CODE:-123456}"
REG=$(curl -sf -X POST "$PLATFORM/api/v1/auth/register" \
  -H 'Content-Type: application/json' \
  -d "{\"phone\":\"$PHONE\",\"password\":\"Pass1234\",\"nickname\":\"冒烟用户\",\"verifyCode\":\"$CODE\"}")
echo "$REG" | grep -q '"code":200' || { echo "register 失败: $REG"; exit 1; }

TOKEN=$(echo "$REG" | python3 -c "import sys,json; print(json.load(sys.stdin)['data']['accessToken'])")
AUTH="Authorization: Bearer $TOKEN"

echo "==> 3. 查询配额"
QUOTA=$(curl -sf "$PLATFORM/api/v1/user/quota" -H "$AUTH")
echo "$QUOTA" | grep -q '"balance"' || { echo "quota 失败"; exit 1; }

echo "==> 4. 创建会话"
SESS=$(curl -sf -X POST "$PLATFORM/api/v1/ai/sessions" -H "$AUTH" -H 'Content-Type: application/json' -d '{"mode":"CHAT"}')
SESSION_ID=$(echo "$SESS" | python3 -c "import sys,json; print(json.load(sys.stdin)['data']['id'])")

echo "==> 5. 生图（需 OPENAI_API_KEY 与配额）"
GEN=$(curl -sS -m 360 -X POST "$GATEWAY/api/v1/ai/image/generate" \
  -H "$AUTH" -H 'Content-Type: application/json' \
  -d "{\"prompt\":\"smoke test cat\",\"size\":\"1024x1024\",\"sessionId\":\"$SESSION_ID\"}" || true)
echo "$GEN" | grep -qE 'imageUrl|"code":402|"code":409|"code":502' || { echo "生图异常: $GEN"; exit 1; }

echo "==> 6. 支付套餐列表"
curl -sf "$PLATFORM/api/v1/pay/packages" | grep -q '"code":200'

echo "==> 冒烟完成"
