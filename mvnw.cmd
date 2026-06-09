@REM ----------------------------------------------------------------------------
@REM Licensed to the Apache Software Foundation (ASF)
@REM Maven Wrapper startup batch script
@REM ----------------------------------------------------------------------------
@IF "%__MVNW_ARG0_NAME__%"=="" (SET __MVNW_ARG0_NAME__=%~nx0)
@SET ___MVNW_BNME=%__MVNW_ARG0_NAME__%
@SET ___MVNW_CMD=%MAVEN_WRAPPER_JAR%
@IF "%___MVNW_CMD%"=="" SET ___MVNW_CMD=.mvn\wrapper\maven-wrapper.jar

@SET DOWNLOAD_URL=https://repo.maven.apache.org/maven2/org/apache/maven/wrapper/maven-wrapper/3.2.0/maven-wrapper-3.2.0.jar

@IF EXIST %___MVNW_CMD% GOTO validateJar

@echo Downloading Maven Wrapper...
@powershell -Command "&{[Net.ServicePointManager]::SecurityProtocol=[Net.SecurityProtocolType]::Tls12; Invoke-WebRequest -Uri '%DOWNLOAD_URL%' -OutFile '.mvn\wrapper\maven-wrapper.jar'}" 2>nul
@if not exist ".mvn\wrapper\maven-wrapper.jar" (
  @echo [ERROR] Could not download maven-wrapper.jar
  @exit /B 1
)

:validateJar
@java -cp .mvn\wrapper\maven-wrapper.jar "-Dmaven.multiModuleProjectDirectory=%MAVEN_PROJECTBASEDIR%" org.apache.maven.wrapper.MavenWrapperMain %* 2>nul
@IF %ERRORLEVEL% NEQ 0 (
  @echo [INFO] Falling back to system mvn...
  @mvn %*
)
