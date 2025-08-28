package com.datacent.agent.entity;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * 计划步骤实体类
 * 对应plan_steps表
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "plan_steps")
public class PlanStep {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "plan_id", nullable = false, length = 100)
    private String planId;
    
    @Column(name = "step_index", nullable = false)
    private Integer stepIndex;
    
    @Column(name = "need_get_data")
    @Builder.Default
    private Boolean needGetData = true;
    
    @Column(name = "title", nullable = false, length = 500)
    private String title;
    
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;
    
    @Column(name = "step_type", length = 50)
    @Builder.Default
    private String stepType = "research";
    
    @Column(name = "execution_res", columnDefinition = "LONGTEXT")
    private String executionRes;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    @Builder.Default
    private StepStatus status = StepStatus.pending;
    
    @Column(name = "created_time")
    private LocalDateTime createdTime;
    
    @Column(name = "updated_time")
    private LocalDateTime updatedTime;
    
    /**
     * 步骤状态枚举
     */
    public enum StepStatus {
        pending, in_progress, completed, failed
    }
    
    @PrePersist
    protected void onCreate() {
        createdTime = LocalDateTime.now();
        updatedTime = LocalDateTime.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedTime = LocalDateTime.now();
    }
}