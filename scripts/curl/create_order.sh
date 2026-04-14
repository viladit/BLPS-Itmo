#!/usr/bin/env bash

set -euo pipefail

. "$(dirname "$0")/common.sh"

print_header "Create Order"
curl_api -X POST "${BASE_URL}/api/orders" \
  -H "Content-Type: application/json" \
  -d '{
    "customerName": "Ivan Petrov",
    "deliveryAddress": "Saint Petersburg, Nevsky 1",
    "items": [
      {
        "sku": "SKU-1",
        "productName": "Phone",
        "quantity": 2,
        "unitPrice": 499.99
      },
      {
        "sku": "SKU-2",
        "productName": "Charger",
        "quantity": 1,
        "unitPrice": 29.99
      }
    ]
  }'
printf '\n'
