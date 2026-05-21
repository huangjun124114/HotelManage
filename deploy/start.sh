#!/bin/sh
set -e

# 确保数据目录存在
mkdir -p /app/data

# 启动后端服务（后台运行）
echo "=== Starting Backend (port 8081) ==="
java -jar /app/backend.jar \
  --server.port=8081 \
  --spring.datasource.url=jdbc:sqlite:/app/data/linxi.db \
  --spring.profiles.active=prod \
  > /app/backend.log 2>&1 &

BACKEND_PID=$!
echo "Backend PID: $BACKEND_PID"

# 等待后端启动
sleep 3

# 启动 Nginx（前台运行，作为容器主进程）
echo "=== Starting Nginx (port 80) ==="
nginx -g 'daemon off;'
