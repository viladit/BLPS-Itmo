#!/usr/bin/env bash

set -euo pipefail

BASE_URL="${BASE_URL:-http://localhost:8080}"

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
