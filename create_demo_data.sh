#!/bin/bash

# 演示如何提取变量和串联多接口测试用例
# 场景：用户注册 → 登录 → 获取用户信息 → 更新用户信息

API_BASE="http://localhost:7777/api"

echo "======================================"
echo "创建演示数据：变量提取与接口串联"
echo "======================================"

# 1. 创建项目
echo -e "\n[1/6] 创建项目: 用户管理系统..."
PROJECT_RESPONSE=$(curl -s -X POST "${API_BASE}/projects" \
  -H "Content-Type: application/json" \
  -d '{
    "projectName": "用户管理系统",
    "description": "演示变量提取和接口串联的完整示例"
  }')
echo "✓ 项目创建成功"
PROJECT_ID=$(echo $PROJECT_RESPONSE | grep -o '"id":[0-9]*' | head -1 | grep -o '[0-9]*')
echo "  Project ID: $PROJECT_ID"

# 2. 创建模块
echo -e "\n[2/6] 创建模块: 用户认证模块..."
MODULE_RESPONSE=$(curl -s -X POST "${API_BASE}/modules" \
  -H "Content-Type: application/json" \
  -d "{
    \"moduleName\": \"用户认证与管理\",
    \"project\": {\"id\": $PROJECT_ID}
  }")
echo "✓ 模块创建成功"
MODULE_ID=$(echo $MODULE_RESPONSE | grep -o '"id":[0-9]*' | head -1 | grep -o '[0-9]*')
echo "  Module ID: $MODULE_ID"

# 3. 创建环境
echo -e "\n[3/6] 创建环境: mock..."
ENV_RESPONSE=$(curl -s -X POST "${API_BASE}/environments" \
  -H "Content-Type: application/json" \
  -d '{
    "envName": "mock",
    "description": "Mock环境 - 用于演示",
    "domain": "https://httpbin.org"
  }')
echo "✓ 环境创建成功"
ENV_ID=$(echo $ENV_RESPONSE | grep -o '"id":[0-9]*' | head -1 | grep -o '[0-9]*')
echo "  Environment ID: $ENV_ID"

# 4. 创建全局变量
echo -e "\n[4/6] 创建全局变量..."
curl -s -X POST "${API_BASE}/variables" \
  -H "Content-Type: application/json" \
  -d "{
    \"keyName\": \"base_url\",
    \"valueContent\": \"https://httpbin.org\",
    \"type\": \"normal\",
    \"description\": \"API基础URL\",
    \"environment\": {\"id\": $ENV_ID}
  }" > /dev/null
echo "✓ 变量 base_url 创建成功"

curl -s -X POST "${API_BASE}/variables" \
  -H "Content-Type: application/json" \
  -d "{
    \"keyName\": \"test_username\",
    \"valueContent\": \"demo_user_\${T(System).currentTimeMillis()}\",
    \"type\": \"normal\",
    \"description\": \"测试用户名（动态生成）\",
    \"environment\": {\"id\": $ENV_ID}
  }" > /dev/null
echo "✓ 变量 test_username 创建成功"

