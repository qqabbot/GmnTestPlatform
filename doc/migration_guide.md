# 数据库迁移指南：允许 Test Plan 中重复添加 Case

## 迁移目的

允许在同一个 Test Plan 中添加多次同一条 Test Case，每次可以配置不同的参数覆盖（parameter overrides）。

## 迁移文件

- SQL 脚本：`doc/sql/migration_allow_duplicate_cases.sql`
- 执行脚本：`run_migration.sh`

## 执行方式

### 方式 1：使用 MySQL 客户端命令行（推荐）

```bash
mysql -h 10.48.0.13 -u insp_dev -p'insp_dev@123' TestPlatform < doc/sql/migration_allow_duplicate_cases.sql
```

### 方式 2：使用执行脚本

```bash
chmod +x run_migration.sh
./run_migration.sh
```

### 方式 3：使用 MySQL Workbench 或其他数据库工具

1. 连接到数据库：
   - Host: `10.48.0.13`
   - Port: `3306`
   - Database: `TestPlatform`
   - Username: `insp_dev`
   - Password: `insp_dev@123`

2. 打开并执行 `doc/sql/migration_allow_duplicate_cases.sql` 文件中的所有 SQL 语句

### 方式 4：手动执行（逐步执行）

如果遇到错误，可以逐步执行每个 SQL 语句：

```sql
-- Step 1: 检查当前表结构
DESCRIBE test_plan_cases;
SHOW CREATE TABLE test_plan_cases;

-- Step 2: 添加自增 ID 列
ALTER TABLE test_plan_cases 
ADD COLUMN id BIGINT AUTO_INCREMENT FIRST;

-- Step 3: 删除旧的复合主键
ALTER TABLE test_plan_cases 
DROP PRIMARY KEY;

-- Step 4: 添加新的主键
ALTER TABLE test_plan_cases 
ADD PRIMARY KEY (id);

-- Step 5: 添加唯一索引（允许相同 case 但不同 order）
CREATE UNIQUE INDEX idx_plan_case_order ON test_plan_cases(plan_id, case_id, case_order);

-- Step 6: 验证迁移结果
DESCRIBE test_plan_cases;
SHOW INDEX FROM test_plan_cases;
```

## 迁移后的变化

### 之前
- 主键：`(plan_id, case_id)`
- 限制：同一个 plan 中不能添加相同的 case 两次

### 之后
- 主键：`id` (自增)
- 唯一索引：`(plan_id, case_id, case_order)`
- 允许：同一个 plan 中可以添加相同的 case 多次，只要 `case_order` 不同

## 验证迁移

执行以下 SQL 验证迁移是否成功：

```sql
-- 检查表结构
DESCRIBE test_plan_cases;

-- 应该看到 id 列作为第一个列，类型为 BIGINT，有 AUTO_INCREMENT

-- 检查索引
SHOW INDEX FROM test_plan_cases;

-- 应该看到：
-- 1. PRIMARY KEY 在 id 列上
-- 2. idx_plan_case_order 唯一索引在 (plan_id, case_id, case_order) 上
```

## 回滚（如果需要）

如果需要回滚迁移，执行以下 SQL：

```sql
-- 删除新添加的索引
DROP INDEX idx_plan_case_order ON test_plan_cases;

-- 删除 id 列
ALTER TABLE test_plan_cases DROP COLUMN id;

-- 恢复复合主键
ALTER TABLE test_plan_cases ADD PRIMARY KEY (plan_id, case_id);
```

## 注意事项

1. **备份数据**：在执行迁移前，建议备份 `test_plan_cases` 表
2. **测试环境**：建议先在测试环境执行，验证无误后再在生产环境执行
3. **执行时间**：迁移操作通常很快，但如果表数据量大，可能需要几秒钟
4. **兼容性**：迁移后，现有的 Test Plan 数据不受影响，可以正常使用

## 常见问题

### Q: 如果 id 列已经存在怎么办？
A: 如果 id 列已经存在，可以跳过 Step 2，直接执行后续步骤。

### Q: 如果删除主键失败怎么办？
A: 检查当前主键结构，可能需要先删除相关的外键约束。

### Q: 迁移后前端需要修改吗？
A: 不需要。前端代码已经支持重复添加 case，迁移只是解除数据库层面的限制。

## 技术支持

如果遇到问题，请检查：
1. 数据库连接是否正常
2. 用户权限是否足够（需要 ALTER TABLE 权限）
3. 表是否被其他进程锁定

