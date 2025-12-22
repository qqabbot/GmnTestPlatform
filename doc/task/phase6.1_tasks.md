# Phase 6.1 - Advanced Features & AI Integration

This phase focuses on enhancing the platform's core capabilities with data separation for test plans and introducing intelligent automation through AI.

## 1. Test Plan Data Separation (Parameter Overrides)
- **Goal**: Allow Test Plans to store specific parameter overrides for each referenced test case without modifying the original Test Case.
- **Tasks**:
    - [x] Add `parameter_overrides` column to `test_plan_cases` table.
    - [x] Update `TestCase` model with transient `parameterOverrides` field.
    - [x] Update `TestPlanMapper` to read/write `parameter_overrides`.
    - [x] Update `TestPlanService` to merge overrides during execution.
    - [x] Frontend: Add "Params" dialog in Test Plan editor to manage overrides.
- **Status**: ✅ Completed

## 2. AI Intelligence "Power Ups"
- **Goal**: Accelerate test creation and analysis using Google Gemini.
- **Completed Features**:
    - [x] **AI Test Case Generation**: Batch generate scenarios from API metadata.
    - [x] **AI Failure Diagnosis**: One-click analysis of failed test logs in reports.
    - [x] **AI Smart Assertions**: Suggest Groovy assertions from dry run results.
    - [x] **AI Smart Data Mocking**: Generate realistic JSON request bodies.
- **Backend**: Implemented `AiService`, `AiTaskService`, and `AiController`.
- **Frontend**: Integrated "Magic Stick" buttons across Editor, Step Details, and Reports.
- **Status**: ✅ Completed (Advanced AI Workflow)

## 3. Future AI Enhancements (Phase 6.2+)
- [ ] **AI Semantic Import**: Group imported APIs into logical modules using AI.
- [ ] **AI Scenario Orchestration**: Chain multiple APIs into a logical business flow.

---
**Last Updated**: 2025-12-19
**Status**: 🚀 Main features deployed. Ready for feedback.
