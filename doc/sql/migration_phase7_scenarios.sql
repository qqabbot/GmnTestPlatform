-- Phase 7.1: Test Scenario Foundation Schema (Optimized)

-- 1. Create test_scenarios table
-- Replaces old test_plan concept with a more robust entity
CREATE TABLE IF NOT EXISTS test_scenarios (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    project_id BIGINT NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    -- Foreign Key to Project
    CONSTRAINT fk_scenario_project FOREIGN KEY (project_id) REFERENCES project(id) ON DELETE CASCADE,
    
    -- Index for faster project-level lookups
    INDEX idx_scenario_project (project_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 2. Create test_scenario_steps table (Tree structure)
-- Stores the hierarchical execution flow (cases, groups, loops, logic)
CREATE TABLE IF NOT EXISTS test_scenario_steps (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    scenario_id BIGINT NOT NULL,
    parent_id BIGINT,             -- For nesting (null for root steps)
    type VARCHAR(50) NOT NULL,    -- 'CASE', 'GROUP', 'LOOP', 'IF', 'WAIT', 'SCRIPT'
    name VARCHAR(255),
    
    -- Reference to Base Case (only used when type='CASE')
    reference_case_id BIGINT,
    
    -- Configuration (JSON)
    -- Stores Loop Count, If Condition, Wait Time, etc.
    control_logic TEXT,
    
    -- Data Overrides (JSON)
    -- Stores specific Param/Body/Header overrides for this step
    data_overrides TEXT,
    
    order_index INT DEFAULT 0,
    
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    -- Foreign Keys
    CONSTRAINT fk_step_scenario FOREIGN KEY (scenario_id) REFERENCES test_scenarios(id) ON DELETE CASCADE,
    CONSTRAINT fk_step_parent FOREIGN KEY (parent_id) REFERENCES test_scenario_steps(id) ON DELETE CASCADE,
    
    -- Indexes for performance (critical for tree traversal and retrieval)
    INDEX idx_step_scenario (scenario_id),
    INDEX idx_step_parent (parent_id),
    INDEX idx_step_order (order_index)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
