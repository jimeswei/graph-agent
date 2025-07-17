package com.datacent.agent.controller;

import com.alibaba.fastjson2.JSONObject;
import com.alibaba.fastjson2.JSONArray;
import com.datacent.agent.service.ChatStreamService;
import com.datacent.agent.util.SSEResponseParser;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * 响应分析控制器
 * 演示如何使用SSE解析工具处理流式响应
 */
@Slf4j
@RestController
@RequestMapping("/api/analysis")
@CrossOrigin(origins = "*")
public class ResponseAnalysisController {

    @Autowired
    private ChatStreamService chatStreamService;

    /**
     * 流式聊天并实时解析content
     * 在流式响应的同时提取content信息
     * 
     * @param request 聊天请求
     * @return 流式响应数据
     */
    @PostMapping(value = "/stream-with-content", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> streamWithContentExtraction(@RequestBody JSONObject request) {
        String message = request.getString("message");
        
        if (message == null || message.trim().isEmpty()) {
            return Flux.just("data: {\"error\":\"消息内容不能为空\"}\n\n");
        }
        
        // 构建完整请求
        JSONObject fullRequest = buildFullRequest(message);
        
        log.info("开始流式聊天并解析content，消息: {}", message);
        
        // 收集所有流式数据
        StringBuilder sseDataBuilder = new StringBuilder();
        
        return chatStreamService.chatStream(fullRequest)
                .doOnNext(chunk -> {
                    // 收集每个数据块
                    sseDataBuilder.append(chunk).append("\n");
                })
                .doOnComplete(() -> {
                    // 流式完成后解析content
                    String fullSSEData = sseDataBuilder.toString();
                    String extractedContent = SSEResponseParser.extractContent(fullSSEData);
                    List<String> contentList = SSEResponseParser.extractContentList(fullSSEData);
                    SSEResponseParser.SSEInfo sseInfo = SSEResponseParser.extractSSEInfo(fullSSEData);
                    
                    log.info("流式响应完成，提取的内容长度: {}, 片段数: {}, 基本信息: {}", 
                            extractedContent.length(), contentList.size(), sseInfo);
                })
                .doOnError(error -> {
                    log.error("流式响应出错", error);
                });
    }
    
    /**
     * 完整聊天并返回解析后的content
     * 等待流式完成后返回完整的content内容
     * 
     * @param request 聊天请求
     * @return 解析后的content信息
     */
    @PostMapping("/chat-and-extract")
    public Mono<JSONObject> chatAndExtractContent(@RequestBody JSONObject request) {
        try {
            String message = request.getString("message");
            
            if (message == null || message.trim().isEmpty()) {
                return Mono.just(createErrorResponse("消息内容不能为空"));
            }
            
            // 构建完整请求
            JSONObject fullRequest = buildFullRequest(message);
            
            log.info("开始聊天并提取content，消息: {}", message);
            log.debug("构建的请求: {}", fullRequest.toJSONString());
            
            return chatStreamService.chatStream(fullRequest)
                    .timeout(java.time.Duration.ofMinutes(3)) // 添加3分钟超时
                    .collectList()
                    .map(chunks -> {
                        try {
                            log.info("收集到 {} 个数据块", chunks.size());
                            
                            if (chunks.isEmpty()) {
                                log.warn("未收到任何流式数据");
                                return createErrorResponse("未收到流式响应数据");
                            }
                            
                            // 将所有chunk合并
                            String fullSSEData = String.join("\n", chunks);
                            
                            log.debug("收集到的SSE数据长度: {}", fullSSEData.length());
                            log.debug("SSE数据前200字符: {}", 
                                    fullSSEData.length() > 200 ? fullSSEData.substring(0, 200) : fullSSEData);
                            
                            if (fullSSEData.trim().isEmpty()) {
                                log.warn("收集到的SSE数据为空");
                                return createErrorResponse("收集到的流式数据为空");
                            }
                            
                            // 解析content
                            String extractedContent = SSEResponseParser.extractContent(fullSSEData);
                            List<String> contentList = SSEResponseParser.extractContentList(fullSSEData);
                            SSEResponseParser.SSEInfo sseInfo = SSEResponseParser.extractSSEInfo(fullSSEData);
                            
                            // 构建响应
                            JSONObject response = new JSONObject();
                            response.put("success", true);
                            response.put("message", "内容提取成功");
                            response.put("thread_id", sseInfo.getThreadId() != null ? sseInfo.getThreadId() : "unknown");
                            response.put("agent", sseInfo.getAgent() != null ? sseInfo.getAgent() : "unknown");
                            response.put("role", sseInfo.getRole() != null ? sseInfo.getRole() : "unknown");
                            response.put("content", extractedContent != null ? extractedContent : "");
                            response.put("content_length", extractedContent != null ? extractedContent.length() : 0);
                            response.put("content_fragments", contentList.size());
                            response.put("content_fragments_list", contentList);
                            response.put("chunks_received", chunks.size());
                            response.put("raw_chunks", chunks);
                            response.put("raw_sse_data", fullSSEData);
                            response.put("timestamp", System.currentTimeMillis());
                            
                            log.info("content提取完成，内容长度: {}, 片段数: {}, 数据块数: {}", 
                                    extractedContent != null ? extractedContent.length() : 0, 
                                    contentList.size(), chunks.size());
                            
                            return response;
                            
                        } catch (Exception e) {
                            log.error("解析SSE数据时发生错误，数据块数: {}", chunks.size(), e);
                            return createErrorResponse("解析SSE数据失败: " + e.getClass().getSimpleName() + " - " + e.getMessage());
                        }
                    })
                    .doOnError(error -> {
                        log.error("流式请求失败: {}", error.getClass().getSimpleName(), error);
                    })
                    .onErrorResume(error -> {
                        log.error("聊天和提取失败", error);
                        String errorMsg = String.format("聊天和提取失败: %s - %s", 
                                error.getClass().getSimpleName(), error.getMessage());
                        return Mono.just(createErrorResponse(errorMsg));
                    });
                    
        } catch (Exception e) {
            log.error("请求处理失败", e);
            return Mono.just(createErrorResponse("请求处理失败: " + e.getMessage()));
        }
    }
    
    /**
     * 提取MCP工具调用结果的专门接口
     * 分析JSON数据，只提取agent调用MCP工具返回的那部分值
     * 
     * @param request 聊天请求
     * @return MCP工具调用的结果
     */
    @PostMapping("/extract-mcp-tool-results")
    public JSONObject extractMcpToolResults(@RequestBody JSONObject request) {
        String message = request.getString("message");
        
        if (message == null || message.trim().isEmpty()) {
            return createErrorResponse("消息内容不能为空");
        }
        
        try {
            // 构建完整请求
            JSONObject fullRequest = buildFullRequest(message);
            
            log.info("开始聊天并提取MCP工具调用结果，消息: {}", message);
            
            // 使用同步方式获取数据
            List<String> chunks = chatStreamService.chatStream(fullRequest)
                    .timeout(java.time.Duration.ofMinutes(3))
                    .collectList()
                    .block();
            
            if (chunks == null || chunks.isEmpty()) {
                return createErrorResponse("未收到流式响应数据");
            }
            
            // 提取包含tool_call_id的数据块和工具调用信息
            JSONArray mcpToolResults = new JSONArray();
            JSONArray toolCallNames = new JSONArray();
            int validResults = 0;
            
            for (String chunk : chunks) {
                // 处理数据块 - 支持两种格式：SSE格式("data:"开头)和纯JSON格式
                String jsonStr;
                if (chunk.startsWith("data:")) {
                    jsonStr = chunk.substring(5).trim();
                } else {
                    jsonStr = chunk.trim();
                }
                
                try {
                    if (!jsonStr.isEmpty()) {
                        JSONObject jsonData = com.alibaba.fastjson2.JSON.parseObject(jsonStr);
                        
                        // 检查是否包含tool_call_id
                        String toolCallId = jsonData.getString("tool_call_id");
                        if (toolCallId != null && !toolCallId.trim().isEmpty()) {
                            validResults++;
                            
                            // 输出包含tool_call_id的完整JSON数据块
                            mcpToolResults.add(jsonData);
                            
                            log.info("✅ 找到tool_call_id数据块 [{}]: {}", validResults, jsonData.toJSONString());
                        }
                        
                        // 检查是否包含tool_calls数组（工具调用定义）
                        JSONArray toolCalls = jsonData.getJSONArray("tool_calls");
                        if (toolCalls != null && !toolCalls.isEmpty()) {
                            for (int i = 0; i < toolCalls.size(); i++) {
                                JSONObject toolCall = toolCalls.getJSONObject(i);
                                if (toolCall != null) {
                                    String name = toolCall.getString("name");
                                    String id = toolCall.getString("id");
                                    String type = toolCall.getString("type");
                                    Object args = toolCall.get("args");
                                    Integer index = toolCall.getInteger("index");
                                    
                                    if (name != null && !name.trim().isEmpty()) {
                                        JSONObject toolCallInfo = new JSONObject();
                                        toolCallInfo.put("name", name);
                                        toolCallInfo.put("id", id);
                                        toolCallInfo.put("type", type);
                                        toolCallInfo.put("args", args);
                                        toolCallInfo.put("index", index);
                                        
                                        toolCallNames.add(toolCallInfo);
                                        
                                        log.info("✅ 找到工具调用: name={}, id={}, type={}", name, id, type);
                                    }
                                }
                            }
                        }
                    }
                } catch (Exception e) {
                    // 跳过无效数据块
                }
            }
            
            // 构建响应 - 包含tool_call_id的数据块和工具调用信息
            JSONObject response = new JSONObject();
            response.put("success", true);
            response.put("message", String.format("提取到%d个包含tool_call_id的数据块，%d个工具调用", validResults, toolCallNames.size()));
            response.put("mcp_tool_results", mcpToolResults);
            response.put("tool_call_names", toolCallNames);
            response.put("results_count", validResults);
            response.put("tool_calls_count", toolCallNames.size());
            response.put("timestamp", System.currentTimeMillis());
            
            log.info("提取完成，共找到{}个包含tool_call_id的数据块，{}个工具调用", validResults, toolCallNames.size());
            
            return response;
            
        } catch (Exception e) {
            log.error("MCP工具调用结果提取失败", e);
            return createErrorResponse("MCP工具调用结果提取失败: " + e.getClass().getSimpleName() + " - " + e.getMessage());
        }
    }
    
    /**
     * 从内容中提取工具名称
     */
    private String extractToolName(String content) {
        if (content == null) return null;
        
        String[] tools = {"query_celebrity_relationships", "mutual_friend_between_stars", 
                         "similarity_between_stars", "relation_chain_between_stars", 
                         "most_recent_common_ancestor", "dream_team_common_works", 
                         "contextualized_content_detail_stars"};
        
        for (String tool : tools) {
            if (content.contains(tool)) {
                return tool;
            }
        }
        return null;
    }
    
    /**
     * 获取JSONObject中最常见的键
     */
    private String getMostCommonKey(JSONObject stats) {
        if (stats.isEmpty()) return null;
        
        String maxKey = null;
        int maxCount = 0;
        
        for (String key : stats.keySet()) {
            int count = stats.getIntValue(key);
            if (count > maxCount) {
                maxCount = count;
                maxKey = key;
            }
        }
        
        return maxKey;
    }
    
    /**
     * 构建完整的请求体
     */
    private JSONObject buildFullRequest(String message) {
        JSONObject request = new JSONObject();
        
        // 构建messages数组
        JSONArray messages = new JSONArray();
        JSONObject messageObj = new JSONObject();
        messageObj.put("role", "user");
        messageObj.put("content", message);
        messages.add(messageObj);
        request.put("messages", messages);
        
        // 设置thread_id（可变参数）
        request.put("thread_id", "analysis_" + System.currentTimeMillis());
        
        // 固定参数（根据文档要求不能变）
        request.put("resources", new JSONArray());
        request.put("auto_accepted_plan", true);
        request.put("enable_deep_thinking", false);
        request.put("enable_background_investigation", false);
        request.put("max_plan_iterations", 1);
        request.put("max_step_num", 5);
        request.put("max_search_results", 5);
        request.put("report_style", "academic");
        
        // MCP配置信息（根据文档固定格式）
        JSONObject mcpSettings = new JSONObject();
        JSONObject servers = new JSONObject();
        
        // knowledge-graph-general-query-service
        JSONObject generalQueryService = new JSONObject();
        generalQueryService.put("name", "knowledge-graph-general-query-service");
        generalQueryService.put("transport", "sse");
        generalQueryService.put("env", null);
        generalQueryService.put("url", "http://192.168.3.78:5823/sse");
        JSONArray generalTools = new JSONArray();
        generalTools.add("query_celebrity_relationships");
        generalQueryService.put("enabled_tools", generalTools);
        JSONArray generalAgents = new JSONArray();
        generalAgents.add("researcher");
        generalAgents.add("coder");
        generalQueryService.put("add_to_agents", generalAgents);
        servers.put("knowledge-graph-general-query-service", generalQueryService);
        
        // knowledge-graph-algorithrm-service
        JSONObject algorithmService = new JSONObject();
        algorithmService.put("name", "knowledge-graph-algorithrm-service");
        algorithmService.put("transport", "sse");
        algorithmService.put("env", null);
        algorithmService.put("url", "http://192.168.3.78:5821/sse");
        JSONArray algorithmTools = new JSONArray();
        algorithmTools.add("most_recent_common_ancestor");
        algorithmTools.add("relation_chain_between_stars");
        algorithmTools.add("similarity_between_stars");
        algorithmTools.add("mutual_friend_between_stars");
        algorithmTools.add("dream_team_common_works");
        algorithmService.put("enabled_tools", algorithmTools);
        JSONArray algorithmAgents = new JSONArray();
        algorithmAgents.add("researcher");
        algorithmAgents.add("coder");
        algorithmService.put("add_to_agents", algorithmAgents);
        servers.put("knowledge-graph-algorithrm-service", algorithmService);
        
        // knowledge-content-detail-service
        JSONObject contentDetailService = new JSONObject();
        contentDetailService.put("name", "knowledge-content-detail-service");
        contentDetailService.put("transport", "sse");
        contentDetailService.put("env", null);
        contentDetailService.put("url", "http://192.168.3.78:5822/sse");
        JSONArray contentTools = new JSONArray();
        contentTools.add("contextualized_content_detail_stars");
        contentDetailService.put("enabled_tools", contentTools);
        JSONArray contentAgents = new JSONArray();
        contentAgents.add("researcher");
        contentAgents.add("coder");
        contentDetailService.put("add_to_agents", contentAgents);
        servers.put("knowledge-content-detail-service", contentDetailService);
        
        mcpSettings.put("servers", servers);
        request.put("mcp_settings", mcpSettings);
        
        return request;
    }
    
    /**
     * 创建错误响应
     */
    private JSONObject createErrorResponse(String message) {
        JSONObject response = new JSONObject();
        response.put("success", false);
        response.put("message", message);
        response.put("timestamp", System.currentTimeMillis());
        return response;
    }
    

}