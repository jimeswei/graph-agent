package com.datacent.agent.service;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.datacent.agent.entity.AnalysisSession;
import com.datacent.agent.entity.McpToolResult;
import com.datacent.agent.entity.ToolCallName;
import com.datacent.agent.repository.AnalysisSessionRepository;
import com.datacent.agent.repository.McpToolResultRepository;
import com.datacent.agent.repository.ToolCallNameRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * MCP工具调用结果服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class McpToolResultService {
    
    private final McpToolResultRepository mcpToolResultRepository;
    private final ToolCallNameRepository toolCallNameRepository;
    private final AnalysisSessionRepository analysisSessionRepository;
    
    @Autowired
    private ChatStreamService chatStreamService;
    
    @Autowired 
    private ChatRequestBuilderService chatRequestBuilderService;
    
    /**
     * 获取流式数据并返回 - 纯流式版本
     * 从消息构建请求并获取流式响应数据
     */
    public Flux<String> extractMcpToolResultsStream(String message) {
        return extractMcpToolResultsStream(message, null);
    }
    
    /**
     * 获取流式数据并返回 - 纯流式版本（支持自定义thread_id）
     * 从消息构建请求并获取流式响应数据
     */
    public Flux<String> extractMcpToolResultsStream(String message, String threadId) {
        if (message == null || message.trim().isEmpty()) {
            return Flux.just("data: " + chatRequestBuilderService.createErrorResponse("消息内容不能为空").toJSONString() + "\n\n");
        }
        
        try {
            // 构建完整请求
            JSONObject fullRequest = chatRequestBuilderService.buildFullRequest(message, threadId);
            
            log.info("开始流式聊天，消息: {}", message);
            
            // 获取流式数据
            Flux<String> streamData = chatStreamService.chatStream(fullRequest)
                    .timeout(java.time.Duration.ofMinutes(3))
                    .doOnError(error -> log.error("获取流式数据失败", error))
                    .onErrorResume(throwable -> {
                        log.error("⚠️ 流式数据获取异常恢复: {}", throwable.getMessage());
                        return Flux.just("data: " + chatRequestBuilderService.createErrorResponse("流式数据获取失败: " + throwable.getMessage()).toJSONString() + "\n\n");
                    })
                    .cache(); // 缓存流数据供后续处理使用
            
            // 在后台异步处理数据提取和保存
            extractMcpToolResults(streamData);
            
            return streamData;
            
        } catch (Exception e) {
            log.error("流式数据获取失败", e);
            return Flux.just("data: " + chatRequestBuilderService.createErrorResponse("流式数据获取失败: " + e.getClass().getSimpleName() + " - " + e.getMessage()).toJSONString() + "\n\n");
        }
    }

    /**
     * 提取MCP工具调用结果 - 接收Flux<String>参数
     * 分析流式JSON数据，提取agent调用MCP工具返回的那部分值并保存到数据库
     * 异步非阻塞执行，立即返回
     */
    public void extractMcpToolResults(Flux<String> streamData) {
        log.info("开始从流式数据中提取MCP工具调用结果");
        
        streamData
                .collectList()
                .subscribeOn(Schedulers.boundedElastic()) // 在弹性线程池中执行，避免阻塞
                .subscribe(chunks -> {
                    if (chunks == null || chunks.isEmpty()) {
                        log.warn("未收到流式响应数据");
                        return;
                    }
                    
                    // 提取包含tool_call_id的数据块和工具调用信息
                    JSONArray mcpToolResults = new JSONArray();
                    JSONArray toolCallNames = new JSONArray();
                    java.util.concurrent.atomic.AtomicInteger validResults = new java.util.concurrent.atomic.AtomicInteger(0);
                    
                    for (String chunk : chunks) {
                        processChunkForMcpData(chunk, mcpToolResults, toolCallNames, validResults);
                    }
                    
                    // 构建响应
                    JSONObject response = chatRequestBuilderService.buildAnalysisResult(mcpToolResults, toolCallNames, validResults.get());
                    
                    // 保存到数据库
                    try {
                        saveAnalysisResults(response);
                        log.info("✅ 数据已保存到数据库");
                    } catch (Exception e) {
                        log.error("❌ 保存数据到数据库失败", e);
                    }
                    
                    log.info("提取完成，共找到{}个包含tool_call_id的数据块，{}个工具调用", 
                            validResults.get(), toolCallNames.size());
                    
                }, error -> log.error("MCP工具调用结果提取失败", error));
    }
    
    /**
     * 处理单个数据块，提取MCP相关数据
     */
    private void processChunkForMcpData(String chunk, JSONArray mcpToolResults, JSONArray toolCallNames, 
                                       java.util.concurrent.atomic.AtomicInteger validResults) {
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
                    int currentCount = validResults.incrementAndGet();
                    
                    // 输出包含tool_call_id的完整JSON数据块
                    synchronized (mcpToolResults) {
                        mcpToolResults.add(jsonData);
                    }
                    
                    log.info("✅ 找到tool_call_id数据块 [{}]: {}", currentCount, jsonData.toJSONString());
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
                                
                                synchronized (toolCallNames) {
                                    toolCallNames.add(toolCallInfo);
                                }
                                
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
    
    /**
     * 保存MCP工具调用结果和工具调用名称到数据库
     */
    @Transactional
    public void saveAnalysisResults(JSONObject analysisResult) {
        try {
            // 生成会话ID
            String sessionId = "session_" + System.currentTimeMillis() + "_" + UUID.randomUUID().toString().substring(0, 8);
            
            // 提取基本信息
            Boolean success = analysisResult.getBoolean("success");
            String message = analysisResult.getString("message");
            Integer resultsCount = analysisResult.getInteger("results_count");
            Integer toolCallsCount = analysisResult.getInteger("tool_calls_count");
            Long timestamp = analysisResult.getLong("timestamp");
            
            // 保存会话信息
            AnalysisSession session = AnalysisSession.builder()
                    .sessionId(sessionId)
                    .success(success != null ? success : false)
                    .message(message)
                    .resultsCount(resultsCount != null ? resultsCount : 0)
                    .toolCallsCount(toolCallsCount != null ? toolCallsCount : 0)
                    .timestamp(timestamp)
                    .build();
            
            analysisSessionRepository.save(session);
            log.info("保存分析会话: {}", sessionId);
            
            // 保存MCP工具调用结果
            JSONArray mcpToolResults = analysisResult.getJSONArray("mcp_tool_results");
            if (mcpToolResults != null && !mcpToolResults.isEmpty()) {
                List<McpToolResult> toolResults = new ArrayList<>();
                
                for (int i = 0; i < mcpToolResults.size(); i++) {
                    JSONObject result = mcpToolResults.getJSONObject(i);
                    if (result != null) {
                        McpToolResult toolResult = McpToolResult.builder()
                                .threadId(result.getString("thread_id"))
                                .agent(result.getString("agent"))
                                .resultId(result.getString("id"))
                                .role(result.getString("role"))
                                .content(result.getString("content"))
                                .toolCallId(result.getString("tool_call_id"))
                                .sessionId(sessionId)
                                .build();
                        
                        toolResults.add(toolResult);
                    }
                }
                
                if (!toolResults.isEmpty()) {
                    mcpToolResultRepository.saveAll(toolResults);
                    log.info("保存{}条MCP工具调用结果", toolResults.size());
                }
            }
            
            // 保存工具调用名称
            JSONArray toolCallNames = analysisResult.getJSONArray("tool_call_names");
            if (toolCallNames != null && !toolCallNames.isEmpty()) {
                List<ToolCallName> callNames = new ArrayList<>();
                
                for (int i = 0; i < toolCallNames.size(); i++) {
                    JSONObject toolCall = toolCallNames.getJSONObject(i);
                    if (toolCall != null) {
                        ToolCallName callName = ToolCallName.builder()
                                .name(toolCall.getString("name"))
                                .callId(toolCall.getString("id"))
                                .type(toolCall.getString("type"))
                                .args(toolCall.get("args") != null ? toolCall.get("args").toString() : null)
                                .callIndex(toolCall.getInteger("index"))
                                .sessionId(sessionId)
                                .build();
                        
                        callNames.add(callName);
                    }
                }
                
                if (!callNames.isEmpty()) {
                    toolCallNameRepository.saveAll(callNames);
                    log.info("保存{}条工具调用名称", callNames.size());
                }
            }
            
            log.info("分析结果保存完成，会话ID: {}", sessionId);
            
        } catch (Exception e) {
            log.error("保存分析结果失败", e);
            throw new RuntimeException("保存分析结果失败: " + e.getMessage(), e);
        }
    }
    
}