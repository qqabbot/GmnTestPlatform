-- Fix and Sample Data for TestPlatform Database
-- This script fixes schema issues and inserts sample data
-- Date: 2025-12-08

-- Step 1: Fix test_case table - add headers column if missing
SET @exist := (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS 
               WHERE TABLE_SCHEMA = 'TestPlatform' 
               AND TABLE_NAME = 'test_case' 
               AND COLUMN_NAME = 'headers');
               
SET @query := IF(@exist = 0, 
    'ALTER TABLE test_case ADD COLUMN headers TEXT AFTER url',
    'SELECT "headers column already exists"');
PREPARE stmt FROM @query;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Step 2: Clear existing test data (to avoid duplicates)
SET FOREIGN_KEY_CHECKS = 0;
TRUNCATE TABLE test_plan_cases;
TRUNCATE TABLE scheduled_task;
TRUNCATE TABLE test_execution_log;
TRUNCATE TABLE test_execution_record;
TRUNCATE TABLE assertion;
TRUNCATE TABLE extractor;
TRUNCATE TABLE test_step;
TRUNCATE TABLE test_case;
TRUNCATE TABLE global_variable;
TRUNCATE TABLE test_step_template;
TRUNCATE TABLE test_plan;
TRUNCATE TABLE test_module;
-- Keep environments and projects that already exist
SET FOREIGN_KEY_CHECKS = 1;

-- Step 3: Insert Modules (IDs will be 1-6)
INSERT INTO test_module (id, module_name, project_id, created_at, updated_at) VALUES
(1, '用户注册模块', 2, NOW(), NOW()),
(2, '用户登录模块', 2, NOW(), NOW()),
(3, '权限管理模块', 2, NOW(), NOW()),
(4, '订单创建模块', 3, NOW(), NOW()),
(5, '订单支付模块', 3, NOW(), NOW()),
(6, '订单查询模块', 3, NOW(), NOW());

-- Step 4: Insert Test Cases (IDs will be 1-6)
INSERT INTO test_case (id, module_id, case_name, method, url, headers, body, precondition, setup_script, assertion_script, is_active, created_at, updated_at) VALUES
(1, 1, '正常用户注册', 'POST', '${base_url}/api/users/register', '{"Content-Type": "application/json"}', '{"username": "testuser", "password": "Test123!", "email": "test@example.com"}', NULL, NULL, 'status == 200 && response.success == true', 1, NOW(), NOW()),
(2, 1, '重复用户名注册', 'POST', '${base_url}/api/users/register', '{"Content-Type": "application/json"}', '{"username": "existinguser", "password": "Test123!", "email": "test2@example.com"}', NULL, NULL, 'status == 400 && response.error != null', 1, NOW(), NOW()),
(3, 2, '正常用户登录', 'POST', '${base_url}/api/users/login', '{"Content-Type": "application/json"}', '{"username": "testuser", "password": "Test123!"}', NULL, NULL, 'status == 200 && response.token != null', 1, NOW(), NOW()),
(4, 2, '错误密码登录', 'POST', '${base_url}/api/users/login', '{"Content-Type": "application/json"}', '{"username": "testuser", "password": "wrongpassword"}', NULL, NULL, 'status == 401', 1, NOW(), NOW()),
(5, 4, '创建订单', 'POST', '${base_url}/api/orders', '{"Content-Type": "application/json", "Authorization": "Bearer ${token}"}', '{"productId": 1, "quantity": 2}', '用户已登录', NULL, 'status == 201 && response.orderId != null', 1, NOW(), NOW()),
(6, 5, '支付订单', 'POST', '${base_url}/api/orders/${orderId}/pay', '{"Content-Type": "application/json", "Authorization": "Bearer ${token}"}', '{"paymentMethod": "alipay"}', '订单已创建', NULL, 'status == 200 && response.paymentStatus == "success"', 1, NOW(), NOW());

