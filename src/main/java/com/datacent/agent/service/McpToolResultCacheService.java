package com.datacent.agent.service;

import com.datacent.agent.entity.McpToolResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * MCP工具调用结果缓存服务
 * 提供支持多用户并发访问的消息队列缓存功能
 * 使用线程安全的数据结构确保高并发场景下的数据一致性
 */
@Slf4j
@Service
public class McpToolResultCacheService {
    
    /**
     * 消息队列缓存
     * Key: threadId (会话线程ID)
     * Value: CacheEntry (包含队列和元数据的缓存条目)
     */
    private final ConcurrentHashMap<String, CacheEntry> messageQueueCache = new ConcurrentHashMap<>();
    
    /**
     * 缓存条目数据结构
     * 包含线程安全的队列和访问时间戳
     */
    private static class CacheEntry {
        private final Queue<McpToolResult> queue;
        private volatile LocalDateTime lastAccessTime;
        private volatile LocalDateTime createTime;
        
        public CacheEntry() {
            this.queue = new ConcurrentLinkedQueue<>();
            this.createTime = LocalDateTime.now();
            this.lastAccessTime = LocalDateTime.now();
        }
        
        public Queue<McpToolResult> getQueue() {
            this.lastAccessTime = LocalDateTime.now();
            return queue;
        }
        
        public LocalDateTime getLastAccessTime() {
            return lastAccessTime;
        }
        
        public LocalDateTime getCreateTime() {
            return createTime;
        }
        
        public int getSize() {
            return queue.size();
        }
    }
    
    /**
     * 将MCP工具调用结果添加到消息队列中
     * 线程安全，支持多用户并发访问
     * 
     * @param threadId 会话线程ID
     * @param toolResult MCP工具调用结果
     */
    public void addToQueue(String threadId, McpToolResult toolResult) {
        if (threadId == null || threadId.trim().isEmpty()) {
            log.warn("threadId不能为空，跳过缓存添加");
            return;
        }
        
        if (toolResult == null) {
            log.warn("toolResult不能为空，跳过缓存添加，threadId: {}", threadId);
            return;
        }
        
        try {
            // 获取或创建缓存条目
            CacheEntry cacheEntry = messageQueueCache.computeIfAbsent(threadId, k -> {
                log.debug("为threadId创建新的消息队列缓存: {}", k);
                return new CacheEntry();
            });
            
            // 添加到队列
            cacheEntry.getQueue().offer(toolResult);
            
            log.debug("成功添加MCP工具调用结果到缓存队列: threadId={}, toolCallId={}, 队列大小={}", 
                     threadId, toolResult.getToolCallId(), cacheEntry.getSize());
            
        } catch (Exception e) {
            log.error("添加MCP工具调用结果到缓存队列失败: threadId={}, toolCallId={}", 
                     threadId, toolResult.getToolCallId(), e);
        }
    }
    
    /**
     * 从消息队列中获取指定threadId的所有结果
     * 非破坏性读取，数据仍保留在队列中
     * 
     * @param threadId 会话线程ID
     * @return MCP工具调用结果列表，如果队列不存在或为空则返回空列表
     */
    public List<McpToolResult> getAllFromQueue(String threadId) {
        if (threadId == null || threadId.trim().isEmpty()) {
            log.warn("threadId不能为空");
            return List.of();
        }
        
        try {
            CacheEntry cacheEntry = messageQueueCache.get(threadId);
            if (cacheEntry == null) {
                log.debug("未找到threadId对应的缓存队列: {}", threadId);
                return List.of();
            }
            
            Queue<McpToolResult> queue = cacheEntry.getQueue();
            List<McpToolResult> results = new ArrayList<>(queue);
            
            log.debug("从缓存队列获取MCP工具调用结果: threadId={}, 结果数量={}", threadId, results.size());
            return results;
            
        } catch (Exception e) {
            log.error("从缓存队列获取MCP工具调用结果失败: threadId={}", threadId, e);
            return List.of();
        }
    }
    
    /**
     * 从消息队列中弹出一个结果（FIFO）
     * 破坏性读取，弹出的数据从队列中移除
     * 
     * @param threadId 会话线程ID
     * @return MCP工具调用结果，如果队列为空则返回null
     */
    public McpToolResult pollFromQueue(String threadId) {
        if (threadId == null || threadId.trim().isEmpty()) {
            log.warn("threadId不能为空");
            return null;
        }
        
        try {
            CacheEntry cacheEntry = messageQueueCache.get(threadId);
            if (cacheEntry == null) {
                log.debug("未找到threadId对应的缓存队列: {}", threadId);
                return null;
            }
            
            Queue<McpToolResult> queue = cacheEntry.getQueue();
            McpToolResult result = queue.poll();
            
            if (result != null) {
                log.debug("从缓存队列弹出MCP工具调用结果: threadId={}, toolCallId={}, 剩余队列大小={}", 
                         threadId, result.getToolCallId(), cacheEntry.getSize());
            } else {
                log.debug("缓存队列为空: threadId={}", threadId);
            }
            
            return result;
            
        } catch (Exception e) {
            log.error("从缓存队列弹出MCP工具调用结果失败: threadId={}", threadId, e);
            return null;
        }
    }
    
