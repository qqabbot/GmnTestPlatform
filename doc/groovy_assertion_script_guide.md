# Groovy Assertion Script 使用指南

## 概述

Groovy Assertion Script 是在测试执行时运行的 Groovy 脚本，用于验证响应结果和提取变量。脚本可以访问响应数据、运行时变量，并使用内置的辅助函数。

## 可用变量和对象

### 1. 响应相关变量

#### `status_code` / `status`
- **类型**: `Integer`
- **说明**: HTTP 响应状态码
- **示例**:
  ```groovy
  assert status_code == 200
  assert status == 200  // status 是 status_code 的别名
  ```

#### `response_body`
- **类型**: `String`
- **说明**: HTTP 响应体的原始字符串（通常是 JSON）
- **示例**:
  ```groovy
  assert response_body.contains("success")
  assert response_body.length() > 0
  ```

#### `response`
- **类型**: `TestResponse` 对象
- **说明**: 完整的响应对象，包含所有响应信息
- **可用方法**:
  - `response.getBody()`: 获取响应体字符串
  - `response.getStatusCode()`: 获取状态码
  - `response.getHeaders()`: 获取响应头 Map
- **示例**:
  ```groovy
  assert response.getStatusCode() == 200
  assert response.getBody() != null
  ```

#### `headers`
- **类型**: `Map<String, String>`
- **说明**: HTTP 响应头映射
- **示例**:
  ```groovy
  assert headers["Content-Type"] == "application/json"
  assert headers.containsKey("Authorization")
  ```

### 2. 变量操作

#### `vars`
- **类型**: `Map<String, Object>`
- **说明**: 运行时变量映射（可读写），用于存储和读取变量
- **常用操作**:
  - `vars.put(key, value)`: 设置变量
  - `vars.get(key)`: 获取变量
  - `vars.containsKey(key)`: 检查变量是否存在
- **示例**:
  ```groovy
  // 提取并保存变量
  vars.put("token", jsonPath(response, '$.data.token'))
  vars.put("userId", jsonPath(response, '$.data.userId'))
  
  // 读取变量
  def token = vars.get("token")
  assert token != null
  ```

### 3. 运行时变量

所有运行时变量（包括环境变量、从 Steps 提取的变量等）都可以直接访问。

- **类型**: 根据变量值类型而定（String, Integer, Object 等）
- **说明**: 所有在测试执行过程中定义的变量都可以直接在脚本中使用
- **示例**:
  ```groovy
  // 如果存在 base_url 变量
  assert base_url != null
  
  // 如果存在 token 变量（从之前的 Step 提取）
  assert token != null
  assert token.length() > 0
  ```

### 4. 辅助函数

#### `jsonPath(response, path)`
- **参数**:
  - `response`: TestResponse 对象
  - `path`: JSONPath 表达式（字符串，使用单引号避免 Groovy 插值问题）
- **返回**: 提取的值，如果路径不存在则返回 `null`
- **说明**: 从响应 JSON 中提取值的辅助函数
- **示例**:
  ```groovy
  // 提取单个值
  def token = jsonPath(response, '$.data.token')
  def userId = jsonPath(response, '$.data.user.id')
  
  // 提取数组元素
  def firstItem = jsonPath(response, '$.data.items[0].name')
  
  // 在断言中使用
  assert jsonPath(response, '$.code') == "0000"
  assert jsonPath(response, '$.data.userId') != null
  ```

## 常用断言模式

### 1. 状态码断言
```groovy
assert status_code == 200
assert status == 201
assert status_code >= 200 && status_code < 300
```

### 2. JSON 字段断言
```groovy
// 使用 jsonPath 函数
assert jsonPath(response, '$.code') == "0000"
assert jsonPath(response, '$.data.userId') != null
assert jsonPath(response, '$.data.items.length()') > 0
```

### 3. 响应体内容断言
```groovy
assert response_body.contains("success")
assert response_body.length() > 100
```

### 4. 响应头断言
```groovy
assert headers["Content-Type"] == "application/json"
assert headers.containsKey("X-Token")
```

### 5. 变量提取和断言
```groovy
// 提取变量
vars.put("token", jsonPath(response, '$.data.token'))
vars.put("userId", jsonPath(response, '$.data.userId'))

// 断言变量值
assert vars.get("token") != null
assert vars.get("userId") > 0
```

### 6. 复杂条件断言
```groovy
// 多个条件
assert status_code == 200 && jsonPath(response, '$.code') == "0000"

// 使用 if-else
if (status_code == 200) {
    assert jsonPath(response, '$.data') != null
} else {
    assert jsonPath(response, '$.error') != null
}

// 使用三元运算符
def result = status_code == 200 ? "success" : "failed"
assert result == "success"
```

## 变量提取示例

### 提取单个值
```groovy
vars.put("token", jsonPath(response, '$.data.token'))
vars.put("userId", jsonPath(response, '$.data.userId'))
```

### 提取嵌套值
```groovy
vars.put("userName", jsonPath(response, '$.data.user.name'))
vars.put("userEmail", jsonPath(response, '$.data.user.email'))
```

