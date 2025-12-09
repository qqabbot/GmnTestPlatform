#!/bin/bash
BASE_URL="http://localhost:7777/api"

echo "=== Phase 4.2 Verification: Scheduling ==="

# 1. Create Test Data
echo "1. Creating test data (Project + Plan)..."
PROJECT_ID=$(curl -s -X POST "$BASE_URL/projects" -H "Content-Type: application/json" -d '{"projectName": "ScheduleTest_'$(date +%s)'"}' | jq -r '.id')
PLAN_ID=$(curl -s -X POST "$BASE_URL/test-plans" -H "Content-Type: application/json" -d "{\"name\": \"Hourly Plan\", \"project\": {\"id\": $PROJECT_ID}}" | jq -r '.id')
echo "   Project ID: $PROJECT_ID, Plan ID: $PLAN_ID"

# 2. Create Schedule
echo "2. Creating scheduled task..."
TASK_PAYLOAD='{
  "name": "Test Schedule",
  "cronExpression": "0 0 * * * ?",
  "planId": '$PLAN_ID',
  "envKey": "dev"
}'

TASK_RES=$(curl -s -X POST "$BASE_URL/schedules" -H "Content-Type: application/json" -d "$TASK_PAYLOAD")
TASK_ID=$(echo "$TASK_RES" | jq -r '.id')
STATUS=$(echo "$TASK_RES" | jq -r '.status')

echo "   Task ID: $TASK_ID"
echo "   Status: $STATUS"

if [ "$TASK_ID" != "null" ] && [ "$TASK_ID" != "" ]; then
  echo "   ✅ Schedule Created Successfully."
else
  echo "   ❌ Schedule Creation Failed."
  echo "   Response: $TASK_RES"
  exit 1
fi

# 3. List Schedules
echo "3. Listing all schedules..."
LIST_RES=$(curl -s "$BASE_URL/schedules")
COUNT=$(echo "$LIST_RES" | jq 'length')
echo "   Total Schedules: $COUNT"

if [ "$COUNT" -ge 1 ]; then
  echo "   ✅ Schedule List Retrieved."
else
  echo "   ❌ Failed to retrieve schedules."
  exit 1
fi

# 4. Pause Schedule
echo "4. Pausing schedule..."
PAUSE_RES=$(curl -s -X POST "$BASE_URL/schedules/$TASK_ID/pause")
PAUSE_STATUS=$(echo "$PAUSE_RES" | jq -r '.status')

if [ "$PAUSE_STATUS" = "PAUSED" ]; then
  echo "   ✅ Schedule Paused."
else
  echo "   ❌ Failed to pause schedule."
  exit 1
fi

# 5. Resume Schedule
echo "5. Resuming schedule..."
RESUME_RES=$(curl -s -X POST "$BASE_URL/schedules/$TASK_ID/resume")
RESUME_STATUS=$(echo "$RESUME_RES" | jq -r '.status')

if [ "$RESUME_STATUS" = "ACTIVE" ]; then
  echo "   ✅ Schedule Resumed."
else
  echo "   ❌ Failed to resume schedule."
  exit 1
fi

# 6. Delete Schedule
echo "6. Deleting schedule..."
curl -s -X DELETE "$BASE_URL/schedules/$TASK_ID"
echo "   ✅ Schedule Deleted."

echo ""
echo "=== Phase 4.2 Scheduling Verification Complete! ==="
