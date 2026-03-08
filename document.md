你希望把这套 Agent 架构的营销内容生成系统改成 **Java 版本**，并得到一份**傻瓜式部署流程**（零基础也能跟着做），我会基于 Spring Boot 框架重构所有代码，同时提供 step-by-step 的部署指南，确保你能一键跑通。

---

## 一、Java 版本完整代码（Spring Boot 3.x）
### 技术栈说明
- 核心框架：Spring Boot 3.2.x + Spring Web
- 依赖管理：Maven
- 通信：RestTemplate（实现 A2A/MCP 协议通信）
- 环境：JDK 17+（Spring Boot 3 最低要求）

### 第一步：统一父工程（pom.xml）
先创建一个 Maven 父工程 `ai-agent-marketing`，管理所有依赖：
```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <groupId>com.agent</groupId>
    <artifactId>ai-agent-marketing</artifactId>
    <version>1.0.0</version>
    <packaging>pom</packaging>

    <!-- 子模块 -->
    <modules>
        <module>mcp-tools</module>
        <module>scheduler-agent</module>
        <module>generator-agent</module>
        <module>review-agent</module>
    </modules>

    <!-- 父依赖 -->
    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>3.2.5</version>
        <relativePath/>
    </parent>

    <!-- 全局依赖版本 -->
    <properties>
        <maven.compiler.source>17</maven.compiler.source>
        <maven.compiler.target>17</maven.compiler.target>
        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
        <fastjson2.version>2.0.48</fastjson2.version>
    </properties>

    <!-- 依赖管理 -->
    <dependencyManagement>
        <dependencies>
            <!-- Spring Boot Web -->
            <dependency>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-starter-web</artifactId>
            </dependency>
            <!-- 配置文件处理器 -->
            <dependency>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-configuration-processor</artifactId>
                <optional>true</optional>
            </dependency>
            <!-- FastJSON2（JSON 解析） -->
            <dependency>
                <groupId>com.alibaba.fastjson2</groupId>
                <artifactId>fastjson2</artifactId>
                <version>${fastjson2.version}</version>
            </dependency>
            <!-- lombok（简化代码） -->
            <dependency>
                <groupId>org.projectlombok</groupId>
                <artifactId>lombok</artifactId>
                <optional>true</optional>
            </dependency>
        </dependencies>
    </dependencyManagement>
</project>
```

### 第二步：MCP 工具服务（子模块 mcp-tools）
#### 1. 子模块 pom.xml
```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <parent>
        <groupId>com.agent</groupId>
        <artifactId>ai-agent-marketing</artifactId>
        <version>1.0.0</version>
    </parent>
    <modelVersion>4.0.0</modelVersion>
    <artifactId>mcp-tools</artifactId>

    <dependencies>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>
        <dependency>
            <groupId>com.alibaba.fastjson2</groupId>
            <artifactId>fastjson2</artifactId>
        </dependency>
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
        </dependency>
    </dependencies>

    <!-- 打包插件 -->
    <build>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
                <configuration>
                    <excludes>
                        <exclude>
                            <groupId>org.projectlombok</groupId>
                            <artifactId>lombok</artifactId>
                        </exclude>
                    </excludes>
                </configuration>
            </plugin>
        </plugins>
    </build>
</project>
```

#### 2. 配置文件（application.yml）
```yaml
server:
  port: 8003  # MCP 工具服务端口

# 大模型配置（替换成自己的 API 信息）
openai:
  api-key: your-api-key
  base-url: https://api.openai.com/v1
```

#### 3. 核心代码
##### （1）启动类 McpToolsApplication.java
```java
package com.agent.mcp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class McpToolsApplication {
    public static void main(String[] args) {
        SpringApplication.run(McpToolsApplication.class, args);
    }
}
```

