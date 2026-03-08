@echo off
echo测试 AI Agent系统...

echo 请确保所有服务已启动：
echo - MCP-Tools: http://localhost:8003
echo - Review-Agent: http://localhost:8002  
echo - Generator-Agent: http://localhost:8001
echo - Scheduler-Agent: http://localhost:8000
echo.

echo 发送测试请求...
echo.

curl -X POST http://localhost:8000/a2a/scheduler/task ^
  -H "Content-Type: application/json" ^
  -d "{\"user_id\":\"1001\",\"product_info\":\"新款智能手机，续航10小时，像素5000万\",\"scene_type\":\"电商推广\"}"

echo.
echo测试完成！
pause