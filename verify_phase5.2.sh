#!/bin/bash

# Phase 5.2 Verification Script
# Verifies bug fixes and pagination feature

set -e

echo "=========================================="
echo "Phase 5.2 Verification Script"
echo "=========================================="
echo ""

# Colors for output
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Track verification results
PASSED=0
FAILED=0

# Function to check if a pattern exists in a file
check_pattern() {
    local file=$1
    local pattern=$2
    local description=$3
    
    if grep -q "$pattern" "$file" 2>/dev/null; then
        echo -e "${GREEN}✓${NC} $description"
        ((PASSED++))
        return 0
    else
        echo -e "${RED}✗${NC} $description"
        echo "  Pattern not found: $pattern"
        ((FAILED++))
        return 1
    fi
}

echo "1. Checking TestCaseList.vue fixes..."
echo "-----------------------------------"

TEST_CASE_LIST="frontend-app/src/views/TestCaseList.vue"

# Check icon imports
check_pattern "$TEST_CASE_LIST" "from '@element-plus/icons-vue'" \
    "Icon imports from @element-plus/icons-vue"

check_pattern "$TEST_CASE_LIST" "Plus.*Search.*Edit.*Delete" \
    "Required icons (Plus, Search, Edit, Delete) imported"

# Check router usage
check_pattern "$TEST_CASE_LIST" "useRouter" \
    "useRouter composable imported"

check_pattern "$TEST_CASE_LIST" "const router = useRouter" \
    "Router instance created"

check_pattern "$TEST_CASE_LIST" "handleNewCase" \
    "handleNewCase method exists"

check_pattern "$TEST_CASE_LIST" "router.push" \
    "router.push used instead of \$router.push"

# Check pagination
check_pattern "$TEST_CASE_LIST" "el-pagination" \
    "Pagination component added"

check_pattern "$TEST_CASE_LIST" "currentPage.*ref" \
    "currentPage state variable"

check_pattern "$TEST_CASE_LIST" "pageSize.*ref" \
    "pageSize state variable"

check_pattern "$TEST_CASE_LIST" "totalCases.*ref" \
    "totalCases state variable"

check_pattern "$TEST_CASE_LIST" "pageSize.*20" \
    "Default page size set to 20"

check_pattern "$TEST_CASE_LIST" "handlePageChange" \
    "handlePageChange method exists"

echo ""
echo "2. Checking EnvironmentList.vue fixes..."
echo "----------------------------------------"

ENV_LIST="frontend-app/src/views/EnvironmentList.vue"

# Check icon imports
check_pattern "$ENV_LIST" "from '@element-plus/icons-vue'" \
    "Icon imports from @element-plus/icons-vue"

check_pattern "$ENV_LIST" "Plus.*Edit.*Delete" \
    "Required icons (Plus, Edit, Delete) imported"

echo ""
echo "3. Checking TestCaseEditor.vue fixes..."
echo "---------------------------------------"

TEST_CASE_EDITOR="frontend-app/src/views/TestCaseEditor.vue"

# Check icon imports
check_pattern "$TEST_CASE_EDITOR" "from '@element-plus/icons-vue'" \
    "Icon imports from @element-plus/icons-vue"

check_pattern "$TEST_CASE_EDITOR" "ArrowLeft.*View.*VideoPlay.*Check.*Link.*DocumentCopy" \
    "Required icons (ArrowLeft, View, VideoPlay, Check, Link, DocumentCopy) imported"

# Check router usage
check_pattern "$TEST_CASE_EDITOR" "router.back" \
    "router.back() used instead of \$router.back()"

echo ""
echo "4. Checking file syntax..."
echo "-------------------------"

# Check if files exist
if [ -f "$TEST_CASE_LIST" ]; then
    echo -e "${GREEN}✓${NC} TestCaseList.vue exists"
    ((PASSED++))
else
    echo -e "${RED}✗${NC} TestCaseList.vue not found"
    ((FAILED++))
fi

if [ -f "$ENV_LIST" ]; then
    echo -e "${GREEN}✓${NC} EnvironmentList.vue exists"
    ((PASSED++))
else
    echo -e "${RED}✗${NC} EnvironmentList.vue not found"
    ((FAILED++))
fi

if [ -f "$TEST_CASE_EDITOR" ]; then
    echo -e "${GREEN}✓${NC} TestCaseEditor.vue exists"
    ((PASSED++))
else
    echo -e "${RED}✗${NC} TestCaseEditor.vue not found"
    ((FAILED++))
fi

echo ""
echo "5. Summary"
echo "=========="
echo -e "Total checks: $((PASSED + FAILED))"
echo -e "${GREEN}Passed: $PASSED${NC}"
if [ $FAILED -gt 0 ]; then
    echo -e "${RED}Failed: $FAILED${NC}"
    echo ""
    echo "Please review the failed checks above."
    exit 1
else
    echo -e "${GREEN}Failed: $FAILED${NC}"
    echo ""
    echo -e "${GREEN}All checks passed!${NC}"
    echo ""
    echo "Phase 5.2 verification complete."
    echo ""
    echo "Next steps:"
    echo "1. Start the frontend development server"
    echo "2. Navigate to Test Cases list and verify:"
    echo "   - 'New Case' button works without errors"
    echo "   - Pagination controls are visible"
    echo "   - Pagination works with filters"
    echo "3. Click 'New Case' and verify:"
    echo "   - TestCaseEditor.vue loads without errors"
    echo "   - No 'Failed to fetch dynamically imported module' errors"
    echo "   - All icons display correctly"
    echo "4. Navigate to Environments page and verify:"
    echo "   - Page loads without errors"
    echo "   - Icons display correctly"
    exit 0
fi

