-- Add reference_case_id column to test_step table for Phase 5.1
-- This allows steps to reference other test cases

ALTER TABLE test_step ADD COLUMN reference_case_id BIGINT NULL;
ALTER TABLE test_step ADD CONSTRAINT fk_step_reference_case FOREIGN KEY (reference_case_id) REFERENCES test_case(id) ON DELETE SET NULL;

