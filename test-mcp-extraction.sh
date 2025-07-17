#!/bin/bash

echo "=== 测试改进的MCP工具调用提取接口 ==="
echo

BASE_URL="http://localhost:58220"

echo "正在调用改进的MCP工具调用提取接口..."
curl -X POST "$BASE_URL/api/analysis/extract-mcp-tool-results" \
  -H "Content-Type: application/json" \
  -d '{"message": "分析刘德华的和周杰伦的共同好友"}' \
  --max-time 180 \
  -s | jq '{
    success: .success,
    message: .message,
    mcp_tool_calls_count: .mcp_tool_calls_count,
    tool_results_count: .tool_results_count,
    agent_actions_count: .agent_actions_count,
    coordinator_content_length: .coordinator_content_length,
    debug_analysis: {
      agent_statistics: .debug_analysis.agent_statistics,
      field_statistics: .debug_analysis.field_statistics,
      data_characteristics: .debug_analysis.data_characteristics,
      total_debug_entries: .debug_analysis.total_debug_entries
    }
  }'

echo
echo "=== 测试完成 ==="