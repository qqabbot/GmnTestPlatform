# Phase 5.2 Task List: Bug Fixes & UI Optimization

**Goal**: Fix critical bugs in test case list and environment pages, and add pagination for better performance.

## Frontend

### 1. Fix Test Case List "New Case" Error (修复测试用例列表新建按钮错误)
- ✅ **Icon Import Fix**
  - ✅ Add missing icon imports (`Plus`, `Search`, `Edit`, `Delete`) from `@element-plus/icons-vue` in `TestCaseList.vue`
  - ✅ Replace `$router.push` with `useRouter()` composable for proper Vue 3 Composition API usage
  - ✅ Add `handleNewCase` method to handle navigation properly

- ✅ **Route Navigation Fix**
  - ✅ Use `router.push()` instead of `$router.push()` in template
  - ✅ Ensure proper router instance is available in component

- ✅ **TestCaseEditor.vue Dynamic Import Fix**
  - ✅ Add missing icon imports (`ArrowLeft`, `View`, `VideoPlay`, `Check`, `Link`, `DocumentCopy`) from `@element-plus/icons-vue` in `TestCaseEditor.vue`
  - ✅ Fix `$router.back()` to use `router.back()` for proper Vue 3 Composition API usage
  - ✅ Fix `placeholder` attribute syntax error: Changed from static string to `:placeholder` binding to handle special characters (single quotes) in cURL example
  - ✅ This fixes the "Failed to fetch dynamically imported module" error and the `[plugin:vite:vue] Attribute name cannot contain` compilation error

### 2. Fix Environment Page Error (修复环境页面错误)
- ✅ **Icon Import Fix**
  - ✅ Add missing icon imports (`Plus`, `Edit`, `Delete`) from `@element-plus/icons-vue` in `EnvironmentList.vue`
  - ✅ Ensure all icon components are properly imported and available

- ✅ **v-model Syntax Fix**
  - ✅ Fix v-model usage in EnvironmentList.vue: Cannot use ternary expressions in v-model
  - ✅ Create `currentEnvForm` computed property to handle edit/new form state
  - ✅ Replace `v-model="editEnv.id ? editEnv.envName : newEnv.envName"` with `v-model="currentEnvForm.envName"`

### 3. Add Pagination to Test Cases List (测试用例列表分页功能)
- ✅ **Pagination Component**
  - ✅ Add `el-pagination` component to `TestCaseList.vue`
  - ✅ Configure pagination with page size options: [10, 20, 50, 100]
  - ✅ Set default page size to 20 items per page
  - ✅ Add pagination state management (`currentPage`, `pageSize`, `totalCases`)

- ✅ **Data Filtering & Pagination Logic**
  - ✅ Update `filteredCases` computed property to apply pagination
  - ✅ Calculate total cases count for pagination display
  - ✅ Implement `handlePageChange` method to handle page navigation
  - ✅ Reset to first page when filters (search, project, module) change
  - ✅ Support page size change with automatic reset to first page

- ✅ **UI Enhancement**
  - ✅ Add pagination container with proper styling
  - ✅ Display pagination controls at bottom of table
  - ✅ Show total count, page sizes, page numbers, and jump to page input

## Testing

### Verification Checklist
- ✅ Test case list "New Case" button navigates correctly without errors
- ✅ Environment page loads without icon-related errors
- ✅ Test cases list displays pagination controls
- ✅ Pagination works correctly with filtering (search, project, module)
- ✅ Page size change resets to first page
- ✅ Total count updates correctly when filters are applied
- ✅ TestCaseEditor.vue loads correctly when navigating to new/edit case pages
- ✅ No "Failed to fetch dynamically imported module" errors in console
- ✅ No Vue template compilation errors (`[plugin:vite:vue] Attribute name cannot contain`)
- ✅ cURL import works without database constraint errors
- ✅ TestCase and TestStep created from cURL have assertionScript set to empty string (not null)

## Backend

### 4. Fix cURL Import Database Error (修复 cURL 导入数据库错误)
- ✅ **Database Constraint Fix**
  - ✅ Fixed `SQLIntegrityConstraintViolationException: Column 'assertion_script' cannot be null` error
  - ✅ Set default empty string `""` for `assertionScript` when creating TestCase from cURL
  - ✅ Set default empty string `""` for `assertionScript` when creating TestStep from cURL
  - ✅ This ensures database constraints are satisfied when importing cURL commands

## Summary

### Completed Tasks
1. ✅ Fixed TestCaseList.vue icon imports and router usage
2. ✅ Fixed EnvironmentList.vue icon imports
3. ✅ Added pagination to test cases list with 20 items per page default
4. ✅ Fixed TestCaseEditor.vue icon imports and router usage (fixes dynamic import error)
5. ✅ Fixed cURL import database constraint error (assertionScript cannot be null)

### Technical Details

**Files Modified:**

**Backend:**
- `backend/src/main/java/com/testing/automation/service/ImportService.java`
  - Added `setAssertionScript("")` when creating TestCase from cURL import
  - Added `setAssertionScript("")` when creating TestStep from cURL import
  - This fixes the database constraint violation error

**Frontend:**
- `frontend-app/src/views/TestCaseList.vue`
  - Added icon imports from `@element-plus/icons-vue`
  - Replaced `$router` with `useRouter()` composable
  - Added pagination state and logic
  - Updated `filteredCases` computed property to support pagination
  - Added pagination component to template

- `frontend-app/src/views/EnvironmentList.vue`
  - Added icon imports from `@element-plus/icons-vue`
  - Fixed v-model syntax: Created `currentEnvForm` computed property to replace ternary expressions in v-model bindings
  - This fixes the "v-model value must be a valid JavaScript member expression" compilation error

- `frontend-app/src/views/TestCaseEditor.vue`
  - Added missing icon imports (`ArrowLeft`, `View`, `VideoPlay`, `Check`, `Link`, `DocumentCopy`) from `@element-plus/icons-vue`
  - Fixed `$router.back()` to use `router.back()` for proper Vue 3 Composition API usage
  - Fixed `placeholder` attribute: Changed from `placeholder="..."` to `:placeholder="curlPlaceholder"` using a constant variable to properly handle special characters (single quotes) in the cURL example string
  - This fixes both the dynamic import error and the Vue template compilation error (`[plugin:vite:vue] Attribute name cannot contain`) that prevented the component from loading

**Key Changes:**
1. **Icon Imports**: All icon components are now properly imported from `@element-plus/icons-vue`
2. **Router Usage**: Using Vue 3 Composition API's `useRouter()` instead of `$router` in template
3. **Pagination**: Implemented client-side pagination with configurable page size
4. **Filter Integration**: Pagination resets when filters change to ensure consistent user experience

### Next Steps
- Consider implementing server-side pagination if test case count becomes very large (>1000)
- Add loading states during pagination changes
- Consider adding sorting capabilities to complement pagination

