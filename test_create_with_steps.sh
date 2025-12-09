#!/bin/bash

echo "创建一个完整的测试用例（带Steps）..."

curl -s -X POST http://localhost:7777/api/cases \
  -H "Content-Type: application/json" \
  -d '{
    "caseName": "完整用例测试",
    "method": "POST",
    "url": "${base_url}/post",
    "headers": "{\"Content-Type\": \"application/json\"}",
    "body": "{}",
    "assertionScript": "status_code == 200",
    "module": {"id": 1},
    "steps": [
      {
        "stepName": "Step 1: 测试步骤",
        "stepOrder": 1,
        "method": "GET",
        "url": "${base_url}/get",
        "headers": "{}",
        "body": "",
        "assertionScript": "status_code == 200"
      }
    ]
  }' | python3 -m json.tool

echo -e "\n查看所有测试用例："
curl -s http://localhost:7777/api/cases | python3 -m json.tool
