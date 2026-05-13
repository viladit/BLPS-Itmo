#!/usr/bin/env bash

set -euo pipefail

. "$(dirname "$0")/common.sh"

print_header "Create Order With Notification Failure"
curl_api -X POST "${BASE_URL}/api/orders?failNotification=true" \
  -H "Content-Type: application/json" \
  -d '{
    "customerName": "Rollback Test",
    "deliveryAddress": "Saint Petersburg, Test 1",
    "items": [
      {
        "sku": "SKU-FAIL",
        "productName": "Rollback product",
        "quantity": 1,
        "unitPrice": 10.00
      }
    ]
  }'
printf '\n'
