package com.agent.mcp.controller;

import com.agent.mcp.client.OpenAiClient;
import com.agent.mcp.common.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/mcp")
@RequiredArgsConstructor
public class McpController {
    private final OpenAiClient openAiClient;

    // 生成营销文案（MCP 工具1）
    @PostMapping("/generateText")
    public Result<String> generateText(@RequestBody Map<String, Object> params) {
        try {
            String prompt = (String) params.get("prompt");
            double temperature = params.containsKey("temperature") ? (double) params.get("temperature") : 0.7;
            String content = openAiClient.generateText(prompt, temperature);
            return Result.success(content);
        } catch (Exception e) {
            return Result.fail(e.getMessage());
        }
    }

    // 内容审核（MCP 工具2）
    @PostMapping("/reviewText")
    public Result<Map<String, Object>> reviewText(@RequestBody Map<String, Object> params) {
        try {
            String content = (String) params.get("content");
            // 简化版审核逻辑（实际可替换成第三方审核 API）
            String[] sensitiveWords = {"违规", "违法", "虚假"};
            boolean isCompliant = true;
            String reason = "无敏感词";
            for (String word : sensitiveWords) {
                if (content.contains(word)) {
                    isCompliant = false;
                    reason = "包含敏感词：" + word;
                    break;
                }
            }
            Map<String, Object> result = Map.of(
                    "is_compliant", isCompliant,
                    "reason", reason
            );
            return Result.success(result);
        } catch (Exception e) {
            return Result.fail(e.getMessage());
        }
    }
}