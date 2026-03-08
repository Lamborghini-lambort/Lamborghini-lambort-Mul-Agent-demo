package com.agent.generator.controller;

import com.agent.generator.client.McpClient;
import com.agent.generator.common.Result;
import com.alibaba.fastjson2.JSONObject;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/a2a/generator")
@RequiredArgsConstructor
public class GeneratorController {
    private final McpClient mcpClient;

    // 处理调度 Agent 发来的任务
    @PostMapping("/task")
    public Result<JSONObject> handleTask(@RequestBody JSONObject task) {
        try {
            // 1. 解析任务参数
            JSONObject taskInput = task.getJSONObject("task").getJSONObject("input");
            String userId = taskInput.getString("user_id");
            String productInfo = taskInput.getString("product_info");
            String sceneType = taskInput.getString("scene_type");

            // 2. 构造提示词
            String prompt = String.format("""
                    你是专业的营销文案生成专家，场景：%s
                    产品信息：%s
                    要求：
                    1. 文案简洁有吸引力，符合电商推广风格
                    2. 突出产品核心卖点
                    3. 语言风格适配移动端传播
                    """, sceneType, productInfo);

            // 3. 调用 MCP 工具生成文案
            String content = mcpClient.callGenerateText(prompt, 0.7);

            // 4. 构建返回结果
            JSONObject resultData = new JSONObject();
            resultData.put("task_id", task.getString("task_id"));
            resultData.put("content", content);
            resultData.put("user_id", userId);

            return Result.success(resultData);
        } catch (Exception e) {
            return Result.fail(e.getMessage());
        }
    }
}