package com.datacent.agent.repository;

import com.datacent.agent.entity.AgentReport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 代理报告Repository
 */
@Repository
public interface AgentReportRepository extends JpaRepository<AgentReport, Long> {
    
    /**
     * 根据线程ID查询
     */
    List<AgentReport> findByThreadId(String threadId);
    
    /**
     * 根据代理名称查询
     */
    List<AgentReport> findByAgent(String agent);
    
    /**
     * 根据线程ID和代理名称查询
     */
    Optional<AgentReport> findByThreadIdAndAgent(String threadId, String agent);
    
    /**
     * 根据线程ID查询最新的报告（所有agent类型）
     */
    @Query("SELECT ar FROM AgentReport ar WHERE ar.threadId = :threadId ORDER BY ar.createdTime DESC LIMIT 1")
    Optional<AgentReport> findLatestByThreadId(@Param("threadId") String threadId);
    
    /**
     * 根据线程ID统计数量
     */
    @Query("SELECT COUNT(ar) FROM AgentReport ar WHERE ar.threadId = :threadId")
    Long countByThreadId(@Param("threadId") String threadId);
    
    /**
     * 根据线程ID删除
     */
    void deleteByThreadId(String threadId);
    
    /**
     * 检查线程ID是否存在报告
     */
    boolean existsByThreadId(String threadId);
    
    /**
     * 检查线程ID和代理名称是否存在报告（已移除防重复逻辑，保留供其他查询使用）
     */
    boolean existsByThreadIdAndAgent(String threadId, String agent);
    
    /**
     * 根据线程ID查询所有reporter报告（按创建时间排序）
     */
    @Query("SELECT ar FROM AgentReport ar WHERE ar.threadId = :threadId AND ar.agent = 'reporter' ORDER BY ar.createdTime ASC")
    List<AgentReport> findAllReporterReportsByThreadId(@Param("threadId") String threadId);
    
    /**
     * 根据线程ID查询最新的reporter报告
     */
    @Query("SELECT ar FROM AgentReport ar WHERE ar.threadId = :threadId AND ar.agent = 'reporter' ORDER BY ar.createdTime DESC LIMIT 1")
    Optional<AgentReport> findLatestReporterReportByThreadId(@Param("threadId") String threadId);
    
    /**
     * 统计某个threadId的reporter报告数量
     */
    @Query("SELECT COUNT(ar) FROM AgentReport ar WHERE ar.threadId = :threadId AND ar.agent = 'reporter'")
    Long countReporterReportsByThreadId(@Param("threadId") String threadId);
}