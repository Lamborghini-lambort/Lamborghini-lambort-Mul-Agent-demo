package com.agent.generator.client;

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
public class McpClient {
    @Value("${mcp.url}")
    private String mcpUrl;

    private HttpClient httpClient;

    @jakarta.annotation.PostConstruct
    public void init() {
        this.httpClient = HttpClient.newHttpClient();
    }

    // 调用 MCP 生成文本
    public String callGenerateText(String prompt, double temperature) {
        try {
            // 构建 MCP JSON-RPC 2.0 请求
            JSONObject jsonRpcRequest = new JSONObject();
            jsonRpcRequest.put("jsonrpc", "2.0");
            jsonRpcRequest.put("id", "gen-1");
            jsonRpcRequest.put("method", "tools/call");
            
            JSONObject params = new JSONObject();
            params.put("name", "generate_marketing_copy");
            
            JSONObject arguments = new JSONObject();
            arguments.put("product_info", prompt);
            arguments.put("scene_type", "电商推广");
            params.put("arguments", arguments);
            
            jsonRpcRequest.put("params", params);
            
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(mcpUrl + "/mcp/message"))
                .timeout(Duration.ofSeconds(30))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonRpcRequest.toString()))
                .build();
            
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            
            // 解析 MCP 响应
            JSONObject respJson = JSONObject.parseObject(response.body());
            JSONObject result = respJson.getJSONObject("result");
            JSONArray content = result.getJSONArray("content");
            return content.getJSONObject(0).getString("text");
        } catch (Exception e) {
            log.error("调用 MCP 工具失败", e);
            throw new RuntimeException(e.getMessage());
        }
    }
}