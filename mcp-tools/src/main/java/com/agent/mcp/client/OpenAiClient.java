package com.agent.mcp.client;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
@Slf4j
public class OpenAiClient {
    @Value("${openai.api-key}")
    private String apiKey;

    @Value("${openai.base-url}")
    private String baseUrl;

    @Value("${openai.model:gpt-3.5-turbo}")
    private String model;

    private final RestTemplate restTemplate = new RestTemplate();

    // 调用大模型生成文本
    public String generateText(String prompt, double temperature) {
        try {
            // 构建请求头
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + apiKey);

            // 构建请求体
            JSONObject requestBody = new JSONObject();
            requestBody.put("model", model);
            
            JSONObject message = new JSONObject();
            message.put("role", "user");
            message.put("content", prompt);
            requestBody.put("messages", new JSONObject[]{message});
            
            requestBody.put("temperature", temperature);

            // 发送请求
            HttpEntity<String> request = new HttpEntity<>(requestBody.toString(), headers);
            ResponseEntity<String> response = restTemplate.postForEntity(
                    baseUrl + "/chat/completions",
                    request,
                    String.class
            );

            // 解析响应
            JSONObject responseJson = JSON.parseObject(response.getBody());
            return responseJson.getJSONArray("choices")
                    .getJSONObject(0)
                    .getJSONObject("message")
                    .getString("content");
        } catch (Exception e) {
            log.error("调用大模型失败", e);
            throw new RuntimeException("大模型调用失败：" + e.getMessage());
        }
    }
}