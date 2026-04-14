#!/usr/bin/env bash

set -euo pipefail

BASE_URL="${BASE_URL:-http://localhost:8080}"
CURL_COMMON_ARGS=(-sS)
CURL_AUTH_ARGS=()

if [ -n "${API_USERNAME:-}" ] || [ -n "${API_PASSWORD:-}" ]; then
  : "${API_USERNAME:?Set API_USERNAME to use HTTP Basic auth}"
  : "${API_PASSWORD:?Set API_PASSWORD to use HTTP Basic auth}"
  CURL_AUTH_ARGS=(-u "${API_USERNAME}:${API_PASSWORD}")
fi

print_header() {
  printf '\n== %s ==\n' "$1"
}

require_order_id() {
  if [ "${1:-}" = "" ]; then
    echo "Usage: $0 <orderId>" >&2
    exit 1
  fi
}

extract_order_id() {
  grep -o '"id"[[:space:]]*:[[:space:]]*[0-9][0-9]*' | head -n 1 | grep -o '[0-9][0-9]*'
}

curl_api() {
  curl "${CURL_COMMON_ARGS[@]}" "${CURL_AUTH_ARGS[@]}" "$@"
}
