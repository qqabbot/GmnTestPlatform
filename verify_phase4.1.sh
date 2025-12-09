#!/bin/bash
BASE_URL="http://localhost:7777/api"

# 1. Setup Data
echo "1. Setting up Project and Cases..."
PROJECT_ID=$(curl -s -X POST "$BASE_URL/projects" -H "Content-Type: application/json" -d '{"projectName": "PlanTest_Project_'$(date +%s)'"}' | jq -r '.id')
MODULE_ID=$(curl -s -X POST "$BASE_URL/modules" -H "Content-Type: application/json" -d "{\"moduleName\": \"PlanTest_Module\", \"project\": {\"id\": $PROJECT_ID}}" | jq -r '.id')

# Create Case 1
C1_PAYLOAD='{
  "caseName": "Case 1",
  "module": {"id": '$MODULE_ID'},
  "method": "GET",
  "url": "https://httpbin.org/get",
  "assertionScript": "status_code == 200",
  "isActive": true
}'
C1_ID=$(curl -s -X POST "$BASE_URL/cases" -H "Content-Type: application/json" -d "$C1_PAYLOAD" | jq -r '.id')

# Create Case 2
C2_PAYLOAD='{
  "caseName": "Case 2",
  "module": {"id": '$MODULE_ID'},
  "method": "POST",
  "url": "https://httpbin.org/post",
  "body": "{\"test\": \"plan\"}",
  "assertionScript": "status_code == 200",
  "isActive": true
}'
C2_ID=$(curl -s -X POST "$BASE_URL/cases" -H "Content-Type: application/json" -d "$C2_PAYLOAD" | jq -r '.id')

echo "   Created Cases: $C1_ID, $C2_ID"

# 2. Create Test Plan
echo "2. Creating Test Plan..."
PLAN_PAYLOAD='{
  "name": "My First Plan",
  "description": "Testing Plan Creation",
  "project": {"id": '$PROJECT_ID'},
  "testCases": [
    {"id": '$C1_ID'},
    {"id": '$C2_ID'}
  ]
}'
PLAN_RES=$(curl -s -X POST "$BASE_URL/test-plans" -H "Content-Type: application/json" -d "$PLAN_PAYLOAD")
PLAN_ID=$(echo "$PLAN_RES" | jq -r '.id')

echo "   Plan Created ID: $PLAN_ID"
echo "   Response: $PLAN_RES"

# Verify Case Count
COUNT=$(echo "$PLAN_RES" | jq '.testCases | length')
if [ "$COUNT" -eq 2 ]; then
  echo "   ✅ Plan has 2 cases."
else
  echo "   ❌ Plan case count mismatch: $COUNT"
  exit 1
fi

# 3. Execute Plan
echo "3. Executing Plan..."
EXEC_RES=$(curl -s -X POST "$BASE_URL/test-plans/$PLAN_ID/execute?envKey=dev" -H "Content-Type: application/json")

# Verify Results
PASS_COUNT=$(echo "$EXEC_RES" | jq '[.[] | select(.status=="PASS")] | length')
TOTAL_COUNT=$(echo "$EXEC_RES" | jq 'length')

echo "   Execution Result: $PASS_COUNT / $TOTAL_COUNT passed."

if [ "$PASS_COUNT" -eq 2 ]; then
  echo "   ✅ Plan Execution Successful!"
else
  echo "   ❌ Plan Execution Failed."
  echo "   Result: $EXEC_RES"
  exit 1
fi

echo "Phase 4.1 Verification Complete!"
