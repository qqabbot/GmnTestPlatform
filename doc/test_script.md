# 测试执行脚本

## 后端 API 测试

### 1. 测试获取所有用例
```bash
curl -X GET http://localhost:7777/api/cases
```

### 2. 测试创建用例
```bash
curl -X POST http://localhost:7777/api/cases \
  -H "Content-Type: application/json" \
  -d '{
    "module": {"id": 1},
    "caseName": "测试用例1",
    "method": "GET",
    "url": "http://httpbin.org/get",
    "body": "",
    "assertionScript": "status_code == 200",
    "isActive": true
  }'
```

### 3. 测试更新用例
```bash
curl -X PUT http://localhost:7777/api/cases/1 \
  -H "Content-Type: application/json" \
  -d '{
    "id": 1,
    "module": {"id": 1},
    "caseName": "更新后的测试用例",
    "method": "POST",
    "url": "http://httpbin.org/post",
    "body": "{\"test\": \"data\"}",
    "assertionScript": "status_code == 200",
    "isActive": true
  }'
```

### 4. 测试执行用例
```bash
curl -X POST "http://localhost:7777/api/cases/execute?envKey=dev"
```

### 5. 测试删除用例
```bash
curl -X DELETE http://localhost:7777/api/cases/1
```

## 前端功能测试

### 测试步骤
1. 打开浏览器访问 http://localhost:8080
2. 验证页面加载
3. 点击 "Test Cases" 菜单
4. 点击 "New Case" 按钮
5. 填写表单并保存
6. 验证用例出现在列表中
7. 点击编辑按钮修改用例
8. 点击 "Execution" 菜单
9. 选择环境并执行
10. 验证结果显示

## 自动化测试脚本

```bash
#!/bin/bash

echo "=== API 自动化测试平台 - 测试脚本 ==="
echo ""

# 测试后端健康检查
echo "1. 测试后端连接..."
if curl -s http://localhost:7777/api/cases > /dev/null; then
    echo "✅ 后端连接成功"
else
    echo "❌ 后端连接失败"
    exit 1
fi

# 测试创建用例
echo ""
echo "2. 测试创建用例..."
RESPONSE=$(curl -s -X POST http://localhost:7777/api/cases \
  -H "Content-Type: application/json" \
  -d '{
    "module": {"id": 1},
    "caseName": "自动化测试用例",
    "method": "GET",
    "url": "http://httpbin.org/get",
    "assertionScript": "status_code == 200",
    "isActive": true
  }')

if [ $? -eq 0 ]; then
    echo "✅ 创建用例成功"
    echo "响应: $RESPONSE"
else
    echo "❌ 创建用例失败"
fi

# 测试执行
echo ""
echo "3. 测试执行用例..."
EXEC_RESPONSE=$(curl -s -X POST "http://localhost:7777/api/cases/execute?envKey=dev")

if [ $? -eq 0 ]; then
    echo "✅ 执行测试成功"
    echo "结果: $EXEC_RESPONSE"
else
    echo "❌ 执行测试失败"
fi

echo ""
echo "=== 测试完成 ==="
```

保存为 `test.sh` 并执行：
```bash
chmod +x test.sh
./test.sh
```
