#!/usr/bin/env bash

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
. "${SCRIPT_DIR}/common.sh"

MANAGER_USERNAME="${API_MANAGER_USERNAME:-manager}"
MANAGER_PASSWORD="${API_MANAGER_PASSWORD:-manager123}"

run_as_manager() {
  env API_USERNAME="${MANAGER_USERNAME}" API_PASSWORD="${MANAGER_PASSWORD}" "$@"
}

restore_notifications_db() {
  docker compose up -d notifications-db >/dev/null
}

print_header "Stop notifications DB"
docker compose stop notifications-db >/dev/null
trap restore_notifications_db EXIT

print_header "Create order while notifications DB is unavailable"
set +e
CREATE_RESPONSE="$(run_as_manager "${SCRIPT_DIR}/create_order.sh" 2>&1)"
CREATE_EXIT_CODE=$?
set -e
printf '%s\n' "${CREATE_RESPONSE}"

if [ "${CREATE_EXIT_CODE}" -ne 0 ]; then
  echo "Create request failed at curl level with exit code ${CREATE_EXIT_CODE}" >&2
fi

print_header "Restart notifications DB"
restore_notifications_db
sleep "${DB_RESTART_WAIT_SECONDS:-20}"
trap - EXIT

print_header "Orders after rollback"
run_as_manager "${SCRIPT_DIR}/list_orders.sh"

print_header "Notifications after rollback"
run_as_manager "${SCRIPT_DIR}/list_notifications.sh"
