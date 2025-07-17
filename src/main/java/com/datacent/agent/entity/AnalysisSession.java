package com.datacent.agent.entity;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * 分析会话实体类
 * 对应analysis_sessions表
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "analysis_sessions")
public class AnalysisSession {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "session_id", nullable = false, length = 100)
    private String sessionId;
    
    @Column(name = "success", nullable = false)
    private Boolean success;
    
    @Column(name = "message", columnDefinition = "TEXT")
    private String message;
    
    @Column(name = "results_count")
    private Integer resultsCount;
    
    @Column(name = "tool_calls_count")
    private Integer toolCallsCount;
    
    @Column(name = "timestamp")
    private Long timestamp;
    
    @Column(name = "created_time")
    private LocalDateTime createdTime;
    
    @Column(name = "updated_time")
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