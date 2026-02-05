-- Phase 9: Associate Scheduled Tasks with Test Scenarios

-- 1. Add scenario_id to scheduled_task
-- 2. Modify plan_id to be nullable
ALTER TABLE scheduled_task 
ADD COLUMN scenario_id BIGINT AFTER plan_id,
MODIFY COLUMN plan_id BIGINT NULL;

-- 3. Add Foreign Key for scenario_id
ALTER TABLE scheduled_task
ADD CONSTRAINT fk_task_scenario FOREIGN KEY (scenario_id) REFERENCES test_scenarios(id) ON DELETE CASCADE;

-- 4. Add index for performance
CREATE INDEX idx_task_scenario ON scheduled_task(scenario_id);