    /**
     * 获取指定threadId的队列大小
     * 
     * @param threadId 会话线程ID
     * @return 队列大小，如果队列不存在则返回0
     */
    public int getQueueSize(String threadId) {
        if (threadId == null || threadId.trim().isEmpty()) {
            return 0;
        }
        
        CacheEntry cacheEntry = messageQueueCache.get(threadId);
        return cacheEntry != null ? cacheEntry.getSize() : 0;
    }
    
    /**
     * 检查指定threadId是否存在缓存队列
     * 
     * @param threadId 会话线程ID
     * @return 是否存在缓存队列
     */
    public boolean hasQueue(String threadId) {
        if (threadId == null || threadId.trim().isEmpty()) {
            return false;
        }
        
        return messageQueueCache.containsKey(threadId);
    }
    
    /**
     * 清空指定threadId的缓存队列
     * 
     * @param threadId 会话线程ID
     * @return 清空前的队列大小
     */
    public int clearQueue(String threadId) {
        if (threadId == null || threadId.trim().isEmpty()) {
            log.warn("threadId不能为空");
            return 0;
        }
        
        try {
            CacheEntry cacheEntry = messageQueueCache.remove(threadId);
            if (cacheEntry != null) {
                int size = cacheEntry.getSize();
                log.info("清空缓存队列: threadId={}, 清空前大小={}", threadId, size);
                return size;
            } else {
                log.debug("要清空的缓存队列不存在: threadId={}", threadId);
                return 0;
            }
            
        } catch (Exception e) {
            log.error("清空缓存队列失败: threadId={}", threadId, e);
            return 0;
        }
    }
    
    /**
     * 获取缓存统计信息
     * 
     * @return 包含缓存统计信息的Map
     */
    public Map<String, Object> getCacheStatistics() {
        Map<String, Object> statistics = new HashMap<>();
        
        try {
            int totalQueues = messageQueueCache.size();
            int totalResults = messageQueueCache.values().stream()
                    .mapToInt(CacheEntry::getSize)
                    .sum();
            
            statistics.put("totalQueues", totalQueues);
            statistics.put("totalResults", totalResults);
            statistics.put("averageQueueSize", totalQueues > 0 ? (double) totalResults / totalQueues : 0.0);
            
            // 统计队列大小分布
            Map<String, Integer> queueSizes = new HashMap<>();
            messageQueueCache.forEach((threadId, cacheEntry) -> {
                queueSizes.put(threadId, cacheEntry.getSize());
            });
            statistics.put("queueSizes", queueSizes);
            
            log.debug("缓存统计信息: 总队列数={}, 总结果数={}", totalQueues, totalResults);
            
        } catch (Exception e) {
            log.error("获取缓存统计信息失败", e);
            statistics.put("error", e.getMessage());
        }
        
        return statistics;
    }
    
    /**
     * 定时清理过期的缓存队列
     * 清理超过24小时未访问的队列，防止内存泄漏
     */
    @Scheduled(fixedRate = 3600000) // 每小时执行一次
    public void cleanupExpiredQueues() {
        try {
            LocalDateTime expirationTime = LocalDateTime.now().minus(24, ChronoUnit.HOURS);
            int removedCount = 0;
            
            Iterator<Map.Entry<String, CacheEntry>> iterator = messageQueueCache.entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry<String, CacheEntry> entry = iterator.next();
                CacheEntry cacheEntry = entry.getValue();
                
                if (cacheEntry.getLastAccessTime().isBefore(expirationTime)) {
                    iterator.remove();
                    removedCount++;
                    log.debug("清理过期缓存队列: threadId={}, 最后访问时间={}, 队列大小={}", 
                             entry.getKey(), cacheEntry.getLastAccessTime(), cacheEntry.getSize());
                }
            }
            
            if (removedCount > 0) {
                log.info("定时清理完成，移除{}个过期缓存队列，当前活跃队列数: {}", 
                        removedCount, messageQueueCache.size());
            } else {
                log.debug("定时清理完成，无过期队列需要移除，当前活跃队列数: {}", messageQueueCache.size());
            }
            
        } catch (Exception e) {
            log.error("定时清理过期缓存队列失败", e);
        }
    }
}