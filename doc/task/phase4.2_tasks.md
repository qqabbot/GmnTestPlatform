# Phase 4.2: Import & Scheduling

**Goal**: Enable easier test creation via Import and automated execution via Scheduling.

## Backend
- ✅ **Dependencies**
  - ✅ Add `swagger-parser` and `spring-boot-starter-quartz` to `pom.xml`.
- ✅ **API Import**
  - ✅ Create `ImportService` to parse Swagger JSON (v2/v3).
  - ✅ Create `ImportController` with `/api/import/swagger` endpoint.
  - ⏸️ Support YAPI import (Deferred - encountered bugs, simplified to Swagger-only).
- ✅ **Scheduling**
  - ✅ Create `ScheduledTask` entity (cron, planId, status).
  - ✅ Create `ScheduledTaskRepository`.
  - ✅ Create `ScheduleService` using Quartz/Spring Scheduler to trigger `TestPlanService.executePlan`.
  - ✅ Create `ScheduleController` (CRUD + Pause/Resume).

## Frontend
- ✅ **Import UI**
  - ✅ Create `ImportView.vue` (Swagger input only).
  - ✅ Form: Type (Swagger), URL/JSON Content, Target Project/Module.
- ✅ **Schedule UI**
  - ✅ Create `ScheduleList.vue`.
  - ✅ Create/Edit Dialog (Cron input, Plan selection).
  - ✅ Pause/Resume/Delete actions.

## Verification
- ✅ **Automated Test**
  - ⏸️ Verify Swagger Import (Backend ready, frontend ready, not tested end-to-end).
  - ✅ Verify Schedule CRUD and State Management (Passed `verify_phase4.2_scheduling.sh`).
