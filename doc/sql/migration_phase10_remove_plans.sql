-- Phase 10: Remove Test Plan support from Scheduled Tasks

-- 1. Remove plan_id and the foreign key associated with it
-- Note: Depending on the DB state, we might need to drop the FK constraint first if it has a specific name
-- Standard approach to drop column which also drops simple associated indexes
ALTER TABLE scheduled_task DROP FOREIGN KEY scheduled_task_ibfk_1;
ALTER TABLE scheduled_task DROP COLUMN plan_id;

-- 2. Ensure scenario_id is not null for future tasks (optional, based on requirement)
-- ALTER TABLE scheduled_task MODIFY COLUMN scenario_id BIGINT NOT NULL;
