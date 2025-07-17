package com.datacent.agent.repository;

import com.datacent.agent.entity.McpToolResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * MCP工具调用结果Repository
 */
@Repository
public interface McpToolResultRepository extends JpaRepository<McpToolResult, Long> {
    
    /**
     * 根据线程ID查询
     */
    List<McpToolResult> findByThreadId(String threadId);
    
    /**
     * 根据代理名称查询
     */
    List<McpToolResult> findByAgent(String agent);
    
    /**
     * 根据工具调用ID查询
     */
    List<McpToolResult> findByToolCallId(String toolCallId);
    
    /**
     * 根据线程ID和代理名称查询
     */
    List<McpToolResult> findByThreadIdAndAgent(String threadId, String agent);
    
    /**
     * 根据线程ID统计数量
     */
    @Query("SELECT COUNT(m) FROM McpToolResult m WHERE m.threadId = :threadId")
    Long countByThreadId(@Param("threadId") String threadId);
    
    /**
     * 根据线程ID删除
     */
    void deleteByThreadId(String threadId);
}