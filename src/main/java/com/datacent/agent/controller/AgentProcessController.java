package com.datacent.agent.controller;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.datacent.agent.dto.McpToolResultQueryDTO;
import com.datacent.agent.dto.McpToolNameDTO;
import com.datacent.agent.entity.GraphCache;
import com.datacent.agent.service.McpToolResultQueryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * MCP工具调用结果查询控制器
 * 提供工具执行结果的查询接口
 */
@Slf4j
@RestController
@RequestMapping("/api/mcp-tool-results")
@CrossOrigin(origins = "*")
public class AgentProcessController {

    @Autowired
    private  McpToolResultQueryService mcpToolResultQueryService;
    
    /**
     * 根据线程ID和工具名称查询工具执行结果
     * 
     * @param threadId 线程ID
     * @param toolName 工具名称
     * @return 工具执行结果列表
     */
    @GetMapping("/query")
    public ResponseEntity<List<McpToolResultQueryDTO>> queryToolResults(
            @RequestParam String threadId,
            @RequestParam String toolName) {
        
        log.info("接收到工具结果查询请求，threadId: {}, toolName: {}", threadId, toolName);
        
        List<McpToolResultQueryDTO> results = mcpToolResultQueryService.getToolResultsByThreadIdAndToolName(threadId, toolName);
        
        return ResponseEntity.ok(results);
    }
    
    /**
     * 根据线程ID查询所有工具名称（去重）
     * 只返回工具名称，不包含执行结果内容
     * 
     * @param threadId 线程ID
     * @return 所有工具名称列表
     */
    @GetMapping("/query/all")
    public ResponseEntity<List<McpToolNameDTO>> queryAllToolNames(@RequestParam String threadId) {
        
        log.info("接收到线程所有工具名称查询请求，threadId: {}", threadId);
        
        List<McpToolNameDTO> results = mcpToolResultQueryService.getAllToolNamesByThreadId(threadId);
        
        return ResponseEntity.ok(results);
    }
    
    /**
     * 根据线程ID查询所有工具执行结果（包含完整内容）
     * 
     * @param threadId 线程ID
     * @return 所有工具执行结果列表
     */
    @GetMapping("/query/all/details")
    public ResponseEntity<List<McpToolResultQueryDTO>> queryAllToolResults(@RequestParam String threadId) {
        
        log.info("接收到线程所有工具详细结果查询请求，threadId: {}", threadId);
        
        List<McpToolResultQueryDTO> results = mcpToolResultQueryService.getAllToolResultsByThreadId(threadId);
        
        return ResponseEntity.ok(results);
    }
    
    /**
     * POST方式查询工具执行结果（支持复杂参数）
     * 
     * @param request 查询请求体
     * @return 工具执行结果列表
     */
    @PostMapping("/query")
    public ResponseEntity<List<McpToolResultQueryDTO>> queryToolResultsPost(@RequestBody JSONObject request) {
        
        String threadId = request.getString("thread_id");
        String toolName = request.getString("toolName");
        
        log.info("接收到POST工具结果查询请求，threadId: {}, toolName: {}", threadId, toolName);
        
        List<McpToolResultQueryDTO> results = mcpToolResultQueryService.getToolResultsByThreadIdAndToolName(threadId, toolName);
        
        return ResponseEntity.ok(results);
    }
    
