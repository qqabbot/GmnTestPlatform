-- Migration: Restore assertion_script_override field for Test Plan
-- Purpose: Allow overriding logic (assertions and extractors) within a Plan without touching original Case
-- Date: 2026-01-09

USE TestPlatform;

-- Add back the assertion_script_override column
ALTER TABLE test_plan_cases 
ADD COLUMN assertion_script_override TEXT AFTER parameter_overrides;

-- Verify final structure
DESCRIBE test_plan_cases;
