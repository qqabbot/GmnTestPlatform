-- Migration: Allow duplicate cases in test plan
-- Purpose: Enable adding the same test case multiple times in a test plan with different parameter overrides
-- Author: System
-- Date: 2025-12-26
-- 
-- IMPORTANT: Execute this script step by step, or check each step before proceeding
-- If any step fails, check the error message and adjust accordingly

-- Step 1: Check current table structure
-- Run this first to see the current state:
-- DESCRIBE test_plan_cases;
-- SHOW CREATE TABLE test_plan_cases;

-- Step 2: Add an auto-increment ID column
-- This will add the id column as the first column
ALTER TABLE test_plan_cases 
ADD COLUMN id BIGINT AUTO_INCREMENT FIRST;

-- Step 3: Remove the old composite primary key (plan_id, case_id)
-- This step may fail if the primary key structure is different
ALTER TABLE test_plan_cases 
DROP PRIMARY KEY;

-- Step 4: Add the new primary key on id
ALTER TABLE test_plan_cases 
ADD PRIMARY KEY (id);

-- Step 5: Add unique index to prevent exact duplicates (same plan, case, and order)
-- This allows the same case to be added multiple times with different case_order values
-- Each instance can have different parameter_overrides and other overrides
CREATE UNIQUE INDEX idx_plan_case_order ON test_plan_cases(plan_id, case_id, case_order);

-- Step 6: Verify the migration
-- Run these to verify:
-- DESCRIBE test_plan_cases;
-- SHOW INDEX FROM test_plan_cases;

-- Note: After migration, the same case can be added multiple times to a plan
-- Each instance must have a different case_order value
-- Each instance can have different parameter_overrides and other overrides

