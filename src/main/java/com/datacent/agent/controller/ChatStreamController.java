package com.datacent.agent.controller;

import com.alibaba.fastjson2.JSONObject;
import com.datacent.agent.service.ChatStreamService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

/**
 * 聊天流式代理控制器
 * 直接转发请求到外部图谱agent接口，实现流式返回
 */
@Slf4j
@RestController
@RequestMapping("/api/chat")
@CrossOrigin(origins = "*")
public class ChatStreamController {

    @Autowired
    private ChatStreamService chatStreamService;

    /**
     * 流式聊天接口 - 直接转发完整请求体
     * 
     * @param request 完整的请求体，包含messages、thread_id、mcp_settings等所有参数
     * @return 流式响应数据
     */
    @PostMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> chatStream(@RequestBody Object request) {
        log.info("接收到聊天流请求，直接转发到外部接口");
        
        return chatStreamService.chatStream(request);
    }
    
    /**
     * 简化版流式聊天接口
     * 支持简单的message和thread_id参数，自动填充其他默认参数
     * 
     * @param request 简化的请求体
     * @return 流式响应数据
     */
    @PostMapping(value = "/stream/proxy", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> chatStreamSimple(@RequestBody JSONObject request) {
        String message = request.getString("message");
        String threadId = request.getString("thread_id");
        
        if (message == null || message.trim().isEmpty()) {
            return Flux.just("data: {\"error\":\"消息内容不能为空\"}\n\n");
        }
        
        // 如果没有提供thread_id，则自动生成
        if (threadId == null || threadId.trim().isEmpty()) {
            threadId = chatStreamService.generateThreadId();
        }
        
        // 构建完整的请求体（按照接口文档的JSON结构）
        JSONObject fullRequest = new JSONObject();
        
        // 构建messages数组
        JSONObject userMessage = new JSONObject();
        userMessage.put("role", "user");
        userMessage.put("content", message);
        fullRequest.put("messages", new Object[]{userMessage});
        
        // 设置其他参数（照抄文档）
        fullRequest.put("thread_id", threadId);
        fullRequest.put("resources", new Object[]{});
        fullRequest.put("auto_accepted_plan", true);
        fullRequest.put("enable_deep_thinking", false);
        fullRequest.put("enable_background_investigation", false);
        fullRequest.put("max_plan_iterations", 1);
        fullRequest.put("max_step_num", 5);
        fullRequest.put("max_search_results", 5);
        fullRequest.put("report_style", "academic");
        
        // 设置mcp_settings（照抄文档）
        JSONObject mcpSettings = buildMcpSettings();
        fullRequest.put("mcp_settings", mcpSettings);
        
        log.info("构建完整请求体，消息: {}, thread_id: {}", message, threadId);
        
        return chatStreamService.chatStream(fullRequest);
    }
    
    /**
     * 获取接口使用示例
     */
    @GetMapping("/examples")
    public JSONObject getChatExamples() {
        JSONObject examples = new JSONObject();
        
        // 完整POST请求示例
        JSONObject fullExample = new JSONObject();
        fullExample.put("url", "POST /api/chat/stream");
        fullExample.put("content_type", "application/json");
        fullExample.put("description", "直接转发完整请求体，按照接口文档的JSON结构");
        fullExample.put("body", "完整的JSON请求体，包含messages、thread_id、mcp_settings等");
        
        // 简化POST请求示例
        JSONObject simpleExample = new JSONObject();
        simpleExample.put("url", "POST /api/chat/stream/simple");
        simpleExample.put("content_type", "application/json");
        JSONObject simpleBody = new JSONObject();
        simpleBody.put("message", "分析下周杰伦和刘德华共同有哪些共同好友？");
        simpleBody.put("thread_id", "可选，不传会自动生成");
        simpleExample.put("body", simpleBody);
        simpleExample.put("description", "简化接口，只需提供message，其他参数自动填充");
        
        examples.put("full_request", fullExample);
        examples.put("simple_request", simpleExample);
        
        return examples;
    }
    
    /**
     * 构建MCP设置（照抄接口文档）
     */
    private JSONObject buildMcpSettings() {
        JSONObject mcpSettings = new JSONObject();
        JSONObject servers = new JSONObject();
        
        // knowledge-graph-general-query-service
        JSONObject generalService = new JSONObject();
        generalService.put("name", "knowledge-graph-general-query-service");
        generalService.put("transport", "sse");
        generalService.put("env", null);
        generalService.put("url", "http://192.168.3.78:5823/sse");
        generalService.put("enabled_tools", new String[]{"query_celebrity_relationships"});
        generalService.put("add_to_agents", new String[]{"researcher", "coder"});
        servers.put("knowledge-graph-general-query-service", generalService);
        
        // knowledge-graph-algorithrm-service
        JSONObject algorithmService = new JSONObject();
        algorithmService.put("name", "knowledge-graph-algorithrm-service");
        algorithmService.put("transport", "sse");
        algorithmService.put("env", null);
        algorithmService.put("url", "http://192.168.3.78:5821/sse");
        algorithmService.put("enabled_tools", new String[]{
                "most_recent_common_ancestor",
                "relation_chain_between_stars", 
                "similarity_between_stars",
                "mutual_friend_between_stars",
                "dream_team_common_works"
        });
        algorithmService.put("add_to_agents", new String[]{"researcher", "coder"});
        servers.put("knowledge-graph-algorithrm-service", algorithmService);
        
        // knowledge-content-detail-service
        JSONObject contentService = new JSONObject();
        contentService.put("name", "knowledge-content-detail-service");
        contentService.put("transport", "sse");
        contentService.put("env", null);
        contentService.put("url", "http://192.168.3.78:5822/sse");
        contentService.put("enabled_tools", new String[]{"contextualized_content_detail_stars"});
        contentService.put("add_to_agents", new String[]{"researcher", "coder"});
        servers.put("knowledge-content-detail-service", contentService);
        
        mcpSettings.put("servers", servers);
        return mcpSettings;
    }
}