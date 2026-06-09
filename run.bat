@echo off
chcp 65001 > nul
title POS Test Tool

java -version > nul 2>&1
if %ERRORLEVEL% NEQ 0 (
    echo [ERROR] Java not found. Please install Java 17+ and add to PATH.
    echo Download: https://adoptium.net/
    pause
    exit /b 1
)

if not exist "target\POSTestApp-1.0.0.jar" (
    echo [ERROR] JAR not found. Please run build.bat first.
    pause
    exit /b 1
)

:: Copy browser drivers from project root into target\ so the JAR can find them
if exist "msedgedriver.exe"  copy /y "msedgedriver.exe"  "target\msedgedriver.exe"  > nul
if exist "operadriver.exe"   copy /y "operadriver.exe"   "target\operadriver.exe"   > nul
if exist "chromedriver.exe"  copy /y "chromedriver.exe"  "target\chromedriver.exe"  > nul

echo Starting POS Test Tool...
java -jar -Dfile.encoding=UTF-8 target\POSTestApp-1.0.0.jar
