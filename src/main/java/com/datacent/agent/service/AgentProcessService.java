package com.datacent.agent.service;

import com.alibaba.fastjson2.JSONObject;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.UUID;

/**
 * Agent处理服务
 * 负责调用实体提取服务并将结果保存到graph_cache表
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AgentProcessService {

    private final McpToolResultService mcpToolResultService;
    private final AsyncEntityProcessor asyncEntityProcessor;

    /**
     * 处理消息的核心方法
     * 调用实体提取服务并保存结果到graph_cache表
     *
     * @param request  待处理的消息
     * @return 保存的GraphCache实体
     */

    public Flux<String> process(JSONObject request) {

        String message = request.getString("message");
        String threadId = request.getString("thread_id");
        try {

            log.info("开始流式提取MCP工具调用结果，消息: {}, 线程ID: {}", message, threadId);

            if (message == null || message.trim().isEmpty()) {
                log.warn("输入消息为空，无法进行处理，threadId: {}", threadId);
                throw new IllegalArgumentException("消息内容不能为空");
            }

            if (threadId == null || threadId.trim().isEmpty()) {
                threadId = "thread-" + UUID.randomUUID();
            }

            log.info("开始处理消息，threadId: {}, 消息长度: {}", threadId, message.length());

            // 异步调用实体提取和缓存服务
            asyncEntityProcessor.extractAndCacheEntitiesAsync(request);

            // 立即返回MCP工具调用结果流
            return mcpToolResultService.extractMcpToolResultsStream(request);

        } catch (Exception e) {
            log.error("处理消息失败，threadId: {}", threadId, e);
            throw new RuntimeException("处理消息失败: " + e.getMessage(), e);
        }
    }


}