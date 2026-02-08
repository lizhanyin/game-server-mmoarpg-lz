@echo off
cd /d %~dp0

REM ========================================
echo Step 1: Merging split proto files...
echo ========================================

REM 执行 PowerShell 合并脚本
powershell -ExecutionPolicy Bypass -File proto\mergeProto.ps1
if errorlevel 1 (
    echo ========================================
    echo Error: Proto merge failed!
    echo ========================================
    pause
    exit /b 1
)

echo.
echo ========================================
echo Step 2: Generating Java code from proto files...
echo ========================================

REM 设置输出目录
set OUTPUT_DIR=..\lzWorldSrv\gen
if not exist %OUTPUT_DIR% mkdir %OUTPUT_DIR%

echo ========================================
echo Generating Java code from proto files...
echo Output: %OUTPUT_DIR%
echo ========================================

set ERROR_FOUND=0

REM 遍历 proto 子目录下所有 .proto 文件
for %%f in (proto\*.proto) do (
    echo.
    echo Processing: %%f
    .\protoc-33.5\bin\protoc.exe --proto_path=proto --proto_path=./protoc-33.5/include --java_out=%OUTPUT_DIR% "%%f"
    if errorlevel 1 (
        echo [ERROR] Failed to process: %%f
        set ERROR_FOUND=1
    ) else (
        echo [OK] Generated: %%f
    )
)

echo.
if %ERROR_FOUND%==1 (
    echo ========================================
    echo Error: Some proto files failed to compile!
    echo ========================================
) else (
    echo ========================================
    echo All Java Code Created Successfully!
    echo.
    echo Generated files:
    dir /s /b %OUTPUT_DIR%\org\gof\demo\worldsrv\msg\*.java 2>nul
    echo ========================================
)
pause
