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
    private ChatRequestBuilderService chatRequestBuilderService;
    
    // 移除无参数版本，强制要求提供threadId以确保数据一致性
    
    /**
     * 获取流式数据并返回 - 纯流式版本（支持自定义thread_id）
     * 从消息构建请求并获取流式响应数据
     * @param message 消息内容
     * @param threadId 线程ID（必须提供，不能为空）
     */
    public Flux<String> extractMcpToolResultsStream(String message, String threadId) {

        if (message == null || message.trim().isEmpty()) {
            return Flux.just("data: " + chatRequestBuilderService.createErrorResponse("消息内容不能为空").toJSONString() + "\n\n");
        }
        
        // 严格验证threadId，确保数据一致性
        if (threadId == null || threadId.trim().isEmpty()) {
            log.error("❌ threadId不能为空！这会导致会话数据不一致");
            return Flux.just("data: " + chatRequestBuilderService.createErrorResponse("系统错误：缺少会话标识符").toJSONString() + "\n\n");
        }

        try {
            // 构建完整请求（使用提供的threadId，绝不生成新的）
            JSONObject fullRequest = chatRequestBuilderService.buildFullRequest(message, threadId);
            
            // 验证请求中的thread_id与传入的threadId一致
            String requestThreadId = fullRequest.getString("thread_id");
            if (!threadId.equals(requestThreadId)) {
                log.error("🚨 严重错误：请求中的thread_id({})与传入的threadId({})不一致！", requestThreadId, threadId);
            }
            
            log.info("🚀 开始流式聊天，threadId: {}, 消息长度: {}", threadId, message.length());
            
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
     * 使用threadId关联到analysis_sessions表进行会话跟踪
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
     * 创建或获取分析会话记录（使用threadId作为session_id）
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
                    
                    // 记录外部响应的thread_id用于调试（但不用于保存数据）
                    String externalThreadId = jsonData.getString("thread_id");
                    if (externalThreadId != null && !externalThreadId.equals(threadId)) {
                        log.debug("🔍 外部服务返回不同的thread_id: 本地={}, 外部={}", threadId, externalThreadId);
                    }
                    
                    // 立即保存MCP工具调用结果（使用会话级threadId保持一致性）
                    saveMcpToolResultRealtime(jsonData, threadId, toolCallId);
                    
                    log.info("✅ 找到并保存tool_call_id数据块 [{}]: tool_call_id={}", currentCount, toolCallId);
                }
                
                // 检查是否包含tool_calls数组（工具调用定义）
                JSONArray toolCalls = jsonData.getJSONArray("tool_calls");
                JSONArray toolCallChunks = jsonData.getJSONArray("tool_call_chunks");
                
                if (toolCalls != null && !toolCalls.isEmpty()) {
                    for (int i = 0; i < toolCalls.size(); i++) {
                        JSONObject toolCall = toolCalls.getJSONObject(i);
                        if (toolCall != null) {
                            String name = toolCall.getString("name");
                            String id = toolCall.getString("id");
                            String type = toolCall.getString("type");
                            Object args = toolCall.get("args");
                            
                            // 尝试从tool_call_chunks中获取对应的index
                            Integer index = findIndexFromChunks(id, toolCallChunks);
                            
                            if (name != null && !name.trim().isEmpty()) {
                                toolCallsCount.incrementAndGet();
                                
                                // 立即保存工具调用名称
                                saveToolCallNameRealtime(name, id, type, args, index);
                                
                                log.info("✅ 找到并保存工具调用: name={}, id={}, type={}, index={}", name, id, type, index);
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
     * @param result JSON响应数据
     * @param sessionThreadId 会话级别的threadId（来自本地生成，用于保持一致性）
     * @param toolCallId 工具调用ID
     */
    private void saveMcpToolResultRealtime(JSONObject result, String sessionThreadId, String toolCallId) {
        try {
            // 使用会话级别的threadId，而不是响应中的thread_id，确保数据一致性
            McpToolResult toolResult = McpToolResult.builder()
                    .threadId(sessionThreadId)  // 使用统一的会话threadId
                    .agent(result.getString("agent"))
                    .resultId(result.getString("id"))
                    .role(result.getString("role"))
                    .content(result.getString("content"))
                    .toolCallId(toolCallId)
                    .build();
            
            mcpToolResultRepository.save(toolResult);
            log.debug("✅ 保存MCP工具调用结果: thread_id={}, tool_call_id={}", sessionThreadId, toolCallId);
        } catch (Exception e) {
            log.error("❌ 保存MCP工具调用结果失败: thread_id={}, tool_call_id={}", sessionThreadId, toolCallId, e);
        }
    }
    
    /**
     * 实时保存工具调用名称
     */
    private void saveToolCallNameRealtime(String name, String id, String type, Object args, Integer index) {
        try {
            String argsStr = args != null ? args.toString() : null;
            
            ToolCallName callName = ToolCallName.builder()
                    .name(name)
                    .callId(id)
                    .type(type)
                    .args(argsStr)
                    .callIndex(index)
                    .build();
            
            toolCallNameRepository.save(callName);
            log.debug("✅ 保存工具调用名称成功: name={}, id={}, args={}, index={}", 
                     name, id, argsStr, index);
        } catch (Exception e) {
            log.error("❌ 保存工具调用名称失败: name={}, id={}, args={}, index={}", 
                     name, id, args != null ? args.toString() : null, index, e);
        }
    }
    
    /**
     * 从tool_call_chunks中查找对应call_id的index值
     * @param callId 工具调用ID
     * @param toolCallChunks tool_call_chunks数组
     * @return 找到的index值，如果未找到则返回null
     */
    private Integer findIndexFromChunks(String callId, JSONArray toolCallChunks) {
        if (callId == null || toolCallChunks == null || toolCallChunks.isEmpty()) {
            return null;
        }
        
        try {
            for (int i = 0; i < toolCallChunks.size(); i++) {
                JSONObject chunk = toolCallChunks.getJSONObject(i);
                if (chunk != null && callId.equals(chunk.getString("id"))) {
                    Integer index = chunk.getInteger("index");
                    if (index != null) {
                        log.debug("找到call_id={}对应的index={}", callId, index);
                        return index;
                    }
                }
            }
        } catch (Exception e) {
            log.debug("从tool_call_chunks中查找index失败: callId={}", callId, e);
        }
        
        return null;
    }
    
}