@echo off
chcp 65001 >nul
cd /d "%~dp0"
echo ==========================================
echo   搜索机器人模组 - 打包构建
echo ==========================================
echo.
echo 正在编译并打包模组 JAR...
echo.
gradlew build
if %errorlevel% == 0 (
    echo.
    echo ✅ 打包成功！
    echo 模组文件位于：build\libs\ 目录
    explorer "%~dp0build\libs"
) else (
    echo.
    echo ❌ 打包失败，请查看上方错误信息
)
pause
