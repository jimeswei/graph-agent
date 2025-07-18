package com.datacent.agent.controller;

import com.alibaba.fastjson2.JSONObject;
import com.datacent.agent.service.ChatStreamService;
import com.datacent.agent.service.McpToolResultService;
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
    private McpToolResultService mcpToolResultService;

    /**
     * 提取MCP工具调用结果的专门接口 - 流式版本
     * 分析JSON数据，只提取agent调用MCP工具返回的那部分值，使用流式响应
     *
     * @param request 聊天请求
     * @return 流式MCP工具调用结果
     */
    @PostMapping(value = "/stream/proxy", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> extractMcpToolResultsStream(@RequestBody JSONObject request) {
        String message = request.getString("message");
        String threadId = request.getString("thread_id");

        log.info("开始流式提取MCP工具调用结果，消息: {}, 线程ID: {}", message, threadId);

        return mcpToolResultService.extractMcpToolResultsStream(message, threadId);
    }


}