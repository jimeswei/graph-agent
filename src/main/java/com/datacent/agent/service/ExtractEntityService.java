package com.datacent.agent.service;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class ExtractEntityService {

    private final WebClient webClient;

    @Value("${llm.api.base-url:https://dashscope.aliyuncs.com/compatible-mode/v1}")
    private String baseUrl;

    @Value("${llm.api.model:deepseek-r1}")
    private String model;

    @Value("${llm.api.api-key}")
    private String apiKey;

    public ExtractEntityService() {
        this.webClient = WebClient.builder()
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(10 * 1024 * 1024))
                .build();
    }

    /**
     * 实体提取方法
     * @param message 待提取实体的消息
     * @return 提取后的实体JSON字符串
     */
    public String extractEntity(String message) {
        try {
            if (message == null || message.trim().isEmpty()) {
                log.warn("输入消息为空，无法进行实体提取");
                return createEmptyEntityResult();
            }

            log.info("开始进行实体提取，消息长度: {}", message.length());

            // 读取实体提取模板
            String entityTemplate = loadEntityTemplate();
            
            // 构建提示词
            String prompt = buildExtractionPrompt(message, entityTemplate);
            
            // 调用大模型API
            String result = callLlmApi(prompt);
            
            log.info("实体提取完成");
            return result;

        } catch (Exception e) {
            log.error("实体提取失败", e);
            return createErrorEntityResult(e.getMessage());
        }
    }

    /**
     * 加载实体提取模板
     */
    private String loadEntityTemplate() {
        try {
            // 尝试从文件系统加载（开发环境）
            java.io.File file = new java.io.File("doc/entities.json");
            if (file.exists()) {
                return java.nio.file.Files.readString(file.toPath(), StandardCharsets.UTF_8);
            }
            
            // 如果文件不存在，使用默认模板
            log.warn("实体模板文件不存在，使用默认模板");
            return getDefaultEntityTemplate();
            
        } catch (Exception e) {
            log.warn("无法加载实体模板文件，使用默认模板: {}", e.getMessage());
            return getDefaultEntityTemplate();
        }
    }

    /**
     * 获取默认实体模板
     */
    private String getDefaultEntityTemplate() {
        return """
        {
          "target_persons": ["name1", "name2"],
          "extracted_entities": [
            {
              "name": "name",
              "type": "celebrity/event/work"
            }
          ]
        }
        """;
    }

    /**
     * 构建实体提取提示词
     */
    private String buildExtractionPrompt(String message, String template) {
        return String.format("""
        你是一个专业的实体提取助手。请从以下文本中提取实体信息，并按照指定的JSON格式返回结果。

        提取要求：
        1. 识别文本中的人名、事件、作品等实体
        2. 对于人名，请准确识别目标人物
        3. 对于其他实体，请分类为celebrity（名人）、event（事件）、work（作品）等类型
        4. 严格按照以下JSON格式返回结果

        JSON格式模板：
        %s

        待分析文本：
        %s

        请直接返回JSON格式的结果，不要包含其他解释文字：
        """, template, message);
    }

    /**
     * 调用大模型API
     */
    private String callLlmApi(String prompt) {
        String requestUrl = baseUrl + "/chat/completions";
        
        // 构建请求体
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", model);
        requestBody.put("temperature", 0.1);
        requestBody.put("max_tokens", 2000);
        
        List<Map<String, String>> messages = new ArrayList<>();
        Map<String, String> userMessage = new HashMap<>();
        userMessage.put("role", "user");
        userMessage.put("content", prompt);
        messages.add(userMessage);
        requestBody.put("messages", messages);

        log.info("调用大模型API: {}", requestUrl);
        log.debug("请求体: {}", JSON.toJSONString(requestBody));

        try {
            String response = webClient.post()
                    .uri(requestUrl)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .body(BodyInserters.fromValue(requestBody))
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(Duration.ofMinutes(2))
                    .block();

            return extractContentFromResponse(response);

        } catch (Exception e) {
            log.error("调用大模型API失败", e);
            throw new RuntimeException("调用大模型API失败: " + e.getMessage(), e);
        }
    }

    /**
     * 从API响应中提取内容
     */
    private String extractContentFromResponse(String response) {
        try {
            JSONObject responseObj = JSON.parseObject(response);
            JSONObject choices = responseObj.getJSONArray("choices").getJSONObject(0);
            JSONObject message = choices.getJSONObject("message");
            String content = message.getString("content");
            
            log.debug("提取到的内容: {}", content);
            
            // 尝试解析为JSON以验证格式
            JSON.parseObject(content);
            
            return content;
        } catch (Exception e) {
            log.error("解析API响应失败", e);
            log.debug("原始响应: {}", response);
            return createErrorEntityResult("解析API响应失败");
        }
    }

    /**
     * 创建空的实体结果
     */
    private String createEmptyEntityResult() {
        JSONObject result = new JSONObject();
        result.put("target_persons", new ArrayList<>());
        result.put("extracted_entities", new ArrayList<>());
        return result.toJSONString();
    }

    /**
     * 创建错误的实体结果
     */
    private String createErrorEntityResult(String errorMessage) {
        JSONObject result = new JSONObject();
        result.put("error", errorMessage);
        result.put("target_persons", new ArrayList<>());
        result.put("extracted_entities", new ArrayList<>());
        return result.toJSONString();
    }
}