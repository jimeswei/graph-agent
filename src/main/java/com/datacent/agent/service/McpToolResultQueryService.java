package com.datacent.agent.service;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.datacent.agent.dto.McpToolResultQueryDTO;
import com.datacent.agent.dto.McpToolNameDTO;
import com.datacent.agent.dto.McpToolDetailQueryDTO;
import com.datacent.agent.entity.GraphCache;
import com.datacent.agent.entity.AgentReport;
import com.datacent.agent.entity.McpToolResult;
import com.datacent.agent.entity.ToolCallName;
import com.datacent.agent.repository.GraphCacheRepository;
import com.datacent.agent.repository.McpToolResultRepository;
import com.datacent.agent.repository.AgentReportRepository;
import com.datacent.agent.repository.ToolCallNameRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
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
    
    private final AgentReportRepository agentReportRepository;
    
    private final GraphQueryService graphQueryService;
    
    private final McpToolResultCacheService mcpToolResultCacheService;
    
    private final ToolCallNameRepository toolCallNameRepository;


    
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
     * 优先从缓存队列查询单条MCP工具调用结果，队列为空时查询数据库
     * 参考queryMcpTools的逻辑，但每次只返回一条数据
     * 
     * @param threadId 线程ID
     * @return 格式化后的单条工具调用结果，如果没有数据则返回null
     */
    public JSONObject queryMcpTool(String threadId) {
        log.info("查询单条MCP工具结果，threadId: {}", threadId);
        
        if (threadId == null || threadId.trim().isEmpty()) {
            log.warn("线程ID不能为空");
            return null;
        }
        
        try {
            // 1. 优先从缓存队列获取单条MCP工具调用结果（破坏性读取）
            McpToolResult cacheResult = mcpToolResultCacheService.pollFromQueue(threadId);
            
            if (cacheResult != null) {
                log.info("从缓存队列获取到单条工具调用结果，toolCallId: {}", cacheResult.getToolCallId());
                
                // 通过toolCallId获取工具名称
                String toolName = getToolNameByCallId(cacheResult.getToolCallId());
                if (toolName == null) {
                    log.warn("无法获取工具名称，toolCallId: {}", cacheResult.getToolCallId());
                    toolName = "unknown"; // 设置默认值
                }
                
                // 处理并格式化content中的数据
                JSONObject extractedData = extractDataFromContent(cacheResult.getContent(), toolName);
                if (extractedData != null) {
                    log.info("成功处理缓存中的工具调用结果");
                    return extractedData;
                } else {
                    log.warn("处理缓存中的工具调用结果失败，无法提取数据");
                    return null;
                }
                
            } else {
//                log.info("缓存队列为空，回退到数据库查询单条数据，threadId: {}", threadId);
//
//                // 2. 缓存为空时，从数据库获取单条工具结果
//                List<McpToolDetailQueryDTO> rawResults = mcpToolResultRepository.findMcpToolsByThreadId(threadId);
//
//                if (rawResults.isEmpty()) {
//                    log.info("数据库中也未找到threadId对应的工具结果，threadId: {}", threadId);
//                    return null;
//                }
//
//                // 获取第一条数据
//                McpToolDetailQueryDTO dbResult = rawResults.get(0);
//                log.info("从数据库获取到单条工具调用结果，工具名称: {}", dbResult.getName());
//
//                // 处理并格式化content中的数据
//                JSONObject extractedData = extractDataFromContent(dbResult.getContent(), dbResult.getName());
//                if (extractedData != null) {
//                    log.info("成功处理数据库中的工具调用结果");
//                    return extractedData;
//                } else {
//                    log.warn("处理数据库中的工具调用结果失败，工具名称: {}", dbResult.getName());
//
//                }
                return null;
            }
            
        } catch (Exception e) {
            log.error("查询单条MCP工具结果失败，threadId: {}", threadId, e);
            return null;
        }
    }
    
    /**
     * 通过工具调用ID获取工具名称
     * 
     * @param toolCallId 工具调用ID
     * @return 工具名称，如果未找到则返回null
     */
    private String getToolNameByCallId(String toolCallId) {
        try {
            if (toolCallId == null || toolCallId.trim().isEmpty()) {
                return null;
            }
            
            List<ToolCallName> toolCallNames = toolCallNameRepository.findByCallId(toolCallId);
            if (!toolCallNames.isEmpty()) {
                String toolName = toolCallNames.get(0).getName();
                log.debug("通过toolCallId获取到工具名称: {} -> {}", toolCallId, toolName);
                return toolName;
            } else {
                log.debug("未找到toolCallId对应的工具名称: {}", toolCallId);
                return null;
            }
        } catch (Exception e) {
            log.error("通过toolCallId获取工具名称失败: {}", toolCallId, e);
            return null;
        }
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
                // 根据args查询图数据库
                JSONArray vertices = queryVerticesFromArgs(args);
                data.put("vertices", vertices);
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
     * 根据args参数查询图数据库顶点信息
     * 
     * @param args 包含查询参数的JSON对象
     * @return 图数据库查询结果的顶点数组
     */
    private JSONArray queryVerticesFromArgs(JSONObject args) {
        if (args == null || args.isEmpty()) {
            log.debug("args为空，返回空的顶点数组");
            return new JSONArray();
        }
        
        // 增加详细调试日志
        log.info("开始处理args: {}", args.toJSONString());
        
        try {
            // 提取人名列表，支持多种可能的字段名
            List<String> entityNames = extractEntityNamesFromArgs(args);
            
            if (entityNames.isEmpty()) {
                log.warn("从args中未提取到实体名称，args内容: {}, 返回空的顶点数组", args.toJSONString());
                return new JSONArray();
            }
            
            log.info("从args中提取到{}个实体名称: {}", entityNames.size(), entityNames);
            
            // 调用图查询服务
            JSONArray vertices = graphQueryService.queryCelebrityVertices(entityNames);

            log.info("图查询服务返回{}个顶点", vertices.size());
            
            if (vertices.isEmpty()) {
                log.warn("图查询服务返回空结果，实体名称: {}", entityNames);
            } else {
                log.debug("图查询结果: {}", vertices.toJSONString());
            }
            
            return vertices;
            
        } catch (Exception e) {
            log.error("根据args查询图数据库失败，args: {}", args.toJSONString(), e);
            return new JSONArray();
        }
    }
    
    /**
     * 从args JSON对象中提取实体名称列表
     * 支持多种可能的字段格式：names, entityNames, celebrities等
     * 
     * @param args args JSON对象
     * @return 实体名称列表
     */
    private List<String> extractEntityNamesFromArgs(JSONObject args) {
        List<String> entityNames = new ArrayList<>();
        
        log.info("开始从args中提取实体名称，args所有字段: {}", args.keySet());
        
        // 支持多种可能的字段名，按优先级排序
        String[] possibleFieldNames = {
            "names", "entityNames", "celebrities", "persons", "people", 
            "actors", "stars", "entities", "targets", "subjects",
            "name", "celebrity", "person", "actor", "star"
        };
        
        // 首先尝试直接字段匹配
        for (String fieldName : possibleFieldNames) {
            if (args.containsKey(fieldName)) {
                Object value = args.get(fieldName);
                log.info("找到字段'{}', 值类型: {}, 值: {}", fieldName, value.getClass().getSimpleName(), value);
                
                List<String> extracted = extractNamesFromValue(value, fieldName);
                if (!extracted.isEmpty()) {
                    entityNames.addAll(extracted);
                    log.info("从字段'{}'中提取到{}个实体名称: {}", fieldName, extracted.size(), extracted);
                    return entityNames; // 找到就返回，不再继续查找
                }
            }
        }
        
        // 如果直接字段匹配失败，尝试递归查找所有字符串数组
        log.info("直接字段匹配失败，开始递归查找字符串数组");
        for (Map.Entry<String, Object> entry : args.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            
            log.debug("检查字段'{}', 值类型: {}", key, value.getClass().getSimpleName());
            
            List<String> extracted = extractNamesFromValue(value, key);
            if (!extracted.isEmpty()) {
                entityNames.addAll(extracted);
                log.info("从通用字段'{}'中提取到{}个实体名称: {}", key, extracted.size(), extracted);
                break; // 找到第一个有效的就停止
            }
        }
        
        // 如果还是没找到，尝试递归查找嵌套对象
        if (entityNames.isEmpty()) {
            log.info("在顶层未找到，开始递归查找嵌套对象");
            entityNames.addAll(extractNamesRecursively(args));
        }
        
        log.info("最终提取到{}个实体名称: {}", entityNames.size(), entityNames);
        return entityNames;
    }
    
    /**
     * 从单个值中提取实体名称
     * 
     * @param value 值对象
     * @param fieldName 字段名（用于日志）
     * @return 提取到的实体名称列表
     */
    private List<String> extractNamesFromValue(Object value, String fieldName) {
        List<String> names = new ArrayList<>();
        
        if (value == null) {
            return names;
        }
        
        if (value instanceof JSONArray) {
            JSONArray array = (JSONArray) value;
            log.debug("处理数组字段'{}', 数组长度: {}", fieldName, array.size());
            
            for (int i = 0; i < array.size(); i++) {
                Object item = array.get(i);
                if (item instanceof String) {
                    String name = item.toString().trim();
                    if (!name.isEmpty()) {
                        names.add(name);
                        log.debug("从数组索引{}提取到实体名称: {}", i, name);
                    }
                } else if (item != null) {
                    log.debug("数组索引{}的值不是字符串，类型: {}, 值: {}", i, item.getClass().getSimpleName(), item);
                }
            }
        } else if (value instanceof String) {
            String name = value.toString().trim();
            if (!name.isEmpty()) {
                names.add(name);
                log.debug("从字符串字段'{}'提取到实体名称: {}", fieldName, name);
            }
        } else {
            log.debug("字段'{}'的值既不是数组也不是字符串，类型: {}", fieldName, value.getClass().getSimpleName());
        }
        
        return names;
    }
    
    /**
     * 递归查找嵌套对象中的实体名称
     * 
     * @param jsonObject JSON对象
     * @return 提取到的实体名称列表
     */
    private List<String> extractNamesRecursively(JSONObject jsonObject) {
        List<String> entityNames = new ArrayList<>();
        
        for (Map.Entry<String, Object> entry : jsonObject.entrySet()) {
            Object value = entry.getValue();
            
            if (value instanceof JSONObject) {
                // 递归处理嵌套对象
                JSONObject nestedObj = (JSONObject) value;
                entityNames.addAll(extractEntityNamesFromArgs(nestedObj));
            } else if (value instanceof JSONArray) {
                JSONArray array = (JSONArray) value;
                for (Object item : array) {
                    if (item instanceof JSONObject) {
                        entityNames.addAll(extractEntityNamesFromArgs((JSONObject) item));
                    }
                }
            }
        }
        
        return entityNames;
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
        // TODO 优先从队列取，队列为空查询数据库
        mcpToolResultCacheService.pollFromQueue(threadId);

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
    
    /**
     * 根据MCP工具ID查询GraphCache数据
     * 直接根据传入的mcpToolsId作为threadId查询graph_cache表
     * 
     * @param mcpToolsId MCP工具ID（作为threadId使用）
     * @return GraphCache数据列表
     */
    public List<GraphCache> queryGraphCacheByMcpToolsId(String mcpToolsId) {
        log.info("根据MCP工具ID查询GraphCache，mcpToolsId: {}", mcpToolsId);
        
        if (mcpToolsId == null || mcpToolsId.trim().isEmpty()) {
            log.warn("MCP工具ID不能为空");
            return List.of();
        }
        
        try {
            List<GraphCache> results = graphCacheRepository.findByThreadId(mcpToolsId);
            log.info("查询GraphCache完成，找到{}条记录", results.size());
            return results;
        } catch (Exception e) {
            log.error("根据MCP工具ID查询GraphCache失败，mcpToolsId: {}", mcpToolsId, e);
            return List.of();
        }
    }
    
    /**
     * 根据线程ID查询代理报告
     * 查询agent_report表中的报告数据，返回格式化的JSON对象列表
     * 
     * @param threadId 线程ID
     * @return 代理报告列表
     */
    public List<JSONObject> queryAgentReport(String threadId) {
        log.info("查询代理报告，threadId: {}", threadId);
        
        if (threadId == null || threadId.trim().isEmpty()) {
            log.warn("线程ID不能为空");
            return List.of();
        }
        
        try {
            // 查询agent_report表中的数据
            List<AgentReport> reports = agentReportRepository.findByThreadId(threadId);
            log.info("查询到{}条代理报告", reports.size());
            
            // 转换为JSONObject格式
            List<JSONObject> results = new ArrayList<>();
            for (AgentReport report : reports) {
                JSONObject result = new JSONObject();
                result.put("id", report.getId());
                result.put("threadId", report.getThreadId());
                result.put("agent", report.getAgent());
                result.put("content", report.getContent());
                result.put("reasoningContent", report.getReasoningContent());
                result.put("createdTime", report.getCreatedTime());
                result.put("updatedTime", report.getUpdatedTime());
                results.add(result);
            }
            
            log.info("成功转换{}条报告为JSON格式", results.size());
            return results;
            
        } catch (Exception e) {
            log.error("查询代理报告失败，threadId: {}", threadId, e);
            return List.of();
        }
    }
    
    /**
     * 从消息队列缓存中查询MCP工具调用结果
     * 优先从缓存队列获取数据，如果缓存为空则回退到数据库查询
     * 
     * @param threadId 线程ID
     * @return MCP工具调用结果列表
     */
    public List<McpToolResult> getToolResultsFromCache(String threadId) {
        log.info("从消息队列缓存查询MCP工具调用结果，threadId: {}", threadId);
        
        if (threadId == null || threadId.trim().isEmpty()) {
            log.warn("线程ID不能为空");
            return List.of();
        }
        
        try {
            // 1. 首先尝试从缓存队列获取数据
            List<McpToolResult> cacheResults = mcpToolResultCacheService.getAllFromQueue(threadId);
            
            if (!cacheResults.isEmpty()) {
                log.info("从缓存队列获取到{}条MCP工具调用结果", cacheResults.size());
                return cacheResults;
            }
            
            // 2. 如果缓存为空，回退到数据库查询
            log.debug("缓存队列为空，回退到数据库查询，threadId: {}", threadId);
            
            // 直接使用JPA repository方法查询完整的实体对象
            List<McpToolResult> entityResults = mcpToolResultRepository.findByThreadId(threadId);
            
            log.info("从数据库回退查询获取到{}条MCP工具调用结果", entityResults.size());
            return entityResults;
            
        } catch (Exception e) {
            log.error("从消息队列缓存查询MCP工具调用结果失败，threadId: {}", threadId, e);
            return List.of();
        }
    }
    
    /**
     * 检查指定threadId是否存在缓存队列数据
     * 
     * @param threadId 线程ID
     * @return 是否存在缓存数据
     */
    public boolean hasCacheData(String threadId) {
        if (threadId == null || threadId.trim().isEmpty()) {
            return false;
        }
        
        return mcpToolResultCacheService.hasQueue(threadId) && 
               mcpToolResultCacheService.getQueueSize(threadId) > 0;
    }
    
    /**
     * 获取缓存队列大小
     * 
     * @param threadId 线程ID
     * @return 缓存队列大小
     */
    public int getCacheQueueSize(String threadId) {
        if (threadId == null || threadId.trim().isEmpty()) {
            return 0;
        }
        
        return mcpToolResultCacheService.getQueueSize(threadId);
    }
    
    /**
     * 清空指定threadId的缓存队列
     * 用于会话结束后的资源清理
     * 
     * @param threadId 线程ID
     * @return 清空前的队列大小
     */
    public int clearCacheQueue(String threadId) {
        if (threadId == null || threadId.trim().isEmpty()) {
            log.warn("线程ID不能为空，无法清空缓存队列");
            return 0;
        }
        
        try {
            int clearedSize = mcpToolResultCacheService.clearQueue(threadId);
            log.info("清空缓存队列完成，threadId: {}, 清空前大小: {}", threadId, clearedSize);
            return clearedSize;
        } catch (Exception e) {
            log.error("清空缓存队列失败，threadId: {}", threadId, e);
            return 0;
        }
    }
    
    /**
     * 获取消息队列缓存的统计信息
     * 
     * @return 缓存统计信息
     */
    public Map<String, Object> getCacheStatistics() {
        try {
            Map<String, Object> statistics = mcpToolResultCacheService.getCacheStatistics();
            log.debug("获取缓存统计信息成功");
            return statistics;
        } catch (Exception e) {
            log.error("获取缓存统计信息失败", e);
            return Map.of("error", e.getMessage());
        }
    }
    
    /**
     * 优先从缓存队列查询单条MCP工具调用结果，队列为空时查询数据库
     * 参考queryGraphCache的逻辑，但每次只返回一条数据
     * 
     * @param threadId 线程ID
     * @return MCP工具调用结果，如果没有数据则返回null
     */
    public JSONObject mcpResultCache(String threadId) {
        log.info("开始从缓存优先查询单条MCP工具调用结果，threadId: {}", threadId);
        
        if (threadId == null || threadId.trim().isEmpty()) {
            log.warn("线程ID不能为空");
            return null;
        }
        
        try {
            // 1. 优先从缓存队列获取单条MCP工具调用结果（破坏性读取）
            McpToolResult cacheResult = mcpToolResultCacheService.pollFromQueue(threadId);
            
            if (cacheResult != null) {
                log.info("从缓存队列获取到单条工具调用结果，toolCallId: {}", cacheResult.getToolCallId());
                
                // 构建返回的JSON对象，包含完整的MCP工具调用结果信息
                JSONObject result = new JSONObject();
                result.put("threadId", cacheResult.getThreadId());
                result.put("agent", cacheResult.getAgent());
                result.put("resultId", cacheResult.getResultId());
                result.put("role", cacheResult.getRole());
                result.put("content", cacheResult.getContent());
                result.put("toolCallId", cacheResult.getToolCallId());
                result.put("createdTime", cacheResult.getCreatedTime());
                result.put("updatedTime", cacheResult.getUpdatedTime());
                result.put("source", "cache"); // 标识数据来源
                
                // 从content字段中提取id并查询graph_cache表
                enhanceWithGraphCacheData(result, cacheResult.getContent());
                
                return result;
            } else {
                log.info("缓存队列为空，回退到数据库查询单条数据，threadId: {}", threadId);
                
                // 2. 缓存为空时，从数据库获取单条工具结果
                List<McpToolResult> dbResults = mcpToolResultRepository.findByThreadId(threadId);
                
                if (dbResults.isEmpty()) {
                    log.info("数据库中也未找到threadId对应的工具结果，threadId: {}", threadId);
                    return null;
                }
                
                // 获取第一条数据
                McpToolResult dbResult = dbResults.get(0);
                log.info("从数据库获取到单条工具调用结果，toolCallId: {}", dbResult.getToolCallId());
                
                // 构建返回的JSON对象
                JSONObject result = new JSONObject();
                result.put("threadId", dbResult.getThreadId());
                result.put("agent", dbResult.getAgent());
                result.put("resultId", dbResult.getResultId());
                result.put("role", dbResult.getRole());
                result.put("content", dbResult.getContent());
                result.put("toolCallId", dbResult.getToolCallId());
                result.put("createdTime", dbResult.getCreatedTime());
                result.put("updatedTime", dbResult.getUpdatedTime());
                result.put("source", "database"); // 标识数据来源
                
                // 从content字段中提取id并查询graph_cache表
                enhanceWithGraphCacheData(result, dbResult.getContent());
                
                return result;
            }
            
        } catch (Exception e) {
            log.error("从缓存优先查询单条MCP工具调用结果失败，threadId: {}", threadId, e);
            return null;
        }
    }
    
    /**
     * 增强返回结果，添加graph_cache相关数据
     * 从content字段中提取id，用提取到的id查询graph_cache表，并将结果添加到返回的JSON对象中
     * 
     * @param result 要增强的JSON结果对象
     * @param content MCP工具调用结果的content内容
     */
    private void enhanceWithGraphCacheData(JSONObject result, String content) {
        try {
            // 2. 从content字段中提取所有的id
            Set<String> extractedIds = new HashSet<>();
            if (content != null && !content.trim().isEmpty()) {
                Set<String> ids = extractIdsFromContent(content);
                extractedIds.addAll(ids);
            }
            
            log.info("从content字段提取到{}个唯一id", extractedIds.size());
            
            // 3. 使用提取到的id作为threadId查询graph_cache表
            List<GraphCache> graphCaches = new ArrayList<>();
            for (String id : extractedIds) {
                List<GraphCache> caches = graphCacheRepository.findByThreadId(id);
                graphCaches.addAll(caches);
            }
            
            log.info("根据提取的id查询GraphCache完成，找到{}条记录", graphCaches.size());
            
            // 将GraphCache数据添加到返回结果中
            JSONArray graphCacheArray = new JSONArray();
            for (GraphCache cache : graphCaches) {
                JSONObject cacheObj = new JSONObject();
                cacheObj.put("id", cache.getId());
                cacheObj.put("threadId", cache.getThreadId());
                cacheObj.put("content", cache.getContent());
                cacheObj.put("createdTime", cache.getCreatedTime());
                cacheObj.put("updatedTime", cache.getUpdatedTime());
                graphCacheArray.add(cacheObj);
            }
            
            // 添加GraphCache相关信息到结果中
            result.put("extractedIds", new JSONArray(extractedIds));
            result.put("extractedIdsCount", extractedIds.size());
            result.put("graphCaches", graphCacheArray);
            result.put("graphCachesCount", graphCaches.size());
            
        } catch (Exception e) {
            log.error("增强返回结果时提取GraphCache数据失败", e);
            // 出错时添加错误信息，但不影响原有数据的返回
            result.put("graphCacheError", e.getMessage());
            result.put("extractedIds", new JSONArray());
            result.put("extractedIdsCount", 0);
            result.put("graphCaches", new JSONArray());
            result.put("graphCachesCount", 0);
        }
    }
}