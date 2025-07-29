package com.datacent.agent.service;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.datacent.agent.dto.McpToolResultQueryDTO;
import com.datacent.agent.dto.McpToolNameDTO;
import com.datacent.agent.dto.McpToolDetailQueryDTO;
import com.datacent.agent.entity.GraphCache;
import com.datacent.agent.repository.GraphCacheRepository;
import com.datacent.agent.repository.McpToolResultRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * MCP工具调用结果查询服务
 * 专门处理工具调用结果的复杂查询业务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class McpToolResultQueryService {
    
    private final McpToolResultRepository mcpToolResultRepository;
    
    private final GraphCacheRepository graphCacheRepository;


    
    /**
     * 根据线程ID和工具名称查询工具执行结果
     * 
     * @param threadId 线程ID
     * @param toolName 工具名称
     * @return 工具名称和执行结果列表
     */
    public List<McpToolResultQueryDTO> getToolResultsByThreadIdAndToolName(String threadId, String toolName) {
        log.info("查询工具执行结果，threadId: {}, toolName: {}", threadId, toolName);
        
        if (threadId == null || threadId.trim().isEmpty()) {
            log.warn("线程ID不能为空");
            return List.of();
        }
        
        if (toolName == null || toolName.trim().isEmpty()) {
            log.warn("工具名称不能为空");
            return List.of();
        }
        
        List<McpToolResultQueryDTO> results = mcpToolResultRepository.findToolResultsByThreadIdAndToolName(threadId, toolName);
        log.info("查询完成，找到{}条记录", results.size());
        
        return results;
    }
    
    /**
     * 根据线程ID查询所有工具执行结果
     * 
     * @param threadId 线程ID
     * @return 所有工具名称和执行结果列表
     */
    public List<McpToolResultQueryDTO> getAllToolResultsByThreadId(String threadId) {
        log.info("查询线程所有工具执行结果，threadId: {}", threadId);
        
        if (threadId == null || threadId.trim().isEmpty()) {
            log.warn("线程ID不能为空");
            return List.of();
        }
        
        List<McpToolResultQueryDTO> results = mcpToolResultRepository.findAllToolResultsByThreadId(threadId);
        log.info("查询完成，找到{}条记录", results.size());
        
        return results;
    }
    
    /**
     * 根据线程ID查询所有工具名称（去重）
     * 只返回工具名称，不包含执行结果内容
     * 
     * @param threadId 线程ID
     * @return 工具名称列表
     */
    public List<McpToolNameDTO> getAllToolNamesByThreadId(String threadId) {
        log.info("查询线程所有工具名称，threadId: {}", threadId);
        
        if (threadId == null || threadId.trim().isEmpty()) {
            log.warn("线程ID不能为空");
            return List.of();
        }
        
        List<McpToolNameDTO> results = mcpToolResultRepository.findAllToolNamesByThreadId(threadId);
        log.info("查询完成，找到{}个不同的工具", results.size());
        
        return results;
    }
    
    /**
     * 检查指定线程ID是否存在工具执行结果
     * 
     * @param threadId 线程ID
     * @return 是否存在结果
     */
    public boolean hasToolResults(String threadId) {
        if (threadId == null || threadId.trim().isEmpty()) {
            return false;
        }
        
        Long count = mcpToolResultRepository.countByThreadId(threadId);
        return count != null && count > 0;
    }
    
    /**
     * 根据线程ID查询MCP工具结果
     * 查询工具名称、内容和创建时间，并从content中提取数据结构
     * 
     * @param threadId 线程ID
     * @return 格式化后的工具调用结果列表
     */
    public List<JSONObject> queryMcpTools(String threadId) {
        log.info("查询MCP工具结果，threadId: {}", threadId);
        
        if (threadId == null || threadId.trim().isEmpty()) {
            log.warn("线程ID不能为空");
            return List.of();
        }
        
        // 1. 从数据库查询原始数据
        List<McpToolDetailQueryDTO> rawResults = mcpToolResultRepository.findMcpToolsByThreadId(threadId);
        log.info("从数据库查询到{}条记录", rawResults.size());
        
        // 2. 处理每条记录，提取并格式化content中的数据
        List<JSONObject> formattedResults = new ArrayList<>();
        for (McpToolDetailQueryDTO result : rawResults) {
            try {
                JSONObject extractedData = extractDataFromContent(result.getContent(), result.getName());
                if (extractedData != null) {
                    formattedResults.add(extractedData);
                }
            } catch (Exception e) {
                log.warn("处理记录时出错，工具名称: {}, 错误: {}", result.getName(), e.getMessage());
            }
        }
        
        log.info("成功提取{}条有效记录", formattedResults.size());
        return formattedResults;
    }
    
    /**
     * 从content字段中提取数据并格式化为指定的数据结构
     * 
     * @param content 原始content内容
     * @param toolName 工具名称
     * @return 格式化后的数据对象
     */
    private JSONObject extractDataFromContent(String content, String toolName) {
        if (content == null || content.trim().isEmpty()) {
            return null;
        }
        
        try {
            // 尝试解析content为JSON
            JSONObject contentJson = JSON.parseObject(content);
            
            // 构建返回的数据结构
            JSONObject data = new JSONObject();
            
            // 提取id字段
            String id = extractIdFromJson(contentJson);
            if (id != null) {
                data.put("id", id);
            }
            
            // 设置mcp_tool_function为工具名称
            data.put("mcp_tool_function", toolName);
            
            // 提取args字段
            JSONObject args = extractArgsFromJson(contentJson);
            if (args != null) {
                data.put("args", args);
            }
            
            // 构建最终的返回结构
            JSONObject result = new JSONObject();
            result.put("data", data);
            
            return result;
            
        } catch (Exception e) {
            log.debug("无法解析content为JSON，工具名称: {}, 错误: {}", toolName, e.getMessage());
            return null;
        }
    }
    
    /**
     * 从JSON对象中提取id字段
     * 
     * @param json JSON对象
     * @return 提取到的id，如果未找到则返回null
     */
    private String extractIdFromJson(JSONObject json) {
        // 直接查找id字段
        if (json.containsKey("id")) {
            Object idValue = json.get("id");
            return idValue != null ? idValue.toString() : null;
        }
        
        // 递归查找嵌套对象中的id字段
        for (Object value : json.values()) {
            if (value instanceof JSONObject) {
                String id = extractIdFromJson((JSONObject) value);
                if (id != null) {
                    return id;
                }
            } else if (value instanceof JSONArray) {
                JSONArray array = (JSONArray) value;
                for (Object item : array) {
                    if (item instanceof JSONObject) {
                        String id = extractIdFromJson((JSONObject) item);
                        if (id != null) {
                            return id;
                        }
                    }
                }
            }
        }
        
        return null;
    }
    
    /**
     * 从JSON对象中提取args字段
     * 
     * @param json JSON对象
     * @return 提取到的args对象，如果未找到则返回空对象
     */
    private JSONObject extractArgsFromJson(JSONObject json) {
        // 直接查找args字段
        if (json.containsKey("args")) {
            Object argsValue = json.get("args");
            if (argsValue instanceof JSONObject) {
                return (JSONObject) argsValue;
            }
        }
        
        // 递归查找嵌套对象中的args字段
        for (Object value : json.values()) {
            if (value instanceof JSONObject) {
                JSONObject args = extractArgsFromJson((JSONObject) value);
                if (args != null && !args.isEmpty()) {
                    return args;
                }
            } else if (value instanceof JSONArray) {
                JSONArray array = (JSONArray) value;
                for (Object item : array) {
                    if (item instanceof JSONObject) {
                        JSONObject args = extractArgsFromJson((JSONObject) item);
                        if (args != null && !args.isEmpty()) {
                            return args;
                        }
                    }
                }
            }
        }
        
        return new JSONObject();
    }
    
    /**
     * 根据threadId获取所有工具结果，从content字段提取id，然后查询graph_cache表
     * 
     * @param threadId 线程ID
     * @return GraphCache列表
     */
    public List<GraphCache> queryGraphCache(String threadId) {
        log.info("开始根据threadId查询GraphCache，threadId: {}", threadId);
        
        if (threadId == null || threadId.trim().isEmpty()) {
            log.warn("线程ID不能为空");
            return List.of();
        }
        
        // 1. 首先获取该threadId的所有工具结果
        List<McpToolResultQueryDTO> toolResults = getAllToolResultsByThreadId(threadId);
        if (toolResults.isEmpty()) {
            log.info("未找到threadId对应的工具结果，threadId: {}", threadId);
            return List.of();
        }
        
        // 2. 从content字段中提取所有的id
        Set<String> extractedIds = new HashSet<>();
        for (McpToolResultQueryDTO result : toolResults) {
            Set<String> ids = extractIdsFromContent(result.getContent());
            extractedIds.addAll(ids);
        }
        
        log.info("从content字段提取到{}个唯一id", extractedIds.size());
        
        // 3. 使用提取到的id作为threadId查询graph_cache表
        List<GraphCache> graphCaches = new ArrayList<>();
        for (String id : extractedIds) {
            List<GraphCache> caches = graphCacheRepository.findByThreadId(id);
            graphCaches.addAll(caches);
        }
        
        log.info("查询GraphCache完成，找到{}条记录", graphCaches.size());
        return graphCaches;
    }
    
    /**
     * 从content字段中提取id
     * 支持多种格式的id提取
     * 
     * @param content 内容字符串
     * @return 提取到的id集合
     */
    private Set<String> extractIdsFromContent(String content) {
        Set<String> ids = new HashSet<>();
        
        if (content == null || content.trim().isEmpty()) {
            return ids;
        }
        
        try {
            // 尝试解析为JSON对象
            JSONObject jsonObject = JSON.parseObject(content);
            extractIdsFromJson(jsonObject, ids);
        } catch (Exception e) {
            // 如果不是有效的JSON，尝试使用正则表达式提取
            log.debug("内容不是有效JSON，使用正则表达式提取id: {}", e.getMessage());
            extractIdsWithRegex(content, ids);
        }
        
        return ids;
    }
    
    /**
     * 从JSON对象中递归提取id字段
     * 
     * @param json JSON对象
     * @param ids id集合
     */
    private void extractIdsFromJson(Object json, Set<String> ids) {
        if (json instanceof JSONObject) {
            JSONObject jsonObject = (JSONObject) json;
            
            // 检查是否有id字段
            if (jsonObject.containsKey("id")) {
                Object idValue = jsonObject.get("id");
                if (idValue != null) {
                    ids.add(idValue.toString());
                }
            }
            
            // 递归处理所有值
            for (Object value : jsonObject.values()) {
                extractIdsFromJson(value, ids);
            }
            
        } else if (json instanceof JSONArray) {
            JSONArray jsonArray = (JSONArray) json;
            for (Object item : jsonArray) {
                extractIdsFromJson(item, ids);
            }
        }
    }
    
    /**
     * 使用正则表达式从内容中提取id
     * 
     * @param content 内容字符串
     * @param ids id集合
     */
    private void extractIdsWithRegex(String content, Set<String> ids) {
        // 匹配常见的id模式
        List<Pattern> patterns = List.of(
            Pattern.compile("\"id\"\\s*:\\s*\"([^\"]+)\""),  // "id": "value"
            Pattern.compile("'id'\\s*:\\s*'([^']+)'"),      // 'id': 'value'
            Pattern.compile("id\\s*=\\s*([\\w\\-_]+)"),     // id=value
            Pattern.compile("\\bid\\s*:\\s*([\\w\\-_]+)")   // id: value
        );
        
        for (Pattern pattern : patterns) {
            Matcher matcher = pattern.matcher(content);
            while (matcher.find()) {
                String id = matcher.group(1);
                if (id != null && !id.trim().isEmpty()) {
                    ids.add(id.trim());
                }
            }
        }
    }
    
    /**
     * 调试工具结果到图缓存的完整映射过程
     * 分析工具结果内容、ID提取、以及graph_cache表关联情况
     * 
     * @param threadId 线程ID
     * @return 包含内容分析、ID提取、缓存映射的综合调试信息
     */
    public JSONObject debugToolResultToGraphCacheMapping(String threadId) {
        JSONObject debugInfo = new JSONObject();
        
        // 获取工具结果
        List<McpToolResultQueryDTO> toolResults = getAllToolResultsByThreadId(threadId);
        debugInfo.put("toolResultsCount", toolResults.size());
        
        Set<String> allExtractedIds = new HashSet<>();
        JSONArray contentAnalysis = new JSONArray();
        
        for (McpToolResultQueryDTO result : toolResults) {
            JSONObject contentInfo = new JSONObject();
            contentInfo.put("toolName", result.getName());
            
            if (result.getContent() != null) {
                contentInfo.put("contentLength", result.getContent().length());
                
                // 提取ID
                Set<String> extractedIds = extractIdsFromContent(result.getContent());
                contentInfo.put("extractedIdsCount", extractedIds.size());
                contentInfo.put("extractedIds", new JSONArray(extractedIds));
                
                allExtractedIds.addAll(extractedIds);
                
                // 添加内容预览
                String preview = result.getContent().length() > 300 
                    ? result.getContent().substring(0, 300) + "..." 
                    : result.getContent();
                contentInfo.put("contentPreview", preview);
            } else {
                contentInfo.put("contentLength", 0);
                contentInfo.put("extractedIdsCount", 0);
                contentInfo.put("extractedIds", new JSONArray());
            }
            
            contentAnalysis.add(contentInfo);
        }
        
        debugInfo.put("contentAnalysis", contentAnalysis);
        debugInfo.put("totalUniqueIds", allExtractedIds.size());
        debugInfo.put("allExtractedIds", new JSONArray(allExtractedIds));
        
        // 检查每个提取的ID在graph_cache表中的情况
        JSONArray graphCacheCheck = new JSONArray();
        for (String id : allExtractedIds) {
            JSONObject cacheInfo = new JSONObject();
            cacheInfo.put("extractedId", id);
            
            List<GraphCache> caches = graphCacheRepository.findByThreadId(id);
            cacheInfo.put("foundInGraphCache", caches.size());
            
            if (!caches.isEmpty()) {
                JSONArray cacheDetails = new JSONArray();
                for (GraphCache cache : caches) {
                    JSONObject cacheDetail = new JSONObject();
                    cacheDetail.put("id", cache.getId());
                    cacheDetail.put("threadId", cache.getThreadId());
                    cacheDetail.put("dataLength", cache.getContent() != null ? cache.getContent().length() : 0);
                    cacheDetails.add(cacheDetail);
                }
                cacheInfo.put("cacheDetails", cacheDetails);
            }
            
            graphCacheCheck.add(cacheInfo);
        }
        
        debugInfo.put("graphCacheCheck", graphCacheCheck);
        
        return debugInfo;
    }
    
    /**
     * 获取graph_cache表中的数据样本
     * 
     * @param limit 限制返回条数
     * @return 样本数据信息
     */
    public JSONObject getGraphCacheSample(int limit) {
        JSONObject sampleInfo = new JSONObject();
        
        // 获取总记录数
        long totalCount = graphCacheRepository.count();
        sampleInfo.put("totalCount", totalCount);
        
        if (totalCount > 0) {
            // 获取样本数据
            List<GraphCache> samples = graphCacheRepository.findAll().stream()
                .limit(limit)
                .toList();
            
            JSONArray sampleData = new JSONArray();
            for (GraphCache cache : samples) {
                JSONObject cacheInfo = new JSONObject();
                cacheInfo.put("id", cache.getId());
                cacheInfo.put("threadId", cache.getThreadId());
                cacheInfo.put("dataLength", cache.getContent() != null ? cache.getContent().length() : 0);
                cacheInfo.put("createdTime", cache.getCreatedTime());
                cacheInfo.put("updatedTime", cache.getUpdatedTime());
                
                // 添加缓存数据预览
                if (cache.getContent() != null) {
                    String preview = cache.getContent().length() > 200
                        ? cache.getContent().substring(0, 200) + "..."
                        : cache.getContent();
                    cacheInfo.put("dataPreview", preview);
                }
                
                sampleData.add(cacheInfo);
            }
            
            sampleInfo.put("samples", sampleData);
            sampleInfo.put("sampleCount", samples.size());
        } else {
            sampleInfo.put("samples", new JSONArray());
            sampleInfo.put("sampleCount", 0);
        }
        
        return sampleInfo;
    }
    
    /**
     * 诊断数据库连接和表访问问题
     * 
     * @return 诊断信息
     */
    public JSONObject diagnoseDatabaseConnection() {
        JSONObject diagnostic = new JSONObject();
        
        try {
            // 1. 查询当前连接的数据库名称
            String currentDatabase = graphCacheRepository.getCurrentDatabase();
            diagnostic.put("currentDatabase", currentDatabase);
            
            // 2. 查询所有可用的数据库
            List<String> databases = graphCacheRepository.showDatabases();
            diagnostic.put("availableDatabases", databases);
            
            // 3. 检查graph_cache表是否存在
            List<String> tableExists = graphCacheRepository.checkTableExists();
            diagnostic.put("graphCacheTableExists", !tableExists.isEmpty());
            diagnostic.put("tableCheckResult", tableExists);
            
            // 4. 使用原生SQL查询记录数
            try {
                Long nativeCount = graphCacheRepository.countByNativeQuery();
                diagnostic.put("nativeQueryCount", nativeCount);
            } catch (Exception e) {
                diagnostic.put("nativeQueryError", e.getMessage());
            }
            
            // 5. 查询现有的thread_id样本
            try {
                List<String> threadIds = graphCacheRepository.findDistinctThreadIds();
                diagnostic.put("sampleThreadIds", threadIds);
                diagnostic.put("distinctThreadIdCount", threadIds.size());
            } catch (Exception e) {
                diagnostic.put("threadIdQueryError", e.getMessage());
            }
            
            diagnostic.put("success", true);
            
        } catch (Exception e) {
            log.error("数据库连接诊断失败", e);
            diagnostic.put("success", false);
            diagnostic.put("error", e.getMessage());
        }
        
        return diagnostic;
    }
}