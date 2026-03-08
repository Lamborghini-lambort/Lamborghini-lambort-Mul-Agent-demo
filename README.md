# AI Agent 营销内容生成系统

基于 Spring Boot 3.2 的多 Agent 协作营销文案生成系统。

## 系统架构

```
用户请求 → Scheduler-Agent (8000) → Generator-Agent (8001) → MCP-Tools (8003)
                                    ↓
                              Review-Agent (8002) → MCP-Tools (8003)
```

## 模块说明

### 1. MCP-Tools (端口 8003)
- 提供大模型调用服务
- 提供内容审核服务
- 依赖：OpenAI API 或其他大模型服务

### 2. Scheduler-Agent (端口 8000)
- 任务调度中心
- 协调各 Agent 工作流程
- 接收用户请求并分发任务

### 3. Generator-Agent (端口 8001)
- 专门负责文案生成
- 调用 MCP-Tools 生成营销文案

### 4. Review-Agent (端口 8002)
- 专门负责内容审核
- 调用 MCP-Tools 进行内容合规检查

## 部署步骤

### 1. 环境准备
- JDK 17+
- Maven 3.6+

### 2. 配置 API 密钥
编辑 `mcp-tools/src/main/resources/application.yml`：
```yaml
openai:
  api-key: your-api-key  # 替换为你的 OpenAI API 密钥
```

### 3. 打包项目
```bash
mvn clean package -DskipTests
```

### 4. 启动服务
按以下顺序启动服务：

1. **MCP-Tools**: `java -jar mcp-tools/target/mcp-tools-1.0.0.jar`
2. **Review-Agent**: `java -jar review-agent/target/review-agent-1.0.0.jar`
3. **Generator-Agent**: `java -jar generator-agent/target/generator-agent-1.0.0.jar`
4. **Scheduler-Agent**: `java -jar scheduler-agent/target/scheduler-agent-1.0.0.jar`

或者使用启动脚本：
```bash
start-all-services.bat
```

## 测试接口

使用 Postman 或 curl 测试：

```bash
curl -X POST http://localhost:8000/a2a/scheduler/task \
  -H "Content-Type: application/json" \
  -d '{
    "user_id": "1001",
    "product_info": "新款智能手机，续航10小时，像素5000万",
    "scene_type": "电商推广"
  }'
```

## 项目结构

```
ai-agent-marketing/
├── pom.xml                    # 父工程配置
├── mcp-tools/                 # MCP 工具服务
│   ├── pom.xml
│   └── src/main/
│       ├── java/com/agent/mcp/
│       │   ├── McpToolsApplication.java
│       │   ├── common/Result.java
│       │   ├── client/OpenAiClient.java
│       │   └── controller/McpController.java
│       └── resources/application.yml
├── scheduler-agent/           # 调度 Agent
│   ├── pom.xml
│   └── src/main/
│       ├── java/com/agent/scheduler/
│       │   ├── SchedulerAgentApplication.java
│       │   ├── common/Result.java
│       │   ├── client/A2aClient.java
│       │   └── controller/SchedulerController.java
│       └── resources/application.yml
├── generator-agent/           # 生成 Agent
│   ├── pom.xml
│   └── src/main/
│       ├── java/com/agent/generator/
│       │   ├── GeneratorAgentApplication.java
│       │   ├── common/Result.java
│       │   ├── client/McpClient.java
│       │   └── controller/GeneratorController.java
│       └── resources/application.yml
└── review-agent/              # 审核 Agent
    ├── pom.xml
    └── src/main/
        ├── java/com/agent/review/
        │   ├── ReviewAgentApplication.java
        │   ├── common/Result.java
        │   ├── client/McpReviewClient.java
        │   └── controller/ReviewController.java
        └── resources/application.yml
```