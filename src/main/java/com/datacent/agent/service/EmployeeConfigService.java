package com.datacent.agent.service;

import com.datacent.agent.dto.EmployeeConfigCreateRequest;
import com.datacent.agent.dto.EmployeeConfigResponse;
import com.datacent.agent.entity.EmployeeConfig;
import com.datacent.agent.repository.EmployeeConfigRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.List;

/**
 * 员工配置服务类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EmployeeConfigService {

    private final EmployeeConfigRepository employeeConfigRepository;

    /**
     * 创建员工配置
     */
    @Transactional
    public EmployeeConfigResponse createEmployeeConfig(EmployeeConfigCreateRequest request) {
        log.info("创建员工配置 - threadId: {}, userId: {}", request.getThreadId(), request.getUserId());
        
        // 检查是否已存在相同的threadId和userId组合
        if (employeeConfigRepository.existsByThreadIdAndUserId(request.getThreadId(), request.getUserId())) {
            throw new RuntimeException("该线程ID和用户ID的配置已存在");
        }

        // 创建实体对象
        EmployeeConfig employeeConfig = EmployeeConfig.builder()
                .model(request.getModel())
                .threadId(request.getThreadId())
                .userId(request.getUserId())
                .employeeType(request.getEmployeeType())
                .employeeDesc(request.getEmployeeDesc())
                .maxIterations(request.getMaxIterations())
                .iterationsPerRound(request.getIterationsPerRound())
                .maxSearchResults(request.getMaxSearchResults())
                .knowledgeBase(request.getKnowledgeBase())
                .mcpService(request.getMcpService())
                .tools(request.getTools())
                .build();

        // 保存到数据库
        EmployeeConfig savedConfig = employeeConfigRepository.save(employeeConfig);
        log.info("员工配置创建成功 - ID: {}, threadId: {}, userId: {}", 
                savedConfig.getId(), savedConfig.getThreadId(), savedConfig.getUserId());

        // 转换为响应DTO
        return convertToResponse(savedConfig);
    }

    /**
     * 根据线程ID和用户ID查询配置
     */
    public Optional<EmployeeConfigResponse> findByThreadIdAndUserId(String threadId, String userId) {
        log.info("查询员工配置 - threadId: {}, userId: {}", threadId, userId);
        
        Optional<EmployeeConfig> config = employeeConfigRepository.findByThreadIdAndUserId(threadId, userId);
        
        if (config.isPresent()) {
            log.info("找到员工配置 - ID: {}", config.get().getId());
            return Optional.of(convertToResponse(config.get()));
        } else {
            log.warn("未找到员工配置 - threadId: {}, userId: {}", threadId, userId);
            return Optional.empty();
        }
    }

    /**
     * 根据ID查询配置
     */
    public Optional<EmployeeConfigResponse> findById(Long id) {
        log.info("根据ID查询员工配置 - ID: {}", id);
        
        Optional<EmployeeConfig> config = employeeConfigRepository.findById(id);
        return config.map(this::convertToResponse);
    }

    /**
     * 根据用户ID查询所有配置
     */
    public List<EmployeeConfigResponse> findByUserId(String userId) {
        log.info("根据用户ID查询员工配置 - userId: {}", userId);
        
        List<EmployeeConfig> configs = employeeConfigRepository.findByUserId(userId);
        
        if (!configs.isEmpty()) {
            log.info("找到 {} 个员工配置", configs.size());
            return configs.stream()
                    .map(this::convertToResponse)
                    .collect(java.util.stream.Collectors.toList());
        } else {
            log.warn("未找到员工配置 - userId: {}", userId);
            return java.util.Collections.emptyList();
        }
    }

    /**
     * 更新员工配置
     */
    @Transactional
    public EmployeeConfigResponse updateEmployeeConfig(Long id, EmployeeConfigCreateRequest request) {
        log.info("更新员工配置 - ID: {}", id);
        
        EmployeeConfig existingConfig = employeeConfigRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("员工配置不存在，ID: " + id));

        // 检查是否修改了threadId和userId，如果修改了需要检查唯一性
        if (!existingConfig.getThreadId().equals(request.getThreadId()) || 
            !existingConfig.getUserId().equals(request.getUserId())) {
            if (employeeConfigRepository.existsByThreadIdAndUserId(request.getThreadId(), request.getUserId())) {
                throw new RuntimeException("该线程ID和用户ID的配置已存在");
            }
        }

        // 更新字段
        existingConfig.setModel(request.getModel());
        existingConfig.setThreadId(request.getThreadId());
        existingConfig.setUserId(request.getUserId());
        existingConfig.setEmployeeType(request.getEmployeeType());
        existingConfig.setEmployeeDesc(request.getEmployeeDesc());
        existingConfig.setMaxIterations(request.getMaxIterations());
        existingConfig.setIterationsPerRound(request.getIterationsPerRound());
        existingConfig.setMaxSearchResults(request.getMaxSearchResults());
        existingConfig.setKnowledgeBase(request.getKnowledgeBase());
        existingConfig.setMcpService(request.getMcpService());
        existingConfig.setTools(request.getTools());

        EmployeeConfig updatedConfig = employeeConfigRepository.save(existingConfig);
        log.info("员工配置更新成功 - ID: {}", updatedConfig.getId());

        return convertToResponse(updatedConfig);
    }

    /**
     * 删除员工配置
     */
    @Transactional
    public void deleteEmployeeConfig(Long id) {
        log.info("删除员工配置 - ID: {}", id);
        
        if (!employeeConfigRepository.existsById(id)) {
            throw new RuntimeException("员工配置不存在，ID: " + id);
        }

        employeeConfigRepository.deleteById(id);
        log.info("员工配置删除成功 - ID: {}", id);
    }

    /**
     * 根据线程ID和用户ID删除员工配置
     */
    @Transactional
    public void deleteEmployeeConfigByThreadIdAndUserId(String threadId, String userId) {
        log.info("删除员工配置 - threadId: {}, userId: {}", threadId, userId);
        
        if (!employeeConfigRepository.existsByThreadIdAndUserId(threadId, userId)) {
            throw new RuntimeException("员工配置不存在，threadId: " + threadId + ", userId: " + userId);
        }

        employeeConfigRepository.deleteByThreadIdAndUserId(threadId, userId);
        log.info("员工配置删除成功 - threadId: {}, userId: {}", threadId, userId);
    }

    /**
     * 将实体转换为响应DTO
     */
    private EmployeeConfigResponse convertToResponse(EmployeeConfig config) {
        return EmployeeConfigResponse.builder()
                .id(config.getId())
                .model(config.getModel())
                .threadId(config.getThreadId())
                .userId(config.getUserId())
                .employeeType(config.getEmployeeType())
                .employeeDesc(config.getEmployeeDesc())
                .maxIterations(config.getMaxIterations())
                .iterationsPerRound(config.getIterationsPerRound())
                .maxSearchResults(config.getMaxSearchResults())
                .knowledgeBase(config.getKnowledgeBase())
                .mcpService(config.getMcpService())
                .tools(config.getTools())
                .createdTime(config.getCreatedTime())
                .updatedTime(config.getUpdatedTime())
                .build();
    }
}