##### （2）通用返回类 Result.java
```java
package com.agent.mcp.common;

import lombok.Data;

@Data
public class Result<T> {
    private boolean success;
    private String error;
    private T data;

    // 成功返回
    public static <T> Result<T> success(T data) {
        Result<T> result = new Result<>();
        result.setSuccess(true);
        result.setData(data);
        return result;
    }

    // 失败返回
    public static <T> Result<T> fail(String error) {
        Result<T> result = new Result<>();
        result.setSuccess(false);
        result.setError(error);
        return result;
    }
}
```

##### （3）大模型调用工具类 OpenAiClient.java
```java
package com.agent.mcp.client;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
@Slf4j
public class OpenAiClient {
    @Value("${openai.api-key}")
    private String apiKey;

    @Value("${openai.base-url}")
    private String baseUrl;

    private final RestTemplate restTemplate = new RestTemplate();

    // 调用大模型生成文本
    public String generateText(String prompt, double temperature) {
        try {
            // 构建请求头
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + apiKey);

            // 构建请求体
            JSONObject requestBody = new JSONObject();
            requestBody.put("model", "gpt-3.5-turbo");
            
            JSONObject message = new JSONObject();
            message.put("role", "user");
            message.put("content", prompt);
            requestBody.put("messages", new JSONObject[]{message});
            
            requestBody.put("temperature", temperature);

            // 发送请求
            HttpEntity<String> request = new HttpEntity<>(requestBody.toString(), headers);
            ResponseEntity<String> response = restTemplate.postForEntity(
                    baseUrl + "/chat/completions",
                    request,
                    String.class
            );

            // 解析响应
            JSONObject responseJson = JSON.parseObject(response.getBody());
            return responseJson.getJSONArray("choices")
                    .getJSONObject(0)
                    .getJSONObject("message")
                    .getString("content");
        } catch (Exception e) {
            log.error("调用大模型失败", e);
            throw new RuntimeException("大模型调用失败：" + e.getMessage());
        }
    }
}
```

##### （4）MCP 接口控制器 McpController.java
```java
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
```

### 第三步：调度 Agent（子模块 scheduler-agent）
#### 1. 子模块 pom.xml
```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <parent>
        <groupId>com.agent</groupId>
        <artifactId>ai-agent-marketing</artifactId>
        <version>1.0.0</version>
    </parent>
    <modelVersion>4.0.0</modelVersion>
    <artifactId>scheduler-agent</artifactId>

    <dependencies>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>
        <dependency>
            <groupId>com.alibaba.fastjson2</groupId>
            <artifactId>fastjson2</artifactId>
        </dependency>
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
            </plugin>
        </plugins>
    </build>
</project>
```

#### 2. 配置文件（application.yml）
```yaml
server:
  port: 8000  # 调度 Agent 端口

# 其他 Agent 地址
agent:
  generator:
    url: http://localhost:8001
  review:
    url: http://localhost:8002
```

#### 3. 核心代码
##### （1）启动类 SchedulerAgentApplication.java
```java
package com.agent.scheduler;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class SchedulerAgentApplication {
    public static void main(String[] args) {
        SpringApplication.run(SchedulerAgentApplication.class, args);
    }
}
```

##### （2）通用返回类 Result.java（和 MCP 模块一样，复制即可）

##### （3）A2A 通信工具类 A2aClient.java
```java
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
```

##### （4）调度控制器 SchedulerController.java
```java
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

import java.util.UUID;

@RestController
@RequestMapping("/a2a/scheduler")
@RequiredArgsConstructor
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
```

### 第四步：文案生成 Agent（子模块 generator-agent）
#### 1. 子模块 pom.xml（和调度 Agent 基本一致，复制后改 artifactId 为 generator-agent）
#### 2. 配置文件（application.yml）
```yaml
server:
  port: 8001  # 生成 Agent 端口

# MCP 工具地址
mcp:
  url: http://localhost:8003
```

#### 3. 核心代码
##### （1）启动类 GeneratorAgentApplication.java
```java
package com.agent.generator;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class GeneratorAgentApplication {
    public static void main(String[] args) {
        SpringApplication.run(GeneratorAgentApplication.class, args);
    }
}
```

