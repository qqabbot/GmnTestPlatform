# Test Plan 数据库迁移指南

## 迁移目的

简化 Test Plan 功能,从复杂的"用例覆盖"模式转变为简单的"参数传递"模式。

## 背景

当前系统在 `test_plan_cases` 表中存储了 7 个覆盖字段,试图在测试计划中"覆盖"用例的所有属性:
- `case_name_override`
- `url_override`
- `method_override`
- `headers_override`
- `body_override`
- `assertion_script_override`
- `steps_override`

这导致了UI复杂、概念混乱、用户困惑。

## 新设计理念

**Test Plan = Cases 的顺序执行链 + 参数传递**

只需要一个字段:`parameter_overrides` (JSON),用于配置 Case 间的参数传递。

---

## 迁移步骤

### 1. 备份数据 (可选但推荐)

```sql
-- 备份整个表
CREATE TABLE test_plan_cases_backup_20260109 AS 
SELECT * FROM test_plan_cases;

-- 或只导出数据
mysqldump -u root -p TestPlatform test_plan_cases > test_plan_cases_backup_20260109.sql
```

### 2. 执行迁移脚本

**文件位置**: `/Users/xhb/IdeaProjects/GmnTestPlatform/doc/sql/migration_simplify_test_plan.sql`

```sql
-- 删除不必要的覆盖字段
ALTER TABLE test_plan_cases
DROP COLUMN IF EXISTS case_name_override,
DROP COLUMN IF EXISTS url_override,
DROP COLUMN IF EXISTS method_override,
DROP COLUMN IF EXISTS headers_override,
DROP COLUMN IF EXISTS body_override,
DROP COLUMN IF EXISTS assertion_script_override,
DROP COLUMN IF EXISTS steps_override;

-- 添加索引优化查询
CREATE INDEX IF NOT EXISTS idx_test_plan_cases_order 
ON test_plan_cases(plan_id, case_order);
```

### 3. 验证迁移结果

```sql
-- 查看表结构
DESCRIBE test_plan_cases;

-- 应该只包含以下字段:
-- - plan_id
-- - case_id
-- - case_order
-- - parameter_overrides (JSON)
-- - enabled (BOOLEAN)

-- 验证数据完整性
SELECT COUNT(*) FROM test_plan_cases;
-- 与备份表对比数量
SELECT COUNT(*) FROM test_plan_cases_backup_20260109;
```

---

## 数据影响分析

### ✅ 不受影响的功能
- Test Case 原始数据完全不受影响
- Test Plan 的用例列表和顺序保持不变
- 参数传递功能保持不变

### ⚠️ 将丢失的数据
如果您的系统已经使用了以下覆盖功能,这些数据将被删除:

1. **case_name_override**: 在计划中给用例改名
2. **url_override**: 在计划中覆盖 URL
3. **method_override**: 在计划中覆盖 HTTP 方法
4. **headers_override**: 在计划中覆盖 Headers
5. **body_override**: 在计划中覆盖 Body
6. **assertion_script_override**: 在计划中覆盖断言脚本
7. **steps_override**: 在计划中覆盖 Steps

**建议**: 
- 如果您确实使用了这些功能,请在迁移前导出数据
- 新系统中,这些修改应该直接在 Test Case 本身完成,而非在 Test Plan 中覆盖

---

## 回滚方案

如果迁移后发现问题,可以从备份恢复:

```sql
-- 方案1: 从备份表恢复
DROP TABLE test_plan_cases;
RENAME TABLE test_plan_cases_backup_20260109 TO test_plan_cases;

-- 方案2: 从SQL文件恢复
mysql -u root -p TestPlatform < test_plan_cases_backup_20260109.sql
```

---

## 执行命令

### 方式1: 在 MySQL 客户端执行

```bash
mysql -u root -p TestPlatform < /Users/xhb/IdeaProjects/GmnTestPlatform/doc/sql/migration_simplify_test_plan.sql
```

### 方式2: 在 MySQL Workbench 或其他工具中执行

直接打开 `migration_simplify_test_plan.sql` 文件并执行。

---

## 验证清单

迁移完成后,请检查:

- [ ] 表结构正确(只有5个字段)
- [ ] 数据行数与迁移前一致
- [ ] 后端服务启动无报错
- [ ] Test Plan 列表页面正常显示
- [ ] Test Plan 执行功能正常
- [ ] 参数传递功能正常

---

## 常见问题

### Q: 迁移后如何修改 Test Case?
A: 直接在 Test Case 列表中编辑原始用例,所有使用该用例的 Test Plan 都会自动使用最新版本。

### Q: 如果我需要同一个 Case 在不同 Plan 中有不同行为怎么办?
A: 通过 `parameter_overrides` 配置不同的输入参数,实现不同的行为。如果差异太大,建议创建两个独立的 Case。

### Q: 迁移需要多长时间?
A: 取决于数据量,通常几秒到几分钟。建议在低峰期执行。

---

## 技术支持

如有问题,请参考:
- 实施计划: [implementation_plan.md](file:///Users/xhb/.gemini/antigravity/brain/5a08b765-4b53-404c-8df1-9ebed664d75f/implementation_plan.md)
- 任务追踪: [task.md](file:///Users/xhb/.gemini/antigravity/brain/5a08b765-4b53-404c-8df1-9ebed664d75f/task.md)
