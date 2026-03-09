package com.agent.mcp.service;

import com.agent.mcp.client.OpenAiClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * MCP Tool Service - MCP 工具服务层
 * 
 * 提供具体的工具实现，包括：
 * - generate_marketing_copy: 营销文案生成
 * - review_content: 内容合规性审核
 */
@Service
@RequiredArgsConstructor
public class McpToolService {
    private final OpenAiClient openAiClient;

    /**
     * 调用 MCP 工具
     * 
     * @param toolName 工具名称
     * @param arguments 工具参数
     * @return 工具执行结果
     * @throws Exception 工具执行异常
     */
    public String callTool(String toolName, Map<String, Object> arguments) throws Exception {
        switch (toolName) {
            case "generate_marketing_copy":
                return generateMarketingCopy(arguments);
            case "review_content":
                return reviewContent(arguments);
            default:
                throw new IllegalArgumentException("Unknown tool: " + toolName);
        }
    }

    /**
     * 生成营销文案
     * 
     * @param arguments 包含 product_info 和 scene_type
     * @return 生成的营销文案
     */
    private String generateMarketingCopy(Map<String, Object> arguments) {
        String productInfo = (String) arguments.get("product_info");
        String sceneType = (String) arguments.get("scene_type");
        
        // 构建专业的提示词
        String prompt = String.format("""
            你是专业的营销文案生成专家，场景：%s
            产品信息：%s
            要求：
            1. 文案简洁有吸引力，符合%s风格
            2. 突出产品核心卖点
            3. 语言风格适配移动端传播
            """, sceneType, productInfo, sceneType);
        
        // 调用大模型生成文案
        return openAiClient.generateText(prompt, 0.7);
    }

    /**
     * 审核内容合规性
     * 
     * @param arguments 包含 content
     * @return JSON 格式的审核结果 {"is_compliant": boolean, "reason": string}
     */
    private String reviewContent(Map<String, Object> arguments) {
        String content = (String) arguments.get("content");
        
        // 敏感词检测列表
        String[] sensitiveWords = {"违规", "违法", "虚假", "广告", "最佳"};
        boolean isCompliant = true;
        String reason = "无敏感词，内容合规";
        
        for (String word : sensitiveWords) {
            if (content.contains(word)) {
                isCompliant = false;
                reason = "包含敏感词：" + word;
                break;
            }
        }
        
        // 返回 JSON 格式结果
        return String.format(
            "{\"is_compliant\": %b, \"reason\": \"%s\"}",
            isCompliant, reason
        );
    }
}
