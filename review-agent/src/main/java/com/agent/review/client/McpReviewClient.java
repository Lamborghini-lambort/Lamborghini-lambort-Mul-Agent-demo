package com.agent.review.client;

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
public class McpReviewClient {
    @Value("${mcp.url}")
    private String mcpUrl;

    private final RestTemplate restTemplate = new RestTemplate();

    // 调用 MCP 审核工具
    public JSONObject callReviewText(String content) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            JSONObject params = new JSONObject();
            params.put("content", content);
            
            HttpEntity<String> request = new HttpEntity<>(params.toString(), headers);
            ResponseEntity<String> response = restTemplate.postForEntity(
                    mcpUrl + "/mcp/reviewText",
                    request,
                    String.class
            );
            
            JSONObject respJson = JSONObject.parseObject(response.getBody());
            if (!respJson.getBoolean("success")) {
                throw new RuntimeException("MCP 审核工具调用失败：" + respJson.getString("error"));
            }
            return respJson.getJSONObject("data");
        } catch (Exception e) {
            log.error("调用 MCP 审核工具失败", e);
            throw new RuntimeException(e.getMessage());
        }
    }
}