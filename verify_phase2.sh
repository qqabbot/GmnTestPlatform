#!/bin/bash

echo "=== Phase 2 Backend Verification ==="

# 1. Create Project
echo "1. Creating Project..."
PROJ_RES=$(curl -s -X POST http://localhost:7777/api/projects \
  -H "Content-Type: application/json" \
  -d '{
    "projectName": "Phase 2 Project",
    "description": "Testing Phase 2 Features"
  }')
echo "Response: $PROJ_RES"
PROJ_ID=$(echo $PROJ_RES | python3 -c "import sys, json; print(json.load(sys.stdin).get('id', ''))")

if [ -z "$PROJ_ID" ]; then
    echo "❌ Failed to create project"
    exit 1
else
    echo "✅ Project Created with ID: $PROJ_ID"
fi

# 2. Create Module under Project
echo ""
echo "2. Creating Module under Project..."
MOD_RES=$(curl -s -X POST http://localhost:7777/api/modules \
  -H "Content-Type: application/json" \
  -d "{
    \"moduleName\": \"Phase 2 Module\",
    \"project\": {\"id\": $PROJ_ID}
  }")
echo "Response: $MOD_RES"
MOD_ID=$(echo $MOD_RES | python3 -c "import sys, json; print(json.load(sys.stdin).get('id', ''))")

if [ -z "$MOD_ID" ]; then
    echo "❌ Failed to create module"
    exit 1
else
    echo "✅ Module Created with ID: $MOD_ID"
fi

# 3. Create Test Case under Module
echo ""
echo "3. Creating Test Case..."
CASE_RES=$(curl -s -X POST http://localhost:7777/api/cases \
  -H "Content-Type: application/json" \
  -d "{
    \"module\": {\"id\": $MOD_ID},
    \"caseName\": \"Phase 2 Case\",
    \"method\": \"GET\",
    \"url\": \"http://httpbin.org/get\",
    \"assertionScript\": \"status_code == 200\",
    \"isActive\": true
  }")
echo "Response: $CASE_RES"
CASE_ID=$(echo $CASE_RES | python3 -c "import sys, json; print(json.load(sys.stdin).get('id', ''))")

if [ -z "$CASE_ID" ]; then
    echo "❌ Failed to create test case"
    exit 1
else
    echo "✅ Test Case Created with ID: $CASE_ID"
fi

# 4. Execute by Project ID
echo ""
echo "4. Executing by Project ID..."
EXEC_RES=$(curl -s -X POST "http://localhost:7777/api/cases/execute?projectId=$PROJ_ID&envKey=dev")
echo "Response: $EXEC_RES"

if echo "$EXEC_RES" | grep -q "PASS"; then
    echo "✅ Project Execution Passed"
else
    echo "❌ Project Execution Failed"
fi

# 5. Check Report
echo ""
echo "5. Checking Report..."
REPORT_RES=$(curl -s "http://localhost:7777/api/reports/project/$PROJ_ID")
echo "Response: $REPORT_RES"

if echo "$REPORT_RES" | grep -q "Phase 2 Case"; then
    echo "✅ Report contains execution record"
else
    echo "❌ Report missing execution record"
fi

echo ""
echo "=== Phase 2 Verification Complete ==="
