@echo off
echo启动单个服务
echo 1. MCP-Tools (8003)
echo 2. Review-Agent (8002)
echo 3. Generator-Agent (8001)
echo 4. Scheduler-Agent (8000)
echo.

set /p choice=请选择要启动的服务 (1-4): 

if "%choice%"=="1" (
    echo启动 MCP-Tools 服务...
    cd mcp-tools\target
    java -jar mcp-tools-1.0.0.jar
) else if "%choice%"=="2" (
    echo启动 Review-Agent 服务...
    cd review-agent\target
    java -jar review-agent-1.0.0.jar
) else if "%choice%"=="3" (
    echo启 Generator-Agent 服务...
    cd generator-agent\target
    java -jar generator-agent-1.0.0.jar
) else if "%choice%"=="4" (
    echo 启动 Scheduler-Agent 服务...
    cd scheduler-agent\target
    java -jar scheduler-agent-1.0.0.jar
) else (
    echo 无效选择
)

pause