package com.datacent.agent.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;


/**
 * 员工配置创建请求DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EmployeeConfigCreateRequest {

    private String model;

    private String threadId;

    private String userId;

    private String employeeType;

    private String employeeDesc;

    private Integer maxIterations = 0;

    private Integer iterationsPerRound = 0;

    private Integer maxSearchResults = 0;

    private String knowledgeBase;

    private String mcpService;

    private String tools;
}