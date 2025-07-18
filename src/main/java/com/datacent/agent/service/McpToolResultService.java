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
    
    /**
     * 提取MCP工具调用结果
     * 分析JSON数据，只提取agent调用MCP工具返回的那部分值
     */
    public JSONObject extractMcpToolResults(String message) {
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
            response.put("message", String.format("提取到%d个包含tool_call_id的数据块，%d个工具调用", 
                    validResults, toolCallNames.size()));
            response.put("mcp_tool_results", mcpToolResults);
            response.put("tool_call_names", toolCallNames);
            response.put("results_count", validResults);
            response.put("tool_calls_count", toolCallNames.size());
            response.put("timestamp", System.currentTimeMillis());
            
            // 保存到数据库
            try {
                saveAnalysisResults(response);
                log.info("✅ 数据已保存到数据库");
            } catch (Exception e) {
                log.error("❌ 保存数据到数据库失败", e);
                response.put("database_save_error", "保存到数据库失败: " + e.getMessage());
            }
            
            log.info("提取完成，共找到{}个包含tool_call_id的数据块，{}个工具调用", 
                    validResults, toolCallNames.size());
            
            return response;
            
        } catch (Exception e) {
            log.error("MCP工具调用结果提取失败", e);
            return createErrorResponse("MCP工具调用结果提取失败: " + e.getClass().getSimpleName() + " - " + e.getMessage());
        }
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