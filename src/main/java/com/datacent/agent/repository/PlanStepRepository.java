package com.datacent.agent.repository;

import com.datacent.agent.entity.PlanStep;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 计划步骤Repository
 */
@Repository
public interface PlanStepRepository extends JpaRepository<PlanStep, Long> {
    
    /**
     * 根据计划ID查询所有步骤（按步骤索引排序）
     */
    @Query("SELECT ps FROM PlanStep ps WHERE ps.planId = :planId ORDER BY ps.stepIndex ASC")
    List<PlanStep> findByPlanIdOrderByStepIndex(@Param("planId") String planId);
    
    /**
     * 根据计划ID和步骤索引查询
     */
    Optional<PlanStep> findByPlanIdAndStepIndex(String planId, Integer stepIndex);
    
    /**
     * 根据计划ID查询
     */
    List<PlanStep> findByPlanId(String planId);
    
    /**
     * 根据状态查询步骤
     */
    List<PlanStep> findByStatus(PlanStep.StepStatus status);
    
    /**
     * 根据计划ID和状态查询步骤
     */
    List<PlanStep> findByPlanIdAndStatus(String planId, PlanStep.StepStatus status);
    
    /**
     * 根据计划ID删除所有步骤
     */
    void deleteByPlanId(String planId);
    
    /**
     * 根据计划ID统计步骤数量
     */
    @Query("SELECT COUNT(ps) FROM PlanStep ps WHERE ps.planId = :planId")
    Long countByPlanId(@Param("planId") String planId);
    
    /**
     * 根据计划ID查询最大步骤索引
     */
    @Query("SELECT MAX(ps.stepIndex) FROM PlanStep ps WHERE ps.planId = :planId")
    Optional<Integer> findMaxStepIndexByPlanId(@Param("planId") String planId);
    
    /**
     * 检查计划是否有步骤
     */
    boolean existsByPlanId(String planId);
}