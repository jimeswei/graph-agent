# JSON数据块输出格式说明

## 新增的JSON输出功能

我已经为您修改了接口，现在返回的数据包含完整的数据块JSON结构，方便您查看和调试。

## 返回数据结构

### 1. 所有接口都新增的字段

```json
{
  "content_fragments_list": ["根据", "知识", "图谱数据的", "综合分析", ...],
  "raw_chunks": ["data:{...}", "data:{...}", ...],
  "raw_sse_data": "完整的SSE原始数据"
}
```

### 2. 专门的数据块详细展示接口

`POST /api/analysis/chat-with-chunks-json` 提供最详细的JSON结构：

```json
{
  "success": true,
  "message": "数据块详细解析成功",
  "input_message": "用户输入的消息",
  
  // 基本信息
  "thread_id": "分析会话ID",
  "agent": "coordinator",
  "role": "assistant",
  
  // 内容信息
  "final_content": "拼接后的完整内容",
  "content_length": 1277,
  "content_fragments_count": 361,
  "content_fragments_list": ["根据", "知识", "图谱数据的", ...],
  
  // 数据块统计
  "total_chunks": 450,
  "content_chunks_count": 361,
  
  // 详细的数据块解析
  "parsed_chunks": [
    {
      "index": 0,
      "raw_data": "data:{\"thread_id\": \"xxx\", \"agent\": \"coordinator\", ...}",
      "length": 120,
      "parsed_json": {
        "thread_id": "xxx",
        "agent": "coordinator",
        "id": "run-xxx",
        "role": "assistant"
      }
    },
    {
      "index": 198,
      "raw_data": "data:{\"thread_id\": \"xxx\", \"content\": \"根据\", ...}",
      "length": 150,
      "parsed_json": {
        "thread_id": "xxx",
        "agent": "coordinator",
        "content": "根据"
      }
    }
  ],
  
  // 只包含content的数据块
  "content_chunks": [
    {
      "index": 198,
      "content": "根据",
      "thread_id": "xxx",
      "agent": "coordinator"
    },
    {
      "index": 199,
      "content": "知识",
      "thread_id": "xxx",
      "agent": "coordinator"
    }
  ],
  
  // 原始数据
  "raw_chunks_array": ["data:{...}", "data:{...}", ...],
  "raw_sse_data": "完整的SSE格式数据",
  
  "timestamp": 1642234567890
}
```

## 测试命令

### 1. 查看详细JSON结构（推荐）
```bash
curl -X POST http://localhost:58220/api/analysis/chat-with-chunks-json \
  -H "Content-Type: application/json" \
  -d '{"message": "分析下周杰伦和刘德华共同有哪些共同好友？"}' \
  | jq '.'
```

### 2. 查看简化的JSON结构
```bash
curl -X POST http://localhost:58220/api/analysis/chat-and-extract-sync \
  -H "Content-Type: application/json" \
  -d '{"message": "分析下周杰伦和刘德华共同有哪些共同好友？"}' \
  | jq '.'
```

### 3. 只查看content相关数据
```bash
curl -X POST http://localhost:58220/api/analysis/chat-with-chunks-json \
  -H "Content-Type: application/json" \
  -d '{"message": "测试查询"}' \
  | jq '.content_chunks'
```

### 4. 查看原始数据块
```bash
curl -X POST http://localhost:58220/api/analysis/chat-with-chunks-json \
  -H "Content-Type: application/json" \
  -d '{"message": "测试查询"}' \
  | jq '.raw_chunks_array'
```

## 数据块类型说明

1. **元数据块**：包含thread_id、agent、role但不含content
2. **内容块**：包含content字段的数据块
3. **其他块**：可能包含状态信息、错误信息等

## 使用建议

1. **调试时**：使用 `/chat-with-chunks-json` 查看详细结构
2. **生产环境**：使用 `/chat-and-extract-sync` 获取处理后的数据
3. **错误排查**：检查 `parsed_chunks` 中的 `parse_error` 字段
4. **性能分析**：查看 `total_chunks` 和处理时间的关系