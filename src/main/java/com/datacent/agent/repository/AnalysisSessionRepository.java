package com.datacent.agent.repository;

import com.datacent.agent.entity.AnalysisSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 分析会话Repository
 */
@Repository
public interface AnalysisSessionRepository extends JpaRepository<AnalysisSession, Long> {
    
    /**
     * 根据会话ID查询
     */
    Optional<AnalysisSession> findBySessionId(String sessionId);
    
    /**
     * 根据成功状态查询
     */
    List<AnalysisSession> findBySuccess(Boolean success);
    
    /**
     * 根据时间戳范围查询
     */
    @Query("SELECT a FROM AnalysisSession a WHERE a.timestamp BETWEEN :startTime AND :endTime ORDER BY a.timestamp DESC")
    List<AnalysisSession> findByTimestampBetween(@Param("startTime") Long startTime, @Param("endTime") Long endTime);
    
    /**
     * 统计成功的会话数
     */
    @Query("SELECT COUNT(a) FROM AnalysisSession a WHERE a.success = true")
    Long countSuccessfulSessions();
    
    /**
     * 统计失败的会话数
     */
    @Query("SELECT COUNT(a) FROM AnalysisSession a WHERE a.success = false")
    Long countFailedSessions();
    
    /**
     * 获取最近的会话
     */
    @Query("SELECT a FROM AnalysisSession a ORDER BY a.createdTime DESC")
    List<AnalysisSession> findRecentSessions();
}