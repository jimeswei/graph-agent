package com.datacent.agent.repository;

import com.datacent.agent.dto.McpToolResultQueryDTO;
import com.datacent.agent.dto.McpToolNameDTO;
import com.datacent.agent.dto.McpToolDetailQueryDTO;
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
    
    /**
     * 根据线程ID和工具名称查询工具名称和内容
     * 基于SQL: SELECT t2.NAME, t1.content FROM mcp_tool_results t1 LEFT JOIN tool_call_names t2 ON t1.tool_call_id = t2.call_id WHERE t1.thread_id = ? AND t2.name = ?
     */
    @Query("SELECT new com.datacent.agent.dto.McpToolResultQueryDTO(tcn.name, mtr.content) " +
           "FROM McpToolResult mtr " +
           "LEFT JOIN ToolCallName tcn ON mtr.toolCallId = tcn.callId " +
           "WHERE mtr.threadId = :threadId AND tcn.name = :toolName")
    List<McpToolResultQueryDTO> findToolResultsByThreadIdAndToolName(@Param("threadId") String threadId, 
                                                                      @Param("toolName") String toolName);
    
    /**
     * 根据线程ID查询所有工具名称和内容
     */
    @Query("SELECT new com.datacent.agent.dto.McpToolResultQueryDTO(tcn.name, mtr.content) " +
           "FROM McpToolResult mtr " +
           "LEFT JOIN ToolCallName tcn ON mtr.toolCallId = tcn.callId " +
           "WHERE mtr.threadId = :threadId")
    List<McpToolResultQueryDTO> findAllToolResultsByThreadId(@Param("threadId") String threadId);
    
    /**
     * 根据线程ID查询所有工具名称（去重）
     * 只返回工具名称，不包含内容
     */
    @Query("SELECT DISTINCT new com.datacent.agent.dto.McpToolNameDTO(tcn.name) " +
           "FROM McpToolResult mtr " +
           "LEFT JOIN ToolCallName tcn ON mtr.toolCallId = tcn.callId " +
           "WHERE mtr.threadId = :threadId AND tcn.name IS NOT NULL")
    List<McpToolNameDTO> findAllToolNamesByThreadId(@Param("threadId") String threadId);
    
    /**
     * 根据线程ID查询工具名称、内容和创建时间
     * 基于SQL: SELECT t2.NAME, t1.content, t1.created_time FROM mcp_tool_results t1 LEFT JOIN tool_call_names t2 ON t1.tool_call_id = t2.call_id WHERE t1.thread_id = ?
     */
    @Query("SELECT new com.datacent.agent.dto.McpToolDetailQueryDTO(tcn.name, mtr.content, mtr.createdTime) " +
           "FROM McpToolResult mtr " +
           "LEFT JOIN ToolCallName tcn ON mtr.toolCallId = tcn.callId " +
           "WHERE mtr.threadId = :threadId")
    List<McpToolDetailQueryDTO> findMcpToolsByThreadId(@Param("threadId") String threadId);
}