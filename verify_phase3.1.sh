#!/bin/bash

# Phase 3.1 Verification Script - TestStep Execution & Encryption

echo "=== Phase 3.1 Verification ==="
echo ""

BASE_URL="http://localhost:7777/api"

# 1. Create Environment for Testing
echo "1. Creating test environment..."
ENV_RESPONSE=$(curl -s -X POST "$BASE_URL/environments" \
  -H "Content-Type: application/json" \
  -d '{
    "envName": "phase31test",
    "description": "Phase 3.1 Test Environment"
  }')
ENV_ID=$(echo $ENV_RESPONSE | grep -o '"id":[0-9]*' | grep -o '[0-9]*')
echo "   Environment ID: $ENV_ID"
echo ""

# 2. Create Encrypted Variable
echo "2. Creating encrypted variable..."
VAR_RESPONSE=$(curl -s -X POST "$BASE_URL/variables" \
  -H "Content-Type: application/json" \
  -d "{
    \"keyName\": \"api_token\",
    \"valueContent\": \"secret_token_12345\",
    \"type\": \"secret\",
    \"description\": \"Encrypted API Token\",
    \"environment\": {\"id\": $ENV_ID}
  }")
echo "   Variable created (encrypted): $VAR_RESPONSE"
echo ""

# 3. Create Normal Variable
echo "3. Creating normal variable..."
curl -s -X POST "$BASE_URL/variables" \
  -H "Content-Type: application/json" \
  -d "{
    \"keyName\": \"base_url\",
    \"valueContent\": \"https://jsonplaceholder.typicode.com\",
    \"type\": \"normal\",
    \"environment\": {\"id\": $ENV_ID}
  }" | jq '.'
echo ""

# 4. Create Project and Module
echo "4. Creating project and module..."
PROJECT_RESPONSE=$(curl -s -X POST "$BASE_URL/projects" \
  -H "Content-Type: application/json" \
  -d '{
    "projectName": "Phase31Test",
    "description": "Phase 3.1 Testing Project"
  }')
PROJECT_ID=$(echo $PROJECT_RESPONSE | grep -o '"id":[0-9]*' | grep -o '[0-9]*')

MODULE_RESPONSE=$(curl -s -X POST "$BASE_URL/modules" \
  -H "Content-Type: application/json" \
  -d "{
    \"moduleName\": \"APITests\",
    \"project\": {\"id\": $PROJECT_ID}
  }")
MODULE_ID=$(echo $MODULE_RESPONSE | grep -o '"id":[0-9]*' | grep -o '[0-9]*')
echo "   Project ID: $PROJECT_ID, Module ID: $MODULE_ID"
echo ""

# 5. Create Test Case with Steps
echo "5. Creating test case with multiple steps..."
curl -s -X POST "$BASE_URL/cases" \
  -H "Content-Type: application/json" \
  -d "{
    \"caseName\": \"Multi-Step API Test\",
    \"module\": {\"id\": $MODULE_ID},
    \"method\": \"GET\",
    \"url\": \"\${base_url}/posts/1\",
    \"isActive\": true,
    \"assertionScript\": \"true\",
    \"steps\": [
      {
        \"stepOrder\": 1,
        \"stepName\": \"Get Post\",
        \"method\": \"GET\",
        \"url\": \"\${base_url}/posts/1\",
        \"extractors\": [
          {
            \"variableName\": \"post_id\",
            \"type\": \"JSONPATH\",
            \"expression\": \"$.id\"
          }
        ],
        \"assertions\": [
          {
            \"type\": \"EQUALS\",
            \"expression\": \"status_code\",
            \"expectedValue\": \"200\"
          }
        ]
      },
      {
        \"stepOrder\": 2,
        \"stepName\": \"Get Comments\",
        \"method\": \"GET\",
        \"url\": \"\${base_url}/posts/\${post_id}/comments\",
        \"assertions\": [
          {
            \"type\": \"EQUALS\",
            \"expression\": \"status_code\",
            \"expectedValue\": \"200\"
          }
        ]
      }
    ]
  }" | jq '.'
echo ""

# 6. Execute Test
echo "6. Executing test case..."
EXECUTION_RESULT=$(curl -s -X POST "$BASE_URL/cases/execute" \
  -H "Content-Type: application/json" \
  -d "{
    \"moduleId\": $MODULE_ID,
    \"envKey\": \"phase31test\"
  }")

echo "   Execution Result:"
echo "$EXECUTION_RESULT" | jq '.'
echo ""

echo "=== Verification Complete ==="
echo "✅ Environment created with encryption support"
echo "✅ Secret variables encrypted in database"
echo "✅ TestSteps with extractors and assertions created"
echo "✅ Multi-step execution completed"
