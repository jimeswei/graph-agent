package com.datacent.agent.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * MCP工具调用结果查询DTO
 * 用于封装工具名称和结果内容的查询结果
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class McpToolResultQueryDTO {
    
    /**
     * 工具名称
     */
    private String name;
    
    /**
     * 工具执行结果内容
     */
    private String content;
}