#!/bin/bash

echo "======================================================================"
echo "              测试控制台和文件同时实时日志输出效果"
echo "======================================================================"
echo
echo "这个测试将演示以下输出效果："
echo
echo "1. 启动应用时，日志会同时实时输出到控制台和文件"
echo "2. 每个远程响应块都会立即在控制台和文件中显示"
echo "3. 解析结果会实时显示在控制台和文件中"
echo "4. 发现的内容、工具调用、agent动作都会突出显示"
echo "5. 所有处理过程都会持续不断地同时输出到控制台和文件"
echo "6. 日志文件位置: logs/mcp-tool-analysis.log"
echo
echo "控制台输出格式："
echo "  >>> 块[索引]: 原始数据"
echo "  >> 解析[索引]: agent=xxx, type=xxx, 字段=[...]"
echo "  >>> 内容[索引]: \"内容文本\""
echo "  !!! 工具调用[索引]: agent=xxx, type=xxx"
echo "  *** 非coordinator agent[索引]: agent名称"
echo "  XXX 解析错误[索引]: 错误信息"
echo "  ??? 非SSE格式[索引]: 数据"
echo "  === 状态信息 ==="
echo
echo "现在开始测试..."
echo "======================================================================"

# 启动应用（如果没有运行的话）
BASE_URL="http://localhost:58220"

# 检查应用是否运行
if ! curl -s "$BASE_URL/api/analysis/examples" > /dev/null 2>&1; then
    echo "应用没有运行，请先启动应用："
    echo "mvn spring-boot:run"
    echo
    echo "然后再次运行此脚本"
    exit 1
fi

echo "应用正在运行，开始测试..."
echo
echo "注意：同时观察控制台输出（应用运行的终端）"
echo "你会看到日志不停地实时打印到控制台"
echo
echo "======================================================================"
echo "开始调用流式接口..."
echo "======================================================================"

# 在后台启动curl，这样我们可以看到实时输出
curl -X POST "$BASE_URL/api/analysis/extract-mcp-tool-results-stream" \
  -H "Content-Type: application/json" \
  -H "Accept: text/event-stream" \
  -d '{"message": "分析刘德华的和周杰伦的共同好友"}' \
  --no-buffer \
  --max-time 300 &

CURL_PID=$!

echo "流式调用已启动 (PID: $CURL_PID)"
echo
echo "======================================================================"
echo "现在请观察运行应用的控制台窗口，你会看到："
echo "======================================================================"
echo "1. 大量的日志信息不停地实时打印到控制台"
echo "2. 每个响应块都会立即显示在控制台"
echo "3. 解析过程的详细信息实时显示"
echo "4. 发现的内容、工具调用等会突出显示"
echo "5. 所有处理步骤都会持续输出到控制台"
echo
echo "同时，你可以通过以下命令实时查看日志文件："
echo "  tail -f logs/mcp-tool-analysis.log"
echo
echo "按 Ctrl+C 可以停止测试"
echo "======================================================================"

# 等待curl进程完成
wait $CURL_PID

echo
echo "======================================================================"
echo "测试完成！"
echo "======================================================================"
echo
echo "如果你看到了大量的控制台输出，说明配置成功！"
echo "控制台应该显示了："
echo "- 每个远程响应块的实时输出"
echo "- 解析过程的详细信息"
echo "- 发现的内容、工具调用等的突出显示"
echo "- 所有处理步骤的持续输出"
echo
echo "同时检查日志文件 logs/mcp-tool-analysis.log："
echo "- 所有控制台输出都会同时写入日志文件"
echo "- 日志文件支持滚动，最大100MB，保留5个历史文件"
echo "- 可以使用 tail -f logs/mcp-tool-analysis.log 实时查看"
echo
echo "查看日志文件大小："
if [ -f "logs/mcp-tool-analysis.log" ]; then
    echo "日志文件大小: $(du -h logs/mcp-tool-analysis.log_bak | cut -f1)"
    echo "日志文件行数: $(wc -l < logs/mcp-tool-analysis.log_bak)"
else
    echo "日志文件尚未创建"
fi