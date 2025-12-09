#!/bin/bash

# 后端启动脚本

echo "=== 启动后端服务 ==="
echo "端口: 7777"
echo ""

cd "$(dirname "$0")"

# 启动 Spring Boot 应用
mvn spring-boot:run
