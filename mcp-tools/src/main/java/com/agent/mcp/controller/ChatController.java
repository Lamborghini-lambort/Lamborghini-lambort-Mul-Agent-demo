package com.agent.mcp.controller;

import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

import java.io.IOException;

/**
 * MCP Host - 聊天界面控制器
 * 提供用户与 MCP Server 交互的 Web 界面
 */
@Controller
public class ChatController {

    /**
     * 根路径，转发到 chat.html
     */
    @GetMapping("/")
    public ResponseEntity<String> index() throws IOException {
        ClassPathResource resource = new ClassPathResource("static/chat.html");
        if (resource.exists()) {
            return ResponseEntity.ok()
                .contentType(MediaType.TEXT_HTML)
                .body(new String(resource.getInputStream().readAllBytes()));
        }
        return ResponseEntity.notFound().build();
    }
}
