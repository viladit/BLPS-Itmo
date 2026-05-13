#!/usr/bin/env bash

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck source=common.sh
source "${SCRIPT_DIR}/common.sh"

print_header "List orders waiting for manager acceptance"
curl_api "${BASE_URL}/api/orders?status=CREATED"
printf '\n'
