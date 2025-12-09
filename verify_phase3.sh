#!/bin/bash

echo "=== Phase 3 Backend Verification ==="

# 1. Create Environment
echo "1. Creating Environment 'QA'..."
ENV_RES=$(curl -s -X POST http://localhost:7777/api/environments \
  -H "Content-Type: application/json" \
  -d '{
    "envName": "QA",
    "description": "Quality Assurance Environment",
    "domain": "http://httpbin.org"
  }')
echo "Response: $ENV_RES"
ENV_ID=$(echo $ENV_RES | python3 -c "import sys, json; print(json.load(sys.stdin).get('id', ''))")

if [ -z "$ENV_ID" ]; then
    echo "❌ Failed to create environment"
    exit 1
else
    echo "✅ Environment Created with ID: $ENV_ID"
fi

# 2. Create Global Variable
echo ""
echo "2. Creating Global Variable 'base_url'..."
VAR_RES=$(curl -s -X POST http://localhost:7777/api/variables \
  -H "Content-Type: application/json" \
  -d "{
    \"keyName\": \"base_url\",
    \"valueContent\": \"http://httpbin.org\",
    \"environment\": {\"id\": $ENV_ID}
  }")
echo "Response: $VAR_RES"
VAR_ID=$(echo $VAR_RES | python3 -c "import sys, json; print(json.load(sys.stdin).get('id', ''))")

if [ -z "$VAR_ID" ]; then
    echo "❌ Failed to create variable"
    exit 1
else
    echo "✅ Variable Created with ID: $VAR_ID"
fi

# 3. Create Test Case using Variable
echo ""
echo "3. Creating Test Case using \${base_url}..."
# First create a project and module to hold the case
PROJ_RES=$(curl -s -X POST http://localhost:7777/api/projects \
  -H "Content-Type: application/json" \
  -d '{"projectName": "Phase 3 Project"}')
PROJ_ID=$(echo $PROJ_RES | python3 -c "import sys, json; print(json.load(sys.stdin).get('id', ''))")

MOD_RES=$(curl -s -X POST http://localhost:7777/api/modules \
  -H "Content-Type: application/json" \
  -d "{
    \"moduleName\": \"Phase 3 Module\",
    \"project\": {\"id\": $PROJ_ID}
  }")
MOD_ID=$(echo $MOD_RES | python3 -c "import sys, json; print(json.load(sys.stdin).get('id', ''))")

CASE_RES=$(curl -s -X POST http://localhost:7777/api/cases \
  -H "Content-Type: application/json" \
  -d "{
    \"module\": {\"id\": $MOD_ID},
    \"caseName\": \"Dynamic Variable Case\",
    \"method\": \"GET\",
    \"url\": \"\${base_url}/get\",
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

# 4. Execute with 'QA' Environment
echo ""
echo "4. Executing with envKey='QA'..."
EXEC_RES=$(curl -s -X POST "http://localhost:7777/api/cases/execute?projectId=$PROJ_ID&envKey=QA")
echo "Response: $EXEC_RES"

if echo "$EXEC_RES" | grep -q "PASS"; then
    echo "✅ Execution Passed (Variable Resolved)"
else
    echo "❌ Execution Failed"
fi

echo ""
echo "=== Phase 3 Verification Complete ==="
