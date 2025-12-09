#!/bin/bash

# Test Dry Run Mode
BASE_URL="http://localhost:7777/api"

echo "=== Testing Dry Run Mode ==="
echo ""

# 1. Setup Data
echo "1. Setting up Test Data..."
SUFFIX=$(date +%s)
PROJ_RES=$(curl -s -X POST "$BASE_URL/projects" -H "Content-Type: application/json" -d "{\"projectName\": \"DryRunProj_$SUFFIX\", \"description\": \"Test\"}")
echo "Project Response: $PROJ_RES"
PROJ_ID=$(echo "$PROJ_RES" | jq -r '.id')

MOD_RES=$(curl -s -X POST "$BASE_URL/modules" -H "Content-Type: application/json" -d "{\"moduleName\": \"DryRunMod_$SUFFIX\", \"project\": {\"id\": $PROJ_ID}}")
echo "Module Response: $MOD_RES"
MOD_ID=$(echo "$MOD_RES" | jq -r '.id')

ENV_RES=$(curl -s -X POST "$BASE_URL/environments" -H "Content-Type: application/json" -d "{\"envName\": \"DryRunEnv_$SUFFIX\", \"description\": \"Test\"}")
echo "Env Response: $ENV_RES"
ENV_ID=$(echo "$ENV_RES" | jq -r '.id')

# Create Variable
VAR_RES=$(curl -s -X POST "$BASE_URL/variables" -H "Content-Type: application/json" -d "{\"keyName\": \"base_url\", \"valueContent\": \"https://api.example.com\", \"type\": \"normal\", \"environment\": {\"id\": $ENV_ID}}")
echo "Variable Response: $VAR_RES"

# Create Test Case
CASE_RES=$(curl -s -X POST "$BASE_URL/cases" -H "Content-Type: application/json" -d "{
    \"caseName\": \"Dry Run Test\",
    \"module\": {\"id\": $MOD_ID},
    \"method\": \"GET\",
    \"url\": \"\${base_url}/users/1\",
    \"assertionScript\": \"assert response.status == 200\",
    \"isActive\": true
  }")
echo "Case Response: $CASE_RES"
CASE_ID=$(echo "$CASE_RES" | jq -r '.id')

echo "   Case ID: $CASE_ID"
echo ""

echo "2. Testing dry run on test case..."

# Dry run request
RESULT=$(curl -s -X POST "$BASE_URL/cases/$CASE_ID/dry-run" \
  -H "Content-Type: application/json" \
  -d "{\"envKey\": \"DryRunEnv_$SUFFIX\"}")

echo "Dry Run Result:"
echo "$RESULT" | jq '.'
echo ""

echo "Expected: Should show resolved URL, body, and variables without executing HTTP request"
