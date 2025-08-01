package com.datacent.agent.service;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * 聊天请求构建服务
 * 负责构建各种类型的聊天请求体
 */
@Slf4j
@Service
public class ChatRequestBuilderService {
    
    /**
     * 构建完整的请求体
     */
    public JSONObject buildFullRequest(String message) {
        return buildFullRequest(message, null);
    }


    /**
     * 生成统一格式的threadId
     * 使用与AgentProcessService一致的格式确保系统一致性
     */
    public String generateThreadId() {
        return "thread-" + UUID.randomUUID();
    }
    /**
     * 构建完整的请求体（支持自定义thread_id）
     */
    public JSONObject buildFullRequest(String message, String threadId) {
        JSONObject request = new JSONObject();
        
        // 构建messages数组
        JSONArray messages = new JSONArray();
        JSONObject messageObj = new JSONObject();
        messageObj.put("role", "user");
        messageObj.put("content", message);
        messages.add(messageObj);
        request.put("messages", messages);
        
        // 设置thread_id（严格使用提供的threadId，确保数据一致性）
        if (threadId != null && !threadId.trim().isEmpty()) {
            request.put("thread_id", threadId);
            log.debug("✅ 使用提供的threadId: {}", threadId);
        } else {
            // 只有在明确没有提供threadId时才生成新的
            String newThreadId = generateThreadId();
            request.put("thread_id", newThreadId);
            log.warn("⚠️ 未提供threadId，生成新的: {}", newThreadId);
        }
        
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
        algorithmTools.add("query_celebrity_relationships");
        algorithmTools.add("recent_common_celebrity_event");
        algorithmService.put("enabled_tools", algorithmTools);
        JSONArray algorithmAgents = new JSONArray();
        algorithmAgents.add("researcher");
        algorithmAgents.add("coder");
        algorithmService.put("add_to_agents", algorithmAgents);
        servers.put("knowledge-graph-algorithrm-service", algorithmService);

        mcpSettings.put("servers", servers);
        request.put("mcp_settings", mcpSettings);
        
        return request;
    }
    
    /**
     * 创建错误响应
     */
    public JSONObject createErrorResponse(String message) {
        JSONObject response = new JSONObject();
        response.put("success", false);
        response.put("message", message);
        response.put("timestamp", System.currentTimeMillis());
        return response;
    }
    
    /**
     * 构建分析结果
     */
    public JSONObject buildAnalysisResult(JSONArray mcpToolResults, JSONArray toolCallNames, int validResults) {
        JSONObject response = new JSONObject();
        response.put("success", true);
        response.put("message", String.format("提取到%d个包含tool_call_id的数据块，%d个工具调用", 
                validResults, toolCallNames.size()));
        response.put("mcp_tool_results", mcpToolResults);
        response.put("tool_call_names", toolCallNames);
        response.put("results_count", validResults);
        response.put("tool_calls_count", toolCallNames.size());
        response.put("timestamp", System.currentTimeMillis());
        return response;
    }
}