-- Phase 8.4: Scenario Execution History Tables
-- This migration adds tables to track scenario execution history and detailed step logs

-- Main execution record table
CREATE TABLE IF NOT EXISTS scenario_execution_records (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    scenario_id BIGINT NOT NULL,
    scenario_name VARCHAR(255),
    env_key VARCHAR(50),
    status VARCHAR(20) NOT NULL, -- PASS/FAIL/PARTIAL/RUNNING
    total_steps INT DEFAULT 0,
    passed_steps INT DEFAULT 0,
    failed_steps INT DEFAULT 0,
    duration_ms BIGINT,
    started_at DATETIME NOT NULL,
    completed_at DATETIME,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    
    FOREIGN KEY (scenario_id) REFERENCES test_scenarios(id) ON DELETE CASCADE,
    INDEX idx_scenario_id (scenario_id),
    INDEX idx_started_at (started_at),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Scenario execution history records';

-- Detailed step execution logs
CREATE TABLE IF NOT EXISTS scenario_step_execution_logs (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    record_id BIGINT NOT NULL,
    step_id BIGINT,
    step_name VARCHAR(255),
    step_type VARCHAR(50), -- CASE/LOOP/IF/GROUP/WAIT
    status VARCHAR(20) NOT NULL, -- PASS/FAIL/SKIP
    request_url VARCHAR(500),
    request_method VARCHAR(10),
    request_headers TEXT,
    request_body TEXT,
    response_code INT,
    response_headers TEXT,
    response_body TEXT,
    duration_ms BIGINT,
    error_message TEXT,
    executed_at DATETIME NOT NULL,
    
    FOREIGN KEY (record_id) REFERENCES scenario_execution_records(id) ON DELETE CASCADE,
    INDEX idx_record_id (record_id),
    INDEX idx_step_id (step_id),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Detailed step execution logs';

-- Add index for performance on large datasets
CREATE INDEX idx_scenario_env_date ON scenario_execution_records(scenario_id, env_key, started_at DESC);
