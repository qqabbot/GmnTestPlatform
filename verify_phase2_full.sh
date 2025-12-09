#!/bin/bash

echo "=== Phase 2 Full Stack Verification ==="

# 1. Create Project
echo "1. Creating Project..."
PROJ_RES=$(curl -s -X POST http://localhost:7777/api/projects \
  -H "Content-Type: application/json" \
  -d '{
    "projectName": "E2E Test Project",
    "description": "End-to-end testing"
  }')
PROJ_ID=$(echo $PROJ_RES | python3 -c "import sys, json; print(json.load(sys.stdin).get('id', ''))")
echo "✅ Project Created: ID=$PROJ_ID"

# 2. Create 2 Modules
echo ""
echo "2. Creating Modules..."
MOD1_RES=$(curl -s -X POST http://localhost:7777/api/modules \
  -H "Content-Type: application/json" \
  -d "{\"moduleName\": \"Auth Module\", \"project\": {\"id\": $PROJ_ID}}")
MOD1_ID=$(echo $MOD1_RES | python3 -c "import sys, json; print(json.load(sys.stdin).get('id', ''))")
echo "✅ Module 1 Created: ID=$MOD1_ID"

MOD2_RES=$(curl -s -X POST http://localhost:7777/api/modules \
  -H "Content-Type: application/json" \
  -d "{\"moduleName\": \"User Module\", \"project\": {\"id\": $PROJ_ID}}")
MOD2_ID=$(echo $MOD2_RES | python3 -c "import sys, json; print(json.load(sys.stdin).get('id', ''))")
echo "✅ Module 2 Created: ID=$MOD2_ID"

# 3. Create Test Cases
echo ""
echo "3. Creating Test Cases..."
for i in {1..3}; do
  curl -s -X POST http://localhost:7777/api/cases \
    -H "Content-Type: application/json" \
    -d "{
      \"module\": {\"id\": $MOD1_ID},
      \"caseName\": \"Auth Case $i\",
      \"method\": \"GET\",
      \"url\": \"http://httpbin.org/get\",
      \"assertionScript\": \"status_code == 200\",
      \"isActive\": true
    }" > /dev/null
  echo "  ✅ Created Auth Case $i"
done

for i in {1..2}; do
  curl -s -X POST http://localhost:7777/api/cases \
    -H "Content-Type: application/json" \
    -d "{
      \"module\": {\"id\": $MOD2_ID},
      \"caseName\": \"User Case $i\",
      \"method\": \"GET\",
      \"url\": \"http://httpbin.org/get\",
      \"assertionScript\": \"status_code == 200\",
      \"isActive\": true
    }" > /dev/null
  echo "  ✅ Created User Case $i"
done

# 4. Execute by Project
echo ""
echo "4. Executing by Project (should run all 5 cases)..."
PROJ_EXEC=$(curl -s -X POST "http://localhost:7777/api/cases/execute?projectId=$PROJ_ID&envKey=dev")
PROJ_COUNT=$(echo $PROJ_EXEC | python3 -c "import sys, json; print(len(json.load(sys.stdin)))")
echo "  Executed $PROJ_COUNT cases"
if [ "$PROJ_COUNT" -eq 5 ]; then
  echo "✅ Project execution correct (5 cases)"
else
  echo "❌ Project execution incorrect (expected 5, got $PROJ_COUNT)"
fi

# 5. Execute by Module
echo ""
echo "5. Executing by Module (should run 3 cases)..."
MOD_EXEC=$(curl -s -X POST "http://localhost:7777/api/cases/execute?moduleId=$MOD1_ID&envKey=dev")
MOD_COUNT=$(echo $MOD_EXEC | python3 -c "import sys, json; print(len(json.load(sys.stdin)))")
echo "  Executed $MOD_COUNT cases"
if [ "$MOD_COUNT" -eq 3 ]; then
  echo "✅ Module execution correct (3 cases)"
else
  echo "❌ Module execution incorrect (expected 3, got $MOD_COUNT)"
fi

# 6. Check Reports
echo ""
echo "6. Checking Reports..."
REPORT=$(curl -s "http://localhost:7777/api/reports/project/$PROJ_ID")
REPORT_COUNT=$(echo $REPORT | python3 -c "import sys, json; print(len(json.load(sys.stdin)))")
echo "  Found $REPORT_COUNT execution records"
if [ "$REPORT_COUNT" -ge 5 ]; then
  echo "✅ Reports contain execution history"
else
  echo "❌ Reports missing records"
fi

echo ""
echo "=== Phase 2 Verification Complete ==="
echo ""
echo "Summary:"
echo "  - Project: $PROJ_ID"
echo "  - Modules: $MOD1_ID, $MOD2_ID"
echo "  - Test Cases: 5 total (3 in Auth, 2 in User)"
echo "  - Project Execution: $PROJ_COUNT cases"
echo "  - Module Execution: $MOD_COUNT cases"
echo "  - Execution Records: $REPORT_COUNT"
echo ""
echo "Frontend URLs:"
echo "  - Projects: http://localhost:8888/projects"
echo "  - Test Cases: http://localhost:8888/cases"
echo "  - Execution: http://localhost:8888/execution"
echo "  - Reports: http://localhost:8888/reports"
