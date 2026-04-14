#!/usr/bin/env bash

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
. "${SCRIPT_DIR}/common.sh"

MANAGER_USERNAME="${API_MANAGER_USERNAME:-manager}"
MANAGER_PASSWORD="${API_MANAGER_PASSWORD:-manager123}"
WAREHOUSE_USERNAME="${API_WAREHOUSE_USERNAME:-warehouse}"
WAREHOUSE_PASSWORD="${API_WAREHOUSE_PASSWORD:-warehouse123}"
DELIVERY_USERNAME="${API_DELIVERY_USERNAME:-delivery}"
DELIVERY_PASSWORD="${API_DELIVERY_PASSWORD:-delivery123}"

run_as() {
  local username="$1"
  local password="$2"
  shift 2
  API_USERNAME="${username}" API_PASSWORD="${password}" "$@"
}

CREATE_RESPONSE="$(run_as "${MANAGER_USERNAME}" "${MANAGER_PASSWORD}" "${SCRIPT_DIR}/create_order.sh")"
printf '%s\n' "${CREATE_RESPONSE}"

ORDER_ID="$(printf '%s\n' "${CREATE_RESPONSE}" | extract_order_id)"

if [ "${ORDER_ID}" = "" ]; then
  echo "Could not extract order id from create response" >&2
  exit 1
fi

print_header "Detected Order ID ${ORDER_ID}"

run_as "${MANAGER_USERNAME}" "${MANAGER_PASSWORD}" "${SCRIPT_DIR}/accept_order.sh" "${ORDER_ID}"
run_as "${WAREHOUSE_USERNAME}" "${WAREHOUSE_PASSWORD}" "${SCRIPT_DIR}/pack_order.sh" "${ORDER_ID}"
run_as "${DELIVERY_USERNAME}" "${DELIVERY_PASSWORD}" "${SCRIPT_DIR}/handoff_to_delivery.sh" "${ORDER_ID}"
run_as "${DELIVERY_USERNAME}" "${DELIVERY_PASSWORD}" "${SCRIPT_DIR}/mark_delivered.sh" "${ORDER_ID}"
run_as "${MANAGER_USERNAME}" "${MANAGER_PASSWORD}" "${SCRIPT_DIR}/get_order.sh" "${ORDER_ID}"
