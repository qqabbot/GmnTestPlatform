-- UI Testing Schema
-- Decoupled from API Testing tables

-- UI Test Case Table
CREATE TABLE IF NOT EXISTS ui_test_case (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    project_id BIGINT,
    module_id BIGINT,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    start_url VARCHAR(1024),
    browser_type VARCHAR(20) DEFAULT 'chromium',
    headless BOOLEAN DEFAULT TRUE,
    viewport_width INT DEFAULT 1280,
    viewport_height INT DEFAULT 720,
    custom_headers TEXT,  -- JSON format: {"x-token": "xxx", "Authorization": "Bearer xxx"}
    custom_cookies TEXT,  -- JSON format: [{"name": "foo", "value": "bar"}]
    auto_dismiss_dialogs BOOLEAN DEFAULT FALSE,  -- Automatically dismiss alert/confirm/prompt
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (project_id) REFERENCES project(id) ON DELETE SET NULL,
    FOREIGN KEY (module_id) REFERENCES test_module(id) ON DELETE SET NULL
);

-- UI Test Step Table
CREATE TABLE IF NOT EXISTS ui_test_step (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    case_id BIGINT NOT NULL,
    step_order INT NOT NULL,
    action_type VARCHAR(50) NOT NULL, -- NAVIGATE, CLICK, FILL, HOVER, etc.
    selector VARCHAR(512),
    value TEXT,
    wait_condition VARCHAR(100),
    parent_id BIGINT DEFAULT NULL, -- For nesting steps (IF/LOOP)
    condition_expression TEXT,  -- For IF actions: Logic expression or selector
    loop_source TEXT,           -- For LOOP actions: Selector or logic
    screenshot_on_failure BOOLEAN DEFAULT TRUE,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (case_id) REFERENCES ui_test_case(id) ON DELETE CASCADE
    -- FOREIGN KEY (parent_id) REFERENCES ui_test_step(id) ON DELETE CASCADE -- Optional: Enforce referential integrity
);

-- UI Test Execution Record Table
CREATE TABLE IF NOT EXISTS ui_test_execution_record (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    project_id BIGINT,
    case_id BIGINT NOT NULL,
    status VARCHAR(20), -- SUCCESS, FAILURE, RUNNING
    video_path VARCHAR(1024),
    duration BIGINT,
    error_message TEXT,
    executed_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (case_id) REFERENCES ui_test_case(id) ON DELETE CASCADE
);

-- UI Test Execution Log Table
CREATE TABLE IF NOT EXISTS ui_test_execution_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    record_id BIGINT NOT NULL,
    step_id BIGINT,
    step_name VARCHAR(255),
    action VARCHAR(50),
    selector VARCHAR(512),  -- Element selector that was interacted with
    status VARCHAR(20),
    screenshot_path VARCHAR(1024),
    error_detail TEXT,
    executed_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (record_id) REFERENCES ui_test_execution_record(id) ON DELETE CASCADE
);

CREATE INDEX idx_ui_test_case_project ON ui_test_case(project_id);
CREATE INDEX idx_ui_test_step_case ON ui_test_step(case_id);
CREATE INDEX idx_ui_test_record_case ON ui_test_execution_record(case_id);