##### （2）通用返回类 Result.java（复制）

##### （3）MCP 调用工具类 McpClient.java
```java
package com.agent.generator.client;

import com.alibaba.fastjson2.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
@Slf4j
public class McpClient {
    @Value("${mcp.url}")
    private String mcpUrl;

    private final RestTemplate restTemplate = new RestTemplate();

    // 调用 MCP 生成文本
    public String callGenerateText(String prompt, double temperature) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            JSONObject params = new JSONObject();
            params.put("prompt", prompt);
            params.put("temperature", temperature);
            
            HttpEntity<String> request = new HttpEntity<>(params.toString(), headers);
            ResponseEntity<String> response = restTemplate.postForEntity(
                    mcpUrl + "/mcp/generateText",
                    request,
                    String.class
            );
            
            JSONObject respJson = JSONObject.parseObject(response.getBody());
            if (!respJson.getBoolean("success")) {
                throw new RuntimeException("MCP 工具调用失败：" + respJson.getString("error"));
            }
            return respJson.getString("data");
        } catch (Exception e) {
            log.error("调用 MCP 工具失败", e);
            throw new RuntimeException(e.getMessage());
        }
    }
}
```

##### （4）生成控制器 GeneratorController.java
```java
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
```

### 第五步：内容审核 Agent（子模块 review-agent）
#### 1. 子模块 pom.xml（复制 generator-agent，改 artifactId 为 review-agent）
#### 2. 配置文件（application.yml）
```yaml
server:
  port: 8002  # 审核 Agent 端口

# MCP 工具地址
mcp:
  url: http://localhost:8003
```

#### 3. 核心代码
##### （1）启动类 ReviewAgentApplication.java
```java
package com.agent.review;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class ReviewAgentApplication {
    public static void main(String[] args) {
        SpringApplication.run(ReviewAgentApplication.class, args);
    }
}
```

##### （2）通用返回类 Result.java（复制）

##### （3）MCP 审核工具类 McpReviewClient.java
```java
package com.agent.review.client;

import com.alibaba.fastjson2.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
@Slf4j
public class McpReviewClient {
    @Value("${mcp.url}")
    private String mcpUrl;

    private final RestTemplate restTemplate = new RestTemplate();

    // 调用 MCP 审核工具
    public JSONObject callReviewText(String content) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            JSONObject params = new JSONObject();
            params.put("content", content);
            
            HttpEntity<String> request = new HttpEntity<>(params.toString(), headers);
            ResponseEntity<String> response = restTemplate.postForEntity(
                    mcpUrl + "/mcp/reviewText",
                    request,
                    String.class
            );
            
            JSONObject respJson = JSONObject.parseObject(response.getBody());
            if (!respJson.getBoolean("success")) {
                throw new RuntimeException("MCP 审核工具调用失败：" + respJson.getString("error"));
            }
            return respJson.getJSONObject("data");
        } catch (Exception e) {
            log.error("调用 MCP 审核工具失败", e);
            throw new RuntimeException(e.getMessage());
        }
    }
}
```

##### （4）审核控制器 ReviewController.java
```java
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
```

---

## 二、傻瓜式部署流程（零基础也能会）
### 前置准备（必做）
1. **安装 JDK 17**
    - 下载地址：https://www.oracle.com/cn/java/technologies/downloads/#java17
    - 安装步骤：下一步到底，记住安装路径（比如 `C:\Program Files\Java\jdk-17.0.9`）
    - 配置环境变量：
        - 新建系统变量 `JAVA_HOME`，值为 JDK 安装路径
        - 编辑系统变量 `Path`，新增 `%JAVA_HOME%\bin`
    - 验证：打开 cmd，输入 `java -version`，显示 JDK 17 版本即可

2. **安装 Maven**
    - 下载地址：https://maven.apache.org/download.cgi（选 Binary zip archive）
    - 解压到任意目录（比如 `D:\apache-maven-3.9.6`）
    - 配置环境变量：
        - 新建系统变量 `MAVEN_HOME`，值为解压路径
        - 编辑 `Path`，新增 `%MAVEN_HOME%\bin`
    - 验证：cmd 输入 `mvn -v`，显示 Maven 版本即可

