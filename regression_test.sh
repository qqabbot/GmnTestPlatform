#!/bin/bash

echo "============================================"
echo "Full Regression Test Suite for Steps Save"
echo "============================================"
echo ""

API_BASE="http://localhost:7777/api"
PASS=0
FAIL=0

# Test 1: Create case without steps
echo "[Test 1/8] Creating case without steps..."
RESPONSE=$(curl -s -X POST "${API_BASE}/cases" \
  -H "Content-Type: application/json" \
  -d '{
    "caseName": "Test Case No Steps",
    "method": "GET",
    "url": "http://example.com",
    "headers": "{}",
    "body": "",
    "assertionScript": "status_code == 200",
    "module": {"id": 1}
  }')

if echo "$RESPONSE" | grep -q '"id"'; then
  echo "✓ PASS: Case created without steps"
  CASE_NO_STEPS_ID=$(echo $RESPONSE | grep -o '"id":[0-9]*' | head -1 | grep -o '[0-9]*')
  ((PASS++))
else
  echo "✗ FAIL: Failed to create case without steps"
  echo "Response: $RESPONSE"
  ((FAIL++))
fi
echo ""

# Test 2: Create case with 1 step
echo "[Test 2/8] Creating case with 1 step..."
RESPONSE=$(curl -s -X POST "${API_BASE}/cases" \
  -H "Content-Type: application/json" \
  -d '{
    "caseName": "Test Case 1 Step",
    "method": "POST",
    "url": "${base_url}/post",
    "headers": "{}",
    "body": "{}",
    "assertionScript": "status_code == 200",
    "module": {"id": 1},
    "steps": [
      {
        "stepName": "Step 1",
        "stepOrder": 1,
        "method": "GET",
        "url": "${base_url}/get",
        "headers": "{}",
        "body": "",
        "assertionScript": "status_code == 200"
      }
    ]
  }')

if echo "$RESPONSE" | grep -q '"id"'; then
  echo "✓ PASS: Case created with 1 step"
  CASE_1_STEP_ID=$(echo $RESPONSE | grep -o '"id":[0-9]*' | head -1 | grep -o '[0-9]*')
  ((PASS++))
else
  echo "✗ FAIL: Failed to create case with 1 step"
  echo "Response: $RESPONSE"
  ((FAIL++))
fi
echo ""

# Test 3: Create case with multiple steps
echo "[Test 3/8] Creating case with 3 steps..."
RESPONSE=$(curl -s -X POST "${API_BASE}/cases" \
  -H "Content-Type: application/json" \
  -d '{
    "caseName": "Test Case 3 Steps",
    "method": "POST",
    "url": "${base_url}/post",
    "headers": "{}",
    "body": "{}",
    "assertionScript": "status_code == 200",
    "module": {"id": 1},
    "steps": [
      {
        "stepName": "Step 1: Register",
        "stepOrder": 1,
        "method": "POST",
        "url": "${base_url}/post",
        "headers": "{\"Content-Type\": \"application/json\"}",
        "body": "{\"username\": \"test\"}",
        "assertionScript": "status_code == 200"
      },
      {
        "stepName": "Step 2: Login",
        "stepOrder": 2,
        "method": "POST",
        "url": "${base_url}/post",
        "headers": "{\"Content-Type\": \"application/json\"}",
        "body": "{\"username\": \"test\", \"password\": \"123\"}",
        "assertionScript": "status_code == 200"
      },
      {
        "stepName": "Step 3: GetInfo",
        "stepOrder": 3,
        "method": "GET",
        "url": "${base_url}/get",
        "headers": "{\"Authorization\": \"Bearer token\"}",
        "body": "",
        "assertionScript": "status_code == 200"
      }
    ]
  }')

if echo "$RESPONSE" | grep -q '"id"'; then
  echo "✓ PASS: Case created with 3 steps"
  CASE_3_STEPS_ID=$(echo $RESPONSE | grep -o '"id":[0-9]*' | head -1 | grep -o '[0-9]*')
  ((PASS++))
else
  echo "✗ FAIL: Failed to create case with 3 steps"
  echo "Response: $RESPONSE"
  ((FAIL++))
fi
echo ""

