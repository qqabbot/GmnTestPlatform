#!/bin/bash

# API Base URL
BASE_URL="http://localhost:7777/api"

echo "=== 插入测试数据 ==="

# 1. Create Environment
echo -e "\n1. 创建环境..."
ENV_RESPONSE=$(curl -s -X POST "${BASE_URL}/environments" \
  -H "Content-Type: application/json" \
  -d '{
    "envName": "dev",
    "description": "开发环境",
    "domain": "https://api-dev.example.com"
  }')
echo "Environment created: $ENV_RESPONSE"

ENV_RESPONSE2=$(curl -s -X POST "${BASE_URL}/environments" \
  -H "Content-Type: application/json" \
  -d '{
    "envName": "test",
    "description": "测试环境",
    "domain": "https://api-test.example.com"
  }')
echo "Environment created: $ENV_RESPONSE2"

ENV_RESPONSE3=$(curl -s -X POST "${BASE_URL}/environments" \
  -H "Content-Type: application/json" \
  -d '{
    "envName": "prod",
    "description": "生产环境",
    "domain": "https://api.example.com"
  }')
echo "Environment created: $ENV_RESPONSE3"

# Extract env IDs
ENV_ID=$(echo $ENV_RESPONSE | grep -o '"id":[0-9]*' | head -1 | grep -o '[0-9]*')

# 2. Create Projects
echo -e "\n2. 创建项目..."
PROJECT1=$(curl -s -X POST "${BASE_URL}/projects" \
  -H "Content-Type: application/json" \
  -d '{
    "projectName": "用户服务",
    "description": "用户管理相关API测试"
  }')
echo "Project created: $PROJECT1"
PROJECT1_ID=$(echo $PROJECT1 | grep -o '"id":[0-9]*' | head -1 | grep -o '[0-9]*')

PROJECT2=$(curl -s -X POST "${BASE_URL}/projects" \
  -H "Content-Type: application/json" \
  -d '{
    "projectName": "订单服务",
    "description": "订单管理相关API测试"
  }')
echo "Project created: $PROJECT2"
PROJECT2_ID=$(echo $PROJECT2 | grep -o '"id":[0-9]*' | head -1 | grep -o '[0-9]*')

# 3. Create Modules
echo -e "\n3. 创建模块..."
MODULE1=$(curl -s -X POST "${BASE_URL}/modules" \
  -H "Content-Type: application/json" \
  -d "{
    \"moduleName\": \"用户注册登录\",
    \"project\": {\"id\": $PROJECT1_ID}
  }")
echo "Module created: $MODULE1"
MODULE1_ID=$(echo $MODULE1 | grep -o '"id":[0-9]*' | head -1 | grep -o '[0-9]*')

MODULE2=$(curl -s -X POST "${BASE_URL}/modules" \
  -H "Content-Type: application/json" \
  -d "{
    \"moduleName\": \"用户信息管理\",
    \"project\": {\"id\": $PROJECT1_ID}
  }")
echo "Module created: $MODULE2"
MODULE2_ID=$(echo $MODULE2 | grep -o '"id":[0-9]*' | head -1 | grep -o '[0-9]*')

MODULE3=$(curl -s -X POST "${BASE_URL}/modules" \
  -H "Content-Type: application/json" \
  -d "{
    \"moduleName\": \"订单创建\",
    \"project\": {\"id\": $PROJECT2_ID}
  }")
echo "Module created: $MODULE3"
MODULE3_ID=$(echo $MODULE3 | grep -o '"id":[0-9]*' | head -1 | grep -o '[0-9]*')

# 4. Create Global Variables
echo -e "\n4. 创建全局变量..."
curl -s -X POST "${BASE_URL}/variables" \
  -H "Content-Type: application/json" \
  -d "{
    \"varKey\": \"base_url\",
    \"varValue\": \"https://api-dev.example.com\",
    \"varType\": \"STRING\",
    \"description\": \"基础URL\",
    \"environment\": {\"id\": $ENV_ID}
  }"
echo "Variable base_url created"

