package com.datacent.agent.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.util.UUID;

@Slf4j
@Service
public class ChatStreamService {
    
    private final WebClient webClient;
    
    @Value("${chat.stream.base-url:http://192.168.3.78:48558}")
    private String baseUrl;
    
    public ChatStreamService() {
        this.webClient = WebClient.builder()
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(10 * 1024 * 1024))
                .build();
    }
    
    /**
     * 流式聊天 - 直接转发请求体到外部接口
     * @param requestBody 完整的请求体，直接转发
     * @return 流式响应
     */
    public Flux<String> chatStream(Object requestBody) {
        String requestUrl = baseUrl + "/api/chat/stream";
        long startTime = System.currentTimeMillis();
        
        log.info("==================== 开始流式聊天请求 ====================");
        log.info("目标URL: {}", requestUrl);
        log.info("请求体: {}", requestBody);
        log.info("开始时间: {}", new java.util.Date(startTime));
        
        log.info("============================================================");
        
        return webClient.post()
                .uri(requestUrl)
                .contentType(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromValue(requestBody))
                .retrieve()
                .bodyToFlux(String.class)
                .timeout(Duration.ofMinutes(5))
                .index() // 添加索引
                .doOnSubscribe(subscription -> {
                    log.info("🔄 流式订阅开始...");
                })
                .doOnNext(indexedChunk -> {
                    int index = indexedChunk.getT1().intValue();
                    String chunk = indexedChunk.getT2();
                    
                    // 详细日志输出
                    log.info("📦 接收到流数据块 [{}]: {}", index, chunk);
                })
                .map(indexedChunk -> indexedChunk.getT2()) // 去掉索引，只返回数据
                .doOnComplete(() -> {
                    long duration = System.currentTimeMillis() - startTime;
                    log.info("============================================================");
                    log.info("✅ 流式聊天请求完成");
                    log.info("⏱️ 总耗时: {}ms", duration);
                    log.info("============================================================");
                })
                .doOnError(error -> {
                    long duration = System.currentTimeMillis() - startTime;
                    log.error("============================================================");
                    log.error("❌ 聊天流请求失败");
                    log.error("⏱️ 耗时: {}ms", duration);
                    log.error("🚫 错误: {}", error.getMessage());
                    log.error("============================================================");
                })
                .onErrorResume(throwable -> {
                    log.error("⚠️ 流式调用异常恢复: {}", throwable.getMessage());
                    return Flux.just("data: {\"error\":\"" + throwable.getMessage() + "\"}\n\n");
                });
    }
    

}