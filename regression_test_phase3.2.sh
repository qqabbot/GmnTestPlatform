#!/bin/bash

# Phase 3.2 Regression Test Script
# Focus: Test Case Addition, Link Series Execution (Chain), Data Retention

echo "=== Phase 3.2 Regression Test ==="
echo "Note: This script retains all created data for manual inspection."
echo ""

BASE_URL="http://localhost:7777/api"
TIMESTAMP=$(date +%s)
sleep 2

# 1. Setup Environment and Project
echo "1. Setup Environment and Project..."
ENV_ID=$(curl -s -X POST "$BASE_URL/environments" \
  -H "Content-Type: application/json" \
  -d "{\"envName\": \"Regression_Env_$TIMESTAMP\", \"description\": \"Created by Regression Test\"}" | jq -r '.id')
echo "   Created Environment ID: $ENV_ID"

PROJECT_ID=$(curl -s -X POST "$BASE_URL/projects" \
  -H "Content-Type: application/json" \
  -d "{\"projectName\": \"Regression_Proj_$TIMESTAMP\"}" | jq -r '.id')
echo "   Created Project ID: $PROJECT_ID"

MODULE_RES=$(curl -s -X POST "$BASE_URL/modules" \
  -H "Content-Type: application/json" \
  -d "{\"moduleName\": \"UserFlow_$TIMESTAMP\", \"project\": {\"id\": $PROJECT_ID}}")
MODULE_ID=$(echo "$MODULE_RES" | jq -r '.id')

if [ "$MODULE_ID" == "null" ] || [ -z "$MODULE_ID" ]; then
  echo "❌ Failed to create module. Response: $MODULE_RES"
  exit 1
fi
echo "   Created Module ID: $MODULE_ID"

# 2. Create Global Variable (Base URL)
echo "2. Create Global Variable..."
curl -s -X POST "$BASE_URL/variables" \
  -H "Content-Type: application/json" \
  -d "{\"keyName\": \"base_url\", \"valueContent\": \"https://httpbin.org\", \"type\": \"string\", \"environment\": {\"id\": $ENV_ID}}" > /dev/null
echo "   Created Variable 'base_url' -> https://httpbin.org"

# 3. Create Test Case A: Login (Simulated)
# This Step will simulate a login by calling httpbin/get with a query param and extracting it.
# Assuming we have "Steps" support or we just use Single Case with Extractor.
# Phase 3.1 added "Extractors" (implicitly via assertion script or similar).
# Wait, Phase 3.2 added UI for Extractors, but backend logic was likely Phase 3.1 or 2.
# Let's check `test_create_with_steps.sh`. It shows `steps` array.
# I will create a Case with 2 Steps to simulate "Link Series Execution" within a Case.
# Step 1: Login (Get Token)
# Step 2: User Profile (Use Token)

echo "3. Create Test Case with Steps (Link Execution)..."
CASE_PAYLOAD=$(cat <<EOF
{
  "caseName": "Login and Profile Flow (Regress $TIMESTAMP)",
  "module": {"id": $MODULE_ID},
  "method": "POST",
  "url": "\${base_url}/post", 
  "headers": "{\"Content-Type\": \"application/json\"}",
  "body": "{\"user\": \"test\"}",
  "assertionScript": "status_code == 200",
  "isActive": true,
  "steps": [
    {
      "stepName": "1. Get Token",
      "stepOrder": 1,
      "method": "GET",
      "url": "\${base_url}/get?token=abcdef123456",
      "headers": "{}",
      "body": "",
      "assertionScript": "status_code == 200\nvars.put(\"auth_token\", jsonPath(response, \"$.args.token\"))",
      "enabled": true
    },
    {
      "stepName": "2. Use Token",
      "stepOrder": 2,
      "method": "GET",
      "url": "\${base_url}/headers",
      "headers": "{\"Authorization\": \"Bearer \${auth_token}\"}",
      "body": "",
      "assertionScript": "status_code == 200\nassert jsonPath(response, \"$.headers.Authorization\") contains \"Bearer abcdef123456\"",
      "enabled": true
    }
  ]
}
EOF
)

CASE_ID=$(curl -s -X POST "$BASE_URL/cases" \
  -H "Content-Type: application/json" \
  -d "$CASE_PAYLOAD" | jq -r '.id')
echo "   Created Test Case ID: $CASE_ID"

# 4. Execute Test Case
echo "4. Execute Test Case (Chain)..."
# Using dry-run to see details or execute for persistence?
# User said "Link Series Execution".
# Let's run execute endpoint.
# Using query parameters as required by backend Controller
EXEC_RESULT=$(curl -s -X POST "$BASE_URL/cases/execute?moduleId=$MODULE_ID&envKey=Regression_Env_$TIMESTAMP" \
  -H "Content-Type: application/json")

# 5. Verify Result
echo "5. Verify Result..."
# We expect 1 result (for the case) or maybe steps details if supported.
STATUS=$(echo "$EXEC_RESULT" | jq -r '.[0].status')
TOTAL_TIME=$(echo "$EXEC_RESULT" | jq -r '.[0].duration')

if [ "$STATUS" == "PASS" ]; then
  echo "   ✅ Execution Passed!"
  echo "   Duration: ${TOTAL_TIME}ms"
else
  echo "   ❌ Execution Failed!"
  echo "   Status: $STATUS"
  echo "   Response: $EXEC_RESULT"
fi

echo ""
echo "=== Test Data Info ==="
echo "Environment ID: $ENV_ID (Name: Regression_Env_$TIMESTAMP)"
echo "Project ID: $PROJECT_ID"
echo "Module ID: $MODULE_ID"
echo "Case ID: $CASE_ID"
echo "Data has been retained."
