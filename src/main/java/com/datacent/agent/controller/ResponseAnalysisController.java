package com.datacent.agent.controller;

import com.alibaba.fastjson2.JSONObject;
import com.datacent.agent.service.McpToolResultService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

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
    private McpToolResultService mcpToolResultService;

    
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
        return mcpToolResultService.extractMcpToolResults(message);
    }
    

}