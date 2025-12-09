#!/bin/bash

# Simplified Phase 3.1 Variable Inheritance Test
BASE_URL="http://localhost:7777/api"

echo "=== Testing Variable Inheritance ==="
echo ""

# Use existing Project ID=1, Module ID=1, Environment ID=1
PROJ_ID=1
MOD_ID=1
ENV_NAME="InheritanceTestEnv"

echo "1. Creating test variables at different levels..."

# Project Level: base_url = "https://project.example.com"
curl -s -X POST "$BASE_URL/variables" \
  -H "Content-Type: application/json" \
  -d "{\"keyName\": \"base_url\", \"valueContent\": \"https://project.example.com\", \"type\": \"normal\", \"project\": {\"id\": $PROJ_ID}}" > /dev/null

# Module Level: base_url = "https://module.example.com" (should override project)
curl -s -X POST "$BASE_URL/variables" \
  -H "Content-Type: application/json" \
  -d "{\"keyName\": \"base_url\", \"valueContent\": \"https://module.example.com\", \"type\": \"normal\", \"module\": {\"id\": $MOD_ID}}" > /dev/null

# Environment Level: base_url = "https://env.example.com" (should override all)
curl -s -X POST "$BASE_URL/variables" \
  -H "Content-Type: application/json" \
  -d "{\"keyName\": \"base_url\", \"valueContent\": \"https://env.example.com\", \"type\": \"normal\", \"environment\": {\"id\": 1}}" > /dev/null

echo "   Variables created."
echo ""

# Create a simple test case
echo "2. Creating test case..."
CASE_ID=$(curl -s -X POST "$BASE_URL/cases" \
  -H "Content-Type: application/json" \
  -d "{
    \"caseName\": \"Variable Inheritance Test\",
    \"module\": {\"id\": $MOD_ID},
    \"method\": \"GET\",
    \"url\": \"\${base_url}/test\",
    \"isActive\": true
  }" | jq -r '.id')

echo "   Case ID: $CASE_ID"
echo ""

# Execute
echo "3. Executing test case..."
RESULT=$(curl -s -X POST "$BASE_URL/cases/execute" \
  -H "Content-Type: application/json" \
  -d "{\"moduleId\": $MOD_ID, \"envKey\": \"$ENV_NAME\"}")

echo "   Result:"
echo "$RESULT" | jq '.'
echo ""

echo "4. Expected: URL should be 'https://env.example.com/test' (Environment overrides Module and Project)"
echo "   Check the logs or response above to verify."
