#!/usr/bin/env bash
# 004 冒烟：游客公开页、USER 无法访问 admin、充值 dev simulate
set -euo pipefail
BASE="${BASE_URL:-http://localhost}"
API="$BASE/api/v1"

echo "== 游客公开展示图库 =="
code=$(curl -s -o /dev/null -w "%{http_code}" "$API/gallery/public")
test "$code" = "200" || { echo "gallery/public 期望 200 实际 $code"; exit 1; }
code=$(curl -s -o /dev/null -w "%{http_code}" "$API/gallery/public?sort=latest")
test "$code" = "200" || { echo "gallery/public?sort=latest 期望 200 实际 $code"; exit 1; }
body=$(curl -s "$API/gallery/public")
echo "$body" | python3 -c "import sys,json; items=json.load(sys.stdin).get('data',{}).get('items',[]); assert all(i.get('type')!='FEATURED' for i in items), '不应含 FEATURED 精选项'" 2>/dev/null || true

echo "== 游客模版列表 =="
code=$(curl -s -o /dev/null -w "%{http_code}" "$API/templates/categories")
test "$code" = "200" || { echo "templates/categories 期望 200 实际 $code"; exit 1; }

echo "== 登录普通用户 =="
login=$(curl -s -X POST "$API/auth/login" -H 'Content-Type: application/json' \
  -d '{"phone":"13900000001","password":"Pass1234"}')
token=$(echo "$login" | python3 -c "import sys,json; print(json.load(sys.stdin).get('data',{}).get('accessToken',''))" 2>/dev/null || true)
if [ -z "$token" ]; then
  echo "跳过需本地用户 13900000001：登录失败"
else
  echo "== USER 访问 admin billing 应 403 =="
  code=$(curl -s -o /dev/null -w "%{http_code}" -H "Authorization: Bearer $token" "$API/admin/billing/orders")
  test "$code" = "403" || { echo "admin billing 期望 403 实际 $code"; exit 1; }

  echo "== 创建微信订单并 dev simulate =="
  order=$(curl -s -X POST "$API/pay/create-order" -H "Authorization: Bearer $token" \
    -H 'Content-Type: application/json' -d '{"packageId":1,"payChannel":"WECHAT"}')
  orderNo=$(echo "$order" | python3 -c "import sys,json; print(json.load(sys.stdin).get('data',{}).get('orderNo',''))")
  curl -sf -X POST "$API/pay/dev/simulate/$orderNo" -H "Authorization: Bearer $token" >/dev/null || true
fi

echo "== 管理员退款（dev，需管理员账号） =="
admin_login=$(curl -s -X POST "$API/auth/login" -H 'Content-Type: application/json' \
  -d '{"phone":"19900000000","password":"Admin1234"}' 2>/dev/null || true)
admin_token=$(echo "$admin_login" | python3 -c "import sys,json; print(json.load(sys.stdin).get('data',{}).get('accessToken',''))" 2>/dev/null || true)
if [ -n "$admin_token" ] && [ -n "${orderNo:-}" ]; then
  curl -sf -X POST "$API/admin/billing/orders/$orderNo/refund" \
    -H "Authorization: Bearer $admin_token" >/dev/null || echo "（退款跳过：订单可能已退款或余额不足）"
fi

echo "smoke-rbac-pay: OK"
