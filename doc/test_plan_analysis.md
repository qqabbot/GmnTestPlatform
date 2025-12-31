# Test Plan 模块实现逻辑分析报告

## 需求回顾

1. **Test Case 模块**：单接口测试，独立运行
2. **Test Plan 模块**：串联逻辑测试，可以引用 Test Case
3. **隔离机制**：Test Plan 中的改动不应影响 Test Case
4. **动态传参**：Test Plan 支持用例间的动态参数传递

## 当前实现分析

### ✅ 已正确实现的部分

#### 1. 数据隔离机制（部分正确）

**数据库层面**：
- ✅ 使用 `test_plan_cases` 中间表存储 overrides
- ✅ Override 字段包括：`case_name_override`, `url_override`, `method_override`, `headers_override`, `body_override`, `assertion_script_override`, `steps_override`
- ✅ 原始 `test_case` 表不会被修改

**模型层面**：
- ✅ `TestCase` 模型提供了 `getEffective*()` 方法：
  - `getEffectiveCaseName()`
  - `getEffectiveUrl()`
  - `getEffectiveMethod()`
  - `getEffectiveHeaders()`
  - `getEffectiveBody()`
  - `getEffectiveAssertionScript()`
  - `getEffectiveSteps()`

#### 2. 参数传递机制（正确）

**Parameter Overrides**：
- ✅ `parameterOverrides` 存储在 `test_plan_cases` 表中
- ✅ 在 `executePlan()` 中被解析并合并到 `baseRuntimeVariables` Map 中
- ✅ 使用共享的 `runtimeVariables` Map 实现用例间的变量传递

**变量传递流程**：
```
1. 初始化：从 Environment 加载全局变量 → baseRuntimeVariables
2. 执行用例1：
   - 应用 parameterOverrides → 合并到 baseRuntimeVariables
   - 执行用例（使用 baseRuntimeVariables）
   - 执行 assertion script → vars.put("token", value) → 添加到 baseRuntimeVariables
3. 执行用例2：
   - 应用 parameterOverrides → 合并到 baseRuntimeVariables
   - 执行用例（可以使用用例1设置的变量，如 ${token}）
   - 执行 assertion script → 继续添加变量
```

### ❌ 存在的问题

#### 问题 1：Test Plan 查询时未正确合并所有 Overrides（中等严重）

**位置**：`TestPlanService.findById()`

**问题描述**：
- ✅ `executeSingleCaseLogic()` 已经正确使用了 `getEffective*()` 方法
- ❌ 但在 `findById()` 中，只保留了 `parameterOverrides`
- ❌ 其他 overrides（`urlOverride`, `methodOverride`, `bodyOverride`, `headersOverride`, `assertionScriptOverride`, `stepsOverride`）在合并 details 时丢失

**影响**：
- Test Plan 中设置的 URL、Method、Body、Headers 等 overrides 虽然存储在数据库中
- 但在查询时没有正确合并到 TestCase 对象中
- 导致 `getEffective*()` 方法无法获取到 override 值

**代码位置**：
```java
// TestPlanService.java:70-77
for (int i = 0; i < plan.getTestCases().size(); i++) {
    TestCase tc = plan.getTestCases().get(i);
    TestCase details = caseMapper.findByIdWithDetails(tc.getId());
    if (details != null) {
        details.setParameterOverrides(tc.getParameterOverrides()); // 只保留了 parameterOverrides
        plan.getTestCases().set(i, details);
    }
}
```

#### 问题 2：Test Plan 查询时未正确加载 Overrides

**位置**：`TestPlanService.findById()`

**问题描述**：
- `findById()` 方法中，虽然从 `test_plan_cases` 表查询了 overrides
- 但在合并 details 时，只保留了 `parameterOverrides`
- 其他 overrides（如 `urlOverride`, `methodOverride` 等）可能丢失

**代码位置**：
```java
// TestPlanService.java:70-77
for (int i = 0; i < plan.getTestCases().size(); i++) {
    TestCase tc = plan.getTestCases().get(i);
    TestCase details = caseMapper.findByIdWithDetails(tc.getId());
    if (details != null) {
        details.setParameterOverrides(tc.getParameterOverrides()); // 只保留了 parameterOverrides
        plan.getTestCases().set(i, details);
    }
}
```

#### 问题 3：前端保存时未传递所有 Overrides

