#!/bin/bash
BASE_URL="http://localhost:7777/api"

echo "Retrieving existing Case ID..."
# Assume Case 1 exists from previous run, or list all and pick one
CASE_ID=$(curl -s "$BASE_URL/cases" | jq -r '.[0].id')

if [ "$CASE_ID" == "null" ]; then
    echo "No case found. Creating one..."
    PROJECT_ID=$(curl -s -X POST "$BASE_URL/projects" -H "Content-Type: application/json" -d '{"projectName": "LogTestProject"}' | jq -r '.id')
    MODULE_ID=$(curl -s -X POST "$BASE_URL/modules" -H "Content-Type: application/json" -d "{\"moduleName\": \"LogTestModule\", \"project\": {\"id\": $PROJECT_ID}}" | jq -r '.id')
    CASE_PAYLOAD='{
      "caseName": "Log Test Case",
      "module": {"id": '$MODULE_ID'},
      "method": "GET",
      "url": "https://httpbin.org/get",
      "assertionScript": "status_code == 200",
      "isActive": true,
      "steps": [
        {
          "stepName": "Step 1",
          "stepOrder": 1,
          "method": "GET",
          "url": "https://httpbin.org/get",
          "enabled": true
        }
      ]
    }'
    CASE_ID=$(curl -s -X POST "$BASE_URL/cases" -H "Content-Type: application/json" -d "$CASE_PAYLOAD" | jq -r '.id')
fi

echo "Executing Case $CASE_ID..."
EXEC_RES=$(curl -s -X POST "$BASE_URL/cases/execute?caseId=$CASE_ID&envKey=dev" -H "Content-Type: application/json")

echo "Checking for logs..."
LOGS_COUNT=$(echo "$EXEC_RES" | jq '.[0].logs | length')
echo "Logs count: $LOGS_COUNT"

if [ "$LOGS_COUNT" -gt 0 ]; then
   VAR_SNAPSHOT=$(echo "$EXEC_RES" | jq '.[0].logs[0].variableSnapshot')
   echo "Variable Snapshot: $VAR_SNAPSHOT"
   if [ "$VAR_SNAPSHOT" != "null" ]; then
       echo "✅ Variable Snapshot present."
   else
       echo "❌ Variable Snapshot MISSING."
       exit 1
   fi
else
   echo "❌ No logs found in result."
   echo "Result: $EXEC_RES"
   exit 1
fi
