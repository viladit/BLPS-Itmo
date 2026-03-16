#!/usr/bin/env bash

set -euo pipefail

. "$(dirname "$0")/common.sh"

print_header "List Orders"
curl -sS "${BASE_URL}/api/orders"
printf '\n'
