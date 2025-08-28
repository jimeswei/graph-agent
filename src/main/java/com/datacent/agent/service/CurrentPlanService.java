package com.datacent.agent.service;

import com.datacent.agent.dto.CurrentPlanResponseDTO;
import com.datacent.agent.dto.PlanStepDTO;
import com.datacent.agent.entity.CurrentPlan;
import com.datacent.agent.entity.PlanStep;
import com.datacent.agent.repository.CurrentPlanRepository;
import com.datacent.agent.repository.PlanStepRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 当前计划服务层
 * 处理与计划相关的业务逻辑
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CurrentPlanService {

    private final CurrentPlanRepository currentPlanRepository;
    private final PlanStepRepository planStepRepository;

    /**
     * 根据线程ID查询最新的计划及其步骤信息
     * 先根据thread_id从current_plan表获取最新的plan_id
     * 再根据plan_id从plan_steps表查询所有步骤信息
     * 
     * @param threadId 线程ID
     * @return 最新的计划及其步骤信息
     */
    public Optional<CurrentPlanResponseDTO> queryCurrentPlanByThreadId(String threadId) {
        
        log.info("开始查询线程ID对应的最新计划，threadId: {}", threadId);
        
        try {
            // 1. 根据threadId查询最新的计划
            Optional<CurrentPlan> currentPlanOpt = currentPlanRepository.findLatestByThreadId(threadId);
            
            if (currentPlanOpt.isEmpty()) {
                log.warn("未找到线程ID对应的计划，threadId: {}", threadId);
                return Optional.empty();
            }
            
            CurrentPlan currentPlan = currentPlanOpt.get();
            String planId = currentPlan.getPlanId();
            
            // 2. 根据planId查询所有步骤
            List<PlanStep> planSteps = planStepRepository.findByPlanIdOrderByStepIndex(planId);
            
            // 3. 转换为DTO
            List<PlanStepDTO> stepDTOs = planSteps.stream()
                    .map(this::convertToPlanStepDTO)
                    .collect(Collectors.toList());
            
            // 4. 构建响应DTO
            CurrentPlanResponseDTO responseDTO = CurrentPlanResponseDTO.builder()
                    .id(currentPlan.getId())
                    .threadId(currentPlan.getThreadId())
                    .planId(currentPlan.getPlanId())
                    .agent(currentPlan.getAgent())
                    .role(currentPlan.getRole())
                    .locale(currentPlan.getLocale())
                    .hasEnoughContext(currentPlan.getHasEnoughContext())
                    .thought(currentPlan.getThought())
                    .title(currentPlan.getTitle())
                    .createdTime(currentPlan.getCreatedTime())
                    .updatedTime(currentPlan.getUpdatedTime())
                    .steps(stepDTOs)
                    .build();
            
            log.info("成功查询到计划信息，threadId: {}, planId: {}, 步骤数量: {}", 
                    threadId, planId, stepDTOs.size());
            
            return Optional.of(responseDTO);
            
        } catch (Exception e) {
            log.error("查询计划信息失败，threadId: {}", threadId, e);
            throw new RuntimeException("查询计划信息失败: " + e.getMessage());
        }
    }
    
    /**
     * 将PlanStep实体转换为PlanStepDTO
     * 根据execution_res字段动态设置状态：
     * - execution_res = null → 步骤未执行（pending）
     * - execution_res = "..." → 步骤已完成（completed）
     * 
     * @param planStep 计划步骤实体
     * @return 计划步骤DTO
     */
    private PlanStepDTO convertToPlanStepDTO(PlanStep planStep) {
        // 根据execution_res字段判断步骤状态
        String dynamicStatus;
        String executionRes = planStep.getExecutionRes();
        
        if (executionRes == null || executionRes.trim().isEmpty()) {
            // execution_res为空或null，表示步骤未执行
            dynamicStatus = "pending";
        } else {
            // execution_res有值，表示步骤已完成
            dynamicStatus = "completed";
        }
        
        return PlanStepDTO.builder()
                .id(planStep.getId())
                .planId(planStep.getPlanId())
                .stepIndex(planStep.getStepIndex())
                .needGetData(planStep.getNeedGetData())
                .title(planStep.getTitle())
                .description(planStep.getDescription())
                .stepType(planStep.getStepType())
                .executionRes(planStep.getExecutionRes())
                .status(dynamicStatus)
                .createdTime(planStep.getCreatedTime())
                .updatedTime(planStep.getUpdatedTime())
                .build();
    }
}