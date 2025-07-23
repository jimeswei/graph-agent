package com.datacent.agent.util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

/**
 * 响应内容提取器
 * 用于从response.json文件中提取content信息
 */
public class ResponseContentExtractor {

    public static void main(String[] args) {
        String filePath = "reponse.json";
        
        if (args.length > 0) {
            filePath = args[0];
        }
        
        try {
            // 读取响应文件
            Path path = Paths.get(filePath);
            if (!Files.exists(path)) {
                System.err.println("文件不存在: " + filePath);
                return;
            }
            
            String sseResponse = Files.readString(path);
            
            // 提取content信息
            String fullContent = SSEResponseParser.extractContent(sseResponse);
            List<String> contentList = SSEResponseParser.extractContentList(sseResponse);
            SSEResponseParser.SSEInfo sseInfo = SSEResponseParser.extractSSEInfo(sseResponse);
            
            // 输出结果
            System.out.println("=== SSE响应解析结果 ===");
            System.out.println("文件路径: " + filePath);
            System.out.println("基本信息: " + sseInfo);
            System.out.println("内容片段数量: " + contentList.size());
            System.out.println("完整内容长度: " + fullContent.length());
            System.out.println();
            
            System.out.println("=== 完整内容 ===");
            System.out.println(fullContent);
            
            System.out.println();
            System.out.println("=== 内容片段详情 ===");
            for (int i = 0; i < contentList.size(); i++) {
                System.out.println(String.format("片段 %d: [%s]", i + 1, contentList.get(i)));
            }
            
        } catch (IOException e) {
            System.err.println("读取文件失败: " + e.getMessage());
        } catch (Exception e) {
            System.err.println("解析失败: " + e.getMessage());
        }
    }
}