# 修复chat-and-extract接口报错

## 问题分析

原始的`chat-and-extract`方法可能遇到以下问题：

1. **流式数据收集问题**：`collectList()`可能因为流式数据格式或网络问题失败
2. **SSE解析异常**：解析Server-Sent Events格式数据时可能出现JSON格式错误
3. **MCP配置不完整**：简化的MCP设置可能导致外部API调用失败
4. **响应式编程错误处理**：Mono/Flux的错误处理不够完善

## 修复措施

### 1. 增强错误处理
```java
.map(chunks -> {
    try {
        // 添加try-catch包裹所有解析逻辑
        String fullSSEData = String.join("\n", chunks);
        log.debug("收集到的SSE数据长度: {}", fullSSEData.length());
        // ... 解析逻辑
        return response;
    } catch (Exception e) {
        log.error("解析SSE数据时发生错误", e);
        return createErrorResponse("解析SSE数据失败: " + e.getMessage());
    }
})
```

### 2. 完善MCP配置
```java
// 使用完整的MCP设置替换简化版本
fullRequest.put("mcp_settings", buildMcpSettings());
```

### 3. 添加详细日志
```java
.doOnError(error -> {
    log.error("流式请求失败", error);
})
```

### 4. 创建测试接口
提供了`POST /api/analysis/test-extract`接口用于测试解析功能，不依赖外部服务。

## 测试方法

### 1. 测试解析功能（不依赖外部服务）
```bash
curl -X POST http://localhost:58220/api/analysis/test-extract \
  -H "Content-Type: application/json" \
  -d '{"message": "测试消息"}'
```

### 2. 测试完整流程（需要外部服务）
```bash
curl -X POST http://localhost:58220/api/analysis/chat-and-extract \
  -H "Content-Type: application/json" \
  -d '{"message": "分析下周杰伦和刘德华共同有哪些共同好友？"}'
```

## 可能的错误原因

1. **外部服务不可用**：`http://192.168.3.78:48558`无法访问
2. **网络超时**：流式响应时间过长
3. **数据格式问题**：外部API返回的SSE格式与预期不符
4. **依赖问题**：WebFlux相关依赖配置问题

## 建议调试步骤

1. 先测试`/api/analysis/test-extract`确认解析逻辑正常
2. 检查外部服务`http://192.168.3.78:48558`是否可访问
3. 查看应用日志，特别关注错误信息
4. 如果仍有问题，可以尝试简化请求参数或使用原始的`/api/chat/stream`接口