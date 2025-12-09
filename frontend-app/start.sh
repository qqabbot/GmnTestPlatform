#!/bin/bash

# 前端启动脚本

echo "=== 启动前端服务 ==="
echo "端口: 8888"
echo ""

cd "$(dirname "$0")"

# 检查 node_modules 是否存在
if [ ! -d "node_modules" ]; then
    echo "首次运行，正在安装依赖..."
    npm install --registry=https://registry.npmmirror.com
fi

# 启动开发服务器
npm run dev