    /**
     * 检查指定线程是否存在工具执行结果
     * 
     * @param threadId 线程ID
     * @return 检查结果
     */
    @GetMapping("/exists")
    public ResponseEntity<JSONObject> checkToolResultsExist(@RequestParam String threadId) {
        
        log.info("检查线程工具结果是否存在，threadId: {}", threadId);
        
        boolean exists = mcpToolResultQueryService.hasToolResults(threadId);
        
        JSONObject response = new JSONObject();
        response.put("threadId", threadId);
        response.put("exists", exists);
        response.put("timestamp", System.currentTimeMillis());
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * 获取查询示例（用于API文档）
     * 
     * @return 查询示例
     */
    @GetMapping("/examples")
    public ResponseEntity<JSONObject> getQueryExamples() {
        
        JSONObject examples = new JSONObject();
        
        // GET查询示例
        JSONObject getExample = new JSONObject();
        getExample.put("url", "/api/mcp-tool-results/query?threadId=analysis_1752819308392&toolName=mutual_friend_between_stars");
        getExample.put("method", "GET");
        getExample.put("description", "根据线程ID和工具名称查询结果");
        
        // POST查询示例
        JSONObject postExample = new JSONObject();
        postExample.put("url", "/api/mcp-tool-results/query");
        postExample.put("method", "POST");
        postExample.put("description", "POST方式查询结果");
        JSONObject postBody = new JSONObject();
        postBody.put("threadId", "analysis_1752819308392");
        postBody.put("toolName", "mutual_friend_between_stars");
        postExample.put("body", postBody);
        
        // 查询所有结果示例
        JSONObject allExample = new JSONObject();
        allExample.put("url", "/api/mcp-tool-results/query/all?threadId=analysis_1752819308392");
        allExample.put("method", "GET");
        allExample.put("description", "查询线程所有工具执行结果");
        
        examples.put("queryByToolName", getExample);
        examples.put("queryByPost", postExample);
        examples.put("queryAll", allExample);
        
        return ResponseEntity.ok(examples);
    }
    
    /**
     * 根据线程ID查询MCP工具调用结果
     * 从content字段中提取id、mcp_tool_function和args信息
     * 
     * @param threadId 线程ID
     * @return MCP工具调用结果列表
     */
//    @GetMapping("/query/mcp_tools")
//    public ResponseEntity<List<JSONObject>> queryMcpToolsByThreadId(@RequestParam String threadId) {
//
//        log.info("接收到MCP工具查询请求，threadId: {}", threadId);
//
//        List<JSONObject> results = mcpToolResultQueryService.queryMcpTools(threadId);
//
//        return ResponseEntity.ok(results);
//    }


    @GetMapping("/query/mcp_tool")
    public ResponseEntity<JSONObject> queryMcpToolByThreadId(@RequestParam String threadId) {

        log.info("接收到MCP工具查询请求，threadId: {}", threadId);

        JSONObject results = mcpToolResultQueryService.queryMcpTool(threadId);

        return ResponseEntity.ok(results);
    }
    
    /**
     * 从缓存优先查询单条MCP工具调用结果
     * 优先从队列获取数据，队列为空时回退到数据库查询，每次只返回一条数据
     * 返回格式与queryMcpTools相同，但只返回单条数据
     * 
     * @param threadId 线程ID
     * @return 单条格式化的MCP工具调用结果，如果没有数据则返回204 No Content
     */
    @GetMapping("/cache/mcp_tool")
    public ResponseEntity<JSONObject> queryMcpToolFromCache(@RequestParam String threadId) {
        
        log.info("接收到缓存优先单条MCP工具查询请求，threadId: {}", threadId);
        
        JSONObject result = mcpToolResultQueryService.queryMcpTool(threadId);
        
        if (result != null) {
            return ResponseEntity.ok(result);
        } else {
            // 如果没有数据，返回204 No Content状态码
            return ResponseEntity.noContent().build();
        }
    }
    
    /**
     * 根据线程ID查询GraphCache数据
     * 先获取该threadId的所有工具结果，从content字段提取id，然后用提取的id查询graph_cache表
     * 
     * @param threadId 线程ID
     * @return GraphCache数据列表
     */
    @GetMapping("/query/graph_cache")
    public ResponseEntity<List<GraphCache>> queryGraphCacheByThreadId(@RequestParam String threadId) {
        
        log.info("接收到GraphCache查询请求，threadId: {}", threadId);
        
        List<GraphCache> results = mcpToolResultQueryService.queryGraphCache(threadId);
        
        return ResponseEntity.ok(results);
    }
    
    /**
     * 根据MCP工具ID查询GraphCache数据
     * 直接使用mcpToolsId作为threadId查询graph_cache表
     * 
     * @param mcpToolsId MCP工具ID
     * @return GraphCache数据列表
     */
    @GetMapping("/query/mcptools_graph_cache")
    public ResponseEntity<List<GraphCache>> queryGraphCacheByMcpToolsId(@RequestParam String mcpToolsId) {
        
        log.info("接收到根据MCP工具ID查询GraphCache请求，mcpToolsId: {}", mcpToolsId);
        
        List<GraphCache> results = mcpToolResultQueryService.queryGraphCacheByMcpToolsId(mcpToolsId);
        
        return ResponseEntity.ok(results);
    }
    
    /**
     * 调试接口：查看指定threadId的详细处理过程
     * 
     * @param threadId 线程ID
     * @return 调试信息
     */
    @GetMapping("/debug/graph_cache")
    public ResponseEntity<JSONObject> debugGraphCacheQuery(@RequestParam String threadId) {
        
        log.info("接收到GraphCache调试查询请求，threadId: {}", threadId);
        
        JSONObject debugInfo = new JSONObject();
        debugInfo.put("threadId", threadId);
        debugInfo.put("timestamp", System.currentTimeMillis());
        
        // 1. 检查是否有工具结果
        List<McpToolResultQueryDTO> toolResults = mcpToolResultQueryService.getAllToolResultsByThreadId(threadId);
        debugInfo.put("toolResultsCount", toolResults.size());
        
        if (!toolResults.isEmpty()) {
            JSONArray toolResultsDebug = new JSONArray();
            for (McpToolResultQueryDTO result : toolResults) {
                JSONObject toolResultDebug = new JSONObject();
                toolResultDebug.put("name", result.getName());
                toolResultDebug.put("contentLength", result.getContent() != null ? result.getContent().length() : 0);
                toolResultDebug.put("contentPreview", result.getContent() != null && result.getContent().length() > 200 
                    ? result.getContent().substring(0, 200) + "..." 
                    : result.getContent());
                toolResultsDebug.add(toolResultDebug);
            }
            debugInfo.put("toolResults", toolResultsDebug);
        }
        
        // 2. 测试id提取和图缓存映射
        JSONObject extractionInfo = mcpToolResultQueryService.debugToolResultToGraphCacheMapping(threadId);
        debugInfo.put("idExtraction", extractionInfo);
        
        return ResponseEntity.ok(debugInfo);
    }
    
    /**
     * 查看graph_cache表中的实际数据样本
     * 
     * @param limit 限制返回条数，默认10条
     * @return graph_cache表中的数据样本
     */
    @GetMapping("/debug/graph_cache-sample")
    public ResponseEntity<JSONObject> getGraphCacheSample(@RequestParam(defaultValue = "10") int limit) {
        
        log.info("查看graph_cache表数据样本，limit: {}", limit);
        
        JSONObject response = new JSONObject();
        response.put("timestamp", System.currentTimeMillis());
        
        try {
            JSONObject sampleInfo = mcpToolResultQueryService.getGraphCacheSample(limit);
            response.put("success", true);
            response.put("data", sampleInfo);
        } catch (Exception e) {
            log.error("查询graph_cache样本数据失败", e);
            response.put("success", false);
            response.put("error", e.getMessage());
        }
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * 诊断数据库连接和表访问问题
     * 
     * @return 诊断信息
     */
    @GetMapping("/debug/database-connection")
    public ResponseEntity<JSONObject> diagnoseDatabaseConnection() {
        
        log.info("开始诊断数据库连接");
        
        JSONObject response = new JSONObject();
        response.put("timestamp", System.currentTimeMillis());
        
        try {
            JSONObject diagnostic = mcpToolResultQueryService.diagnoseDatabaseConnection();
            response.put("success", true);
            response.put("diagnostic", diagnostic);
        } catch (Exception e) {
            log.error("数据库连接诊断失败", e);
            response.put("success", false);
            response.put("error", e.getMessage());
        }
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * 根据线程ID查询代理报告
     * 查询agent_report表中的所有代理报告数据
     * 
     * @param threadId 线程ID
     * @return 代理报告列表
     */
    @GetMapping("/query/agent_report")
    public ResponseEntity<List<JSONObject>> queryAgentReport(@RequestParam String threadId) {
        
        log.info("接收到代理报告查询请求，threadId: {}", threadId);
        
        List<JSONObject> results = mcpToolResultQueryService.queryAgentReport(threadId);
        
        return ResponseEntity.ok(results);
    }
    
    /**
     * 从缓存优先查询单条MCP工具调用结果
     * 优先从队列获取数据，队列为空时回退到数据库查询
     * 每次只返回一条数据，支持逐条消费模式
     * 
     * @param threadId 线程ID
     * @return 单条MCP工具调用结果，如果没有数据则返回null
     */
    @GetMapping("/cache/mcp_result")
    public ResponseEntity<JSONObject> mcpResultCache(@RequestParam String threadId) {
        
        log.info("接收到缓存优先MCP工具结果查询请求，threadId: {}", threadId);
        
        JSONObject result = mcpToolResultQueryService.mcpResultCache(threadId);
        
        if (result != null) {
            return ResponseEntity.ok(result);
        } else {
            // 如果没有数据，返回204 No Content状态码
            return ResponseEntity.noContent().build();
        }
    }
}