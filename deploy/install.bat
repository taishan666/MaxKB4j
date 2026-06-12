@echo off
setlocal enabledelayedexpansion

REM ===========================================================================
REM  MaxKB4j -- One-click Install ^& Deploy Script (Windows)
REM  Supports: Docker-Compose (prebuilt image) ^| Source build -> image -> compose
REM ===========================================================================

title MaxKB4j Installer

cd /d "%~dp0.."
set "PROJECT_DIR=%CD%"
set "START_DIR=%PROJECT_DIR%\maxkb4j-start"
set "JAR_PATH=%START_DIR%\target\maxkb4j-start.jar"
set "DOCKERFILE_PATH=%START_DIR%\Dockerfile"
set "LOG_DIR=%PROJECT_DIR%\logs"
set "DOCKER_COMPOSE_FILE=%PROJECT_DIR%\docker-compose.yml"
set "DOCKER_COMPOSE_DEV_FILE=%PROJECT_DIR%\docker-compose.dev.yml"
set "DEFAULT_PORT=8080"
set "APP_IMAGE=registry.cn-hangzhou.aliyuncs.com/tarzanx/maxkb4j:latest"

:main
cls
echo ==============================================
echo    MaxKB4j -- One-click Install ^& Deploy
echo ==============================================
echo.
echo Select deployment mode:
echo.
echo   1^) Docker Compose (Prebuilt Image^)
echo       -- Fast, pulls remote image (recommended^)
echo.
echo   2^) Source Build -^> Image -^> Docker Compose
echo       -- Build from source into a local image,
echo          then deploy via docker-compose
echo.
echo   3^) Uninstall
echo       -- Stop and remove
echo.
set /p "mode_choice=Enter choice [1-3]: "

if "%mode_choice%"=="1" goto :deploy_docker
if "%mode_choice%"=="2" goto :deploy_build
if "%mode_choice%"=="3" goto :uninstall
echo Invalid choice.
timeout /t 2 >nul
goto :main

REM ===========================================================================
REM  Prerequisite Checks
REM ===========================================================================
:check_docker
echo.
echo [INFO] Checking Docker...
where docker >nul 2>&1
if errorlevel 1 (
    echo [ERROR] Docker not found. Please install Docker Desktop first.
    echo         https://docs.docker.com/desktop/setup/install/windows-install/
    pause
    exit /b 1
)
echo [OK] Docker found:
docker --version
goto :eof

:check_java
echo [INFO] Checking Java...
where java >nul 2>&1
if errorlevel 1 (
    echo [ERROR] Java not found. Please install JDK 21+.
    echo         https://adoptium.net/
    pause
    exit /b 1
)
echo [OK] Java found:
java -version 2>&1 | findstr "version"
goto :eof

:check_maven
echo [INFO] Checking Maven...
where mvn >nul 2>&1
if errorlevel 1 (
    echo [ERROR] Maven not found. Please install Maven 3.8+.
    echo         https://maven.apache.org/download.cgi
    pause
    exit /b 1
)
echo [OK] Maven found:
mvn --version 2>&1 | findstr "Maven"
goto :eof

REM ===========================================================================
REM  Helper: Prepare host directories used as bind mounts
REM ===========================================================================
:prepare_dirs
if not exist "%LOG_DIR%" mkdir "%LOG_DIR%"
if not exist "%PROJECT_DIR%\postgres\data" mkdir "%PROJECT_DIR%\postgres\data"
if not exist "%PROJECT_DIR%\mongo\data" mkdir "%PROJECT_DIR%\mongo\data"
if not exist "%PROJECT_DIR%\mongo\configdb" mkdir "%PROJECT_DIR%\mongo\configdb"
goto :eof

REM ===========================================================================
REM  Helper: Bring up the docker-compose stack and report status
REM     Arg %1 = "pull" to pull image first, "nopull" to use local image only
REM ===========================================================================
:compose_up
cd /d "%PROJECT_DIR%"
if /i "%~1"=="pull" (
    echo [INFO] Pulling images...
    docker compose -f docker-compose.yml pull
    if errorlevel 1 (
        echo [WARN] Pull failed, will use existing images if available.
    )
)

echo [INFO] Starting services...
docker compose -f docker-compose.yml up -d
if errorlevel 1 (
    echo [ERROR] docker compose up failed.
    pause
    exit /b 1
)

echo [INFO] Waiting for services...
timeout /t 10 /nobreak >nul