# 5. 创建测试用例 - 完整用户流程（4个步骤）
echo -e "\n[5/6] 创建测试用例: 完整用户流程（4步骤串联）..."
CASE_RESPONSE=$(curl -s -X POST "${API_BASE}/cases" \
  -H "Content-Type: application/json" \
  -d "{
    \"caseName\": \"用户完整流程测试\",
    \"method\": \"POST\",
    \"url\": \"\${base_url}/post\",
    \"body\": \"{}\",
    \"assertionScript\": \"status_code == 200\",
    \"module\": {\"id\": $MODULE_ID},
    \"steps\": [
      {
        \"stepName\": \"Step 1: 用户注册\",
        \"stepOrder\": 1,
        \"method\": \"POST\",
        \"url\": \"\${base_url}/post\",
        \"headers\": \"{\\\"Content-Type\\\": \\\"application/json\\\"}\",
        \"body\": \"{\\\"username\\\": \\\"\${test_username}\\\", \\\"password\\\": \\\"123456\\\", \\\"email\\\": \\\"demo@example.com\\\"}\",
        \"assertionScript\": \"status_code == 200\"
      },
      {
        \"stepName\": \"Step 2: 用户登录（提取token）\",
        \"stepOrder\": 2,
        \"method\": \"POST\",
        \"url\": \"\${base_url}/post\",
        \"headers\": \"{\\\"Content-Type\\\": \\\"application/json\\\"}\",
        \"body\": \"{\\\"username\\\": \\\"\${test_username}\\\", \\\"password\\\": \\\"123456\\\"}\",
        \"assertionScript\": \"status_code == 200\"
      },
      {
        \"stepName\": \"Step 3: 获取用户信息（使用token）\",
        \"stepOrder\": 3,
        \"method\": \"GET\",
        \"url\": \"\${base_url}/get?user=\${test_username}\",
        \"headers\": \"{\\\"Authorization\\\": \\\"Bearer \${auth_token}\\\"}\",
        \"body\": \"\",
        \"assertionScript\": \"status_code == 200\"
      },
      {
        \"stepName\": \"Step 4: 更新用户信息\",
        \"stepOrder\": 4,
        \"method\": \"PUT\",
        \"url\": \"\${base_url}/put\",
        \"headers\": \"{\\\"Authorization\\\": \\\"Bearer \${auth_token}\\\", \\\"Content-Type\\\": \\\"application/json\\\"}\",
        \"body\": \"{\\\"username\\\": \\\"\${test_username}\\\", \\\"email\\\": \\\"updated@example.com\\\", \\\"phone\\\": \\\"13800138000\\\"}\",
        \"assertionScript\": \"status_code == 200\"
      }
    ]
  }")
echo "✓ 测试用例创建成功（包含4个步骤）"
CASE_ID=$(echo $CASE_RESPONSE | grep -o '"id":[0-9]*' | head -1 | grep -o '[0-9]*')
echo "  Case ID: $CASE_ID"

# 6. 创建单步骤测试用例示例
echo -e "\n[6/6] 创建额外的测试用例示例..."
curl -s -X POST "${API_BASE}/cases" \
  -H "Content-Type: application/json" \
  -d "{
    \"caseName\": \"健康检查\",
    \"method\": \"GET\",
    \"url\": \"\${base_url}/status/200\",
    \"body\": \"\",
    \"assertionScript\": \"status_code == 200\",
    \"module\": {\"id\": $MODULE_ID}
  }" > /dev/null
echo "✓ 测试用例 '健康检查' 创建成功"

curl -s -X POST "${API_BASE}/cases" \
  -H "Content-Type: application/json" \
  -d "{
    \"caseName\": \"延迟测试\",
    \"method\": \"GET\",
    \"url\": \"\${base_url}/delay/2\",
    \"body\": \"\",
    \"assertionScript\": \"status_code == 200\",
    \"module\": {\"id\": $MODULE_ID}
  }" > /dev/null
echo "✓ 测试用例 '延迟测试' 创建成功"

echo -e "\n======================================"
echo "✅ 演示数据创建完成！"
echo "======================================"
echo ""
echo "📋 创建的数据摘要："
echo "  - 项目: 用户管理系统 (ID: $PROJECT_ID)"
echo "  - 模块: 用户认证与管理 (ID: $MODULE_ID)"
echo "  - 环境: mock (ID: $ENV_ID)"
echo "  - 测试用例: 3个 (主要演示用例ID: $CASE_ID)"
echo ""
echo "🎯 演示重点："
echo "  1. 变量使用: \${base_url}, \${test_username}"
echo "  2. 动态变量: SpEL表达式生成唯一用户名"
echo "  3. 变量提取: Step 2 提取 auth_token"
echo "  4. 变量传递: Step 3和4 使用提取的 auth_token"
echo "  5. 4步骤串联: 注册 → 登录 → 查询 → 更新"
echo ""
echo "📖 使用说明："
echo "  1. 打开浏览器访问: http://localhost:8888"
echo "  2. 导航到: API Testing → Test Cases"
echo "  3. 编辑 '用户完整流程测试'"
echo "  4. 查看4个Step的配置（变量使用示例）"
echo "  5. 点击 'Dry Run' 预览变量解析结果"
echo "  6. 选择环境 'mock' 执行测试"
echo ""
echo "💡 注意："
echo "  - 使用httpbin.org作为Mock API（无需真实后端）"
echo "  - Step 2 需要配置Extractor提取token（JSONPath: $.json.token）"
echo "  - 所有步骤都可以独立编辑和测试"
echo ""
