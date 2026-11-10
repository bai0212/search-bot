@echo off
chcp 65001 >nul
cd /d "%~dp0"
echo ==========================================
echo   搜索机器人模组 - 启动测试客户端
echo ==========================================
echo.
echo 正在启动 Minecraft 客户端（Forge 1.20.1）...
echo.
gradlew runClient
pause
