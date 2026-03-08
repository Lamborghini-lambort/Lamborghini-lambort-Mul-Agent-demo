@echo off
echo 正在启动所有 Agent 服务...

echo 启动 MCP-Tools 服务 (端口 8003)...
start "MCP-Tools" cmd /k "cd /d %~dp0mcp-tools\target && java -jar mcp-tools-1.0.0.jar"

timeout /t 5 /nobreak >nul

echo 启动 Review-Agent 服务 (端口 8002)...
start "Review-Agent" cmd /k "cd /d %~dp0review-agent\target && java -jar review-agent-1.0.0.jar"

timeout /t 5 /nobreak >nul

echo 启动 Generator-Agent 服务 (端口 8001)...
start "Generator-Agent" cmd /k "cd /d %~dp0generator-agent\target && java -jar generator-agent-1.0.0.jar"

timeout /t 5 /nobreak >nul

echo 启动 Scheduler-Agent 服务 (端口 8000)...
start "Scheduler-Agent" cmd /k "cd /d %~dp0scheduler-agent\target && java -jar scheduler-agent-1.0.0.jar"

echo 所有服务启动完成！
echo MCP-Tools: http://localhost:8003
echo Review-Agent: http://localhost:8002
echo Generator-Agent: http://localhost:8001
echo Scheduler-Agent: http://localhost:8000
pause