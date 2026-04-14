#!/usr/bin/env bash
set -euo pipefail

. "$(dirname "$0")/common.sh"

ORDER_ID="${1:-}"
require_order_id "${ORDER_ID}"

print_header "Accept Order ${ORDER_ID}"
curl -sS -X POST "${BASE_URL}/api/orders/${ORDER_ID}/accept"
printf '\n'