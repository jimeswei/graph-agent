package com.datacent.agent.service;

import com.datacent.agent.entity.GraphCache;
import com.datacent.agent.repository.GraphCacheRepository;
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

    private final GraphCacheRepository graphCacheRepository;
    private final McpToolResultService mcpToolResultService;
    private final AsyncEntityProcessor asyncEntityProcessor;

    /**
     * 处理消息的核心方法
     * 调用实体提取服务并保存结果到graph_cache表
     *
     * @param message  待处理的消息
     * @param threadId 线程ID，用于关联analysis_sessions表
     * @return 保存的GraphCache实体
     */

    public Flux<String> process(String message, String threadId) {
        try {
            if (message == null || message.trim().isEmpty()) {
                log.warn("输入消息为空，无法进行处理，threadId: {}", threadId);
                throw new IllegalArgumentException("消息内容不能为空");
            }

            if (threadId == null || threadId.trim().isEmpty()) {
                threadId = "thread-" + UUID.randomUUID();
            }

            log.info("开始处理消息，threadId: {}, 消息长度: {}", threadId, message.length());


            // 异步调用实体提取和缓存服务
            asyncEntityProcessor.extractAndCacheEntitiesAsync(message, threadId);

            // 立即返回MCP工具调用结果流
            return mcpToolResultService.extractMcpToolResultsStream(message, threadId);

        } catch (Exception e) {
            log.error("处理消息失败，threadId: {}", threadId, e);
            throw new RuntimeException("处理消息失败: " + e.getMessage(), e);
        }
    }


    /**
     * 根据threadId查询已保存的处理结果
     *
     * @param threadId 线程ID
     * @return GraphCache列表
     */
    public java.util.List<GraphCache> getProcessedResults(String threadId) {
        if (threadId == null || threadId.trim().isEmpty()) {
            throw new IllegalArgumentException("线程ID不能为空");
        }

        log.info("查询处理结果，threadId: {}", threadId);
        return graphCacheRepository.findByThreadId(threadId);
    }

    /**
     * 检查指定threadId是否已有处理结果
     *
     * @param threadId 线程ID
     * @return 是否存在处理结果
     */
    public boolean hasProcessedResults(String threadId) {
        if (threadId == null || threadId.trim().isEmpty()) {
            return false;
        }

        return graphCacheRepository.existsByThreadId(threadId);
    }

    /**
     * 获取指定threadId的处理结果数量
     *
     * @param threadId 线程ID
     * @return 结果数量
     */
    public Long getProcessedResultsCount(String threadId) {
        if (threadId == null || threadId.trim().isEmpty()) {
            return 0L;
        }

        return graphCacheRepository.countByThreadId(threadId);
    }
    
    
}