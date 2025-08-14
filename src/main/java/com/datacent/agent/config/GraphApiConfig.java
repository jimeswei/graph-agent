package com.datacent.agent.config;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
@Getter
public class GraphApiConfig {
    
    @Value("${graph.api.base-url:http://192.168.3.78:28080/api/v1.2/graph-connections/1/gremlin-query}")
    private String baseUrl;
}