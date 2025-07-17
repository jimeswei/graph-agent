# 控制台和文件同时日志输出配置

## 🎯 功能说明

这个配置确保MCP工具调用分析的日志会**同时实时输出到控制台和文件**，让你可以：

- 在控制台实时查看处理过程
- 在日志文件中保存完整的处理历史
- 支持日志文件滚动，避免文件过大

## 📁 文件结构

```
├── src/main/resources/
│   ├── application.yml          # 基础日志配置
│   └── logback-spring.xml       # 详细日志配置
├── logs/
│   └── mcp-tool-analysis.log    # 主日志文件
├── test-console-output.sh       # 测试控制台输出脚本
└── monitor-logs.sh              # 实时监控日志脚本
```

## 🚀 使用方法

### 1. 启动应用
```bash
mvn spring-boot:run
```

### 2. 监控日志（可选）
在另一个终端窗口运行：
```bash
./monitor-logs.sh
```

### 3. 测试功能
```bash
./test-console-output.sh
```

## 📊 日志输出格式

### 控制台和文件同时输出
```
2025-07-16 10:30:45.123 [http-nio-58220-exec-1] INFO  c.d.a.controller.ResponseAnalysisController - ==================== 开始流式聊天并提取MCP工具调用结果 ====================
2025-07-16 10:30:45.124 [http-nio-58220-exec-1] INFO  c.d.a.controller.ResponseAnalysisController - 输入消息: 分析刘德华的和周杰伦的共同好友
2025-07-16 10:30:45.125 [http-nio-58220-exec-1] INFO  c.d.a.controller.ResponseAnalysisController - 收到远程响应块 [0]: data:{"thread_id": "xxx", "agent": "coordinator"}
2025-07-16 10:30:45.126 [http-nio-58220-exec-1] INFO  c.d.a.controller.ResponseAnalysisController - 解析块 [0] 成功: agent=coordinator, type=null, 字段=[thread_id, agent, role]
2025-07-16 10:30:45.127 [http-nio-58220-exec-1] INFO  c.d.a.controller.ResponseAnalysisController - 发现内容块 [198]: content="根据"
```

### 同时的System.out输出
```
=== 开始MCP工具调用分析 ===
消息: 分析刘德华的和周杰伦的共同好友
开始时间: Wed Jul 16 10:30:45 CST 2025
>>> 块[0]: data:{"thread_id": "xxx", "agent": "coordinator", "role": "assistant"}
  >> 解析[0]: agent=coordinator, type=null, 字段=[thread_id, agent, role]
>>> 块[198]: data:{"thread_id": "xxx", "agent": "coordinator", "content": "根据"}
  >> 解析[198]: agent=coordinator, type=null, 字段=[thread_id, agent, content]
  >>> 内容[198]: "根据"
```

## 🔧 配置详情

### application.yml
```yaml
logging:
  level:
    com.datacent.agent: INFO
    root: WARN
  console:
    enabled: true
    immediateFlush: true
  file:
    name: logs/mcp-tool-analysis.log_bak
    immediateFlush: true
```

### logback-spring.xml
- **控制台输出**: 立即刷新，UTF-8编码
- **文件输出**: 立即刷新，UTF-8编码，支持滚动
- **滚动策略**: 最大100MB，保留5个历史文件

## 📈 监控命令

### 实时查看日志文件
```bash
tail -f logs/mcp-tool-analysis.log_bak
```

### 查看日志文件大小
```bash
du -h logs/mcp-tool-analysis.log_bak
```

### 查看日志文件行数
```bash
wc -l logs/mcp-tool-analysis.log_bak
```

### 搜索特定内容
```bash
grep "工具调用" logs/mcp-tool-analysis.log_bak
grep "内容块" logs/mcp-tool-analysis.log_bak
```

## 🎯 输出特点

1. **实时性**: 所有日志立即输出，不缓存
2. **双重输出**: 同时输出到控制台和文件
3. **格式化**: 统一的时间戳和日志格式
4. **突出显示**: 不同类型的信息有不同的标识
5. **持久化**: 日志文件保存完整历史记录

## 🚨 注意事项

1. 确保有足够的磁盘空间存储日志文件
2. 日志文件会自动滚动，不会无限增长
3. 可以通过修改logback-spring.xml调整日志级别和格式
4. 如果需要禁用控制台输出，可以在logback-spring.xml中移除CONSOLE appender

## 🔍 故障排除

### 日志不显示
- 检查应用是否正常启动
- 确认日志级别设置正确
- 查看是否有权限问题

### 日志文件不创建
- 检查logs目录是否存在
- 确认应用有写入权限
- 查看应用启动日志是否有错误

### 控制台输出乱码
- 确认终端支持UTF-8编码
- 检查logback-spring.xml中的charset设置