#!/bin/bash

echo "======================================================================"
echo "                  测试改进的MCP工具调用提取逻辑"
echo "======================================================================"
echo
echo "这个测试将验证改进后的MCP工具调用提取功能："
echo
echo "🔍 改进的识别策略："
echo "   1. 详细的日志输出 - 每个数据块都会被记录"
echo "   2. 扩展的模式匹配 - 检查更多字段和条件"
echo "   3. 基于内容的检测 - 从content中识别工具名称"
echo "   4. 增强的调试信息 - 详细分析为什么提取为空"
echo
echo "🎯 新增的检测模式："
echo "   • 传统字段检测: tool_calls, tool_call, function_call, mcp_call"
echo "   • 类型检测: type包含tool/function/mcp"
echo "   • 内容检测: content包含工具名称"
echo "   • Agent检测: 非coordinator的agent动作"
echo
echo "📝 支持的工具名称："
echo "   • query_celebrity_relationships"
echo "   • mutual_friend_between_stars"
echo "   • similarity_between_stars"
echo "   • relation_chain_between_stars"
echo "   • most_recent_common_ancestor"
echo "   • dream_team_common_works"
echo "   • contextualized_content_detail_stars"
echo
echo "======================================================================"

BASE_URL="http://localhost:58220"

# 检查应用是否运行
if ! curl -s "$BASE_URL/api/analysis/examples" > /dev/null 2>&1; then
    echo "❌ 应用没有运行，请先启动应用："
    echo "   mvn spring-boot:run"
    echo
    echo "然后再次运行此脚本"
    exit 1
fi

echo "✅ 应用正在运行，开始测试改进的提取逻辑..."
echo
echo "🚀 调用改进的MCP工具调用提取接口..."
echo "======================================================================"

# 调用改进的接口
curl -X POST "$BASE_URL/api/analysis/extract-mcp-tool-results" \
  -H "Content-Type: application/json" \
  -d '{"message": "分析刘德华的和周杰伦的共同好友"}' \
  --max-time 180 | jq '{
    success: .success,
    message: .message,
    input_message: .input_message,
    statistics: {
      mcp_tool_calls_count: .mcp_tool_calls_count,
      tool_results_count: .tool_results_count,
      agent_actions_count: .agent_actions_count
    },
    debug_analysis: {
      agent_statistics: .debug_analysis.agent_statistics,
      field_statistics: .debug_analysis.field_statistics,
      data_characteristics: .debug_analysis.data_characteristics
    },
    mcp_tool_calls: .mcp_tool_calls,
    tool_results: .tool_results,
    agent_actions: .agent_actions
  }'

echo
echo "======================================================================"
echo "                        测试完成"
echo "======================================================================"
echo
echo "🔍 请检查以下内容："
echo "   1. 查看应用日志中的详细处理信息"
echo "   2. 检查 mcp_tool_calls_count 是否大于0"
echo "   3. 查看 debug_analysis 中的数据特征"
echo "   4. 检查是否有新的检测结果"
echo
echo "💡 如果仍然为空，请检查应用日志中的详细信息："
echo "   • 数据块的实际内容"
echo "   • agent和type的值"
echo "   • 字段统计信息"
echo "   • 内容检测结果"
echo
echo "📊 日志分析命令："
echo "   tail -f logs/mcp-tool-analysis.log | grep -E '(处理数据块|解析数据块|发现工具|发现非coordinator)'"