**位置**：`TestPlanList.vue` 的 `savePlan()` 方法

**问题描述**：
- 前端只传递了 `parameterOverrides`
- 没有传递其他 overrides（如 `caseNameOverride`, `urlOverride` 等）
- 虽然 `CaseDetailDrawer` 组件支持编辑 overrides，但保存 Plan 时可能丢失

**代码位置**：
```javascript
// TestPlanList.vue:631-637
const payload = {
    ...currentPlan.value,
    project: { id: currentPlan.value.projectId },
    testCases: selectedCases.value.map(c => ({ 
        id: c.id,
        parameterOverrides: c.parameterOverrides // 只传递了 parameterOverrides
    }))
}
```

## 修复建议

### 修复 1：在 TestPlanService.findById() 中正确合并所有 Overrides

**修改文件**：`TestPlanService.java`

**修改内容**：
```java
for (int i = 0; i < plan.getTestCases().size(); i++) {
    TestCase tc = plan.getTestCases().get(i);
    TestCase details = caseMapper.findByIdWithDetails(tc.getId());
    if (details != null) {
        // 保留所有 overrides
        details.setParameterOverrides(tc.getParameterOverrides());
        details.setCaseNameOverride(tc.getCaseNameOverride());
        details.setUrlOverride(tc.getUrlOverride());
        details.setMethodOverride(tc.getMethodOverride());
        details.setHeadersOverride(tc.getHeadersOverride());
        details.setBodyOverride(tc.getBodyOverride());
        details.setAssertionScriptOverride(tc.getAssertionScriptOverride());
        details.setStepsOverride(tc.getStepsOverride());
        details.setPlanEnabled(tc.getPlanEnabled());
        plan.getTestCases().set(i, details);
    }
}
```

### 修复 2：前端保存时传递所有 Overrides

**修改文件**：`TestPlanList.vue`

**修改内容**：
```javascript
const payload = {
    ...currentPlan.value,
    project: { id: currentPlan.value.projectId },
    testCases: selectedCases.value.map(c => ({ 
        id: c.id,
        parameterOverrides: c.parameterOverrides || '',
        // 添加其他 overrides（如果前端支持编辑）
        caseNameOverride: c.caseNameOverride,
        urlOverride: c.urlOverride,
        methodOverride: c.methodOverride,
        headersOverride: c.headersOverride,
        bodyOverride: c.bodyOverride,
        assertionScriptOverride: c.assertionScriptOverride,
        stepsOverride: c.stepsOverride,
        enabled: c.enabled !== false // 默认启用
    }))
}
```

## 测试建议

### 测试场景 1：隔离机制测试
1. 创建一个 Test Case：URL = `http://api.example.com/users`
2. 创建一个 Test Plan，引用该 Test Case
3. 在 Test Plan 中设置 URL Override = `http://api.example.com/orders`
4. 执行 Test Plan
5. **预期**：应该使用 `http://api.example.com/orders`，而不是原始 URL
6. **验证**：检查 Test Case 的原始 URL 是否仍然是 `http://api.example.com/users`

### 测试场景 2：参数传递测试
1. 创建 Test Case 1：在 assertion script 中设置 `vars.put("token", "abc123")`
2. 创建 Test Case 2：URL = `${base_url}/api/users?token=${token}`
3. 创建 Test Plan，按顺序添加 Test Case 1 和 Test Case 2
4. 执行 Test Plan
5. **预期**：Test Case 2 应该能够使用 Test Case 1 设置的 `token` 变量

### 测试场景 3：Parameter Overrides 测试
1. 创建 Test Case：URL = `${base_url}/api/users?id=${userId}`
2. 创建 Test Plan，引用该 Test Case
3. 设置 Parameter Override：`{ "userId": "12345" }`
4. 执行 Test Plan
5. **预期**：URL 应该被解析为 `${base_url}/api/users?id=12345`

## 总结

### 当前状态
- ✅ **数据隔离**：数据库层面已实现，但执行时未应用
- ✅ **参数传递**：机制正确，已实现
- ❌ **Override 应用**：模型已支持，但执行时未使用

### 优先级
1. **高优先级**：修复 `TestPlanService.findById()` 合并所有 Overrides（影响核心功能，导致 overrides 不生效）
2. **中优先级**：前端保存时传递所有 Overrides（如果前端已支持编辑，则需要修复）

