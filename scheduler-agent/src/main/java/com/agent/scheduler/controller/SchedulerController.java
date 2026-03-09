package com.agent.scheduler.controller;

import com.agent.scheduler.client.A2aClient;
import com.agent.scheduler.common.Result;
import com.alibaba.fastjson2.JSONObject;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.CrossOrigin;

import java.util.UUID;

@RestController
@RequestMapping("/a2a/scheduler")
@RequiredArgsConstructor
@CrossOrigin(origins = "*") // 允许跨域访问
public class SchedulerController {
    private final A2aClient a2aClient;

    @Value("${agent.generator.url}")
    private String generatorAgentUrl;

    @Value("${agent.review.url}")
    private String reviewAgentUrl;

    // 接收用户任务
    @PostMapping("/task")
    public Result<JSONObject> receiveTask(@RequestBody JSONObject params) {
        try {
            // 1. 生成唯一任务 ID
            String taskId = "task_" + UUID.randomUUID().toString().substring(0, 8);
            String userId = params.getString("user_id");
            String productInfo = params.getString("product_info");
            String sceneType = params.getString("scene_type");

            // 2. 构建 A2A 任务，发送给文案生成 Agent
            JSONObject generateTask = new JSONObject();
            generateTask.put("protocol", "A2A/1.0");
            generateTask.put("task_id", taskId);
            generateTask.put("from", "scheduler_agent");
            generateTask.put("to", "generator_agent");
            
            JSONObject taskContent = new JSONObject();
            taskContent.put("input", JSONObject.of(
                    "user_id", userId,
                    "product_info", productInfo,
                    "scene_type", sceneType
            ));
            taskContent.put("priority", 1);
            taskContent.put("timeout", 30000);
            generateTask.put("task", taskContent);

            // 调用生成 Agent
            JSONObject generateResp = a2aClient.sendTask(
                    generatorAgentUrl + "/a2a/generator/task",
                    generateTask
            );
            if (!generateResp.getBoolean("success")) {
                return Result.fail("文案生成失败：" + generateResp.getString("error"));
            }
            String content = generateResp.getJSONObject("data").getString("content");

            // 3. 构建 A2A 任务，发送给审核 Agent
            JSONObject reviewTask = new JSONObject();
            reviewTask.put("protocol", "A2A/1.0");
            reviewTask.put("task_id", taskId);
            reviewTask.put("from", "scheduler_agent");
            reviewTask.put("to", "review_agent");
            
            JSONObject reviewTaskContent = new JSONObject();
            reviewTaskContent.put("input", JSONObject.of("content", content));
            reviewTaskContent.put("priority", 1);
            reviewTaskContent.put("timeout", 10000);
            reviewTask.put("task", reviewTaskContent);

            // 调用审核 Agent
            JSONObject reviewResp = a2aClient.sendTask(
                    reviewAgentUrl + "/a2a/review/task",
                    reviewTask
            );
            if (!reviewResp.getBoolean("success")) {
                return Result.fail("内容审核失败：" + reviewResp.getString("error"));
            }
            JSONObject reviewResult = reviewResp.getJSONObject("data");

            // 4. 汇总结果
            JSONObject finalResult = new JSONObject();
            finalResult.put("task_id", taskId);
            finalResult.put("content", content);
            finalResult.put("review_result", reviewResult);
            finalResult.put("user_id", userId);

            return Result.success(finalResult);
        } catch (Exception e) {
            return Result.fail(e.getMessage());
        }
    }
}