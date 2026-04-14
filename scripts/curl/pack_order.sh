#!/usr/bin/env bash

set -euo pipefail

. "$(dirname "$0")/common.sh"

ORDER_ID="${1:-}"
require_order_id "${ORDER_ID}"

print_header "Pack Order ${ORDER_ID}"
curl_api -X POST "${BASE_URL}/api/orders/${ORDER_ID}/pack"
printf '\n'