for %%c in ("maxkb4j-pgvector" "maxkb4j-mongo" "maxkb4j-app") do (
    docker ps --format "{{.Names}}" 2>nul | findstr /C:"%%~c" >nul
    if !errorlevel! equ 0 (
        echo [OK] Container %%~c is running.
    ) else (
        echo [WARN] Container %%~c not running. Check: docker logs %%~c
    )
)
goto :eof

REM ===========================================================================
REM  Mode 1: Docker Compose with prebuilt remote image
REM ===========================================================================
:deploy_docker
cls
echo =============================================
echo  Mode: Docker Compose (Prebuilt Image^)
echo =============================================
echo.

call :check_docker

if not exist "%DOCKER_COMPOSE_FILE%" (
    echo [ERROR] docker-compose.yml not found.
    pause
    exit /b 1
)

call :prepare_dirs
call :compose_up pull

echo.
echo [OK] === Deployment complete! ===
echo.
echo  Web UI:  http://localhost:%DEFAULT_PORT%/admin/login
echo  Default: admin / tarzan@123456
echo.
echo  View logs:  docker compose -f docker-compose.yml logs -f
echo  Stop all:   docker compose -f docker-compose.yml down
echo.
pause
exit /b 0

REM ===========================================================================
REM  Mode 2: Build source -> local image -> deploy via docker-compose
REM ===========================================================================
:deploy_build
cls
echo =============================================
echo  Mode: Source Build -^> Image -^> Compose
echo =============================================
echo.

call :check_docker
call :check_java
call :check_maven

if not exist "%DOCKER_COMPOSE_FILE%" (
    echo [ERROR] docker-compose.yml not found.
    pause
    exit /b 1
)

if not exist "%DOCKERFILE_PATH%" (
    echo [ERROR] Dockerfile not found at %DOCKERFILE_PATH%
    pause
    exit /b 1
)

echo.
echo [INFO] Step 1: Building project with Maven...
echo [INFO] This may take a few minutes...
cd /d "%PROJECT_DIR%"
call mvn clean package -pl maxkb4j-start -am -DskipTests
if errorlevel 1 (
    echo [ERROR] Build failed. Check the output above.
    pause
    exit /b 1
)
echo [OK] Maven build successful.

if not exist "%JAR_PATH%" (
    echo [ERROR] JAR not found at %JAR_PATH%
    pause
    exit /b 1
)

echo.
echo [INFO] Step 2: Building Docker image %APP_IMAGE% ...
cd /d "%START_DIR%"
docker build -t "%APP_IMAGE%" .
if errorlevel 1 (
    echo [ERROR] Docker image build failed.
    pause
    exit /b 1
)
echo [OK] Docker image built: %APP_IMAGE%

echo.
echo [INFO] Step 3: Deploying via docker-compose (using local image^)...
call :prepare_dirs
call :compose_up nopull

echo.
echo [OK] === Deployment complete! ===
echo.
echo  Web UI:  http://localhost:%DEFAULT_PORT%/admin/login
echo  Default: admin / tarzan@123456
echo.
echo  View logs:  docker compose -f docker-compose.yml logs -f
echo  Stop all:   docker compose -f docker-compose.yml down
echo.
pause
exit /b 0

REM ===========================================================================
REM  Uninstall
REM ===========================================================================
:uninstall
cls
echo =============================================
echo  Uninstall MaxKB4j
echo =============================================
echo.
echo [WARN] This will stop all containers and optionally remove data.
set /p "confirm=Continue? [y/N]: "
if /i not "!confirm!"=="y" (
    echo Uninstall cancelled.
    timeout /t 2 >nul
    goto :main
)

echo [INFO] Stopping Docker Compose services...
cd /d "%PROJECT_DIR%"
if exist "%DOCKER_COMPOSE_FILE%" (
    docker compose -f docker-compose.yml down 2>nul
)
if exist "%DOCKER_COMPOSE_DEV_FILE%" (
    docker compose -f docker-compose.dev.yml down 2>nul
)

set /p "rm_data=Remove database data volumes? [y/N]: "
if /i "!rm_data!"=="y" (
    echo [INFO] Removing data volumes...
    docker compose -f docker-compose.yml down -v 2>nul
    docker compose -f docker-compose.dev.yml down -v 2>nul
    if exist "%PROJECT_DIR%\postgres\data" rmdir /s /q "%PROJECT_DIR%\postgres\data"
    if exist "%PROJECT_DIR%\mongo" rmdir /s /q "%PROJECT_DIR%\mongo"
)

set /p "rm_logs=Remove logs? [y/N]: "
if /i "!rm_logs!"=="y" (
    if exist "%LOG_DIR%" rmdir /s /q "%LOG_DIR%"
)

echo [OK] Uninstall complete.
pause
goto :main
