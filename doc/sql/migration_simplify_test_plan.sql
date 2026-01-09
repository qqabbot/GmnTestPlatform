-- Migration: Remove complex override fields, keep only parameter_overrides
-- Purpose: Simplify Test Plan to focus on case chaining and parameter passing
-- Date: 2026-01-09
-- Note: This will drop data in override columns. User will regenerate data.

USE TestPlatform;

-- Remove unnecessary override columns (one by one for MySQL compatibility)
ALTER TABLE test_plan_cases DROP COLUMN case_name_override;
ALTER TABLE test_plan_cases DROP COLUMN url_override;
ALTER TABLE test_plan_cases DROP COLUMN method_override;
ALTER TABLE test_plan_cases DROP COLUMN headers_override;
ALTER TABLE test_plan_cases DROP COLUMN body_override;
ALTER TABLE test_plan_cases DROP COLUMN assertion_script_override;
ALTER TABLE test_plan_cases DROP COLUMN steps_override;

-- Add index for faster queries
CREATE INDEX idx_test_plan_cases_order ON test_plan_cases(plan_id, case_order);

-- Verify final structure
DESCRIBE test_plan_cases;

-- Expected columns after migration:
-- - plan_id (BIGINT)
-- - case_id (BIGINT)  
-- - case_order (INT)
-- - parameter_overrides (TEXT) - JSON for parameter mapping
-- - enabled (BOOLEAN) - Enable/disable case in plan
