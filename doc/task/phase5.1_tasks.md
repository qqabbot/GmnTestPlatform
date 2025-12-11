# Phase 5.1 Task List: Import Enhancement & UI Improvements

**Goal**: Enhance import capabilities with cURL support, improve test case editing workflow, and optimize logging configuration.

## Backend

### 1. cURL Import Support (cURL 导入支持)
- ⏭️ **cURL Parser Implementation**
  - ⏭️ Create `CurlParser` utility class to parse cURL commands
  - ⏭️ Support parsing HTTP method (GET, POST, PUT, DELETE, etc.)
  - ⏭️ Support parsing URL with query parameters
  - ⏭️ Support parsing headers (including Content-Type, Authorization, etc.)
  - ⏭️ Support parsing request body (JSON, form-data, raw text)
  - ⏭️ Handle escaped characters and special cases in cURL commands
  - ⏭️ Support both single-line and multi-line cURL commands

- ⏭️ **Import Service Enhancement**
  - ⏭️ Add `importFromCurl()` method to `ImportService`
  - ⏭️ Create API endpoint `/api/import/curl` in `ImportController`
  - ⏭️ Accept cURL command string in request body
  - ⏭️ Parse cURL and create TestCase or TestStep
  - ⏭️ Support importing as new test case or adding as step to existing case
  - ⏭️ Handle errors gracefully with detailed error messages

- ⏭️ **Integration with Test Case Editor**
  - ⏭️ Support pasting cURL in URL or Body input fields
  - ⏭️ Auto-detect cURL format and trigger parsing
  - ⏭️ Auto-populate Method, URL, Headers, and Body fields
  - ⏭️ Provide visual feedback when cURL is detected and parsed

### 2. Test Case Reference in Steps (步骤中引用已有测试用例)
- ⏭️ **Backend Support**
  - ⏭️ Add `referenceCaseId` field to `TestStep` model (optional)
  - ⏭️ Update database schema to add `reference_case_id` column
  - ⏭️ Modify `TestStepMapper` to support reference case lookup
  - ⏭️ Implement step execution logic to handle referenced cases
  - ⏭️ When step has `referenceCaseId`, execute the referenced test case
  - ⏭️ Support nested references (case A references case B which references case C)
  - ⏭️ Add circular reference detection to prevent infinite loops

- ⏭️ **API Endpoints**
  - ⏭️ Add endpoint to get list of test cases for reference selection
  - ⏭️ Support filtering by project/module for easier selection
  - ⏭️ Return case metadata (name, method, URL) for display

### 3. Environment Edit Functionality (环境编辑功能)
- ⏭️ **Backend Enhancement**
  - ⏭️ Verify `EnvironmentController.updateEnvironment()` is working correctly
  - ⏭️ Ensure all environment fields can be updated (envName, domain, description)
  - ⏭️ Add validation for environment updates
  - ⏭️ Handle cascading updates if needed (e.g., update related variables)

- ⏭️ **Frontend Implementation**
  - ⏭️ Add "Edit" button to environment list table
  - ⏭️ Create edit dialog similar to create dialog
  - ⏭️ Pre-populate form with existing environment data
  - ⏭️ Call update API endpoint on save
  - ⏭️ Refresh environment list after update
  - ⏭️ Show success/error messages

### 4. SQL Logging & Log Optimization (SQL 日志与日志优化)
- ⏭️ **SQL Logging Configuration**
  - ⏭️ Configure MyBatis to log SQL statements
  - ⏭️ Set `mybatis.configuration.log-impl` to appropriate logger
  - ⏭️ Add SQL logging level configuration in `application.yml`
  - ⏭️ Log SQL statements with parameters (prepared statements)
  - ⏭️ Log execution time for slow queries (optional)
  - ⏭️ Format SQL logs for better readability

- ⏭️ **Log Level Optimization**
  - ⏭️ Review current log levels in `application.yml`
  - ⏭️ Reduce verbose logging from HikariCP connection pool (from DEBUG to INFO)
  - ⏭️ Reduce verbose logging from MySQL driver (from DEBUG to INFO or WARN)
  - ⏭️ Keep application-level logging at appropriate levels
  - ⏭️ Keep SQL logging enabled for debugging
  - ⏭️ Remove or reduce unnecessary Spring framework logs
  - ⏭️ Configure log rotation and file size limits

