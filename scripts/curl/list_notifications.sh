#!/usr/bin/env bash

set -euo pipefail

. "$(dirname "$0")/common.sh"

print_header "List Notifications"
curl_api "${BASE_URL}/api/notifications"
printf '\n'
