package com.datacent.agent.controller;

import com.alibaba.fastjson2.JSONObject;
import com.datacent.agent.dto.McpToolResultQueryDTO;
import com.datacent.agent.dto.McpToolNameDTO;
import com.datacent.agent.service.McpToolResultQueryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
@RequiredArgsConstructor
public class McpToolResultQueryController {
    
    private final McpToolResultQueryService mcpToolResultQueryService;
    
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
}