#!/usr/bin/env bash

set -euo pipefail

. "$(dirname "$0")/common.sh"

ORDER_ID="${1:-}"
require_order_id "${ORDER_ID}"

print_header "Hand Off To Delivery ${ORDER_ID}"
curl -sS -X POST "${BASE_URL}/api/orders/${ORDER_ID}/handoff" \
  -H "Content-Type: application/json" \
  -d '{
    "carrierName": "OZON Delivery",
    "trackingNumber": "TRACK-001"
  }'
printf '\n'
