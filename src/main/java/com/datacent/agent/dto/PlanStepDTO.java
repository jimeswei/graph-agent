package com.datacent.agent.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 计划步骤DTO
 * 用于封装计划步骤查询结果
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlanStepDTO {
    
    /**
     * 步骤ID
     */
    private Long id;
    
    /**
     * 计划ID
     */
    private String planId;
    
    /**
     * 步骤序号
     */
    private Integer stepIndex;
    
    /**
     * 是否需要获取数据
     */
    private Boolean needGetData;
    
    /**
     * 步骤标题
     */
    private String title;
    
    /**
     * 步骤描述
     */
    private String description;
    
    /**
     * 步骤类型
     */
    private String stepType;
    
    /**
     * 执行结果
     */
    private String executionRes;
    
    /**
     * 执行状态
     */
    private String status;
    
    /**
     * 创建时间
     */
    private LocalDateTime createdTime;
    
    /**
     * 更新时间
     */
    private LocalDateTime updatedTime;
}