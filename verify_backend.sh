#!/bin/bash

echo "=== Backend Health Check ==="
curl -s http://localhost:7777/api/cases > /dev/null
if [ $? -eq 0 ]; then
    echo "✅ Backend is up and running"
else
    echo "❌ Backend is not reachable"
    exit 1
fi

echo ""
echo "=== Running Test Cases ==="

# 1. Create a Test Module
echo "1. Creating Test Module..."
MODULE_RES=$(curl -s -X POST http://localhost:7777/api/modules \
  -H "Content-Type: application/json" \
  -d '{
    "moduleName": "Auto Test Module"
  }')
echo "Response: $MODULE_RES"

# Extract Module ID
MODULE_ID=$(echo $MODULE_RES | python3 -c "import sys, json; print(json.load(sys.stdin).get('id', ''))")

if [ -z "$MODULE_ID" ]; then
    echo "❌ Failed to create test module"
    exit 1
else
    echo "✅ Test Module Created with ID: $MODULE_ID"
fi

# 2. Create a test case
echo ""
echo "2. Creating Test Case..."
CREATE_RES=$(curl -s -X POST http://localhost:7777/api/cases \
  -H "Content-Type: application/json" \
  -d "{
    \"module\": {\"id\": $MODULE_ID},
    \"caseName\": \"Auto Test Case\",
    \"method\": \"GET\",
    \"url\": \"http://httpbin.org/get\",
    \"assertionScript\": \"status_code == 200\",
    \"isActive\": true
  }")
echo "Response: $CREATE_RES"

# Extract Case ID
CASE_ID=$(echo $CREATE_RES | python3 -c "import sys, json; print(json.load(sys.stdin).get('id', ''))")

if [ -z "$CASE_ID" ]; then
    echo "❌ Failed to create test case"
    exit 1
else
    echo "✅ Test Case Created with ID: $CASE_ID"
fi

# 3. Execute the test case
echo ""
echo "3. Executing Test Case..."
EXEC_RES=$(curl -s -X POST "http://localhost:7777/api/cases/execute?envKey=dev")
echo "Response: $EXEC_RES"

# Check if execution result contains "PASS"
if echo "$EXEC_RES" | grep -q "PASS"; then
    echo "✅ Test Execution Passed"
else
    echo "❌ Test Execution Failed"
fi

echo ""
echo "=== Test Complete ==="
