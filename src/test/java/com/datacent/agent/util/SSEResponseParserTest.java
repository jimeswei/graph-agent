package com.datacent.agent.util;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

@Slf4j
@SpringBootTest
public class SSEResponseParserTest {
    
    @Test
    public void testParseRealSSEResponse() {
        try {
            // 读取真实的SSE响应数据
            Path responsePath = Paths.get("reponse.json");
            String sseResponse = Files.readString(responsePath);
            
            // 提取完整内容
            String fullContent = SSEResponseParser.extractContent(sseResponse);
            log.info("提取的完整内容长度: {}", fullContent.length());
            log.info("提取的完整内容: {}", fullContent);
            
            // 提取内容列表
            List<String> contentList = SSEResponseParser.extractContentList(sseResponse);
            log.info("提取的内容片段数量: {}", contentList.size());
            
            // 显示前几个内容片段
            for (int i = 0; i < Math.min(10, contentList.size()); i++) {
                log.info("内容片段 {}: [{}]", i + 1, contentList.get(i));
            }
            
            // 提取基本信息
            SSEResponseParser.SSEInfo sseInfo = SSEResponseParser.extractSSEInfo(sseResponse);
            log.info("提取的基本信息: {}", sseInfo);
            
            // 验证结果
            assert !fullContent.isEmpty() : "完整内容不应为空";
            assert !contentList.isEmpty() : "内容列表不应为空";
            assert sseInfo.getThreadId() != null : "thread_id不应为空";
            
        } catch (IOException e) {
            log.error("读取响应文件失败", e);
        }
    }
    
    @Test
    public void testParseSimpleSSEData() {
        String sseData = """
                data:{"thread_id": "test-123", "agent": "coordinator", "id": "run-456", "role": "assistant"}
                
                data:{"thread_id": "test-123", "agent": "coordinator", "id": "run-456", "role": "assistant", "content": "Hello"}
                
                data:{"thread_id": "test-123", "agent": "coordinator", "id": "run-456", "role": "assistant", "content": " World"}
                
                data:{"thread_id": "test-123", "agent": "coordinator", "id": "run-456", "role": "assistant", "content": "!"}
                """;
        
        // 提取完整内容
        String fullContent = SSEResponseParser.extractContent(sseData);
        log.info("测试完整内容: {}", fullContent);
        
        // 提取内容列表
        List<String> contentList = SSEResponseParser.extractContentList(sseData);
        log.info("测试内容列表: {}", contentList);
        
        // 提取基本信息
        SSEResponseParser.SSEInfo sseInfo = SSEResponseParser.extractSSEInfo(sseData);
        log.info("测试基本信息: {}", sseInfo);
        
        // 验证结果
        assert "Hello World!".equals(fullContent) : "完整内容应该是 'Hello World!'";
        assert contentList.size() == 3 : "内容列表应该有3个元素";
        assert "test-123".equals(sseInfo.getThreadId()) : "thread_id应该是 'test-123'";
        assert "coordinator".equals(sseInfo.getAgent()) : "agent应该是 'coordinator'";
        
        log.info("简单SSE数据解析测试通过");
    }
    
    @Test
    public void testParseEmptyOrInvalidData() {
        // 测试空数据
        String emptyContent = SSEResponseParser.extractContent("");
        assert emptyContent.isEmpty() : "空数据应该返回空字符串";
        
        // 测试无效数据
        String invalidData = "invalid sse data\nno data prefix";
        String invalidContent = SSEResponseParser.extractContent(invalidData);
        assert invalidContent.isEmpty() : "无效数据应该返回空字符串";
        
        // 测试只有基本信息没有content的数据
        String noContentData = "data:{\"thread_id\": \"test\", \"agent\": \"coordinator\", \"role\": \"assistant\"}";
        String noContent = SSEResponseParser.extractContent(noContentData);
        assert noContent.isEmpty() : "没有content字段应该返回空字符串";
        
        log.info("边界情况测试通过");
    }
}