-- Migration: Add override columns to test_plan_cases table
-- Purpose: Enable Test Plan to override Test Case properties without modifying the original
-- Author: System
-- Date: 2025-12-26

-- Add override columns for Test Case fields
ALTER TABLE test_plan_cases
ADD COLUMN case_name_override VARCHAR(255) DEFAULT NULL COMMENT 'Override for case name in this plan',
ADD COLUMN url_override VARCHAR(1024) DEFAULT NULL COMMENT 'Override for URL in this plan',
ADD COLUMN method_override VARCHAR(10) DEFAULT NULL COMMENT 'Override for HTTP method',
ADD COLUMN headers_override TEXT DEFAULT NULL COMMENT 'Override for headers JSON',
ADD COLUMN body_override TEXT DEFAULT NULL COMMENT 'Override for request body',
ADD COLUMN assertion_script_override TEXT DEFAULT NULL COMMENT 'Override for assertion script',
ADD COLUMN steps_override TEXT DEFAULT NULL COMMENT 'JSON array of step-level overrides: [{step_id, field_overrides}]',
ADD COLUMN enabled BOOLEAN DEFAULT TRUE COMMENT 'Enable/disable this case in the plan';

-- Add index for faster queries
CREATE INDEX idx_test_plan_cases_enabled ON test_plan_cases(enabled);

-- Backward compatibility: All existing records will have NULL overrides = use original values
-- No data migration needed
