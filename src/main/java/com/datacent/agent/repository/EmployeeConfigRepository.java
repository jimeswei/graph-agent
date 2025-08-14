package com.datacent.agent.repository;

import com.datacent.agent.entity.EmployeeConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.List;

@Repository
public interface EmployeeConfigRepository extends JpaRepository<EmployeeConfig, Long> {
    
    /**
     * 根据线程ID和用户ID查询配置
     */
    Optional<EmployeeConfig> findByThreadIdAndUserId(String threadId, String userId);
    
    /**
     * 根据线程ID查询所有配置
     */
    List<EmployeeConfig> findByThreadId(String threadId);
    
    /**
     * 根据用户ID查询所有配置
     */
    @Query("SELECT ec FROM EmployeeConfig ec WHERE ec.userId = :userId")
    List<EmployeeConfig> findByUserId(@Param("userId") String userId);
    
    /**
     * 根据员工类型查询配置
     */
    List<EmployeeConfig> findByEmployeeType(String employeeType);
    
    /**
     * 检查线程ID和用户ID的组合是否存在
     */
    boolean existsByThreadIdAndUserId(String threadId, String userId);
    
    /**
     * 根据线程ID和用户ID删除配置
     */
    void deleteByThreadIdAndUserId(String threadId, String userId);
}