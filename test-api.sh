#!/bin/bash

echo "=== 测试图谱Agent接口修复效果 ==="
echo

BASE_URL="http://localhost:58220"

echo "1. 测试解析功能（不依赖外部服务）..."
curl -X POST "$BASE_URL/api/analysis/test-extract" \
  -H "Content-Type: application/json" \
  -d '{"message": "测试消息"}' \
  -w "\n状态码: %{http_code}\n" \
  -s
echo

echo "2. 获取接口使用示例..."
curl -X GET "$BASE_URL/api/analysis/examples" \
  -H "Content-Type: application/json" \
  -w "\n状态码: %{http_code}\n" \
  -s
echo

echo "3. 测试同步聊天和提取（推荐方式）..."
curl -X POST "$BASE_URL/api/analysis/chat-and-extract-sync" \
  -H "Content-Type: application/json" \
  -d '{"message": "简单测试查询"}' \
  -w "\n状态码: %{http_code}\n" \
  -s \
  --max-time 60
echo

echo "4. 测试异步聊天和提取（原始方式）..."
curl -X POST "$BASE_URL/api/analysis/chat-and-extract" \
  -H "Content-Type: application/json" \
  -d '{"message": "简单测试查询"}' \
  -w "\n状态码: %{http_code}\n" \
  -s \
  --max-time 60
echo

echo "=== 测试完成 ==="