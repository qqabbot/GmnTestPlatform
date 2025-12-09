#!/bin/bash
BASE_URL="http://localhost:7777/api"

# 1. Create a Global Template (no project ID)
echo "1. Creating Global Template..."
TEMPLATE_PAYLOAD='{
  "name": "Global Template",
  "method": "GET",
  "url": "https://httpbin.org/get",
  "projectId": null
}'
TEMPLATE_RES=$(curl -s -X POST "$BASE_URL/step-templates" -H "Content-Type: application/json" -d "$TEMPLATE_PAYLOAD")
TEMPLATE_ID=$(echo "$TEMPLATE_RES" | jq -r '.id')
echo "   Template ID: $TEMPLATE_ID"

# 2. Fetch with Project ID
echo "2. Fetching with Project ID=999..."
FETCH_RES=$(curl -s "$BASE_URL/step-templates?projectId=999")
COUNT=$(echo "$FETCH_RES" | jq 'map(select(.id == '$TEMPLATE_ID')) | length')

if [ "$COUNT" -ge 1 ]; then
  echo "   ✅ Global Template found when fetching with Project ID."
else
  echo "   ❌ Global Template NOT found."
  exit 1
fi
