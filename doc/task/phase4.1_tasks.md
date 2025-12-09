# Phase 4.1: Test Plans & Suites

**Goal**: Implement "Test Plans" to allow users to group multiple Test Cases into a suite and execute them sequentially or in parallel.

## Backend
- ✅ **Data Model**
  - ✅ Create `TestPlan` entity (id, name, description, projectId, status, createdAt, updatedAt).
  - ✅ Implement Many-to-Many relationship between `TestPlan` and `TestCase` (Ordered).
  - ✅ Create `TestPlanRepository`.
- ✅ **API Implementation**
  - ✅ Create `TestPlanService` (CRUD + Execution Logic).
  - ✅ Create `TestPlanController` (REST API).
  - ✅ Implement `executePlan(planId, envKey)`: Iterate cases and aggregate results.

## Frontend
- ✅ **Test Plan Management**
  - ✅ Create `TestPlanList.vue` (List View).
  - ✅ Create `TestPlanEdit.vue` (or Dialog) to manage simple properties.
- ✅ **Case Association UI**
  - ✅ Interface to "Add Cases to Plan" (Select items from a list of available cases).
  - ✅ Reordering support (Drag & Drop or Up/Down buttons) for cases in a plan.
- ✅ **Execution UI**
  - ✅ "Run Plan" button in Plan List/Detail.
  - ✅ Display aggregate execution report (Pass/Fail count, detailed log link).

## Verification
- ✅ **Automated Test**
  - ✅ Verify Plan CRUD.
  - ✅ Verify Plan Execution runs all associated cases.
