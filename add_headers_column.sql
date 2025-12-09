-- 为test_case表添加headers字段
ALTER TABLE test_case ADD COLUMN headers TEXT;

-- 更新现有记录，设置默认值为空JSON对象
UPDATE test_case SET headers = '{}' WHERE headers IS NULL;
