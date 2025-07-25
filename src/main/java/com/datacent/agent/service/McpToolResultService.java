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
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;

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
    private ExtractEntityService extractEntityService;
    
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

        if (null == threadId){
            threadId = "thread-" + UUID.randomUUID();
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
            extractMcpToolResults(streamData, threadId);
            
            return streamData;
            
        } catch (Exception e) {
            log.error("流式数据获取失败", e);
            return Flux.just("data: " + chatRequestBuilderService.createErrorResponse("流式数据获取失败: " + e.getClass().getSimpleName() + " - " + e.getMessage()).toJSONString() + "\n\n");
        }
    }

    /**
     * 提取MCP工具调用结果 - 接收Flux<String>参数和threadId
     * 分析流式JSON数据，提取agent调用MCP工具返回的那部分值并实时保存到数据库
     * 异步非阻塞执行，立即返回，每解析到一条数据立即入库
     * 使用传入的threadId作为sessionId
     */
    public void extractMcpToolResults(Flux<String> streamData, String threadId) {
        log.info("开始从流式数据中提取MCP工具调用结果，threadId: {}", threadId);

        // 创建或获取会话记录
        createOrGetAnalysisSession(threadId);
        
        // 计数器，用于统计
        java.util.concurrent.atomic.AtomicInteger validResults = new java.util.concurrent.atomic.AtomicInteger(0);
        java.util.concurrent.atomic.AtomicInteger toolCallsCount = new java.util.concurrent.atomic.AtomicInteger(0);

        
        streamData
                .subscribeOn(Schedulers.boundedElastic())
                .subscribe(
                    chunk -> {
                        // 实时处理每个数据块
                        processChunkForMcpDataRealtime(chunk, threadId, validResults, toolCallsCount);
                    },
                    error -> {
                        log.error("MCP工具调用结果提取失败", error);
                        // 更新会话状态为失败
                        updateAnalysisSessionStatus(threadId, false, "流数据处理失败: " + error.getMessage(),
                                                   validResults.get(), toolCallsCount.get());
                    },
                    () -> {
                        // 流处理完成
                        log.info("提取完成，会话ID: {}, 共找到{}个包含tool_call_id的数据块，{}个工具调用",
                                threadId, validResults.get(), toolCallsCount.get());
                        // 更新会话状态为成功
                        updateAnalysisSessionStatus(threadId, true, "处理成功",
                                                   validResults.get(), toolCallsCount.get());
                    }
                );
    }
    
    /**
     * 创建或获取分析会话记录（使用threadId作为sessionId）
     */
    private void createOrGetAnalysisSession(String threadId) {
        try {
            // 先检查是否已存在该threadId的会话
            if (analysisSessionRepository.findBySessionId(threadId).isEmpty()) {
                // 不存在则创建新会话
                AnalysisSession session = AnalysisSession.builder()
                        .sessionId(threadId)
                        .success(false)
                        .message("处理中...")
                        .resultsCount(0)
                        .toolCallsCount(0)
                        .timestamp(System.currentTimeMillis())
                        .build();
                
                analysisSessionRepository.save(session);
                log.info("创建新分析会话记录，threadId: {}", threadId);
            } else {
                log.debug("会话已存在，threadId: {}", threadId);
            }
        } catch (Exception e) {
            log.error("创建分析会话记录失败，threadId: {}", threadId, e);
        }
    }
    
    /**
     * 更新分析会话状态
     */
    private void updateAnalysisSessionStatus(String threadId, boolean success, String message,
                                           int resultsCount, int toolCallsCount) {
        try {
            analysisSessionRepository.findBySessionId(threadId).ifPresent(session -> {
                session.setSuccess(success);
                session.setMessage(message);
                session.setResultsCount(resultsCount);
                session.setToolCallsCount(toolCallsCount);
                analysisSessionRepository.save(session);
                log.info("更新会话状态 [{}]: success={}, results={}, toolCalls={}",
                        threadId, success, resultsCount, toolCallsCount);
            });
        } catch (Exception e) {
            log.error("更新分析会话状态失败: {}", threadId, e);
        }
    }
    
    /**
     * 实时处理单个数据块，提取MCP相关数据并立即保存
     * 使用传入的threadId直接保存数据
     */
    private void processChunkForMcpDataRealtime(String chunk, String threadId,
                                              java.util.concurrent.atomic.AtomicInteger validResults,
                                              java.util.concurrent.atomic.AtomicInteger toolCallsCount) {
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
                    
                    // 立即保存MCP工具调用结果
                    saveMcpToolResultRealtime(jsonData, threadId, toolCallId);
                    
                    log.info("✅ 找到并保存tool_call_id数据块 [{}]: tool_call_id={}", currentCount, toolCallId);
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
                                toolCallsCount.incrementAndGet();
                                
                                // 立即保存工具调用名称
                                saveToolCallNameRealtime(threadId, name, id, type, args, index);
                                
                                log.info("✅ 找到并保存工具调用: name={}, id={}, type={}", name, id, type);
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            // 跳过无效数据块，但记录错误
            log.debug("跳过无效数据块: {}", e.getMessage());
        }
    }
    
    /**
     * 实时保存MCP工具调用结果
     */
    private void saveMcpToolResultRealtime(JSONObject result, String sessionId, String toolCallId) {
        try {
            McpToolResult toolResult = McpToolResult.builder()
                    .threadId(result.getString("thread_id"))
                    .agent(result.getString("agent"))
                    .resultId(result.getString("id"))
                    .role(result.getString("role"))
                    .content(result.getString("content"))
                    .toolCallId(toolCallId)
                    .sessionId(sessionId)
                    .build();
            
            mcpToolResultRepository.save(toolResult);
            log.debug("保存MCP工具调用结果: tool_call_id={}", toolCallId);
        } catch (Exception e) {
            log.error("保存MCP工具调用结果失败: tool_call_id={}", toolCallId, e);
        }
    }
    
    /**
     * 实时保存工具调用名称
     */
    private void saveToolCallNameRealtime(String sessionId, String name, 
                                        String id, String type, Object args, Integer index) {
        try {
            ToolCallName callName = ToolCallName.builder()
                    .name(name)
                    .callId(id)
                    .type(type)
                    .args(args != null ? args.toString() : null)
                    .callIndex(index)
                    .sessionId(sessionId)
                    .build();
            
            toolCallNameRepository.save(callName);
            log.debug("保存工具调用名称: name={}, id={}", name, id);
        } catch (Exception e) {
            log.error("保存工具调用名称失败: name={}, id={}", name, id, e);
        }
    }
    
}