#!/bin/bash

# Phase 3.1 Advanced Features Verification Script
# Tests Variable Inheritance (Project > Module > Environment)

BASE_URL="http://localhost:7777/api"

echo "=== Phase 3.1 Advanced Verification ==="
echo ""

# 1. Setup Hierarchy
echo "1. Setting up Project Hierarchy..."
PROJ_RES=$(curl -s -X POST "$BASE_URL/projects" \
  -H "Content-Type: application/json" \
  -d '{"projectName": "InheritanceTestProj", "description": "Test Inheritance"}')
echo "Create Project Response: $PROJ_RES"
PROJ_ID=$(echo "$PROJ_RES" | jq -r '.id')

MOD_ID=$(curl -s -X POST "$BASE_URL/modules" \
  -H "Content-Type: application/json" \
  -d "{\"moduleName\": \"InheritanceTestMod\", \"project\": {\"id\": $PROJ_ID}}" | jq -r '.id')

ENV_ID=$(curl -s -X POST "$BASE_URL/environments" \
  -H "Content-Type: application/json" \
  -d '{"envName": "InheritanceTestEnv", "description": "Test Inheritance"}' | jq -r '.id')

echo "   Project ID: $PROJ_ID, Module ID: $MOD_ID, Environment ID: $ENV_ID"
echo ""

# 2. Create Variables at different levels
echo "2. Creating Variables..."

# Project Level: var_proj = "value_from_project"
curl -s -X POST "$BASE_URL/variables" \
  -H "Content-Type: application/json" \
  -d "{\"keyName\": \"var_proj\", \"valueContent\": \"value_from_project\", \"type\": \"normal\", \"project\": {\"id\": $PROJ_ID}}" > /dev/null

# Module Level: var_mod = "value_from_module"
curl -s -X POST "$BASE_URL/variables" \
  -H "Content-Type: application/json" \
  -d "{\"keyName\": \"var_mod\", \"valueContent\": \"value_from_module\", \"type\": \"normal\", \"module\": {\"id\": $MOD_ID}}" > /dev/null

# Environment Level: var_env = "value_from_env"
curl -s -X POST "$BASE_URL/variables" \
  -H "Content-Type: application/json" \
  -d "{\"keyName\": \"var_env\", \"valueContent\": \"value_from_env\", \"type\": \"normal\", \"environment\": {\"id\": $ENV_ID}}" > /dev/null

# Overridden Variable: common_var
# Project: "proj_val"
curl -s -X POST "$BASE_URL/variables" \
  -H "Content-Type: application/json" \
  -d "{\"keyName\": \"common_var\", \"valueContent\": \"proj_val\", \"type\": \"normal\", \"project\": {\"id\": $PROJ_ID}}" > /dev/null

# Module: "mod_val"
curl -s -X POST "$BASE_URL/variables" \
  -H "Content-Type: application/json" \
  -d "{\"keyName\": \"common_var\", \"valueContent\": \"mod_val\", \"type\": \"normal\", \"module\": {\"id\": $MOD_ID}}" > /dev/null

# Environment: "env_val"
curl -s -X POST "$BASE_URL/variables" \
  -H "Content-Type: application/json" \
  -d "{\"keyName\": \"common_var\", \"valueContent\": \"env_val\", \"type\": \"normal\", \"environment\": {\"id\": $ENV_ID}}" > /dev/null

echo "   Variables created."
echo ""

# 3. Create Test Case to verify values
echo "3. Creating Test Case..."
# We will use a mock URL that echoes back the query params or body, but since we don't have a local echo server easily,
# we will rely on the "Assertion" feature to verify the variable replacement.
# We'll use a dummy request and assert on the replaced values if possible, OR
# we can use the "Dry Run" feature if we had it.
# Since we don't have dry run, we'll make a request to a public echo service (e.g. postman-echo.com)
# OR we can just check if the execution log contains the replaced values.

# Let's use postman-echo.com/get?p1=${var_proj}&p2=${var_mod}&p3=${var_env}&p4=${common_var}
CASE_RES=$(curl -s -X POST "$BASE_URL/cases" \
  -H "Content-Type: application/json" \
  -d "{
    \"caseName\": \"Inheritance Verification\",
    \"module\": {\"id\": $MOD_ID},
    \"method\": \"GET\",
    \"url\": \"https://postman-echo.com/get?p1=\${var_proj}&p2=\${var_mod}&p3=\${var_env}&p4=\${common_var}\",
    \"isActive\": true
  }")

echo "Create Case Response: $CASE_RES"
CASE_ID=$(echo "$CASE_RES" | jq -r '.id')

echo "   Case ID: $CASE_ID"
echo ""

# 4. Execute Test Case
echo "4. Executing Test Case..."
RESULT=$(curl -s -X POST "$BASE_URL/cases/execute" \
  -H "Content-Type: application/json" \
  -d "{\"moduleId\": $MOD_ID, \"envKey\": \"InheritanceTestEnv\"}")

echo "   Execution Result:"
echo "$RESULT" | jq '.'

# 5. Verify Logs (Optional, manual check for now)
echo ""
echo "   Check the 'detail' field above. It should show the URL with replaced values."
echo "   Expected URL params:"
echo "   p1=value_from_project"
echo "   p2=value_from_module"
echo "   p3=value_from_env"
echo "   p4=env_val (Environment should override Module and Project)"
