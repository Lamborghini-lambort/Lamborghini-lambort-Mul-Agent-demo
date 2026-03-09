package com.agent.review.client;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

@Component
@Slf4j
public class McpReviewClient {
    @Value("${mcp.url}")
    private String mcpUrl;

    private HttpClient httpClient;

    @jakarta.annotation.PostConstruct
    public void init() {
        this.httpClient = HttpClient.newHttpClient();
    }

    // 调用 MCP 审核工具
    public JSONObject callReviewText(String content) {
        try {
            // 构建 MCP JSON-RPC 2.0 请求
            JSONObject jsonRpcRequest = new JSONObject();
            jsonRpcRequest.put("jsonrpc", "2.0");
            jsonRpcRequest.put("id", "review-1");
            jsonRpcRequest.put("method", "tools/call");
            
            JSONObject params = new JSONObject();
            params.put("name", "review_content");
            
            JSONObject arguments = new JSONObject();
            arguments.put("content", content);
            params.put("arguments", arguments);
            
            jsonRpcRequest.put("params", params);
            
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(mcpUrl + "/mcp/message"))
                .timeout(Duration.ofSeconds(10))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonRpcRequest.toString()))
                .build();
            
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            
            // 解析 MCP 响应
            JSONObject respJson = JSONObject.parseObject(response.body());
            JSONObject result = respJson.getJSONObject("result");
            JSONArray contentArray = result.getJSONArray("content");
            String responseText = contentArray.getJSONObject(0).getString("text");
            return JSONObject.parseObject(responseText);
        } catch (Exception e) {
            log.error("调用 MCP 审核工具失败", e);
            throw new RuntimeException(e.getMessage());
        }
    }
}