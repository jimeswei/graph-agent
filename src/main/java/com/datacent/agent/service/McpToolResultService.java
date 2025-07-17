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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    
    /**
     * 根据线程ID查询MCP工具调用结果
     */
    public List<McpToolResult> getResultsByThreadId(String threadId) {
        return mcpToolResultRepository.findByThreadId(threadId);
    }
    
    /**
     * 根据工具名称查询工具调用
     */
    public List<ToolCallName> getToolCallsByName(String name) {
        return toolCallNameRepository.findByName(name);
    }
    
    /**
     * 获取所有工具名称
     */
    public List<String> getAllToolNames() {
        return toolCallNameRepository.findDistinctNames();
    }
    
    /**
     * 根据会话ID查询分析会话
     */
    public AnalysisSession getSessionById(String sessionId) {
        return analysisSessionRepository.findBySessionId(sessionId).orElse(null);
    }
    
    /**
     * 获取最近的分析会话
     */
    public List<AnalysisSession> getRecentSessions() {
        return analysisSessionRepository.findRecentSessions();
    }
    
    /**
     * 获取统计信息
     */
    public JSONObject getStatistics() {
        JSONObject stats = new JSONObject();
        
        // 会话统计
        long totalSessions = analysisSessionRepository.count();
        long successfulSessions = analysisSessionRepository.countSuccessfulSessions();
        long failedSessions = analysisSessionRepository.countFailedSessions();
        
        stats.put("total_sessions", totalSessions);
        stats.put("successful_sessions", successfulSessions);
        stats.put("failed_sessions", failedSessions);
        
        // 工具调用统计
        long totalToolResults = mcpToolResultRepository.count();
        long totalToolCalls = toolCallNameRepository.count();
        
        stats.put("total_tool_results", totalToolResults);
        stats.put("total_tool_calls", totalToolCalls);
        
        // 工具名称统计
        List<String> toolNames = toolCallNameRepository.findDistinctNames();
        stats.put("unique_tool_names", toolNames.size());
        stats.put("tool_names", toolNames);
        
        return stats;
    }
}