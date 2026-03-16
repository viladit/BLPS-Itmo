#!/usr/bin/env bash

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
. "${SCRIPT_DIR}/common.sh"

CREATE_RESPONSE="$("${SCRIPT_DIR}/create_order.sh")"
printf '%s\n' "${CREATE_RESPONSE}"

ORDER_ID="$(printf '%s\n' "${CREATE_RESPONSE}" | extract_order_id)"

if [ "${ORDER_ID}" = "" ]; then
  echo "Could not extract order id from create response" >&2
  exit 1
fi

print_header "Detected Order ID ${ORDER_ID}"

"${SCRIPT_DIR}/check_stock.sh" "${ORDER_ID}"
"${SCRIPT_DIR}/reserve_items.sh" "${ORDER_ID}"
"${SCRIPT_DIR}/confirm_order.sh" "${ORDER_ID}"
"${SCRIPT_DIR}/create_picking_task.sh" "${ORDER_ID}"
"${SCRIPT_DIR}/mark_picked.sh" "${ORDER_ID}"
"${SCRIPT_DIR}/pack_order.sh" "${ORDER_ID}"
"${SCRIPT_DIR}/handoff_to_delivery.sh" "${ORDER_ID}"
"${SCRIPT_DIR}/mark_delivered.sh" "${ORDER_ID}"
"${SCRIPT_DIR}/get_order.sh" "${ORDER_ID}"