### 提取数组元素
```groovy
vars.put("firstItemId", jsonPath(response, '$.data.items[0].id'))
vars.put("itemCount", jsonPath(response, '$.data.items.length()'))
```

### 提取并验证
```groovy
def token = jsonPath(response, '$.data.token')
assert token != null : "Token not found in response"
vars.put("token", token)
```

## JSONPath 表达式参考

### 基本语法
- `$`: 根对象
- `$.field`: 访问字段
- `$.field.subfield`: 访问嵌套字段
- `$[0]`: 访问数组第一个元素
- `$[*]`: 访问数组所有元素
- `$.field[?(@.name == 'value')]`: 过滤数组

### 常用表达式
```groovy
// 根级别字段
jsonPath(response, '$.code')
jsonPath(response, '$.message')

// 嵌套字段
jsonPath(response, '$.data.user.id')
jsonPath(response, '$.data.user.name')

// 数组访问
jsonPath(response, '$.data.items[0]')
jsonPath(response, '$.data.items[0].name')
jsonPath(response, '$.data.items.length()')

// 复杂路径
jsonPath(response, '$.data.user.addresses[0].city')
```

## 注意事项

### 1. JSONPath 表达式引号
- **必须使用单引号** `'$.token'` 而不是双引号 `"$.token"`
- 原因：Groovy 的双引号字符串会进行 GString 插值，`$` 会被解释为变量
- **正确**: `jsonPath(response, '$.token')`
- **错误**: `jsonPath(response, "$.token")` ❌

### 2. 脚本返回值
- 脚本可以返回 `Boolean` 值（`true`/`false`）表示断言结果
- 如果脚本返回 `null`，会被视为成功（允许 `vars.put()` 等操作）
- 如果脚本抛出异常，断言失败

### 3. 变量命名
- 变量名会自动清理：`$token` 或 `${token}` 会被处理为 `token`
- 建议使用简洁的变量名，避免特殊字符

### 4. 错误处理
- `jsonPath()` 函数在路径不存在时返回 `null`，不会抛出异常
- 使用前应检查返回值是否为 `null`

## 完整示例

### 示例 1: 基本断言
```groovy
// 验证状态码
assert status_code == 200

// 验证响应码
assert jsonPath(response, '$.code') == "0000"

// 验证数据存在
assert jsonPath(response, '$.data') != null
```

### 示例 2: 提取变量
```groovy
// 提取 token
vars.put("token", jsonPath(response, '$.data.token'))

// 提取用户信息
vars.put("userId", jsonPath(response, '$.data.user.id'))
vars.put("userName", jsonPath(response, '$.data.user.name'))

// 验证提取成功
assert vars.get("token") != null
assert vars.get("userId") > 0
```

### 示例 3: 复杂验证
```groovy
// 验证状态码和响应结构
assert status_code == 200
assert jsonPath(response, '$.code') == "0000"

// 提取并验证 token
def token = jsonPath(response, '$.data.token')
assert token != null : "Token is required"
assert token.length() > 10 : "Token seems invalid"
vars.put("token", token)

// 验证用户信息
def userId = jsonPath(response, '$.data.user.id')
assert userId != null && userId > 0
vars.put("userId", userId)
```

### 示例 4: 条件断言
```groovy
if (status_code == 200) {
    assert jsonPath(response, '$.code') == "0000"
    vars.put("token", jsonPath(response, '$.data.token'))
} else {
    assert jsonPath(response, '$.error') != null
    println("Request failed with status: " + status_code)
}
```

### 示例 5: 数组验证
```groovy
// 验证数组不为空
def items = jsonPath(response, '$.data.items')
assert items != null
assert items.size() > 0

// 提取第一个元素
vars.put("firstItemId", jsonPath(response, '$.data.items[0].id'))
```

## 调试技巧

### 1. 使用 println 输出调试信息
```groovy
println("Status code: " + status_code)
println("Response body: " + response_body)
println("Token: " + jsonPath(response, '$.data.token'))
```

### 2. 检查变量值
```groovy
println("Available variables: " + vars.keySet())
println("Token value: " + vars.get("token"))
```

### 3. 验证 JSONPath 表达式
```groovy
def result = jsonPath(response, '$.data.token')
println("Extracted value: " + result)
assert result != null
```

## 常见错误和解决方案

### 错误 1: `token recognition error at: '.'`
- **原因**: JSONPath 表达式使用了双引号
- **解决**: 使用单引号 `'$.token'` 而不是 `"$.token"`

### 错误 2: `No results for path`
- **原因**: JSONPath 路径不存在
- **解决**: 检查响应结构，使用正确的路径，或添加 null 检查

### 错误 3: 变量未定义
- **原因**: 变量名拼写错误或变量未设置
- **解决**: 使用 `vars.containsKey("variableName")` 检查变量是否存在

### 错误 4: 类型转换错误
- **原因**: 期望的类型与实际类型不匹配
- **解决**: 使用类型转换或类型检查
  ```groovy
  def userId = jsonPath(response, '$.data.userId')
  assert userId instanceof Integer || userId instanceof String
  ```

