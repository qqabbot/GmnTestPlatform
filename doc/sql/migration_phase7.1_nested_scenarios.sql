-- Phase 7.1: Add nested scenario reference support
-- Allows scenarios to reference other scenarios for reuse

ALTER TABLE test_scenario_steps 
ADD COLUMN reference_scenario_id BIGINT NULL COMMENT 'Reference to another scenario (for nested scenarios)',
ADD CONSTRAINT fk_step_scenario_ref FOREIGN KEY (reference_scenario_id) REFERENCES test_scenarios(id) ON DELETE SET NULL;

-- Add index for faster lookups
CREATE INDEX idx_step_scenario_ref ON test_scenario_steps(reference_scenario_id);
