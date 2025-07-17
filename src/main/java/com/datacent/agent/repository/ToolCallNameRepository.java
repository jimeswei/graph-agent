package com.datacent.agent.repository;

import com.datacent.agent.entity.ToolCallName;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 工具调用名称Repository
 */
@Repository
public interface ToolCallNameRepository extends JpaRepository<ToolCallName, Long> {
    
    /**
     * 根据工具名称查询
     */
    List<ToolCallName> findByName(String name);
    
    /**
     * 根据调用ID查询
     */
    List<ToolCallName> findByCallId(String callId);
    
    /**
     * 根据类型查询
     */
    List<ToolCallName> findByType(String type);
    
    /**
     * 根据工具名称和类型查询
     */
    List<ToolCallName> findByNameAndType(String name, String type);
    
    /**
     * 统计工具调用次数
     */
    @Query("SELECT COUNT(t) FROM ToolCallName t WHERE t.name = :name")
    Long countByName(@Param("name") String name);
    
    /**
     * 获取所有工具名称（去重）
     */
    @Query("SELECT DISTINCT t.name FROM ToolCallName t ORDER BY t.name")
    List<String> findDistinctNames();
}