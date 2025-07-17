#!/bin/bash

echo "=== 测试流式MCP工具调用提取接口（实时输出版本） ==="
echo

BASE_URL="http://localhost:58220"

echo "调用新的流式MCP工具调用提取接口..."
echo "URL: $BASE_URL/api/analysis/extract-mcp-tool-results-stream"
echo
echo "这将以SSE格式实时输出所有中间结果，包含："
echo "- 初始状态: event_type=status, status=init" 
echo "- 原始响应块: event_type=raw_chunk (每个远程响应块)"
echo "- 解析成功: event_type=parsed_chunk (解析出的字段信息)"
echo "- 内容块: event_type=content_chunk (包含content字段的块)"
echo "- 工具调用: event_type=tool_call_detected (发现MCP工具调用)"
echo "- 非coordinator agent: event_type=non_coordinator_agent (其他agent动作)"
echo "- 解析错误: event_type=parse_error (解析失败的块)"
echo "- 流完成: event_type=stream_complete (远程数据流接收完成)"
echo
echo "同时在服务器日志中实时输出："
echo "- 每个远程响应块的原始内容"
echo "- 每个块的解析结果 (agent, type, 字段)"
echo "- 发现的内容块内容"
echo "- 发现的工具调用和非coordinator agent"
echo "- 解析错误信息"
echo
echo "开始流式调用..."
echo "================================================"

# 使用tee同时输出到终端和文件
curl -X POST "$BASE_URL/api/analysis/extract-mcp-tool-results-stream" \
  -H "Content-Type: application/json" \
  -H "Accept: text/event-stream" \
  -d '{"message": "分析刘德华的和周杰伦的共同好友"}' \
  --no-buffer \
  --max-time 300 | tee stream_output.log

echo
echo "================================================"
echo "流式调用完成"
echo
echo "输出已同时保存到 stream_output.log 文件"
echo
echo "查看不同类型的事件："
echo "- 查看所有原始响应块: grep 'raw_chunk' stream_output.log"
echo "- 查看所有内容块: grep 'content_chunk' stream_output.log"
echo "- 查看工具调用: grep 'tool_call_detected' stream_output.log"
echo "- 查看非coordinator agent: grep 'non_coordinator_agent' stream_output.log"
echo "- 查看解析错误: grep 'parse_error' stream_output.log"
echo
echo "同时检查服务器日志以查看详细的处理信息。"