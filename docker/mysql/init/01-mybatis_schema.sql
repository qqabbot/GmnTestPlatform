-- MyBatis Migration SQL Schema
-- Generated for TestPlatform Database
-- Date: 2025-12-08

-- Environment Table
CREATE TABLE IF NOT EXISTS environment (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    env_name VARCHAR(255) NOT NULL UNIQUE,
    domain VARCHAR(512),
    description TEXT,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- Project Table
CREATE TABLE IF NOT EXISTS project (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    project_name VARCHAR(255) NOT NULL UNIQUE,
    description TEXT,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- Test Module Table
CREATE TABLE IF NOT EXISTS test_module (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    module_name VARCHAR(255) NOT NULL,
    project_id BIGINT,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (project_id) REFERENCES project(id) ON DELETE CASCADE
);

-- Test Case Table
CREATE TABLE IF NOT EXISTS test_case (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    module_id BIGINT NOT NULL,
    case_name VARCHAR(255) NOT NULL,
    method VARCHAR(10) NOT NULL,
    url VARCHAR(512) NOT NULL,
    headers TEXT,
    body TEXT,
    precondition VARCHAR(1024),
    setup_script TEXT,
    assertion_script TEXT NOT NULL,
    is_active BOOLEAN DEFAULT TRUE,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (module_id) REFERENCES test_module(id) ON DELETE CASCADE
);

-- Test Step Table
CREATE TABLE IF NOT EXISTS test_step (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    case_id BIGINT NOT NULL,
    step_order INT NOT NULL,
    step_name VARCHAR(255) NOT NULL,
    method VARCHAR(10) NOT NULL,
    url VARCHAR(1024) NOT NULL,
    headers TEXT,
    body TEXT,
    auth_type VARCHAR(50),
    auth_value VARCHAR(512),
    assertion_script TEXT,
    enabled BOOLEAN DEFAULT TRUE,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (case_id) REFERENCES test_case(id) ON DELETE CASCADE
);

-- Global Variable Table
CREATE TABLE IF NOT EXISTS global_variable (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    key_name VARCHAR(255) NOT NULL,
    value_content TEXT NOT NULL,
    type VARCHAR(50) DEFAULT 'normal',
    description TEXT,
    environment_id BIGINT,
    project_id BIGINT,
    module_id BIGINT,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (environment_id) REFERENCES environment(id) ON DELETE SET NULL,
    FOREIGN KEY (project_id) REFERENCES project(id) ON DELETE SET NULL,
    FOREIGN KEY (module_id) REFERENCES test_module(id) ON DELETE SET NULL
);

-- Test Step Template Table
CREATE TABLE IF NOT EXISTS test_step_template (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(255) NOT NULL,
    method VARCHAR(10) NOT NULL,
    url VARCHAR(1024) NOT NULL,
    headers TEXT,
    body TEXT,
    assertion_script TEXT,
    project_id BIGINT,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (project_id) REFERENCES project(id) ON DELETE SET NULL
);

-- Test Plan Table
CREATE TABLE IF NOT EXISTS test_plan (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    project_id BIGINT,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (project_id) REFERENCES project(id) ON DELETE SET NULL
);

-- Test Plan Cases (ManyToMany Join Table)
CREATE TABLE IF NOT EXISTS test_plan_cases (
    plan_id BIGINT NOT NULL,
    case_id BIGINT NOT NULL,
    case_order INT DEFAULT 0,
    PRIMARY KEY (plan_id, case_id),
    FOREIGN KEY (plan_id) REFERENCES test_plan(id) ON DELETE CASCADE,
    FOREIGN KEY (case_id) REFERENCES test_case(id) ON DELETE CASCADE
);

-- Scheduled Task Table
CREATE TABLE IF NOT EXISTS scheduled_task (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(255) NOT NULL,
    cron_expression VARCHAR(100) NOT NULL,
    plan_id BIGINT NOT NULL,
    env_key VARCHAR(100),
    status VARCHAR(20) DEFAULT 'ACTIVE',
    last_run_time DATETIME,
    next_run_time DATETIME,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (plan_id) REFERENCES test_plan(id) ON DELETE CASCADE
);

-- Test Execution Record Table
CREATE TABLE IF NOT EXISTS test_execution_record (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    project_id BIGINT,
    module_id BIGINT,
    case_id BIGINT,
    case_name VARCHAR(255),
    env_key VARCHAR(100),
    status VARCHAR(20),
    detail TEXT,
    duration BIGINT,
    executed_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Test Execution Log Table
CREATE TABLE IF NOT EXISTS test_execution_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    record_id BIGINT NOT NULL,
    step_name VARCHAR(255),
    request_url VARCHAR(1024),
    request_headers TEXT,
    request_body TEXT,
    response_status INT,
    response_headers TEXT,
    response_body TEXT,
    variable_snapshot TEXT,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (record_id) REFERENCES test_execution_record(id) ON DELETE CASCADE
);

-- Assertion Table
CREATE TABLE IF NOT EXISTS assertion (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    step_id BIGINT NOT NULL,
    type VARCHAR(50) NOT NULL,
    expression VARCHAR(512),
    expected_value TEXT,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (step_id) REFERENCES test_step(id) ON DELETE CASCADE
);

-- Extractor Table
CREATE TABLE IF NOT EXISTS extractor (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    step_id BIGINT NOT NULL,
    variable_name VARCHAR(255) NOT NULL,
    type VARCHAR(50) NOT NULL,
    expression VARCHAR(512) NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (step_id) REFERENCES test_step(id) ON DELETE CASCADE
);

-- Indexes for better performance
CREATE INDEX idx_test_module_project ON test_module(project_id);
CREATE INDEX idx_test_case_module ON test_case(module_id);
CREATE INDEX idx_test_step_case ON test_step(case_id);
CREATE INDEX idx_global_variable_env ON global_variable(environment_id);
CREATE INDEX idx_execution_record_case ON test_execution_record(case_id);
CREATE INDEX idx_execution_log_record ON test_execution_log(record_id);
