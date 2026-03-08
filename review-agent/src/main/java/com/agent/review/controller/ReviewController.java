package com.agent.review.controller;

import com.agent.review.client.McpReviewClient;
import com.agent.review.common.Result;
import com.alibaba.fastjson2.JSONObject;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/a2a/review")
@RequiredArgsConstructor
public class ReviewController {
    private final McpReviewClient mcpReviewClient;

    // 处理调度 Agent 发来的审核任务
    @PostMapping("/task")
    public Result<JSONObject> handleTask(@RequestBody JSONObject task) {
        try {
            // 1. 解析任务参数
            String content = task.getJSONObject("task").getJSONObject("input").getString("content");

            // 2. 调用 MCP 审核工具
            JSONObject reviewResult = mcpReviewClient.callReviewText(content);

            return Result.success(reviewResult);
        } catch (Exception e) {
            return Result.fail(e.getMessage());
        }
    }
}