-- Step 5: Insert Test Steps (using correct case_ids)
INSERT INTO test_step (case_id, step_order, step_name, method, url, headers, body, auth_type, auth_value, assertion_script, enabled, created_at, updated_at) VALUES
(1, 1, '发送注册请求', 'POST', '${base_url}/api/users/register', '{"Content-Type": "application/json"}', '{"username": "testuser", "password": "Test123!"}', 'NONE', NULL, 'status == 200', 1, NOW(), NOW()),
(1, 2, '验证响应内容', 'GET', '${base_url}/api/users/testuser', '{"Content-Type": "application/json"}', NULL, 'NONE', NULL, 'status == 200 && response.username == "testuser"', 1, NOW(), NOW()),
(3, 1, '登录获取Token', 'POST', '${base_url}/api/users/login', '{"Content-Type": "application/json"}', '{"username": "testuser", "password": "Test123!"}', 'NONE', NULL, 'status == 200', 1, NOW(), NOW()),
(3, 2, '验证Token有效', 'GET', '${base_url}/api/users/me', '{"Content-Type": "application/json", "Authorization": "Bearer ${token}"}', NULL, 'BEARER', '${token}', 'status == 200', 1, NOW(), NOW());

-- Step 6: Insert Global Variables
INSERT INTO global_variable (key_name, value_content, type, description, environment_id, project_id, module_id, created_at, updated_at) VALUES
('base_url', 'http://localhost:8080', 'normal', '本地Mock环境URL', 1, NULL, NULL, NOW(), NOW()),
('base_url', 'http://dev.api.example.com', 'normal', '开发环境基础URL', 2, NULL, NULL, NOW(), NOW()),
('base_url', 'http://staging.api.example.com', 'normal', '预发布环境基础URL', 3, NULL, NULL, NOW(), NOW()),
('test_username', 'testuser', 'normal', '测试用户名', NULL, 2, NULL, NOW(), NOW()),
('test_password', 'Test123!', 'secret', '测试密码', NULL, 2, NULL, NOW(), NOW());

-- Step 7: Insert Test Step Templates
INSERT INTO test_step_template (name, method, url, headers, body, assertion_script, project_id, created_at, updated_at) VALUES
('登录请求模板', 'POST', '${base_url}/api/users/login', '{"Content-Type": "application/json"}', '{"username": "${username}", "password": "${password}"}', 'status == 200 && response.token != null', 2, NOW(), NOW()),
('GET请求模板', 'GET', '${base_url}${path}', '{"Content-Type": "application/json", "Authorization": "Bearer ${token}"}', NULL, 'status == 200', NULL, NOW(), NOW());

-- Step 8: Insert Test Plans
INSERT INTO test_plan (id, name, description, project_id, created_at, updated_at) VALUES
(1, '用户管理回归测试', '包含用户注册、登录、权限管理的完整回归测试', 2, NOW(), NOW()),
(2, '订单流程测试', '包含订单创建、支付、查询的端到端测试', 3, NOW(), NOW());

-- Step 9: Insert Test Plan Cases (关联测试计划和测试用例)
INSERT INTO test_plan_cases (plan_id, case_id, case_order) VALUES
(1, 1, 1),
(1, 2, 2),
(1, 3, 3),
(1, 4, 4),
(2, 5, 1),
(2, 6, 2);

-- Step 10: Insert Scheduled Tasks
INSERT INTO scheduled_task (name, cron_expression, plan_id, env_key, status, created_at) VALUES
('每日回归测试', '0 0 2 * * ?', 1, 'Development', 'ACTIVE', NOW()),
('每周完整测试', '0 0 3 ? * MON', 2, 'Staging', 'ACTIVE', NOW());

-- Step 11: Verify data
SELECT 'Data Summary:' AS info;
SELECT 'Environments' AS entity, COUNT(*) AS count FROM environment
UNION ALL SELECT 'Projects', COUNT(*) FROM project
UNION ALL SELECT 'Modules', COUNT(*) FROM test_module
UNION ALL SELECT 'Test Cases', COUNT(*) FROM test_case
UNION ALL SELECT 'Test Steps', COUNT(*) FROM test_step
UNION ALL SELECT 'Global Variables', COUNT(*) FROM global_variable
UNION ALL SELECT 'Test Plans', COUNT(*) FROM test_plan
UNION ALL SELECT 'Scheduled Tasks', COUNT(*) FROM scheduled_task;

SELECT '✅ Sample data inserted successfully!' AS result;
