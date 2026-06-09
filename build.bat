@echo off
chcp 65001 > nul
title POS Test App - Build
echo ========================================
echo   POS Test App v2.0 - Build (Windows)
echo   API Tests + UI Selenium Tests
echo ========================================
echo.

:: ---- Kill any running instance that locks the JAR ----
echo [INFO] Checking for running POSTestApp instances...
set JAR_NAME=POSTestApp-1.0.0.jar
for /f "tokens=2" %%i in ('tasklist /fi "imagename eq javaw.exe" /fo csv 2^>nul ^| findstr /i "javaw"') do (
    set JAVA_PID=%%i
)
:: Kill any java/javaw process that has the JAR open
taskkill /fi "imagename eq javaw.exe" /fi "windowtitle eq POS Test*" /f > nul 2>&1
taskkill /fi "imagename eq java.exe"  /fi "windowtitle eq POS Test*" /f > nul 2>&1

:: Also try to delete the JAR directly; if it fails the process is still locked
if exist "target\%JAR_NAME%" (
    del /f "target\%JAR_NAME%" > nul 2>&1
    if exist "target\%JAR_NAME%" (
        echo [WARN] JAR is still locked by another process.
        echo        Please close any running POS Test App window and press any key...
        pause
        del /f "target\%JAR_NAME%" > nul 2>&1
        if exist "target\%JAR_NAME%" (
            echo [ERROR] Cannot delete target\%JAR_NAME% - it is still in use.
            echo         Close the app completely and run build.bat again.
            pause
            exit /b 1
        )
    )
)
echo [OK] Ready to build.
echo.

:: ---- Check Java ----
java -version > nul 2>&1
if %ERRORLEVEL% NEQ 0 (
    echo [ERROR] Java not found in PATH!
    echo.
    echo Please install Java 17 or higher:
    echo   Option 1: winget install Microsoft.OpenJDK.17
    echo   Option 2: Download from https://adoptium.net/
    echo.
    echo After installing, restart this script.
    pause
    exit /b 1
)
echo [OK] Java found:
java -version 2>&1

:: ---- Find Maven ----
mvn -version > nul 2>&1
if %ERRORLEVEL% EQU 0 (
    set MVN_CMD=mvn
    echo [OK] Maven found in PATH.
) else (
    echo [INFO] Maven not in PATH. Trying Maven Wrapper...
    if exist "mvnw.cmd" (
        set MVN_CMD=mvnw.cmd
        echo [OK] Maven Wrapper found.
    ) else (
        echo [ERROR] Maven not found!
        echo.
        echo Install Maven with one of these commands:
        echo   winget install Apache.Maven
        echo   choco install maven
        echo.
        pause
        exit /b 1
    )
)

echo.
echo [BUILD] Running: %MVN_CMD% clean package...
echo.

call %MVN_CMD% clean package -q --no-transfer-progress

if %ERRORLEVEL% NEQ 0 (
    echo.
    echo [ERROR] Build failed!
    echo Run with full output: %MVN_CMD% clean package
    pause
    exit /b 1
)

echo.
echo ========================================
echo   BUILD SUCCESS!
echo ========================================
echo.
echo JAR file created: target\POSTestApp-1.0.0.jar
echo.
echo To run the application:
echo   Double-click: run.bat
echo   Or: java -jar target\POSTestApp-1.0.0.jar
echo.
set /p LAUNCH="Launch the app now? (Y/N): "
if /i "%LAUNCH%"=="Y" (
    start javaw -jar -Dfile.encoding=UTF-8 target\POSTestApp-1.0.0.jar
)
