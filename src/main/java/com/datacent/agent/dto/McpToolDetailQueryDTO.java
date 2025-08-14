package com.datacent.agent.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

/**
 * MCP工具调用详细结果查询DTO
 * 用于封装工具名称、结果内容和创建时间的查询结果
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class McpToolDetailQueryDTO {
    
    /**
     * 工具名称
     */
    private String name;
    
    /**
     * 工具执行结果内容
     */
    private String content;
    
    /**
     * 创建时间
     */
    private LocalDateTime createdTime;
}