3. **获取代码**
    - 把上面的代码按「父工程 + 4 个子模块」的结构，在本地新建文件夹（比如 `D:\ai-agent-marketing`），按目录存放：
      ```
      ai-agent-marketing/
      ├── pom.xml（父工程）
      ├── mcp-tools/
      │   ├── pom.xml
      │   └── src/main/java/com/agent/mcp/...
      ├── scheduler-agent/
      │   ├── pom.xml
      │   └── src/main/java/com/agent/scheduler/...
      ├── generator-agent/
      │   ├── pom.xml
      │   └── src/main/java/com/agent/generator/...
      └── review-agent/
          ├── pom.xml
          └── src/main/java/com/agent/review/...
      ```

4. **修改配置**
    - 打开 `mcp-tools/src/main/resources/application.yml`，把 `openai.api-key` 改成自己的大模型 API 密钥（OpenAI/文心一言/通义千问均可，注意适配 API 格式）

### 部署步骤（一步一步来）
#### 步骤1：打包所有模块
1. 打开 cmd，进入父工程目录（`cd D:\ai-agent-marketing`）
2. 执行打包命令：`mvn clean package -DskipTests`
    - 等待打包完成（出现 `BUILD SUCCESS` 即为成功）
    - 每个子模块的 `target` 目录下会生成 `xxx-1.0.0.jar` 包

#### 步骤2：启动服务（按顺序）
**注意：每个服务单独开一个 cmd 窗口，不要关闭！**

1. **启动 MCP 工具服务**
   ```bash
   # 进入 mcp-tools 的 target 目录
   cd D:\ai-agent-marketing\mcp-tools\target
   # 启动 jar 包
   java -jar mcp-tools-1.0.0.jar
   ```
    - 看到 `Started McpToolsApplication in xxx seconds` 即为启动成功

2. **启动调度 Agent**
   ```bash
   cd D:\ai-agent-marketing\scheduler-agent\target
   java -jar scheduler-agent-1.0.0.jar
   ```

3. **启动文案生成 Agent**
   ```bash
   cd D:\ai-agent-marketing\generator-agent\target
   java -jar generator-agent-1.0.0.jar
   ```

4. **启动内容审核 Agent**
   ```bash
   cd D:\ai-agent-marketing\review-agent\target
   java -jar review-agent-1.0.0.jar
   ```

#### 步骤3：测试调用
1. 下载 Postman（免费）：https://www.postman.com/downloads/
2. 打开 Postman，新建 POST 请求：
    - URL：`http://localhost:8000/a2a/scheduler/task`
    - 请求体（Body）：选择 `raw` → `JSON`，输入以下内容：
      ```json
      {
        "user_id": "1001",
        "product_info": "新款智能手机，续航10小时，像素5000万",
        "scene_type": "电商推广"
      }
      ```
3. 点击「Send」，即可看到返回结果（包含生成的文案和审核结果）

### 常见问题解决（傻瓜式排错）
1. **端口被占用**：修改对应模块的 `application.yml` 中的 `server.port`（比如 8003 改成 8083）
2. **JDK 版本不对**：确认 `java -version` 是 17，不是 8/11
3. **Maven 打包失败**：检查代码是否有语法错误，或依赖是否缺失
4. **API 调用失败**：确认 API 密钥正确，且大模型服务可访问

---

## 总结
1. Java 版本基于 Spring Boot 3.x 重构，完全贴合企业级开发规范，符合岗位对 Java 开发的要求；
2. 部署流程全程 cmd 操作，无复杂配置，零基础也能跟着完成；
3. 核心亮点：Agent 分工 + A2A 通信 + MCP 工具调用，完全匹配 AI 应用方向岗位要求。

如果部署过程中遇到任何问题（比如打包失败、启动报错），直接告诉我具体的错误提示，我会给你对应的解决方法。