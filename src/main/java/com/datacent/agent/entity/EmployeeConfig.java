package com.datacent.agent.entity;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * EmployeeConfig实体类
 * 对应employee_config表
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "employee_config",
       uniqueConstraints = @UniqueConstraint(name = "uk_thread_employee", 
                                           columnNames = {"thread_id", "user_id"}))
public class EmployeeConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "model", nullable = false, length = 50)
    private String model;

    @Column(name = "thread_id", nullable = false, length = 100)
    private String threadId;

    @Column(name = "user_id", nullable = false, length = 50)
    private String userId;

    @Column(name = "employee_type", nullable = false, length = 50)
    private String employeeType;

    @Column(name = "employee_desc", columnDefinition = "TEXT")
    private String employeeDesc;

    @Column(name = "max_iterations", nullable = false)
    @Builder.Default
    private Integer maxIterations = 0;

    @Column(name = "iterations_per_round", nullable = false)
    @Builder.Default
    private Integer iterationsPerRound = 0;

    @Column(name = "max_search_results", nullable = false)
    @Builder.Default
    private Integer maxSearchResults = 0;

    @Column(name = "knowledge_base", length = 100)
    private String knowledgeBase;

    @Column(name = "mcp_service", length = 100)
    private String mcpService;

    @Column(name = "tools", length = 255)
    private String tools;

    @Column(name = "created_time", nullable = false, updatable = false)
    private LocalDateTime createdTime;

    @Column(name = "updated_time", nullable = false)
    private LocalDateTime updatedTime;

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