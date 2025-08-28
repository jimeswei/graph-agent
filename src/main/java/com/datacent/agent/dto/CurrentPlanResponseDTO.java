package com.datacent.agent.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 当前计划响应DTO
 * 用于封装当前计划及其步骤的查询结果
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CurrentPlanResponseDTO {
    
    /**
     * 计划ID
     */
    private Long id;
    
    /**
     * 线程ID
     */
    private String threadId;
    
    /**
     * 计划唯一标识
     */
    private String planId;
    
    /**
     * 代理名称
     */
    private String agent;
    
    /**
     * 角色
     */
    private String role;
    
    /**
     * 语言环境
     */
    private String locale;
    
    /**
     * 是否有足够上下文
     */
    private Boolean hasEnoughContext;
    
    /**
     * 思考过程
     */
    private String thought;
    
    /**
     * 计划标题
     */
    private String title;
    
    /**
     * 创建时间
     */
    private LocalDateTime createdTime;
    
    /**
     * 更新时间
     */
    private LocalDateTime updatedTime;
    
    /**
     * 计划步骤列表
     */
    private List<PlanStepDTO> steps;
}