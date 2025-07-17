package com.datacent.agent.util;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

/**
 * SSE响应数据解析工具类
 * 用于解析Server-Sent Events格式的流式响应数据
 */
@Slf4j
public class SSEResponseParser {

    /**
     * 解析SSE响应数据，提取所有content字段并拼接成完整内容
     * 
     * @param sseResponse SSE格式的响应数据
     * @return 拼接后的完整内容
     */
    public static String extractContent(String sseResponse) {
        if (sseResponse == null || sseResponse.trim().isEmpty()) {
            return "";
        }
        
        List<String> contentList = extractContentList(sseResponse);
        return String.join("", contentList);
    }
    
    /**
     * 解析SSE响应数据，提取所有content字段为列表
     * 
     * @param sseResponse SSE格式的响应数据
     * @return content内容列表
     */
    public static List<String> extractContentList(String sseResponse) {
        List<String> contentList = new ArrayList<>();
        
        if (sseResponse == null || sseResponse.trim().isEmpty()) {
            return contentList;
        }
        
        try {
            String[] lines = sseResponse.split("\n");
            
            for (String line : lines) {
                line = line.trim();
                
                // 跳过空行
                if (line.isEmpty()) {
                    continue;
                }
                
                // 处理SSE格式的数据行 (以"data:"开头)
                if (line.startsWith("data:")) {
                    String jsonStr = line.substring(5).trim(); // 去掉"data:"前缀
                    
                    // 跳过空的JSON数据
                    if (jsonStr.isEmpty()) {
                        continue;
                    }
                    
                    try {
                        JSONObject jsonObject = JSON.parseObject(jsonStr);
                        
                        // 提取content字段
                        if (jsonObject.containsKey("content")) {
                            String content = jsonObject.getString("content");
                            if (content != null && !content.isEmpty()) {
                                contentList.add(content);
                            }
                        }
                    } catch (Exception e) {
                        log.debug("解析JSON数据失败，跳过该行: {}", jsonStr);
                    }
                }
            }
            
        } catch (Exception e) {
            log.error("解析SSE响应数据失败", e);
        }
        
        return contentList;
    }
    
    /**
     * 从SSE响应数据中提取所有enabled_tools信息
     * 
     * @param sseResponse SSE格式的响应数据
     * @return 所有enabled_tools的列表
     */
    public static List<String> extractEnabledTools(String sseResponse) {
        List<String> enabledTools = new ArrayList<>();
        
        if (sseResponse == null || sseResponse.trim().isEmpty()) {
            return enabledTools;
        }
        
        try {
            String[] lines = sseResponse.split("\n");
            
            for (String line : lines) {
                line = line.trim();
                
                // 处理SSE格式的数据行 (以"data:"开头)
                if (line.startsWith("data:")) {
                    String jsonStr = line.substring(5).trim(); // 去掉"data:"前缀
                    
                    // 跳过空的JSON数据
                    if (jsonStr.isEmpty()) {
                        continue;
                    }
                    
                    try {
                        JSONObject jsonObject = JSON.parseObject(jsonStr);
                        
                        // 提取tool_calls数组中的enabled_tools
                        if (jsonObject.containsKey("tool_calls")) {
                            Object toolCallsObj = jsonObject.get("tool_calls");
                            if (toolCallsObj instanceof com.alibaba.fastjson2.JSONArray) {
                                com.alibaba.fastjson2.JSONArray toolCalls = (com.alibaba.fastjson2.JSONArray) toolCallsObj;
                                for (int i = 0; i < toolCalls.size(); i++) {
                                    Object toolCallObj = toolCalls.get(i);
                                    if (toolCallObj instanceof JSONObject) {
                                        JSONObject toolCall = (JSONObject) toolCallObj;
                                        String toolName = toolCall.getString("name");
                                        if (toolName != null && !toolName.trim().isEmpty() && !enabledTools.contains(toolName)) {
                                            enabledTools.add(toolName);
                                        }
                                    }
                                }
                            }
                        }
                        
                        // 也检查直接的enabled_tools字段
                        if (jsonObject.containsKey("enabled_tools")) {
                            Object enabledToolsObj = jsonObject.get("enabled_tools");
                            if (enabledToolsObj instanceof com.alibaba.fastjson2.JSONArray) {
                                com.alibaba.fastjson2.JSONArray toolsArray = (com.alibaba.fastjson2.JSONArray) enabledToolsObj;
                                for (int i = 0; i < toolsArray.size(); i++) {
                                    String toolName = toolsArray.getString(i);
                                    if (toolName != null && !toolName.trim().isEmpty() && !enabledTools.contains(toolName)) {
                                        enabledTools.add(toolName);
                                    }
                                }
                            }
                        }
                        
                    } catch (Exception e) {
                        log.debug("解析JSON数据失败，跳过该行: {}", jsonStr);
                    }
                }
            }
            
        } catch (Exception e) {
            log.error("解析SSE响应数据中的enabled_tools失败", e);
        }
        
        return enabledTools;
    }
    
    /**
     * 解析SSE响应数据，提取基本信息（thread_id, agent, role等）
     * 
     * @param sseResponse SSE格式的响应数据
     * @return 解析得到的基本信息
     */
    public static SSEInfo extractSSEInfo(String sseResponse) {
        if (sseResponse == null || sseResponse.trim().isEmpty()) {
            return new SSEInfo();
        }
        
        try {
            String[] lines = sseResponse.split("\n");
            
            for (String line : lines) {
                line = line.trim();
                
                if (line.startsWith("data:")) {
                    String jsonStr = line.substring(5).trim();
                    
                    if (!jsonStr.isEmpty()) {
                        try {
                            JSONObject jsonObject = JSON.parseObject(jsonStr);
                            
                            SSEInfo info = new SSEInfo();
                            info.setThreadId(jsonObject.getString("thread_id"));
                            info.setAgent(jsonObject.getString("agent"));
                            info.setId(jsonObject.getString("id"));
                            info.setRole(jsonObject.getString("role"));
                            
                            return info;
                        } catch (Exception e) {
                            log.debug("解析JSON数据失败: {}", jsonStr);
                        }
                    }
                }
            }
            
        } catch (Exception e) {
            log.error("提取SSE基本信息失败", e);
        }
        
        return new SSEInfo();
    }
    
    /**
     * SSE响应的基本信息结构
     */
    public static class SSEInfo {
        private String threadId;
        private String agent;
        private String id;
        private String role;
        
        public String getThreadId() {
            return threadId;
        }
        
        public void setThreadId(String threadId) {
            this.threadId = threadId;
        }
        
        public String getAgent() {
            return agent;
        }
        
        public void setAgent(String agent) {
            this.agent = agent;
        }
        
        public String getId() {
            return id;
        }
        
        public void setId(String id) {
            this.id = id;
        }
        
        public String getRole() {
            return role;
        }
        
        public void setRole(String role) {
            this.role = role;
        }
        
        @Override
        public String toString() {
            return "SSEInfo{" +
                    "threadId='" + threadId + '\'' +
                    ", agent='" + agent + '\'' +
                    ", id='" + id + '\'' +
                    ", role='" + role + '\'' +
                    '}';
        }
    }
}