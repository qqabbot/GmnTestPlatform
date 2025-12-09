# Phase 3.3: Independent Step Templates & Composition

**Goal**: Create a "Step Template" library to manage reusable test steps, and allow Test Cases to fully reuse these steps by importing/copying them.

## Backend
- ✅ **Entity & Schema**
  - ✅ Create `TestStepTemplate` entity (Fields: name, method, url, headers, body, assertions, extractors, project_id).
  - ✅ Create Repository and Service for `TestStepTemplate`.
  - ✅ Create Controller `TestStepTemplateController` (CRUD API).
- ✅ **Integration**
  - ✅ Ensure `TestStep` can be created from `TestStepTemplate` (DTO mapping).

## Frontend
- ✅ **Step Template Management**
  - ✅ Create "Step Library" view (List/Create/Edit/Delete Templates).
- ✅ **Test Case Editor Integration**
  - ✅ Add "Import from Library" button/drawer in `TestCaseEditor` sidebar.
  - ✅ Implement logic to fetching template and converting to a Case Step.
- ✅ **UI Refinement**
  - ✅ Move "Steps Library" to a dedicated top-level menu.
- ✅ **Execution Log Enhancement**
  - ✅ Backend: Ensure `TestExecutionLog` captures URL, Headers, Body, Variables, Response.
  - ✅ Frontend: Display detailed request/response info in `TestCaseEditor` execution result.

## Verification
- ✅ **Automated Test**
  - ✅ Verify Template CRUD.
  - ✅ Verify Case execution with imported steps.
- ✅ **Regression Test**
  - ✅ Ensure existing cases still work.
