package com.agent.mcp.controller;

import com.agent.mcp.service.McpToolService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * MCP Controller - 处理 MCP JSON-RPC 2.0 协议请求
 * 
 * MCP (Model Context Protocol) 是一种标准化的 AI 工具调用协议
 * 基于 JSON-RPC 2.0，提供统一的工具发现和调用接口
 */
@RestController
@RequestMapping("/mcp")
@RequiredArgsConstructor
@Slf4j
public class McpController {
    private final McpToolService toolService;

    /**
     * MCP JSON-RPC 2.0 消息处理端点
     * 
     * 支持的 method:
     * - tools/call: 调用指定工具
     * - tools/list: 获取可用工具列表
     */
    @PostMapping("/message")
    public Map<String, Object> handleMessage(@RequestBody Map<String, Object> request) {
        log.info("收到 MCP 请求：{}", request);
        
        String method = (String) request.get("method");
        Object id = request.get("id");
        
        try {
            if ("tools/call".equals(method)) {
                return handleToolsCall(id, request);
            } else if ("tools/list".equals(method)) {
                return handleToolsList(id);
            } else {
                return createErrorResponse(id, -32601, "Method not found: " + method);
            }
        } catch (Exception e) {
            return createErrorResponse(id, -32000, "Internal error: " + e.getMessage());
        }
    }
    
    /**
     * 处理 tools/call 请求
     */
    private Map<String, Object> handleToolsCall(Object id, Map<String, Object> request) throws Exception {
        Map<String, Object> params = (Map<String, Object>) request.get("params");
        String toolName = (String) params.get("name");
        Map<String, Object> arguments = (Map<String, Object>) params.get("arguments");
        
        // 调用对应的工具
        String result = toolService.callTool(toolName, arguments);
        
        // 返回 MCP 标准响应
        return Map.of(
            "jsonrpc", "2.0",
            "id", id,
            "result", Map.of(
                "content", List.of(Map.of("type", "text", "text", result))
            )
        );
    }
    
    /**
     * 处理 tools/list 请求
     */
    private Map<String, Object> handleToolsList(Object id) {
        return Map.of(
            "jsonrpc", "2.0",
            "id", id,
            "result", Map.of(
                "tools", List.of(
                    Map.of(
                        "name", "generate_marketing_copy",
                        "description", "根据产品信息生成营销文案"
                    ),
                    Map.of(
                        "name", "review_content",
                        "description", "审核内容合规性，检测敏感词"
                    )
                )
            )
        );
    }
    
    /**
     * 创建错误响应
     */
    private Map<String, Object> createErrorResponse(Object id, int code, String message) {
        return Map.of(
            "jsonrpc", "2.0",
            "id", id,
            "error", Map.of("code", code, "message", message)
        );
    }
    
    /**
     * MCP SSE (Server-Sent Events) 连接端点
     * 用于建立实时通信通道
     */
    @GetMapping(value = "/sse", produces = "text/event-stream")
    public String handleSse() {
        log.info("SSE 连接建立");
        // 告知客户端消息端点地址
        return "event: endpoint\ndata: /mcp/message\n\n";
    }
}
