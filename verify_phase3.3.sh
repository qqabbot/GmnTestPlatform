#!/bin/bash

# verification script for Phase 3.3 (Step Templates)
BASE_URL="http://localhost:7777/api"

echo "=== Phase 3.3 Step Template Verification ==="
echo ""

# 1. Create a Project
echo "1. Creating Project..."
TIMESTAMP=$(date +%s)
PROJ_RES=$(curl -s -X POST "$BASE_URL/projects" -H "Content-Type: application/json" -d "{\"projectName\": \"TemplateProject_$TIMESTAMP\"}")
echo "Debug Project Res: $PROJ_RES"
PROJ_ID=$(echo "$PROJ_RES" | jq -r '.id')
echo "   Project ID: $PROJ_ID"

# 2. Create a Step Template
echo "2. Creating Step Template..."
TEMPLATE_RES=$(curl -s -X POST "$BASE_URL/step-templates" \
  -H "Content-Type: application/json" \
  -d "{
    \"name\": \"Login Template\",
    \"method\": \"POST\",
    \"url\": \"https://httpbin.org/post\",
    \"body\": \"{\\\"user\\\": \\\"admin\\\"}\",
    \"headers\": \"{\\\"Content-Type\\\": \\\"application/json\\\"}\",
    \"assertionScript\": \"status_code == 200\",
    \"projectId\": $PROJ_ID
  }")
echo "Debug Template Res: $TEMPLATE_RES"
TEMPLATE_ID=$(echo "$TEMPLATE_RES" | jq -r '.id')
echo "   Template Created: ID=$TEMPLATE_ID"

# 3. Verify Template Retrieval
echo "3. Verifying Template Retrieval..."
GET_RES=$(curl -s "$BASE_URL/step-templates?projectId=$PROJ_ID")
COUNT=$(echo "$GET_RES" | jq '. | length')
if [ "$COUNT" -ge 1 ]; then
  echo "   ✅ Template Retrieval Successful (Count: $COUNT)"
else
  echo "   ❌ Template Retrieval Failed"
  exit 1
fi

# 4. Integrate with Test Case (Simulate Import)
echo "4. Creating Test Case with Imported Step Data..."
# Note: Frontend handles the "copy", so backend just receives a standard TestStep.
# We verify that we can create a case with steps structure similar to what template provides.
TIMESTAMP=$(date +%s)
MODULE_ID=$(curl -s -X POST "$BASE_URL/modules" -H "Content-Type: application/json" -d "{\"moduleName\": \"TemplateModule_$TIMESTAMP\", \"project\": {\"id\": $PROJ_ID}}" | jq -r '.id')
echo "Debug Module ID: $MODULE_ID"

CASE_PAYLOAD=$(cat <<EOF
{
  "caseName": "Case from Template",
  "module": {"id": $MODULE_ID},
  "method": "GET",
  "url": "https://httpbin.org/get",
  "assertionScript": "status_code == 200",
  "isActive": true,
  "steps": [
    {
      "stepName": "Imported Step",
      "stepOrder": 1,
      "method": "POST",
      "url": "https://httpbin.org/post",
      "headers": "{\"Content-Type\": \"application/json\"}",
      "body": "{\"user\": \"admin\"}",
      "assertionScript": "status_code == 200",
      "enabled": true
    }
  ]
}
EOF
)
CASE_RES=$(curl -s -X POST "$BASE_URL/cases" -H "Content-Type: application/json" -d "$CASE_PAYLOAD")
echo "Debug Case Res: $CASE_RES"
CASE_ID=$(echo "$CASE_RES" | jq -r '.id')
echo "   Case Created: ID=$CASE_ID"

# 5. Execute Case
echo "5. Executing Case..."
EXEC_RES=$(curl -s -X POST "$BASE_URL/cases/execute?moduleId=$MODULE_ID&envKey=dev" -H "Content-Type: application/json")
STATUS=$(echo "$EXEC_RES" | jq -r '.[0].status')

if [ "$STATUS" == "PASS" ]; then
  echo "   ✅ Execution Passed!"
else
  echo "   ❌ Execution Failed: $EXEC_RES"
  exit 1
fi

echo ""
echo "Phase 3.3 Verification Complete!"
