package com.agent.generator.client;

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
public class McpClient {
    @Value("${mcp.url}")
    private String mcpUrl;

    private final RestTemplate restTemplate = new RestTemplate();

    // 调用 MCP 生成文本
    public String callGenerateText(String prompt, double temperature) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            JSONObject params = new JSONObject();
            params.put("prompt", prompt);
            params.put("temperature", temperature);
            
            HttpEntity<String> request = new HttpEntity<>(params.toString(), headers);
            ResponseEntity<String> response = restTemplate.postForEntity(
                    mcpUrl + "/mcp/generateText",
                    request,
                    String.class
            );
            
            JSONObject respJson = JSONObject.parseObject(response.getBody());
            if (!respJson.getBoolean("success")) {
                throw new RuntimeException("MCP 工具调用失败：" + respJson.getString("error"));
            }
            return respJson.getString("data");
        } catch (Exception e) {
            log.error("调用 MCP 工具失败", e);
            throw new RuntimeException(e.getMessage());
        }
    }
}