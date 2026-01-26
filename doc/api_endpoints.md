# API 操作指南

Base URL: `http://localhost:7777/api`

## 目录

1. [项目管理 (Project Management)](#项目管理)
2. [模块管理 (Module Management)](#模块管理)
3. [测试用例管理 (Test Case Management)](#测试用例管理)
4. [测试步骤管理 (Test Step Management)](#测试步骤管理)
5. [测试计划管理 (Test Plan Management)](#测试计划管理)
6. [测试场景管理 (Test Scenario Management)](#测试场景管理)
7. [环境管理 (Environment Management)](#环境管理)
8. [全局变量管理 (Global Variables)](#全局变量管理)
9. [测试执行 (Test Execution)](#测试执行)
10. [UI 测试管理 (UI Test Management)](#ui-测试管理)
11. [测试报告 (Test Reports)](#测试报告)
12. [使用示例](#使用示例)

---

## 项目管理

### 获取所有项目
- **URL**: `GET /projects`
- **说明**: 获取系统中的所有项目列表
- **响应示例**:
```json
[
  {
    "id": 1,
    "projectName": "用户服务",
    "description": "用户管理相关API测试",
    "createdAt": "2025-01-01T00:00:00",
    "updatedAt": "2025-01-01T00:00:00"
  }
]
```

### 根据 ID 获取项目
- **URL**: `GET /projects/{id}`
- **说明**: 获取指定项目的详细信息
- **路径参数**: `id` - 项目 ID

### 创建项目
- **URL**: `POST /projects`
- **说明**: 创建一个新项目
- **请求体**:
```json
{
  "projectName": "用户服务",
  "description": "用户管理相关API测试"
}
```

### 更新项目
- **URL**: `PUT /projects/{id}`
- **说明**: 更新项目信息
- **请求体**: 同创建项目

### 删除项目
- **URL**: `DELETE /projects/{id}`
- **说明**: 删除指定项目（级联删除其下的模块和用例）

---

## 模块管理

### 获取所有模块
- **URL**: `GET /modules`
- **说明**: 获取所有模块，可通过 `projectId` 查询参数过滤
- **查询参数**: `projectId` (可选) - 项目 ID

### 根据 ID 获取模块
- **URL**: `GET /modules/{id}`
- **说明**: 获取指定模块的详细信息

### 创建模块
- **URL**: `POST /modules`
- **说明**: 在指定项目下创建模块
- **请求体**:
```json
{
  "moduleName": "用户注册登录",
  "project": {"id": 1}
}
```

### 更新模块
- **URL**: `PUT /modules/{id}`
- **说明**: 更新模块信息

### 删除模块
- **URL**: `DELETE /modules/{id}`
- **说明**: 删除指定模块（级联删除其下的用例）

---

## 测试用例管理

### 获取所有测试用例
- **URL**: `GET /cases`
- **说明**: 获取所有测试用例，支持分页和过滤
- **查询参数**:
  - `moduleId` (可选): 按模块 ID 过滤
  - `projectId` (可选): 按项目 ID 过滤（获取项目下所有用例）
  - `page` (可选): 页码，默认 1
  - `size` (可选): 每页数量，默认 20
- **响应示例**:
```json
{
  "content": [
    {
      "id": 1,
      "caseName": "用户登录测试",
      "method": "POST",
      "url": "${base_url}/users/login",
      "body": "{\"username\": \"admin\", \"password\": \"123456\"}",
      "assertionScript": "assert responseCode == 200",
      "module": {"id": 1}
    }
  ],
  "totalElements": 100,
  "totalPages": 5,
  "page": 1,
  "size": 20
}
```

### 根据项目获取测试用例（分页）
- **URL**: `GET /cases/project?projectId={projectId}&page={page}&size={size}`
- **说明**: 获取指定项目下的所有测试用例（支持分页）
- **查询参数**:
  - `projectId` (必需): 项目 ID
  - `page` (可选): 页码，默认 1
  - `size` (可选): 每页数量，默认 20

### 根据 ID 获取测试用例
- **URL**: `GET /cases/{id}`
- **说明**: 获取测试用例的详细信息，包括所有步骤
- **响应**: 包含完整的用例信息和步骤列表

### 创建测试用例
- **URL**: `POST /cases`
- **说明**: 创建新的测试用例
- **请求体**:
```json
{
  "caseName": "用户登录测试",
  "method": "POST",
  "url": "${base_url}/users/login",
  "headers": "{\"Content-Type\": \"application/json\"}",
  "body": "{\"username\": \"admin\", \"password\": \"123456\"}",
  "precondition": "用户已注册",
  "setupScript": "vars.put('timestamp', System.currentTimeMillis())",
  "assertionScript": "assert responseCode == 200\nvars.put('token', jsonPath(response, '$.data.token'))",
  "module": {"id": 1}
}
```

### 更新测试用例
- **URL**: `PUT /cases/{id}`
- **说明**: 更新测试用例信息

### 删除测试用例
- **URL**: `DELETE /cases/{id}`
- **说明**: 删除指定测试用例

### 测试用例干运行
- **URL**: `POST /cases/{id}/dry-run`
- **说明**: 执行测试用例的干运行（不实际发送请求，只解析变量）
- **请求体**:
```json
{
  "envKey": "dev"
}
```
- **响应**:
```json
{
  "resolvedUrl": "https://api-dev.example.com/users/login",
  "resolvedBody": "{\"username\": \"admin\", \"password\": \"123456\"}",
  "resolvedHeaders": {"Content-Type": "application/json"},
  "variables": {
    "base_url": "https://api-dev.example.com",
    "timestamp": 1704067200000
  }
}
```

---

## 测试步骤管理

### 获取用例的所有步骤
- **URL**: `GET /steps/case/{caseId}`
- **说明**: 获取指定测试用例的所有步骤

### 创建测试步骤
- **URL**: `POST /steps`
- **说明**: 为测试用例添加步骤
- **请求体**:
```json
{
  "testCase": {"id": 1},
  "stepName": "获取用户信息",
  "method": "GET",
  "url": "${base_url}/users/${userId}",
  "headers": "{\"Authorization\": \"Bearer ${token}\"}",
  "body": null,
  "assertionScript": "assert responseCode == 200"
}
```

### 更新测试步骤
- **URL**: `PUT /steps/{id}`
- **说明**: 更新测试步骤信息

### 删除测试步骤
- **URL**: `DELETE /steps/{id}`
- **说明**: 删除指定测试步骤

---

## 测试计划管理

### 获取所有测试计划
- **URL**: `GET /test-plans`
- **说明**: 获取所有测试计划
- **查询参数**: `projectId` (可选) - 项目 ID

### 根据 ID 获取测试计划
- **URL**: `GET /test-plans/{id}`
- **说明**: 获取测试计划的详细信息，包括所有关联的测试用例及其覆盖配置

### 创建测试计划
- **URL**: `POST /test-plans`
- **说明**: 创建新的测试计划
- **请求体**:
```json
{
  "name": "用户流程测试计划",
  "description": "完整的用户注册、登录、信息更新流程",
  "projectId": 1,
  "testCases": [
    {
      "id": 1,
      "parameterOverrides": "{\"userId\": \"123\"}",
      "assertionScriptOverride": null,
      "planEnabled": true
    }
  ]
}
```

### 更新测试计划
- **URL**: `PUT /test-plans/{id}`
- **说明**: 更新测试计划信息

### 删除测试计划
- **URL**: `DELETE /test-plans/{id}`
- **说明**: 删除指定测试计划

### 执行测试计划
- **URL**: `POST /test-plans/{id}/execute?envKey={envKey}`
- **说明**: 执行测试计划中的所有用例
- **查询参数**: `envKey` (必需) - 环境标识（如 dev, test, prod）
- **响应**: 返回所有用例的执行结果数组

### 分析测试计划变量
- **URL**: `GET /test-plans/{id}/variables`
- **说明**: 分析测试计划中每个步骤产生和消费的变量
- **响应**:
```json
{
  "0": {
    "producedVariables": ["token", "userId"],
    "consumedVariables": ["base_url"],
    "availableVariables": ["base_url", "timestamp"]
  },
  "1": {
    "producedVariables": [],
    "consumedVariables": ["token", "userId"],
    "availableVariables": ["base_url", "timestamp", "token", "userId"]
  }
}
```

### 保存用例参数配置
- **URL**: `POST /test-plans/{planId}/cases/{caseId}/parameters`
- **说明**: 保存测试计划中特定用例的参数覆盖配置
- **请求体**:
```json
{
  "parameterOverrides": "{\"userId\": \"123\", \"status\": \"active\"}",
  "assertionScriptOverride": null,
  "enabled": true
}
```

### 获取建议参数
- **URL**: `GET /test-plans/cases/{caseId}/suggest-parameters`
- **说明**: 根据测试用例内容，建议可能需要的参数

---

## 测试场景管理

测试场景 (Test Scenario) 是 Phase 7 引入的新功能，支持复杂的流程控制（循环、条件、等待）和层级嵌套。

### 获取所有测试场景
- **URL**: `GET /scenarios`
- **说明**: 获取所有测试场景
- **查询参数**: `projectId` (可选) - 项目 ID

### 根据 ID 获取测试场景
- **URL**: `GET /scenarios/{id}`
- **说明**: 获取测试场景的基本信息

### 创建测试场景
- **URL**: `POST /scenarios`
- **说明**: 创建新的测试场景
- **请求体**:
```json
{
  "name": "E2E 订单流程",
  "description": "完整的订单创建、支付、发货流程",
  "projectId": 1
}
```

### 更新测试场景
- **URL**: `PUT /scenarios/{id}`
- **说明**: 更新测试场景信息

### 删除测试场景
- **URL**: `DELETE /scenarios/{id}`
- **说明**: 删除指定测试场景（级联删除所有步骤）

### 获取场景步骤树
- **URL**: `GET /scenarios/{id}/steps/tree`
- **说明**: 获取场景的步骤树结构（层级结构）
- **响应**: 返回根步骤列表，每个步骤包含 `children` 数组

### 添加步骤
- **URL**: `POST /scenarios/{id}/steps`
- **说明**: 向场景添加单个步骤
- **请求体**:
```json
{
  "type": "CASE",
  "name": "用户登录",
  "referenceCaseId": 1,
  "parentId": null,
  "orderIndex": 0,
  "controlLogic": null,
  "dataOverrides": "{\"params\": {\"username\": \"admin\"}}"
}
```

### 更新步骤
- **URL**: `PUT /scenarios/{id}/steps/{stepId}`
- **说明**: 更新场景步骤配置

### 删除步骤
- **URL**: `DELETE /scenarios/{id}/steps/{stepId}`
- **说明**: 删除指定步骤（级联删除子步骤）

### 同步步骤树
- **URL**: `POST /scenarios/{id}/steps/sync`
- **说明**: 批量同步场景的步骤树（删除所有现有步骤，重新插入）
- **请求体**: 根步骤数组（包含完整的树结构）
```json
[
  {
    "type": "CASE",
    "name": "步骤1",
    "referenceCaseId": 1,
    "orderIndex": 0,
    "children": []
  },
  {
    "type": "LOOP",
    "name": "循环步骤",
    "orderIndex": 1,
    "controlLogic": {"mode": "count", "count": 5},
    "children": [
      {
        "type": "CASE",
        "name": "循环内的步骤",
        "referenceCaseId": 2,
        "orderIndex": 0,
        "children": []
      }
    ]
  }
]
```

### 执行测试场景
- **URL**: `POST /scenarios/{id}/execute?envKey={envKey}`
- **说明**: 执行测试场景
- **查询参数**: `envKey` (必需) - 环境标识
- **响应**: 返回所有步骤的执行结果数组

### 流式执行测试场景
- **URL**: `GET /scenarios/{id}/execute/stream?envKey={envKey}`
- **说明**: 使用 Server-Sent Events (SSE) 流式返回执行结果
- **Content-Type**: `text/event-stream`
- **事件类型**:
  - `scenario_start`: 场景开始执行
  - `step_start`: 步骤开始执行
  - `step_complete`: 步骤执行完成
  - `scenario_complete`: 场景执行完成
  - `error`: 执行错误

---

## 环境管理

### 获取所有环境
- **URL**: `GET /environments`
- **说明**: 获取所有环境配置

### 根据 ID 获取环境
- **URL**: `GET /environments/{id}`
- **说明**: 获取指定环境的详细信息

### 创建环境
- **URL**: `POST /environments`
- **说明**: 创建新环境
- **请求体**:
```json
{
  "envName": "dev",
  "envKey": "dev",
  "description": "开发环境",
  "domain": "https://api-dev.example.com",
  "project": {"id": 1}
}
```

### 更新环境
- **URL**: `PUT /environments/{id}`
- **说明**: 更新环境配置

### 删除环境
- **URL**: `DELETE /environments/{id}`
- **说明**: 删除指定环境

---

## 全局变量管理

### 根据环境获取变量
- **URL**: `GET /variables/environment/{envId}`
- **说明**: 获取指定环境的所有变量（包括继承的变量）

### 创建变量
- **URL**: `POST /variables`
- **说明**: 创建全局变量
- **请求体**:
```json
{
  "varKey": "base_url",
  "varValue": "https://api-dev.example.com",
  "varType": "STRING",
  "description": "基础URL",
  "environment": {"id": 1},
  "project": {"id": 1}
}
```

### 更新变量
- **URL**: `PUT /variables/{id}`
- **说明**: 更新变量值

### 删除变量
- **URL**: `DELETE /variables/{id}`
- **说明**: 删除指定变量

---

## 测试执行

### 执行测试用例
- **URL**: `POST /cases/execute`
- **说明**: 批量执行测试用例
- **查询参数**:
  - `envKey` (必需): 环境标识
  - `projectId` (可选): 执行项目下所有用例
  - `moduleId` (可选): 执行模块下所有用例
- **响应**: 执行结果数组

### 执行 Groovy 脚本
- **URL**: `POST /cases/groovy`
- **说明**: 执行 Groovy 脚本（用于测试脚本语法）
- **请求体**:
```json
{
  "script": "def result = 1 + 1; return result",
  "variables": {
    "base_url": "https://api.example.com"
  }
}
```

---

## UI 测试管理

### 获取所有 UI 测试用例
- **URL**: `GET /ui-tests/cases`
- **说明**: 获取所有 UI 测试用例

### 根据项目获取 UI 测试用例
- **URL**: `GET /ui-tests/projects/{projectId}/cases`
- **说明**: 获取指定项目下的所有 UI 测试用例

### 根据模块获取 UI 测试用例
- **URL**: `GET /ui-tests/modules/{moduleId}/cases`
- **说明**: 获取指定模块下的所有 UI 测试用例

### 根据 ID 获取 UI 测试用例
- **URL**: `GET /ui-tests/cases/{id}`
- **说明**: 获取 UI 测试用例的详细信息

### 创建 UI 测试用例
- **URL**: `POST /ui-tests/cases`
- **说明**: 创建新的 UI 测试用例
- **请求体**:
```json
{
  "caseName": "登录页面测试",
  "url": "https://example.com/login",
  "browserType": "CHROMIUM",
  "viewport": {"width": 1920, "height": 1080},
  "projectId": 1,
  "moduleId": 1
}
```

### 删除 UI 测试用例
- **URL**: `DELETE /ui-tests/cases/{id}`
- **说明**: 删除指定 UI 测试用例

### 获取用例的步骤
- **URL**: `GET /ui-tests/cases/{caseId}/steps`
- **说明**: 获取 UI 测试用例的所有步骤

### 创建测试步骤
- **URL**: `POST /ui-tests/steps`
- **说明**: 为 UI 测试用例添加步骤
- **请求体**:
```json
{
  "caseId": 1,
  "actionType": "CLICK",
  "selector": "#login-button",
  "value": null,
  "orderIndex": 0
}
```

### 删除测试步骤
- **URL**: `DELETE /ui-tests/steps/{id}`
- **说明**: 删除指定 UI 测试步骤

### 执行 UI 测试用例
- **URL**: `POST /ui-tests/cases/{id}/execute`
- **说明**: 执行 UI 测试用例

---

## 测试报告

### 获取所有报告
- **URL**: `GET /reports`
- **说明**: 获取所有测试执行记录

### 根据项目获取报告
- **URL**: `GET /reports/project/{projectId}`
- **说明**: 获取指定项目的所有执行记录

### 根据模块获取报告
- **URL**: `GET /reports/module/{moduleId}`
- **说明**: 获取指定模块的所有执行记录

---

## 使用示例

### 示例 1: 创建并执行测试用例

```bash
# 1. 创建测试用例
curl -X POST http://localhost:7777/api/cases \
  -H "Content-Type: application/json" \
  -d '{
    "caseName": "用户登录",
    "method": "POST",
    "url": "${base_url}/users/login",
    "body": "{\"username\": \"admin\", \"password\": \"123456\"}",
    "assertionScript": "assert responseCode == 200\nvars.put(\"token\", jsonPath(response, \"$.data.token\"))",
    "module": {"id": 1}
  }'

# 2. 执行测试用例
curl -X POST "http://localhost:7777/api/cases/execute?envKey=dev&moduleId=1"
```

### 示例 2: 创建测试计划并执行

```bash
# 1. 创建测试计划
curl -X POST http://localhost:7777/api/test-plans \
  -H "Content-Type: application/json" \
  -d '{
    "name": "用户流程测试",
    "projectId": 1,
    "testCases": [
      {"id": 1, "parameterOverrides": "{}", "planEnabled": true},
      {"id": 2, "parameterOverrides": "{}", "planEnabled": true}
    ]
  }'

# 2. 执行测试计划
curl -X POST "http://localhost:7777/api/test-plans/1/execute?envKey=dev"
```

### 示例 3: 创建测试场景并执行

```bash
# 1. 创建测试场景
curl -X POST http://localhost:7777/api/scenarios \
  -H "Content-Type: application/json" \
  -d '{
    "name": "订单流程",
    "projectId": 1
  }'

# 2. 添加步骤
curl -X POST http://localhost:7777/api/scenarios/1/steps \
  -H "Content-Type: application/json" \
  -d '{
    "type": "CASE",
    "name": "登录",
    "referenceCaseId": 1,
    "orderIndex": 0
  }'

# 3. 执行场景
curl -X POST "http://localhost:7777/api/scenarios/1/execute?envKey=dev"
```

### 示例 4: 使用变量传递

```groovy
// 在第一个用例的 assertionScript 中提取变量
vars.put("token", jsonPath(response, "$.data.token"))
vars.put("userId", jsonPath(response, "$.data.userId"))

// 在第二个用例的 URL 中使用变量
${base_url}/users/${userId}/profile

// 在 Headers 中使用变量
{"Authorization": "Bearer ${token}"}
```

---

## 变量替换规则

### 变量语法
- 使用 `${variable_name}` 语法在 URL、Headers、Body 中引用变量
- 支持嵌套变量和表达式

### 变量继承顺序
1. **环境变量** (Environment Variables)
2. **项目变量** (Project Variables)
3. **模块变量** (Module Variables)
4. **运行时变量** (Runtime Variables - 由脚本设置)

### 支持的表达式
- 基本变量: `${base_url}`
- SpEL 表达式: `${T(System).currentTimeMillis()}`
- JSONPath: `jsonPath(response, '$.data.token')`

---

## 错误码说明

| 状态码 | 说明 |
|--------|------|
| 200 | 请求成功 |
| 201 | 创建成功 |
| 204 | 删除成功（无响应体） |
| 400 | 请求参数错误 |
| 404 | 资源不存在 |
| 405 | 方法不允许 |
| 500 | 服务器内部错误 |

---

## 注意事项

1. **变量作用域**: 测试计划中的变量在用例间共享，测试场景中的变量在步骤间共享
2. **数据覆盖**: 测试计划和测试场景支持对用例进行数据覆盖，不会影响原始用例
3. **执行顺序**: 测试计划和测试场景按顺序执行用例/步骤
4. **错误处理**: 单个用例/步骤失败不会中断整个计划/场景的执行
5. **分页**: 获取用例列表时建议使用分页，避免一次加载过多数据

---

## 更新日志

- **2025-01-26**: 添加测试场景管理 API 文档
- **2025-01-26**: 添加分页支持说明
- **2025-01-26**: 完善操作指南和使用示例
