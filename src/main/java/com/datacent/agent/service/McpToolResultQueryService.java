package com.datacent.agent.service;

import com.datacent.agent.dto.McpToolResultQueryDTO;
import com.datacent.agent.dto.McpToolNameDTO;
import com.datacent.agent.repository.McpToolResultRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * MCP工具调用结果查询服务
 * 专门处理工具调用结果的复杂查询业务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class McpToolResultQueryService {
    
    private final McpToolResultRepository mcpToolResultRepository;
    
    /**
     * 根据线程ID和工具名称查询工具执行结果
     * 
     * @param threadId 线程ID
     * @param toolName 工具名称
     * @return 工具名称和执行结果列表
     */
    public List<McpToolResultQueryDTO> getToolResultsByThreadIdAndToolName(String threadId, String toolName) {
        log.info("查询工具执行结果，threadId: {}, toolName: {}", threadId, toolName);
        
        if (threadId == null || threadId.trim().isEmpty()) {
            log.warn("线程ID不能为空");
            return List.of();
        }
        
        if (toolName == null || toolName.trim().isEmpty()) {
            log.warn("工具名称不能为空");
            return List.of();
        }
        
        List<McpToolResultQueryDTO> results = mcpToolResultRepository.findToolResultsByThreadIdAndToolName(threadId, toolName);
        log.info("查询完成，找到{}条记录", results.size());
        
        return results;
    }
    
    /**
     * 根据线程ID查询所有工具执行结果
     * 
     * @param threadId 线程ID
     * @return 所有工具名称和执行结果列表
     */
    public List<McpToolResultQueryDTO> getAllToolResultsByThreadId(String threadId) {
        log.info("查询线程所有工具执行结果，threadId: {}", threadId);
        
        if (threadId == null || threadId.trim().isEmpty()) {
            log.warn("线程ID不能为空");
            return List.of();
        }
        
        List<McpToolResultQueryDTO> results = mcpToolResultRepository.findAllToolResultsByThreadId(threadId);
        log.info("查询完成，找到{}条记录", results.size());
        
        return results;
    }
    
    /**
     * 根据线程ID查询所有工具名称（去重）
     * 只返回工具名称，不包含执行结果内容
     * 
     * @param threadId 线程ID
     * @return 工具名称列表
     */
    public List<McpToolNameDTO> getAllToolNamesByThreadId(String threadId) {
        log.info("查询线程所有工具名称，threadId: {}", threadId);
        
        if (threadId == null || threadId.trim().isEmpty()) {
            log.warn("线程ID不能为空");
            return List.of();
        }
        
        List<McpToolNameDTO> results = mcpToolResultRepository.findAllToolNamesByThreadId(threadId);
        log.info("查询完成，找到{}个不同的工具", results.size());
        
        return results;
    }
    
    /**
     * 检查指定线程ID是否存在工具执行结果
     * 
     * @param threadId 线程ID
     * @return 是否存在结果
     */
    public boolean hasToolResults(String threadId) {
        if (threadId == null || threadId.trim().isEmpty()) {
            return false;
        }
        
        Long count = mcpToolResultRepository.countByThreadId(threadId);
        return count != null && count > 0;
    }
}