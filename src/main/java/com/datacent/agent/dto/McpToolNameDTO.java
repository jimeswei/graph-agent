package com.datacent.agent.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * MCP工具名称DTO
 * 只包含工具名称，用于只需要返回工具名称的查询
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class McpToolNameDTO {
    
    /**
     * 工具名称
     */
    private String name;
}