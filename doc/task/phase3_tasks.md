# Phase 3 Task List: Environment & Variable Management

## Backend
- ✅ **Data Model**
  - ✅ Create `Environment` entity (name, domain, description)
  - ✅ Create `GlobalVariable` entity (key, value, environment_id)
- ✅ **API Implementation**
  - ✅ `EnvironmentController` (CRUD)
  - ✅ `GlobalVariableController` (CRUD)
  - ✅ Update `TestCaseService` to load real variables from DB
  - ✅ Remove hardcoded environment logic

## Frontend
- ✅ **Environment Management**
  - ✅ Environment List Page
  - ✅ Environment Create/Edit Dialog
- ✅ **Variable Management**
  - ✅ Variable List Page (filtered by Environment)
  - ✅ Variable Create/Edit Dialog
- ✅ **Execution Page Update**
  - ✅ Fetch dynamic environments for dropdown
  - ✅ Remove hardcoded "dev/staging" options

## Testing
- ✅ Verify Environment creation
- ✅ Verify Variable creation
- ✅ Verify Test Execution using dynamic variables
