package com.datacent.agent.repository;

import com.datacent.agent.entity.GraphCache;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * GraphCache数据访问接口
 */
@Repository
public interface GraphCacheRepository extends JpaRepository<GraphCache, Long> {
    
    /**
     * 根据threadId查询缓存数据
     * 
     * @param threadId 线程ID
     * @return GraphCache列表
     */
    List<GraphCache> findByThreadId(String threadId);
    
    /**
     * 根据threadId统计记录数
     * 
     * @param threadId 线程ID
     * @return 记录数
     */
    Long countByThreadId(String threadId);
    
    /**
     * 根据threadId查询是否存在记录
     * 
     * @param threadId 线程ID
     * @return 是否存在
     */
    boolean existsByThreadId(String threadId);
    
    /**
     * 使用原生SQL查询所有数据库名称
     */
    @Query(value = "SHOW DATABASES", nativeQuery = true)
    List<String> showDatabases();
    
    /**
     * 使用原生SQL查询当前数据库名称
     */
    @Query(value = "SELECT DATABASE()", nativeQuery = true)
    String getCurrentDatabase();
    
    /**
     * 使用原生SQL查询表是否存在
     */
    @Query(value = "SHOW TABLES LIKE 'graph_cache'", nativeQuery = true)
    List<String> checkTableExists();
    
    /**
     * 使用原生SQL直接查询graph_cache表记录数
     */
    @Query(value = "SELECT COUNT(*) FROM `graph_cache`", nativeQuery = true)
    Long countByNativeQuery();
    
    /**
     * 使用原生SQL查询graph_cache表的所有thread_id（去重）
     */
    @Query(value = "SELECT DISTINCT thread_id FROM `graph_cache` LIMIT 10", nativeQuery = true)
    List<String> findDistinctThreadIds();
}