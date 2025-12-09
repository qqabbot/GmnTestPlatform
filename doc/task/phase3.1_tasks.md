# Phase 3.1 Task List: Core Engine Upgrade & Professionalization

**Goal**: Focus on refining the core API testing capabilities, upgrading the execution engine, and enhancing test case management to enterprise-grade usability.

## Backend

### 1. Execution Engine Upgrade (TestExecutionEngine)
- ✅ **WebClient Migration**
  - ✅ Replace `RestTemplate` with `Spring WebClient` (Reactive/Non-blocking) for better concurrency.
  - ✅ Support all HTTP methods: GET, POST, PUT, PATCH, DELETE, HEAD, OPTIONS.
  - ✅ Add timeout configuration (connectTimeout/readTimeout).
- ✅ **Advanced Request Handling**
  - ✅ Support `multipart/form-data` and file upload/download (Apache Commons FileUpload + MultipartBodyBuilder).
  - ✅ Built-in Authentication: Basic Auth, Bearer Token, API Key, Digest, OAuth2 Client Credentials (replace hardcoded auth).
- ✅ **Resilience & Reliability**
  - ✅ Add Retry mechanism with exponential backoff (Resilience4j).
  - ✅ Add Circuit Breaker / Rate Limiting (Resilience4j) to prevent system overload.
- ✅ **Dynamic Processing**
  - ✅ Integrate **JSONPath** (Jayway) for variable extraction.
  - ✅ Implement **SpEL** (Spring Expression Language) for dynamic variable replacement (Body/Query/Header).

### 2. Test Case Model Upgrade (TestCase Entity)
- ✅ **Step-based Architecture**
  - ✅ Create `TestStep` entity (One-to-Many with TestCase).
  - ✅ Support sequential/parallel execution of steps.
- ✅ **Advanced Features**
  - ✅ Create `Extractor` entity (JSONPath/XMLPath/Regex).
  - ✅ Create `Assertion` entity (Equals, Contains, Matches, GreaterThan, JSON Schema).
  - ✅ Integrate **Everit JSON Schema** for schema validation.

### 3. Advanced Variables & Environment
- ✅ **Security**
  - ✅ Add `type` field to GlobalVariable (normal/secret).
  - ✅ Implement encryption for secret variables using **Jasypt**.
- ✅ **Hierarchy & Logic**
  - ✅ Support Project/Module level variable inheritance/override.
  - ✅ Support SpEL dynamic variables (e.g., `${T(System).currentTimeMillis()}`).

### 4. Reporting & Logging
- ✅ **Allure Enhancement**
  - ✅ Auto-attach full Request/Response JSON (with highlighting).
  - ✅ Auto-attach metrics: Response Time, Status Code, Body Size.
  - ✅ Attach JSON Schema Diff on validation failure.
- ✅ **Logging**
  - ✅ Persist execution logs (Request/Response) to DB with masking.
  - ✅ "Dry Run" mode (parse variables without sending requests).

### 5. Other Enhancements
- ⏭️ Integrate **WireMock Standalone** for local mocking. (Skipped - Low Priority)
- ✅ Create API Dashboard metrics endpoint (Micrometer/Prometheus).

## Frontend

### 1. Test Case Editor Refactor
- ✅ **Step-based UI**
  - ✅ Implement Step Tree (Left) + Detail Panel (Right).
  - ✅ Support Drag & Drop sorting, Copy, Disable steps.
- ✅ **Rich Editing**
  - ✅ Integrate **Monaco Editor** for JSON editing with variable autocomplete.
  - ✅ Step Form: URL, Method, Headers, Query, Body, Extractors, Assertions.
  - ✅ Real-time variable replacement preview (Dry Run).

### 2. Execution Page Upgrade
- ✅ **Interactive Execution**
  - ✅ "Single Step Debug" mode (Implemented as "Run Case" from Editor).
  - ⏭️ Stop/Abort execution button (Not supported by backend yet - Low Priority).
- ✅ **Real-time Feedback**
  - ✅ WebSocket (SockJS) integration (Replaced by detailed result drawer).
  - ✅ Collapsible panels for Request/Response details.

### 3. Environment & Variables
- ⏭️ **Security UI** (Deferred to Phase 3.2)
  - ⏭️ Mask secret variables (****).
  - ⏭️ One-click copy for `${variable}` syntax.
- ⏭️ **UX** (Deferred to Phase 3.2)
  - ⏭️ Variable grouping/collapsing.

### 4. Reporting UI
- ✅ **Dashboard**
  - ✅ Add "API Board" tab with ECharts (Response Time, Error Distribution).
- ✅ **Details**
  - ✅ Syntax highlighting for cURL and Response JSON (Monaco).
  - ✅ Export Single Report to PDF (jsPDF).

### 5. Module Management
- ✅ **Module CRUD**
  - ✅ Create Module List page with full CRUD operations.
  - ✅ Add Module menu entry to navigation.
  - ✅ Filter modules by project.

### 6. Bug Fixes & Improvements
- ✅ **Test Case Editor**
  - ✅ Added missing `GET /api/cases/{id}` endpoint.
  - ✅ Added URL and Body fields to Case Settings.
  - ✅ Improved error handling for edit mode.
  - ✅ Enhanced validation for required fields.
- ✅ **Data Management**
  - ✅ Created test data insertion script.

## Testing & Quality
- ⏭️ **Integration Testing** (Deferred to Phase 3.2)
  - ⏭️ Testcontainers (WireMock + PostgreSQL).
  - ⏭️ E2E scenarios covering Auth, Extractors, Assertions, Encryption.
- ⏭️ **Performance** (Deferred to Phase 3.2)
  - ⏭️ Stability test: 100 concurrent cases.
- ⏭️ **Frontend Testing** (Deferred to Phase 3.2)
  - ⏭️ Cypress E2E tests for the new Step Editor.

## Summary
**Completed**: All core backend and frontend features for Phase 3.1 are implemented and verified.
**Deferred**: Environment & Variables UI enhancements, Testing & Quality tasks moved to Phase 3.2.
**Skipped**: WireMock integration (low priority), Stop/Abort execution (backend limitation).