- ⏭️ **Structured Logging**
  - ⏭️ Use structured logging format (JSON or key-value pairs)
  - ⏭️ Include request ID or correlation ID in logs
  - ⏭️ Log important operations (test execution, case creation, etc.)
  - ⏭️ Separate SQL logs from application logs if needed

## Frontend

### 1. cURL Import UI (cURL 导入 UI)
- ⏭️ **Import Dialog Enhancement**
  - ⏭️ Add "Import from cURL" option in test case editor
  - ⏭️ Create cURL input dialog with textarea
  - ⏭️ Add "Paste cURL" button in URL/Body input fields
  - ⏭️ Auto-detect cURL format when pasting
  - ⏭️ Show parse preview before applying
  - ⏭️ Display parsed fields (Method, URL, Headers, Body) for confirmation
  - ⏭️ Support both creating new case and adding as step

- ⏭️ **User Experience**
  - ⏭️ Provide example cURL command as placeholder
  - ⏭️ Show validation errors if cURL parsing fails
  - ⏭️ Highlight parsed components in preview
  - ⏭️ Support undo/clear parsed data

### 2. Test Case Reference in Steps UI (步骤中引用测试用例 UI)
- ⏭️ **Step Editor Enhancement**
  - ⏭️ Add "Reference Existing Case" option in step editor
  - ⏭️ Create case selection dialog/dropdown
  - ⏭️ Show case list filtered by current project/module
  - ⏭️ Display case name, method, and URL for selection
  - ⏭️ Support search/filter in case list
  - ⏭️ Show visual indicator when step references a case
  - ⏭️ Allow switching between "Custom Step" and "Reference Case" modes

- ⏭️ **Visual Indicators**
  - ⏭️ Show referenced case name in step list
  - ⏭️ Display reference icon or badge
  - ⏭️ Show referenced case details on hover
  - ⏭️ Support navigating to referenced case

### 3. Environment Edit UI (环境编辑 UI)
- ⏭️ **Edit Functionality**
  - ⏭️ Add "Edit" button (pencil icon) next to each environment in list
  - ⏭️ Create edit dialog (reuse or modify create dialog)
  - ⏭️ Pre-populate form with existing environment data
  - ⏭️ Call update API on save
  - ⏭️ Handle validation errors
  - ⏭️ Refresh list after successful update

- ⏭️ **User Experience**
  - ⏭️ Show confirmation before saving changes
  - ⏭️ Display success message after update
  - ⏭️ Handle concurrent edits gracefully
  - ⏭️ Support cancel operation

## Testing & Verification

### 1. cURL Import Testing
- ⏭️ Test various cURL command formats
- ⏭️ Test with different HTTP methods
- ⏭️ Test with headers (including Authorization)
- ⏭️ Test with JSON body
- ⏭️ Test with form-data
- ⏭️ Test with query parameters
- ⏭️ Test error handling for invalid cURL commands

### 2. Test Case Reference Testing
- ⏭️ Test step execution with referenced case
- ⏭️ Test nested references
- ⏭️ Test circular reference detection
- ⏭️ Test case deletion when referenced by steps

### 3. Environment Edit Testing
- ⏭️ Test environment update functionality
- ⏭️ Test validation for required fields
- ⏭️ Test update with existing name (should fail or handle)
- ⏭️ Test cascading effects on variables

### 4. Logging Testing
- ⏭️ Verify SQL logs are generated
- ⏭️ Verify log levels are appropriate
- ⏭️ Verify no excessive logging
- ⏭️ Test log file rotation

## Summary
**Status**: ⏭️ In Progress
**Priority**: Medium - Enhancements to improve usability and developer experience
**Estimated Completion**: TBD

### Implementation Notes

1. **cURL Import**: Reference Postman's cURL import feature for inspiration. Support common cURL formats and edge cases.

2. **Test Case Reference**: This enables test case composition and reuse, similar to function calls in programming.

3. **Environment Edit**: Simple CRUD enhancement to allow users to modify environment configurations.

4. **SQL Logging**: Essential for debugging database issues. Balance between useful information and log volume.

### Dependencies

- cURL parsing library (consider using existing Java libraries or implement custom parser)
- MyBatis logging configuration
- Frontend UI components for dialogs and forms

