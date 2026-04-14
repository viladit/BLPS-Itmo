#!/usr/bin/env bash

set -euo pipefail

. "$(dirname "$0")/common.sh"

ORDER_ID="${1:-}"
require_order_id "${ORDER_ID}"

print_header "Get Order ${ORDER_ID}"
curl_api "${BASE_URL}/api/orders/${ORDER_ID}"
printf '\n'
