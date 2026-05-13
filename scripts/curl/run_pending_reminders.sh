#!/usr/bin/env bash

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck source=common.sh
source "${SCRIPT_DIR}/common.sh"

print_header "Run pending-order reminder scheduler now"
curl_api -X POST "${BASE_URL}/api/orders/pending-reminders/run"
printf '\n'
