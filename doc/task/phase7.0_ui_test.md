# Phase 7.0 Task List: UI Test Function Construction

**Goal**: Implement an automated UI testing framework within the platform, allowing users to record/write and execute UI tests using Chromium/Playwright-like capabilities.

## Backend

### 1. Core Framework Integration (核心框架集成)
- ✅ **Playwright Java Integration**
  - ✅ Add Playwright dependency to `pom.xml`
  - ✅ Configure browser binaries download/management
  - ✅ Implement `UiTestRunner` service for browser lifecycle management
  - ✅ Support multiple browser types (Chromium, Firefox, WebKit)

- ✅ **Data Model Design**
  - ✅ Create `UiTestCase` entity (url, browserType, viewport, etc.)
  - ✅ Create `UiTestStep` entity (action, selector, value, etc.)
  - ✅ Implement MyBatis Mappers for persistence
  - ✅ Add support for `customHeaders` and `customCookies`

### 2. Execution Engine Enhancement (执行引擎增强)
- ✅ **Advanced Actions Support**
  - ✅ Implement basic actions (Navigate, Click, Fill)
  - ✅ Implement advanced actions (Hover, DragAndDrop, SelectOption, PressKey)
  - ✅ Implement assertions (AssertVisible, AssertText)
  - ✅ Implement smart waits (WaitForSelector, WaitForLoadState)

- ✅ **Reliability & Debugging**
  - ✅ Implement explicit video recording saving (`videos/` directory)
  - ✅ Fix 0-second video duration issue
  - ✅ Add configurable step execution delay (200ms)
  - ✅ Implement `autoDismissDialogs` to handle native alerts

- [x] **Conditional & Loop Steps (条件分支与循环)**
  - [x] **Model Extension**: Add `condition` field to `UiTestStep`
  - [x] **New Action Types**: Support `IF`, `ELSE`, `FOR`, `WHILE`
  - [x] **Runner Logic**: Implement logic to evaluate conditions (e.g., "If Element Visible") and control execution flow
  - [x] **Loop Support**: Handle index variables for loop iteration (e.g., clicking list items)

## Frontend

### 1. UI Test Editor (UI 测试用例编辑器)
- ✅ **Visual Editor Implementation**
  - ✅ Create `UiTestCaseEditor.vue` with Split View (Sidebar/Details)
  - ✅ Implement "Step List" with reordering capability
  - ✅ Add "Insert Step Below" action for easy editing
  - ✅ Fix "Cannot read properties of null" navigation crash (Monaco Editor disposal)

- ✅ **Smart Code Import (智能导入)**
  - ✅ Implement "Import from Playwright Code" dialog
  - ✅ Parse Java request chains into UI Test Steps
  - ✅ Support Regex-based parsing for `page.click`, `page.fill`, etc.

- [x] **Control Flow Visualization (流程控制可视化)**
  - [x] **Nested Steps Display**: Use Element Plus Collapse or Indentation to show nested blocks (If/For)
  - [x] **Condition Editor**: Add UI to configure conditions (e.g., "Selector Exists")
  - [ ] **Visual Block Grouping**: Distinct styling for control flow blocks

### 2. Reporting & Analysis (报告与分析)
- ✅ **Execution Reports**
  - ✅ Display execution status, duration, and logs
  - ✅ Show "Element Selector" in execution logs for debugging
  - ✅ Add "Test Case" column to report list
  - ✅ Color-code status tags (Success/Fail)

- ⏭️ **Real-time Preview**
  - ⏭️ Stream browser view to frontend via WebSocket (Future Enhancement)

## Verification

### 1. Basic Functionality
- ✅ Verify basic navigation and interaction flow
- ✅ Verify video recording persistence and playback
- ✅ Verify custom headers/cookies injection

### 2. Complex Scenarios
- [ ] Verify conditional logic (e.g., Login if not logged in)
- [ ] Verify loop interaction (e.g., Process multiple items in a table)

## Summary
**Status**: In Progress
**Priority**: High - Core capability for UI Automation
**Completion Date**: TBD

### Implementation Notes
- **Control Flow**: Prioritize `IF` (check visibility/text) and `FOR` (fixed count or selector count).
- **Structure**: Use a `parentId` or recursive structure in frontend, while keeping flat list with `depth` or `blockId` in backend for cleaner SQL storage if possible, or fully recursive steps.
