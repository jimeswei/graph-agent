package com.datacent.agent.entity;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 计划实体类
 * 对应current_plan表
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "current_plan")
public class CurrentPlan {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "thread_id", nullable = false, length = 100)
    private String threadId;
    
    @Column(name = "plan_id", nullable = false, length = 100, unique = true)
    private String planId;
    
    @Column(name = "locale", length = 10)
    @Builder.Default
    private String locale = "zh-CN";
    
    @Column(name = "has_enough_context")
    @Builder.Default
    private Boolean hasEnoughContext = false;
    
    @Column(name = "thought", columnDefinition = "TEXT")
    private String thought;
    
    @Column(name = "title", nullable = false, length = 500)
    private String title;
    
    @Column(name = "agent", length = 50)
    private String agent;
    
    @Column(name = "role", length = 50)
    private String role;
    
    @Column(name = "created_time")
    private LocalDateTime createdTime;
    
    @Column(name = "updated_time")
    private LocalDateTime updatedTime;
    
    @OneToMany(mappedBy = "planId", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<PlanStep> steps;
    
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