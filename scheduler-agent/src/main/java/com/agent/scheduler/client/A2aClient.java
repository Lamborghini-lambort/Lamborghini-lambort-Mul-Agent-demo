package com.agent.scheduler.client;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
@Slf4j
public class A2aClient {
    private final RestTemplate restTemplate = new RestTemplate();

    // 发送 A2A 任务
    public JSONObject sendTask(String url, JSONObject task) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            HttpEntity<String> request = new HttpEntity<>(task.toString(), headers);
            ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);
            
            return JSON.parseObject(response.getBody());
        } catch (Exception e) {
            log.error("A2A 任务发送失败", e);
            throw new RuntimeException("A2A 通信失败：" + e.getMessage());
        }
    }
}