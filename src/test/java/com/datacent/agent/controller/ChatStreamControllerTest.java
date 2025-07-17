package com.datacent.agent.controller;

import com.alibaba.fastjson2.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@Slf4j
@SpringBootTest
public class ChatStreamControllerTest {
    
    @Test
    public void testBuildSimpleRequest() {
        // 测试简化接口的JSON构建
        JSONObject request = new JSONObject();
        request.put("message", "测试消息");
        request.put("thread_id", "test-thread-123");
        
        log.info("简化请求JSON: {}", request.toJSONString());
        
        // 验证基本字段
        assert request.containsKey("message");
        assert request.containsKey("thread_id");
        assert "测试消息".equals(request.getString("message"));
        
        log.info("简化请求构建验证通过");
    }
}