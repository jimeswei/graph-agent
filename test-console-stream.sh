#!/bin/bash

echo "████████████████████████████████████████████████████████████████████████████████"
echo "                        🎯 控制台流式输出测试"
echo "████████████████████████████████████████████████████████████████████████████████"
echo
echo "这个测试将演示控制台流式输出效果："
echo
echo "📺 控制台显示效果："
echo "   ┌─ 🚀 开始MCP工具调用分析"
echo "   ├─ 📝 输入消息: 你的查询"
echo "   ├─ ⏰ 开始时间: 当前时间"
echo "   ├─ 🌐 远程服务: MCP配置信息"
echo "   ├─ 🔄 开始接收远程数据流..."
echo "   ├─ 📦 [0001] data:{...} (每个数据块)"
echo "   │   ├─ 🤖 Agent: coordinator | 🏷️ Type: null | 📋 Fields: [...]"
echo "   │   └─ 💬 Content: \"内容文本\""
echo "   ├─ 📦 [0002] data:{...}"
echo "   │   ├─ 🤖 Agent: coordinator | 🏷️ Type: null | 📋 Fields: [...]"
echo "   │   └─ 🛠️ 工具调用: Agent=xxx, Type=xxx"
echo "   ├─ 📦 [0003] data:{...}"
echo "   │   └─ ⚡ 非Coordinator Agent: researcher"
echo "   ├─ ✅ 远程数据流接收完成"
echo "   ├─ ⏱️ 总耗时: xxxms"
echo "   └─ 🎉 MCP工具调用分析完成！"
echo
echo "🔥 特色功能："
echo "   • 实时流式输出到控制台"
echo "   • 美观的emoji图标显示"
echo "   • 清晰的层级结构"
echo "   • 实时显示处理进度"
echo "   • 突出显示重要信息"
echo "   • 自动格式化输出"
echo
echo "████████████████████████████████████████████████████████████████████████████████"

BASE_URL="http://localhost:58220"

# 检查应用是否运行
if ! curl -s "$BASE_URL/api/analysis/examples" > /dev/null 2>&1; then
    echo "❌ 应用没有运行，请先启动应用："
    echo "   mvn spring-boot:run"
    echo
    echo "然后再次运行此脚本"
    exit 1
fi

echo "✅ 应用正在运行，开始测试控制台流式输出..."
echo
echo "📌 注意事项："
echo "   1. 请观察运行应用的控制台窗口"
echo "   2. 你会看到实时的流式输出效果"
echo "   3. 每个数据块都会立即显示在控制台"
echo "   4. 处理过程会以美观的格式实时展示"
echo
echo "🚀 开始调用控制台流式输出接口..."
echo "████████████████████████████████████████████████████████████████████████████████"

# 调用控制台流式输出接口
curl -X POST "$BASE_URL/api/analysis/extract-mcp-tool-results-console" \
  -H "Content-Type: application/json" \
  -H "Accept: text/event-stream" \
  -d '{"message": "分析刘德华的和周杰伦的共同好友"}' \
  --no-buffer \
  --max-time 300 > /dev/null 2>&1 &

CURL_PID=$!

echo "🔄 流式调用已启动 (PID: $CURL_PID)"
echo
echo "████████████████████████████████████████████████████████████████████████████████"
echo "                   📺 现在请观察应用控制台窗口"
echo "████████████████████████████████████████████████████████████████████████████████"
echo
echo "你应该会看到类似以下的实时输出："
echo
echo "════════════════════════════════════════════════════════════════════════════════"
echo "🚀 开始MCP工具调用分析"
echo "════════════════════════════════════════════════════════════════════════════════"
echo "📝 输入消息: 分析刘德华的和周杰伦的共同好友"
echo "⏰ 开始时间: $(date)"
echo "🌐 远程服务: MCP配置信息"
echo "════════════════════════════════════════════════════════════════════════════════"
echo "🔄 开始接收远程数据流..."
echo "📦 [0001] data:{\"thread_id\": \"xxx\", \"agent\": \"coordinator\", \"role\": \"assistant\"}"
echo "   ├─ 🤖 Agent: coordinator | 🏷️ Type: null | 📋 Fields: [thread_id, agent, role]"
echo "📦 [0002] data:{\"thread_id\": \"xxx\", \"agent\": \"coordinator\", \"content\": \"根据\"}"
echo "   ├─ 🤖 Agent: coordinator | 🏷️ Type: null | 📋 Fields: [thread_id, agent, content]"
echo "   └─ 💬 Content: \"根据\""
echo "📦 [0003] data:{\"thread_id\": \"xxx\", \"agent\": \"coordinator\", \"content\": \"知识\"}"
echo "   ├─ 🤖 Agent: coordinator | 🏷️ Type: null | 📋 Fields: [thread_id, agent, content]"
echo "   └─ 💬 Content: \"知识\""
echo "... (更多数据块) ..."
echo "════════════════════════════════════════════════════════════════════════════════"
echo "✅ 远程数据流接收完成"
echo "⏱️ 总耗时: xxxms"
echo "════════════════════════════════════════════════════════════════════════════════"
echo "🎉 MCP工具调用分析完成！"
echo "════════════════════════════════════════════════════════════════════════════════"
echo
echo "💡 如果你看到上述格式的输出，说明控制台流式输出功能正常工作！"
echo
echo "按 Ctrl+C 可以停止测试"
echo "████████████████████████████████████████████████████████████████████████████████"

# 等待curl进程完成
wait $CURL_PID

echo
echo "████████████████████████████████████████████████████████████████████████████████"
echo "                           🎉 测试完成！"
echo "████████████████████████████████████████████████████████████████████████████████"
echo
echo "✅ 如果你在应用控制台看到了美观的流式输出，说明功能正常！"
echo
echo "🎯 控制台流式输出的特点："
echo "   • 实时显示每个数据块"
echo "   • 美观的emoji图标"
echo "   • 清晰的层级结构"
echo "   • 突出显示重要信息"
echo "   • 自动格式化输出"
echo
echo "🚀 接口地址: POST $BASE_URL/api/analysis/extract-mcp-tool-results-console"
echo "📝 请求格式: {\"message\": \"你的查询\"}"
echo "📺 输出位置: 应用控制台窗口"