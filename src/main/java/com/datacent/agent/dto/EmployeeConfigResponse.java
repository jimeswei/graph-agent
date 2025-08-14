package com.datacent.agent.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.time.LocalDateTime;

/**
 * 员工配置响应DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmployeeConfigResponse {

    private Long id;
    private String model;
    private String threadId;
    private String userId;
    private String employeeType;
    private String employeeDesc;
    private Integer maxIterations;
    private Integer iterationsPerRound;
    private Integer maxSearchResults;
    private String knowledgeBase;
    private String mcpService;
    private String tools;
    private LocalDateTime createdTime;
    private LocalDateTime updatedTime;
}