# Test 4: Verify case retrieval
echo "[Test 4/8] Retrieving case with steps..."
if [ ! -z "$CASE_3_STEPS_ID" ]; then
  RESPONSE=$(curl -s "${API_BASE}/cases/${CASE_3_STEPS_ID}")
  STEP_COUNT=$(echo "$RESPONSE" | grep -o '"stepName"' | wc -l | tr -d ' ')
  
  if [ "$STEP_COUNT" = "3" ]; then
    echo "✓ PASS: Retrieved case has 3 steps"
    ((PASS++))
  else
    echo "✗ FAIL: Expected 3 steps, got $STEP_COUNT"
    ((FAIL++))
  fi
else
  echo "⊘ SKIP: No case ID to retrieve"
fi
echo ""

# Test 5: Update case (add more steps)
echo "[Test 5/8] Updating case to add steps..."
if [ ! -z "$CASE_1_STEP_ID" ]; then
  RESPONSE=$(curl -s -X PUT "${API_BASE}/cases/${CASE_1_STEP_ID}" \
    -H "Content-Type: application/json" \
    -d '{
      "caseName": "Test Case Updated",
      "method": "POST",
      "url": "${base_url}/post",
      "headers": "{}",
      "body": "{}",
      "assertionScript": "status_code == 200",
      "module": {"id": 1},
      "steps": [
        {
          "stepName": "Step 1 Updated",
          "stepOrder": 1,
          "method": "GET",
          "url": "${base_url}/get",
          "headers": "{}",
          "body": "",
          "assertionScript": "status_code == 200"
        },
        {
          "stepName": "Step 2 New",
          "stepOrder": 2,
          "method": "POST",
          "url": "${base_url}/post",
          "headers": "{}",
          "body": "{}",
          "assertionScript": "status_code == 200"
        }
      ]
    }')
  
  if echo "$RESPONSE" | grep -q '"id"'; then
    echo "✓ PASS: Case updated successfully"
    ((PASS++))
  else
    echo "✗ FAIL: Failed to update case"
    echo "Response: $RESPONSE"
    ((FAIL++))
  fi
else
  echo "⊘ SKIP: No case ID to update"
fi
echo ""

# Test 6: Dry Run with steps
echo "[Test 6/8] Testing Dry Run with steps..."
if [ ! -z "$CASE_3_STEPS_ID" ]; then
  RESPONSE=$(curl -s -X POST "${API_BASE}/cases/${CASE_3_STEPS_ID}/dry-run" \
    -H "Content-Type: application/json" \
    -d '{"envName": "mock"}')
  
  if echo "$RESPONSE" | grep -q 'resolvedUrl'; then
    echo "✓ PASS: Dry run executed successfully"
    ((PASS++))
  else
    echo "✗ FAIL: Dry run failed"
    echo "Response: $RESPONSE"
    ((FAIL++))
  fi
else
  echo "⊘ SKIP: No case ID for dry run"
fi
echo ""

# Test 7: List all cases
echo "[Test 7/8] Listing all cases..."
RESPONSE=$(curl -s "${API_BASE}/cases")
CASE_COUNT=$(echo "$RESPONSE" | grep -o '"id":[0-9]*' | wc -l | tr -d ' ')

if [ "$CASE_COUNT" -ge "3" ]; then
  echo "✓ PASS: Retrieved $CASE_COUNT cases"
  ((PASS++))
else
  echo "✗ FAIL: Expected at least 3 cases, got $CASE_COUNT"
  ((FAIL++))
fi
echo ""

# Test 8: Verify assertionScript field
echo "[Test 8/8] Verifying assertionScript field in steps..."
if [ ! -z "$CASE_3_STEPS_ID" ]; then
  RESPONSE=$(curl -s "${API_BASE}/cases/${CASE_3_STEPS_ID}")
  
  if echo "$RESPONSE" | grep -q '"assertionScript"'; then
    echo "✓ PASS: assertionScript field present in steps"
    ((PASS++))
  else
    echo "✗ FAIL: assertionScript field missing in steps"
    ((FAIL++))
  fi
else
  echo "⊘ SKIP: No case ID to verify"
fi
echo ""

# Summary
echo "============================================"
echo "Test Summary"
echo "============================================"
echo "Total tests: 8"
echo "Passed: $PASS"
echo "Failed: $FAIL"
echo ""

if [ $FAIL -eq 0 ]; then
  echo "🎉 ALL TESTS PASSED!"
  exit 0
else
  echo "❌ SOME TESTS FAILED"
  exit 1
fi
