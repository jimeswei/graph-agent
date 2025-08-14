package com.datacent.agent.controller;

import com.datacent.agent.dto.EmployeeConfigCreateRequest;
import com.datacent.agent.dto.EmployeeConfigQueryRequest;
import com.datacent.agent.dto.EmployeeConfigResponse;
import com.datacent.agent.service.EmployeeConfigService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;
import java.util.List;

/**
 * 员工配置控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/employee-config")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class EmployeeConfigController {

    private final EmployeeConfigService employeeConfigService;

    /**
     * 创建员工配置
     */
    @PostMapping
    public ResponseEntity<?> createEmployeeConfig(@RequestBody EmployeeConfigCreateRequest request) {
        try {
            log.info("创建员工配置请求 - threadId: {}, userId: {}", request.getThreadId(), request.getUserId());
            
            EmployeeConfigResponse response = employeeConfigService.createEmployeeConfig(request);
            
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "员工配置创建成功",
                    "data", response
            ));
        } catch (Exception e) {
            log.error("创建员工配置失败: {}", e.getMessage(), e);
            return ResponseEntity.badRequest()
                    .body(Map.of(
                            "success", false,
                            "error", e.getMessage()
                    ));
        }
    }

    /**
     * 根据线程ID和员工编号查询配置
     */
    @GetMapping("/query")
    public ResponseEntity<?> getEmployeeConfig(EmployeeConfigQueryRequest request) {
        try {
            log.info("查询员工配置请求 - threadId: {}, userId: {}", request.getThreadId(), request.getUserId());

            Optional<EmployeeConfigResponse> response = employeeConfigService
                    .findByThreadIdAndUserId(request.getThreadId(), request.getUserId());

            if (response.isPresent()) {
                return ResponseEntity.ok(Map.of(
                        "success", true,
                        "message", "查询成功",
                        "data", response.get()
                ));
            } else {
                return ResponseEntity.ok(Map.of(
                        "success", false,
                        "message", "未找到对应的员工配置"
                ));
            }
        } catch (Exception e) {
            log.error("查询员工配置失败: {}", e.getMessage(), e);
            return ResponseEntity.badRequest()
                    .body(Map.of(
                            "success", false,
                            "error", e.getMessage()
                    ));
        }
    }

    /**
     * 根据线程ID和用户ID查询配置 (使用路径参数)
     */
    @GetMapping("/query/{threadId}/{userId}")
    public ResponseEntity<?> getEmployeeConfigByPath(@PathVariable String threadId,
                                                    @PathVariable String userId) {
        try {
            log.info("查询员工配置请求 - threadId: {}, userId: {}", threadId, userId);

            Optional<EmployeeConfigResponse> response = employeeConfigService
                    .findByThreadIdAndUserId(threadId, userId);

            if (response.isPresent()) {
                return ResponseEntity.ok(Map.of(
                        "success", true,
                        "message", "查询成功",
                        "data", response.get()
                ));
            } else {
                return ResponseEntity.ok(Map.of(
                        "success", false,
                        "message", "未找到对应的员工配置"
                ));
            }
        } catch (Exception e) {
            log.error("查询员工配置失败: {}", e.getMessage(), e);
            return ResponseEntity.badRequest()
                    .body(Map.of(
                            "success", false,
                            "error", e.getMessage()
                    ));
        }
    }

    /**
     * 根据用户ID查询所有配置 (路径参数)
     */
    @GetMapping("/user/{userId}")
    public ResponseEntity<?> getEmployeeConfigsByUserId(@PathVariable String userId) {
        try {
            log.info("根据用户ID查询员工配置 - userId: {}", userId);

            List<EmployeeConfigResponse> responses = employeeConfigService.findByUserId(userId);

            if (!responses.isEmpty()) {
                return ResponseEntity.ok(Map.of(
                        "success", true,
                        "message", "查询成功",
                        "data", responses,
                        "count", responses.size()
                ));
            } else {
                return ResponseEntity.ok(Map.of(
                        "success", false,
                        "message", "未找到对应的员工配置"
                ));
            }
        } catch (Exception e) {
            log.error("查询员工配置失败: {}", e.getMessage(), e);
            return ResponseEntity.badRequest()
                    .body(Map.of(
                            "success", false,
                            "error", e.getMessage()
                    ));
        }
    }

    /**
     * 根据用户ID查询所有配置 (查询参数)
     */
    @GetMapping("/user")
    public ResponseEntity<?> getEmployeeConfigsByUserIdParam(@RequestParam String userId) {
        try {
            log.info("根据用户ID查询员工配置 - userId: {}", userId);

            List<EmployeeConfigResponse> responses = employeeConfigService.findByUserId(userId);

            if (!responses.isEmpty()) {
                return ResponseEntity.ok(Map.of(
                        "success", true,
                        "message", "查询成功",
                        "data", responses,
                        "count", responses.size()
                ));
            } else {
                return ResponseEntity.ok(Map.of(
                        "success", false,
                        "message", "未找到对应的员工配置"
                ));
            }
        } catch (Exception e) {
            log.error("查询员工配置失败: {}", e.getMessage(), e);
            return ResponseEntity.badRequest()
                    .body(Map.of(
                            "success", false,
                            "error", e.getMessage()
                    ));
        }
    }

    /**
     * 根据ID查询配置
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> getEmployeeConfigById(@PathVariable Long id) {
        try {
            log.info("根据ID查询员工配置 - ID: {}", id);
            
            Optional<EmployeeConfigResponse> response = employeeConfigService.findById(id);
            
            if (response.isPresent()) {
                return ResponseEntity.ok(Map.of(
                        "success", true,
                        "message", "查询成功",
                        "data", response.get()
                ));
            } else {
                return ResponseEntity.ok(Map.of(
                        "success", false,
                        "message", "未找到对应的员工配置"
                ));
            }
        } catch (Exception e) {
            log.error("查询员工配置失败: {}", e.getMessage(), e);
            return ResponseEntity.badRequest()
                    .body(Map.of(
                            "success", false,
                            "error", e.getMessage()
                    ));
        }
    }

    /**
     * 更新员工配置
     */
    @PutMapping("/{id}")
    public ResponseEntity<?> updateEmployeeConfig(@PathVariable Long id,
                                                 @RequestBody EmployeeConfigCreateRequest request) {
        try {
            log.info("更新员工配置请求 - ID: {}", id);
            
            EmployeeConfigResponse response = employeeConfigService.updateEmployeeConfig(id, request);
            
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "员工配置更新成功",
                    "data", response
            ));
        } catch (Exception e) {
            log.error("更新员工配置失败: {}", e.getMessage(), e);
            return ResponseEntity.badRequest()
                    .body(Map.of(
                            "success", false,
                            "error", e.getMessage()
                    ));
        }
    }

    /**
     * 删除员工配置
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteEmployeeConfig(@PathVariable Long id) {
        try {
            log.info("删除员工配置请求 - ID: {}", id);
            
            employeeConfigService.deleteEmployeeConfig(id);
            
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "员工配置删除成功"
            ));
        } catch (Exception e) {
            log.error("删除员工配置失败: {}", e.getMessage(), e);
            return ResponseEntity.badRequest()
                    .body(Map.of(
                            "success", false,
                            "error", e.getMessage()
                    ));
        }
    }

    /**
     * 根据用户ID和线程ID删除员工配置
     */
    @DeleteMapping("/user")
    public ResponseEntity<?> deleteEmployeeConfigByUserIdAndThreadId(@RequestParam String userId, 
                                                                    @RequestParam String threadId) {
        try {
            log.info("删除员工配置请求 - userId: {}, threadId: {}", userId, threadId);
            
            employeeConfigService.deleteEmployeeConfigByThreadIdAndUserId(threadId, userId);
            
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "员工配置删除成功"
            ));
        } catch (Exception e) {
            log.error("删除员工配置失败: {}", e.getMessage(), e);
            return ResponseEntity.badRequest()
                    .body(Map.of(
                            "success", false,
                            "error", e.getMessage()
                    ));
        }
    }

    /**
     * 健康检查接口
     */
    @GetMapping("/health")
    public ResponseEntity<?> health() {
        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "员工配置服务正常运行",
                "timestamp", System.currentTimeMillis()
        ));
    }
}