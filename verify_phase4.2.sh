#!/bin/bash
BASE_URL="http://localhost:7777/api"

# 1. Verify YAPI Import
echo "1. Testing YAPI Import..."
PROJECT_ID=$(curl -s -X POST "$BASE_URL/projects" -H "Content-Type: application/json" -d '{"projectName": "ImportProject_'$(date +%s)'"}' | jq -r '.id')

YAPI_JSON='[
  {
    "name": "User Module",
    "list": [
      {
        "title": "Get User",
        "method": "GET",
        "path": "/api/user/1"
      }
    ]
  }
]'

IMPORT_RES=$(curl -s -X POST "$BASE_URL/import/yapi?projectId=$PROJECT_ID" -H "Content-Type: application/json" -d "$YAPI_JSON")
COUNT=$(echo "$IMPORT_RES" | jq '.count')
echo "   Import Result: $IMPORT_RES"

if [ "$COUNT" -eq 1 ]; then
  echo "   ✅ YAPI Import Successful (1 case imported)."
else
  echo "   ❌ YAPI Import Failed."
  exit 1
fi

# 2. Verify Scheduling
echo "2. Testing Schedule Creation..."
PLAN_ID=$(curl -s -X POST "$BASE_URL/test-plans" -H "Content-Type: application/json" -d "{\"name\": \"Scheduled Plan\", \"project\": {\"id\": $PROJECT_ID}}" | jq -r '.id')
TASK_PAYLOAD='{
  "name": "Hourly Run",
  "cronExpression": "0 0 * * * ?",
  "planId": '$PLAN_ID',
  "envKey": "dev"
}'

TASK_RES=$(curl -s -X POST "$BASE_URL/schedules" -H "Content-Type: application/json" -d "$TASK_PAYLOAD")
TASK_ID=$(echo "$TASK_RES" | jq -r '.id')
NEXT_RUN=$(echo "$TASK_RES" | jq -r '.nextRunTime')

echo "   Task ID: $TASK_ID"
echo "   Next Run: $NEXT_RUN"

if [ "$TASK_ID" != "null" ] && [ "$NEXT_RUN" != "null" ]; then
  echo "   ✅ Schedule Created Successfully."
else
  echo "   ❌ Schedule Creation Failed."
  exit 1
fi

echo "Phase 4.2 Verification Complete!"
