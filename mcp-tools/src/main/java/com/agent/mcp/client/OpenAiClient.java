package com.agent.mcp.client;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

@Component
@Slf4j
public class OpenAiClient {
    @Value("${openai.api-key}")
    private String apiKey;

    @Value("${openai.base-url}")
    private String baseUrl;

    @Value("${openai.model:gpt-3.5-turbo}")
    private String model;

    @Value("${openai.timeout:30000}")
    private int timeout;

    private HttpClient httpClient;

    /**
     * 初始化方法，在 Spring 注入依赖后执行
     */
    @PostConstruct
    public void init() {
        log.info("开始初始化 OpenAiClient...");
        log.info("API Key: {}...", apiKey != null && apiKey.length() > 8 ? apiKey.substring(0, 8) : "null");
        log.info("Base URL: {}", baseUrl);
        log.info("Model: {}", model);
        
        // 确保 timeout 大于 0，默认 60 秒
        int effectiveTimeout = timeout > 0 ? timeout : 60000;
        this.httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofMillis(effectiveTimeout))
            .build();
        log.info("OpenAiClient 初始化完成，超时时间：{}ms", effectiveTimeout);
        
        // 异步测试连接（不阻塞启动）
        testConnectionAsync();
    }
    
    /**
     * 异步测试与大模型 API 的连接
     */
    private void testConnectionAsync() {
        new Thread(() -> {
            try {
                Thread.sleep(2000); // 等待服务完全启动
                log.info("正在测试 Kimi API 连接...");
                HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/models"))
                    .timeout(Duration.ofMillis(5000))
                    .header("Authorization", "Bearer " + apiKey)
                    .GET()
                    .build();
                
                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                
                if (response.statusCode() == 200) {
                    log.info("✅ Kimi API 连接成功！状态码：{}", response.statusCode());
                } else {
                    log.error("❌ Kimi API 连接失败！状态码：{}, 响应：{}", response.statusCode(), response.body());
                }
            } catch (Exception e) {
                log.error("❌ Kimi API 连接异常：{}", e.getMessage());
                log.error("可能原因：1) API Key 无效 2) 网络无法访问 3) 防火墙阻止");
            }
        }).start();
    }

    // 调用大模型生成文本
    public String generateText(String prompt, double temperature) {
        try {
            // 构建请求体
            JSONObject requestBody = new JSONObject();
            requestBody.put("model", model);
            
            JSONObject message = new JSONObject();
            message.put("role", "user");
            message.put("content", prompt);
            requestBody.put("messages", new JSONObject[]{message});
            
            requestBody.put("temperature", temperature);

            // 使用 Java 11+ HttpClient 发送请求
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/chat/completions"))
                .timeout(Duration.ofMillis(timeout))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + apiKey)
                .POST(HttpRequest.BodyPublishers.ofString(requestBody.toString()))
                .build();
            
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            // 记录完整响应，方便调试
            log.info("Kimi API 原始响应：{}", response.body());

            // 解析响应
            JSONObject responseJson = JSON.parseObject(response.body());
            
            // 检查是否有错误
            if (responseJson.containsKey("error")) {
                JSONObject error = responseJson.getJSONObject("error");
                String errorMsg = error != null ? error.getString("message") : "未知错误";
                log.error("Kimi API 返回错误：{}", errorMsg);
                throw new RuntimeException("API 错误：" + errorMsg);
            }
            
            // 检查 choices 字段
            var choices = responseJson.getJSONArray("choices");
            if (choices == null || choices.isEmpty()) {
                log.error("Kimi API 响应中没有 choices 字段，完整响应：{}", response.body());
                throw new RuntimeException("API 响应格式异常：缺少 choices 字段");
            }
            
            return choices.getJSONObject(0)
                    .getJSONObject("message")
                    .getString("content");
        } catch (Exception e) {
            log.error("调用大模型失败", e);
            throw new RuntimeException("大模型调用失败：" + e.getMessage());
        }
    }
}