curl -s -X POST "${BASE_URL}/variables" \
  -H "Content-Type: application/json" \
  -d "{
    \"varKey\": \"api_token\",
    \"varValue\": \"Bearer test-token-123456\",
    \"varType\": \"SECRET\",
    \"description\": \"API Token\",
    \"environment\": {\"id\": $ENV_ID}
  }"
echo "Variable api_token created"

curl -s -X POST "${BASE_URL}/variables" \
  -H "Content-Type: application/json" \
  -d "{
    \"varKey\": \"test_user_id\",
    \"varValue\": \"1001\",
    \"varType\": \"NUMBER\",
    \"description\": \"测试用户ID\",
    \"project\": {\"id\": $PROJECT1_ID}
  }"
echo "Variable test_user_id created"

# 5. Create Test Cases
echo -e "\n5. 创建测试用例..."
CASE1=$(curl -s -X POST "${BASE_URL}/cases" \
  -H "Content-Type: application/json" \
  -d "{
    \"caseName\": \"用户登录-成功\",
    \"method\": \"POST\",
    \"url\": \"\${base_url}/users/login\",
    \"body\": \"{\\\"username\\\": \\\"admin\\\", \\\"password\\\": \\\"123456\\\"}\",
    \"assertionScript\": \"status_code == 200\",
    \"module\": {\"id\": $MODULE1_ID}
  }")
echo "Test case created: $CASE1"

CASE2=$(curl -s -X POST "${BASE_URL}/cases" \
  -H "Content-Type: application/json" \
  -d "{
    \"caseName\": \"获取用户信息\",
    \"method\": \"GET\",
    \"url\": \"\${base_url}/users/\${test_user_id}\",
    \"body\": \"\",
    \"assertionScript\": \"status_code == 200 && response.data.id == 1001\",
    \"module\": {\"id\": $MODULE2_ID}
  }")
echo "Test case created: $CASE2"

CASE3=$(curl -s -X POST "${BASE_URL}/cases" \
  -H "Content-Type: application/json" \
  -d "{
    \"caseName\": \"更新用户信息\",
    \"method\": \"PUT\",
    \"url\": \"\${base_url}/users/\${test_user_id}\",
    \"body\": \"{\\\"name\\\": \\\"张三\\\", \\\"email\\\": \\\"zhangsan@example.com\\\"}\",
    \"assertionScript\": \"status_code == 200\",
    \"module\": {\"id\": $MODULE2_ID}
  }")
echo "Test case created: $CASE3"

CASE4=$(curl -s -X POST "${BASE_URL}/cases" \
  -H "Content-Type: application/json" \
  -d "{
    \"caseName\": \"创建订单\",
    \"method\": \"POST\",
    \"url\": \"\${base_url}/orders\",
    \"body\": \"{\\\"userId\\\": \\\"\${test_user_id}\\\", \\\"items\\\": [{\\\"productId\\\": 101, \\\"quantity\\\": 2}]}\",
    \"assertionScript\": \"status_code == 201\",
    \"module\": {\"id\": $MODULE3_ID}
  }")
echo "Test case created: $CASE4"

CASE5=$(curl -s -X POST "${BASE_URL}/cases" \
  -H "Content-Type: application/json" \
  -d "{
    \"caseName\": \"查询订单列表\",
    \"method\": \"GET\",
    \"url\": \"\${base_url}/orders?userId=\${test_user_id}\",
    \"body\": \"\",
    \"assertionScript\": \"status_code == 200 && response.data.length > 0\",
    \"module\": {\"id\": $MODULE3_ID}
  }")
echo "Test case created: $CASE5"

echo -e "\n=== 数据插入完成! ==="
echo "插入数据汇总:"
echo "- 环境: 3个 (dev, test, prod)"
echo "- 项目: 2个 (用户服务, 订单服务)"
echo "- 模块: 3个 (用户注册登录, 用户信息管理, 订单创建)"
echo "- 变量: 3个 (base_url, api_token, test_user_id)"
echo "- 测试用例: 5个"
echo ""
echo "请刷新浏览器查看数据!"
