package com.datacent.agent.service;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.datacent.agent.entity.GraphCache;
import com.datacent.agent.entity.McpToolResult;
import com.datacent.agent.entity.ToolCallName;
import com.datacent.agent.repository.GraphCacheRepository;
import com.datacent.agent.repository.McpToolResultRepository;
import com.datacent.agent.repository.ToolCallNameRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 异步实体处理服务
 * 专门处理异步的实体提取和缓存操作，解决Spring异步自调用问题
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AsyncEntityProcessor {

    private final ExtractEntityService extractEntityService;
    private final GraphCacheRepository graphCacheRepository;
    private final GraphQueryService graphQueryService;
    private final McpToolResultRepository mcpToolResultRepository;
    private final ToolCallNameRepository toolCallNameRepository;
    private final McpToolResultCacheService mcpToolResultCacheService;
    /**
     * 异步提取实体并保存到缓存
     * 
     * @param message 待处理的消息
     * @param threadId 线程ID
     */
    //@Async("entityExtractorExecutor")
    public void extractAndCacheEntitiesAsync(String message, String threadId) {
        try {
            log.info("开始异步实体提取，threadId: {}", threadId);

            String mcpId = UUID.randomUUID().toString();
            String callId = "call_" + UUID.randomUUID();
            String mcpToolFunction = "above_picture";
            // 调用实体提取服务
            String entities = extractEntityService.extractEntity(message);


            if (entities == null || entities.trim().isEmpty()) {
                log.warn("实体提取返回空结果，跳过保存操作，threadId: {}", threadId);
                return;
            }

            log.info("实体提取完成，threadId: {}, 结果长度: {}", threadId, entities.length());
            log.debug("提取的实体内容: {}", entities);

            // 从实体提取结果中解析实体名称
            List<String> entityNames = extractEntityNamesFromJson(entities);


            // 创建并保存实体提取工具结果
            createAndSaveEntityExtractionToolResult(threadId, mcpId, callId, mcpToolFunction, entityNames);

            // 工具名称保存
            createAndSaveEntityExtractionToolCallName(threadId, mcpId, callId, mcpToolFunction, entityNames);

            // 判断是否有有效的实体提取结果
            boolean hasValidEntities = !entityNames.isEmpty();
            boolean hasValidEntityContent = isValidEntityContent(entities);
            
            String graphQueryResult = null;
            boolean hasGraphData = false;

            // 尝试查询图数据库
            if (hasValidEntities) {
                try {
                    log.info("从实体提取结果解析出{}个实体名称: {}", entityNames.size(), entityNames);
                    
                    // 查询图数据库
                     graphQueryResult = graphQueryService.queryGraphByEntityNames(entityNames);
                    log.info("graphQueryResult: {}", graphQueryResult);
                    
                    if (graphQueryResult != null && !graphQueryResult.trim().isEmpty() && 
                        !isEmptyGraphResult(graphQueryResult) && !isErrorGraphResult(graphQueryResult)) {
                        hasGraphData = true;
                        log.info("图数据库查询成功，结果长度: {}", graphQueryResult.length());
                        log.debug("图查询结果内容: {}", graphQueryResult);
                    } else {
                        log.info("图数据库查询未返回有效数据");
                    }
                } catch (Exception e) {
                    log.error("图数据库查询失败，threadId: {}", threadId, e);
                }
            } else {
                log.info("未找到可查询的实体名称");
            }

            // 只有在以下情况下才保存数据：
            // 1. 图数据库查询成功并返回有效数据
            // 2. 或虽然图查询失败但实体提取结果本身有价值
            if (hasGraphData) {
                // 保存图查询结果
                saveToGraphCache(mcpId, graphQueryResult);
                log.info("保存图数据库查询结果到缓存，mcpId: {}", mcpId);

            } else if (hasValidEntityContent) {
                // 保存有效的实体提取结果
                saveToGraphCache(mcpId, graphQueryResult);
                log.info("保存有效的实体提取结果到缓存，mcpId: {}", mcpId);
            } else {
                log.info("未提取到有效数据，跳过保存操作，mcpId: {}", mcpId);
            }
            
        } catch (Exception e) {
            log.error("异步实体提取和缓存失败，threadId: {}", threadId, e);
        }
    }
    
    /**
     * 从实体提取结果JSON中解析实体名称
     * @param entitiesJsonStr 实体提取结果JSON字符串
     * @return 实体名称列表
     */
    private List<String> extractEntityNamesFromJson(String entitiesJsonStr) {
        List<String> entityNames = new ArrayList<>();
        
        try {
            JSONObject entitiesJson = JSON.parseObject(entitiesJsonStr);
            
            // 从target_persons中提取名称
            JSONArray targetPersons = entitiesJson.getJSONArray("target_persons");
            if (targetPersons != null) {
                for (int i = 0; i < targetPersons.size(); i++) {
                    String name = targetPersons.getString(i);
                    if (name != null && !name.trim().isEmpty() && !"name1".equals(name) && !"name2".equals(name)) {
                        entityNames.add(name.trim());
                    }
                }
            }
            
            // 从extracted_entities中提取名称
            JSONArray extractedEntities = entitiesJson.getJSONArray("extracted_entities");
            if (extractedEntities != null) {
                for (int i = 0; i < extractedEntities.size(); i++) {
                    JSONObject entity = extractedEntities.getJSONObject(i);
                    if (entity != null) {
                        String name = entity.getString("name");
                        String type = entity.getString("type");
                        
                        // 过滤有效的实体名称（跳过模板默认值）
                        if (name != null && !name.trim().isEmpty() && 
                            !"name".equals(name) && 
                            type != null && 
                            !"celebrity/event/work".equals(type)) {
                            entityNames.add(name.trim());
                        }
                    }
                }
            }
            
            // 去重
            entityNames = entityNames.stream().distinct().collect(java.util.stream.Collectors.toList());
            
        } catch (Exception e) {
            log.error("解析实体名称失败: {}", e.getMessage());
        }
        
        return entityNames;
    }
    
    /**
     * 判断实体提取内容是否有效
     * @param entities 实体提取结果JSON字符串
     * @return 是否为有效内容
     */
    private boolean isValidEntityContent(String entities) {
        if (entities == null || entities.trim().isEmpty()) {
            return false;
        }
        
        // 排除空JSON对象
        if ("{}".equals(entities.trim())) {
            return false;
        }
        
        try {
            JSONObject entitiesJson = JSON.parseObject(entities);
            
            // 检查是否只包含模板默认值
            JSONArray targetPersons = entitiesJson.getJSONArray("target_persons");
            JSONArray extractedEntities = entitiesJson.getJSONArray("extracted_entities");
            
            boolean hasValidTargetPersons = false;
            if (targetPersons != null) {
                for (int i = 0; i < targetPersons.size(); i++) {
                    String name = targetPersons.getString(i);
                    if (name != null && !name.trim().isEmpty() && !"name1".equals(name) && !"name2".equals(name)) {
                        hasValidTargetPersons = true;
                        break;
                    }
                }
            }
            
            boolean hasValidExtractedEntities = false;
            if (extractedEntities != null) {
                for (int i = 0; i < extractedEntities.size(); i++) {
                    JSONObject entity = extractedEntities.getJSONObject(i);
                    if (entity != null) {
                        String name = entity.getString("name");
                        String type = entity.getString("type");
                        
                        if (name != null && !name.trim().isEmpty() && 
                            !"name".equals(name) && 
                            type != null && 
                            !"celebrity/event/work".equals(type)) {
                            hasValidExtractedEntities = true;
                            break;
                        }
                    }
                }
            }
            
            return hasValidTargetPersons || hasValidExtractedEntities;
            
        } catch (Exception e) {
            log.warn("解析实体内容时出错: {}", e.getMessage());
            return false;
        }
    }
    
    /**
     * 判断图查询结果是否为空
     * @param graphResult 图查询结果
     * @return 是否为空结果
     */
    private boolean isEmptyGraphResult(String graphResult) {
        if (graphResult == null || graphResult.trim().isEmpty()) {
            return true;
        }
        
        String trimmed = graphResult.trim();
        if ("{}".equals(trimmed) || "[]".equals(trimmed) || "null".equals(trimmed)) {
            return true;
        }
        
        // 检查是否为我们自定义的空结果格式
        try {
            JSONObject jsonResult = JSON.parseObject(trimmed);
            String queryStatus = jsonResult.getString("query_status");
            if ("empty".equals(queryStatus)) {
                return true;
            }
            
            JSONArray graphQueryResults = jsonResult.getJSONArray("graph_query_results");
            return graphQueryResults == null || graphQueryResults.isEmpty();
            
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * 判断图查询结果是否为错误结果
     * @param graphResult 图查询结果
     * @return 是否为错误结果
     */
    private boolean isErrorGraphResult(String graphResult) {
        if (graphResult == null || graphResult.trim().isEmpty()) {
            return false;
        }
        
        try {
            JSONObject jsonResult = JSON.parseObject(graphResult.trim());
            
            // 检查我们自定义的错误格式
            String queryStatus = jsonResult.getString("query_status");
            if ("error".equals(queryStatus)) {
                return true;
            }
            
            // 检查API返回的错误格式
            Integer status = jsonResult.getInteger("status");
            if (status != null && status >= 400) {
                return true;
            }
            
            return false;
            
        } catch (Exception e) {
            // 如果包含明显的错误信息关键词，也认为是错误结果
            String lowerCase = graphResult.toLowerCase();
            return lowerCase.contains("error") || lowerCase.contains("exception") || 
                   lowerCase.contains("cannot be null") || lowerCase.contains("invalid");
        }
    }
    
    /**
     * 保存数据到GraphCache
     * @param threadId 线程ID
     * @param content 要保存的内容
     */
    private void saveToGraphCache(String threadId, String content) {
        try {
            GraphCache graphCache = GraphCache.builder()
                    .threadId(threadId)
                    .content(content)
                    .build();

            GraphCache savedCache = graphCacheRepository.save(graphCache);
            log.info("成功保存到graph_cache表，ID: {}, threadId: {}", savedCache.getId(), threadId);
        } catch (Exception e) {
            log.error("保存到GraphCache失败，threadId: {}", threadId, e);
            throw e;
        }
    }

    /**
     * 创建并保存实体提取工具结果
     * @param threadId 会话线程ID
     * @param mcpId MCP工具ID
     * @param entityNames 提取的实体名称列表
     * @return 创建的McpToolResult对象
     */
    private McpToolResult createAndSaveEntityExtractionToolResult(String threadId, String mcpId, String callId, String mcpToolFunction,  List<String> entityNames) {
        try {
            // 构建args对象
            JSONObject args = new JSONObject();
            args.put("names", entityNames);
            
            // 构建data对象（工具调用内容）
            JSONObject dataObject = new JSONObject();
            dataObject.put("id", mcpId);
            dataObject.put("mcp_tool_function", mcpToolFunction);
            dataObject.put("args", args);
            dataObject.put("details", new JSONObject());

            // 构建外层包装结构
            JSONObject wrapperObject = new JSONObject();
            wrapperObject.put("data", dataObject);
            wrapperObject.put("progress", 100);
            wrapperObject.put("error", null);
            wrapperObject.put("message", null);
            wrapperObject.put("status", "COMPLETED");

            // 创建McpToolResult对象
            McpToolResult toolResult = McpToolResult.builder()
                    .threadId(threadId)
                    .agent("extracter")
                    .resultId(UUID.randomUUID().toString())
                    .role("System")
                    .content(wrapperObject.toString())
                    .toolCallId(callId)
                    .build();


            // 先放入队列中，支持缓存优先访问
            mcpToolResultCacheService.addToQueue(threadId, toolResult);
            log.debug("成功将实体提取工具结果添加到缓存队列，threadId: {}, toolCallId: {}", threadId, callId);

            // 保存到数据库（保持原有逻辑不变）
            McpToolResult savedResult = mcpToolResultRepository.save(toolResult);
            log.info("成功保存实体提取工具结果，threadId: {}, resultId: {}", threadId, savedResult.getResultId());
            
            return savedResult;
        } catch (Exception e) {
            log.error("创建并保存实体提取工具结果失败，threadId: {}", threadId, e);
            throw e;
        }
    }

    /**
     * 创建并保存实体提取工具调用名称
     * @param threadId 会话线程ID
     * @param mcpId MCP工具ID
     * @param callId 调用ID
     * @param mcpToolFunction MCP工具函数名
     * @param entityNames 提取的实体名称列表
     */
    private void createAndSaveEntityExtractionToolCallName(String threadId, String mcpId, String callId, String mcpToolFunction, List<String> entityNames) {
        try {
            ToolCallName callName = ToolCallName.builder()
                    .name(mcpToolFunction)
                    .callId(callId)
                    .type("sys_tool_call")
                    .args("{}")
                    .callIndex(0)
                    .build();
                    
            toolCallNameRepository.save(callName);
            log.info("成功保存工具调用名称，threadId: {}, callId: {}, name: {}", threadId, callId, mcpToolFunction);
        } catch (Exception e) {
            log.error("创建并保存工具调用名称失败，threadId: {}, callId: {}", threadId, callId, e);
            throw e;
        }
    }
}