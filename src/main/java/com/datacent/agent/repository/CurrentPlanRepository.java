package com.datacent.agent.repository;

import com.datacent.agent.entity.CurrentPlan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 计划Repository
 */
@Repository
public interface CurrentPlanRepository extends JpaRepository<CurrentPlan, Long> {
    
    /**
     * 根据线程ID查询计划
     */
    List<CurrentPlan> findByThreadId(String threadId);
    
    /**
     * 根据计划ID查询
     */
    Optional<CurrentPlan> findByPlanId(String planId);
    
    /**
     * 根据线程ID查询最新的计划
     */
    @Query("SELECT cp FROM CurrentPlan cp WHERE cp.threadId = :threadId ORDER BY cp.createdTime DESC LIMIT 1")
    Optional<CurrentPlan> findLatestByThreadId(@Param("threadId") String threadId);
    
    /**
     * 检查计划ID是否存在
     */
    boolean existsByPlanId(String planId);
    
    /**
     * 根据线程ID删除计划
     */
    void deleteByThreadId(String threadId);
    
    /**
     * 根据线程ID统计计划数量
     */
    @Query("SELECT COUNT(cp) FROM CurrentPlan cp WHERE cp.threadId = :threadId")
    Long countByThreadId(@Param("threadId") String threadId);
    
    /**
     * 检查线程ID是否存在计划
     */
    boolean existsByThreadId(String threadId);
}