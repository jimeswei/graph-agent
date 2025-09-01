package com.datacent.agent.service;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.datacent.agent.config.GraphApiConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 图数据库查询服务
 */
@Slf4j
@Service
public class GraphQueryService {
    
    private final GraphApiConfig graphApiConfig;
    private final WebClient webClient;
    
    public GraphQueryService(GraphApiConfig graphApiConfig) {
        this.graphApiConfig = graphApiConfig;
        this.webClient = WebClient.builder()
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(10 * 1024 * 1024))
                .build();
    }
    
    /**
     * 根据实体名称查询图数据库
     * @param entityNames 实体名称列表
     * @return 图数据库查询结果的JSON字符串
     */
    public String queryGraphByEntityNames(List<String> entityNames) {
        if (entityNames == null || entityNames.isEmpty()) {
            log.warn("实体名称列表为空，跳过图数据库查询");
            return createEmptyGraphResult();
        }
        
        try {
            log.info("开始查询图数据库，实体数量: {}, 实体列表: {}", entityNames.size(), entityNames);
            
            // 构建gremlin查询语句
            String gremlinQuery = buildGremlinQuery(entityNames);
            
            // 构建请求体 - 修正API期望的参数格式
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("content", gremlinQuery);
            
            log.info("图数据库查询URL: {}", graphApiConfig.getBaseUrl());
            log.info("gremlin查询语句: {}", gremlinQuery);
            
            // 调用图数据库API
            String response = webClient.post()
                    .uri(graphApiConfig.getBaseUrl())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(BodyInserters.fromValue(requestBody))
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(Duration.ofSeconds(30))
                    .block();
            
            log.info("图数据库查询完成，响应长度: {}", response != null ? response.length() : 0);
            log.debug("图数据库查询响应: {}", response);
            
            return response != null ? response : createEmptyGraphResult();
            
        } catch (Exception e) {
            log.error("图数据库查询失败，实体列表: {}", entityNames, e);
            return createErrorGraphResult(e.getMessage());
        }
    }
    
    /**
     * 查询名人顶点信息（仅包含id, label, name）
     * @param entityNames 实体名称列表
     * @return 顶点信息JSON数组
     */
    public JSONArray queryCelebrityVertices(List<String> entityNames) {
        if (entityNames == null || entityNames.isEmpty()) {
            log.warn("实体名称列表为空，返回空数组");
            return new JSONArray();
        }
        
        try {
            log.info("查询名人顶点信息，实体数量: {}, 实体列表: {}", entityNames.size(), entityNames);
            
            // 构建专用的gremlin查询语句
            String gremlinQuery = buildVerticesGremlinQuery(entityNames);
            
            // 构建请求体
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("content", gremlinQuery);
            
            log.info("顶点查询gremlin语句: {}", gremlinQuery);
            
            // 调用图数据库API
            String response = webClient.post()
                    .uri(graphApiConfig.getBaseUrl())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(BodyInserters.fromValue(requestBody))
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(Duration.ofSeconds(30))
                    .block();
            
            log.info("顶点查询完成，响应长度: {}", response != null ? response.length() : 0);
            
            if (response != null) {
                return parseVerticesResponse(response);
            } else {
                return new JSONArray();
            }
            
        } catch (Exception e) {
            log.error("查询名人顶点信息失败，实体列表: {}", entityNames, e);
            return new JSONArray();
        }
    }
    
    /**
     * 构建用于查询顶点信息的gremlin语句
     * @param entityNames 实体名称列表
     * @return gremlin查询语句
     */
    private String buildVerticesGremlinQuery(List<String> entityNames) {
        // 构建实体名称数组字符串，格式：['刘德华', '周杰伦']
        StringBuilder namesArray = new StringBuilder("[");
        for (int i = 0; i < entityNames.size(); i++) {
            if (i > 0) {
                namesArray.append(", ");
            }
            namesArray.append("'").append(entityNames.get(i).replace("'", "\\'")).append("'");
        }
        namesArray.append("]");
        
        // 构建完整的gremlin查询语句（使用union分别查询celebrity、event、work实体类型）
        String gremlinQuery = String.format(
            "g.V().union(" +
            "has('celebrity', 'name', within(%s))," +
            "hasLabel('event').filter(values('event_name').is(within(%s)))," +
            "hasLabel('work').filter(values('title').is(within(%s)))" +
            ").project('id', 'label', 'name')" +
            ".by(id())" +
            ".by(label())" +
            ".by('name')",
            namesArray, namesArray, namesArray
        );
        
        return gremlinQuery;
    }
    
    /**
     * 解析顶点查询响应
     * 支持图数据库返回的标准格式：response.data.json_view.data
     * @param response 图数据库响应
     * @return 解析后的顶点数组
     */
    private JSONArray parseVerticesResponse(String response) {
        log.info("开始解析图数据库响应，响应长度: {}", response != null ? response.length() : 0);
        log.debug("原始响应内容: {}", response);
        
        try {
            JSONObject responseJson = JSON.parseObject(response);
            log.info("响应是JSON对象格式，顶层字段: {}", responseJson.keySet());
            
            // 首先尝试标准的图数据库响应格式：response.data.json_view.data
            JSONArray vertices = parseStandardGraphResponse(responseJson);
            if (vertices != null && !vertices.isEmpty()) {
                log.info("使用标准图数据库格式解析成功，获得{}个顶点", vertices.size());
                return vertices;
            }
            
            // 如果标准格式解析失败，尝试其他常见格式
            log.info("标准格式解析失败，尝试其他格式");
            
            // 如果响应本身就是数组
            if (response.trim().startsWith("[")) {
                JSONArray directArray = JSON.parseArray(response);
                log.info("响应是直接数组格式，解析到{}个顶点", directArray.size());
                return directArray;
            }
            
            // 尝试result字段
            if (responseJson.containsKey("result")) {
                Object result = responseJson.get("result");
                log.info("找到result字段，类型: {}", result.getClass().getSimpleName());
                if (result instanceof JSONArray) {
                    JSONArray resultArray = (JSONArray) result;
                    log.info("result字段是数组，包含{}个顶点", resultArray.size());
                    return resultArray;
                }
            }
            
            // 尝试直接的data字段
            if (responseJson.containsKey("data")) {
                Object data = responseJson.get("data");
                log.info("找到data字段，类型: {}", data.getClass().getSimpleName());
                if (data instanceof JSONArray) {
                    JSONArray dataArray = (JSONArray) data;
                    log.info("data字段是数组，包含{}个顶点", dataArray.size());
                    return dataArray;
                }
            }
            
            // 检查其他可能的字段
            for (Map.Entry<String, Object> entry : responseJson.entrySet()) {
                String key = entry.getKey();
                Object value = entry.getValue();
                if (value instanceof JSONArray) {
                    JSONArray array = (JSONArray) value;
                    if (!array.isEmpty()) {
                        log.info("在字段'{}'中找到非空数组，包含{}个元素", key, array.size());
                        return array;
                    }
                }
            }
            
            log.warn("无法解析顶点查询响应格式，响应内容: {}", response);
            return new JSONArray();
            
        } catch (Exception e) {
            log.error("解析顶点查询响应失败，响应内容: {}", response, e);
            return new JSONArray();
        }
    }
    
    /**
     * 解析标准的图数据库响应格式
     * 预期格式：{ "status": 200, "data": { "json_view": { "data": [...] } } }
     * 
     * @param responseJson 响应JSON对象
     * @return 顶点数组，如果解析失败返回null
     */
    private JSONArray parseStandardGraphResponse(JSONObject responseJson) {
        try {
            // 检查status字段
            if (responseJson.containsKey("status")) {
                Object statusObj = responseJson.get("status");
                int status = statusObj instanceof Number ? ((Number) statusObj).intValue() : -1;
                log.info("响应状态码: {}", status);
                
                if (status != 200) {
                    log.warn("图数据库响应状态码不是200，状态码: {}", status);
                    return null;
                }
            }
            
            // 导航到data字段
            if (!responseJson.containsKey("data")) {
                log.debug("响应中没有找到data字段");
                return null;
            }
            
            Object dataObj = responseJson.get("data");
            if (!(dataObj instanceof JSONObject)) {
                log.debug("data字段不是JSON对象，类型: {}", dataObj.getClass().getSimpleName());
                return null;
            }
            
            JSONObject dataJson = (JSONObject) dataObj;
            log.info("data对象字段: {}", dataJson.keySet());
            
            // 导航到json_view字段
            if (!dataJson.containsKey("json_view")) {
                log.debug("data对象中没有找到json_view字段");
                return null;
            }
            
            Object jsonViewObj = dataJson.get("json_view");
            if (!(jsonViewObj instanceof JSONObject)) {
                log.debug("json_view字段不是JSON对象，类型: {}", jsonViewObj.getClass().getSimpleName());
                return null;
            }
            
            JSONObject jsonViewJson = (JSONObject) jsonViewObj;
            log.info("json_view对象字段: {}", jsonViewJson.keySet());
            
            // 导航到最终的data字段
            if (!jsonViewJson.containsKey("data")) {
                log.debug("json_view对象中没有找到data字段");
                return null;
            }
            
            Object finalDataObj = jsonViewJson.get("data");
            if (!(finalDataObj instanceof JSONArray)) {
                log.debug("json_view.data字段不是数组，类型: {}", finalDataObj.getClass().getSimpleName());
                return null;
            }
            
            JSONArray verticesArray = (JSONArray) finalDataObj;
            log.info("成功解析标准图数据库格式，获得{}个顶点", verticesArray.size());
            
            // 验证数组内容格式
            if (!verticesArray.isEmpty()) {
                Object firstVertex = verticesArray.get(0);
                if (firstVertex instanceof JSONObject) {
                    JSONObject vertex = (JSONObject) firstVertex;
                    log.debug("第一个顶点的字段: {}", vertex.keySet());
                }
            }
            
            return verticesArray;
            
        } catch (Exception e) {
            log.error("解析标准图数据库格式失败", e);
            return null;
        }
    }
    
    /**
     * 构建gremlin查询语句
     * @param entityNames 实体名称列表
     * @return gremlin查询语句
     */
    private String buildGremlinQuery(List<String> entityNames) {
        // 构建实体名称数组字符串，格式：['刘德华', '周杰伦']
        StringBuilder namesArray = new StringBuilder("[");
        for (int i = 0; i < entityNames.size(); i++) {
            if (i > 0) {
                namesArray.append(", ");
            }
            namesArray.append("'").append(entityNames.get(i).replace("'", "\\'")).append("'");
        }
        namesArray.append("]");
        
        // 构建完整的gremlin查询语句（使用union分别查询celebrity、event、work实体类型）
        String gremlinQuery = String.format(
            "g.V().union(" +
            "has('celebrity', 'name', within(%s))," +
            "hasLabel('event').filter(values('event_name').is(within(%s)))," +
            "hasLabel('work').filter(values('title').is(within(%s)))" +
            ").project('id', 'label', 'name', 'profession')" +
            ".by(id())" +
            ".by(label())" +
            ".by('name')" +
            ".by('profession')",
            namesArray, namesArray, namesArray
        );
        
        return gremlinQuery;
    }
    
    /**
     * 创建空的图查询结果
     */
    private String createEmptyGraphResult() {
        JSONObject result = new JSONObject();
        result.put("graph_query_results", new JSONArray());
        result.put("query_status", "empty");
        return result.toJSONString();
    }
    
    /**
     * 创建错误的图查询结果
     */
    private String createErrorGraphResult(String errorMessage) {
        JSONObject result = new JSONObject();
        result.put("graph_query_results", new JSONArray());
        result.put("query_status", "error");
        result.put("error_message", errorMessage);
        return result.toJSONString();
    }
}