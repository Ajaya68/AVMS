@echo off
setlocal EnableExtensions
title AVMS Launcher
cd /d "%~dp0"
set "ROOT=%~dp0"
set "ROOT=%ROOT:~0,-1%"

echo ============================================
echo  AVMS - Ajaya Venture Management System
echo  Starting Database + Backend + Frontend
echo ============================================
echo.

REM ---------- Resolve system tools (robust even if user PATH is incomplete) ----------
if not defined SystemRoot set "SystemRoot=C:\Windows"
set "CURL=curl"
if exist "%SystemRoot%\System32\curl.exe" set "CURL=%SystemRoot%\System32\curl.exe"
set "SLEEP=%SystemRoot%\System32\timeout.exe"
if not exist "%SLEEP%" set "SLEEP=timeout"

REM ---------- Pre-flight checks (probe the tools directly, no 'where' needed) ----------
java -version >nul 2>&1
if errorlevel 1 (
  echo [ERROR] 'java' not found on PATH. Install Java 17+ and retry.
  pause
  exit /b 1
)
node --version >nul 2>&1
if errorlevel 1 (
  echo [ERROR] 'node' not found on PATH. Install Node.js 20+ and retry.
  pause
  exit /b 1
)
call npm --version >nul 2>&1
if errorlevel 1 (
  echo [ERROR] 'npm' not found on PATH. Install Node.js 20+ and retry.
  pause
  exit /b 1
)
sqlplus -v >nul 2>&1
if errorlevel 1 (
  echo [ERROR] 'sqlplus' not found on PATH. Install Oracle 21c client/DB and retry.
  pause
  exit /b 1
)
if not exist "%ROOT%\backend-java\target\avms-backend-0.1.0.jar" (
  echo [ERROR] Backend jar not found: backend-java\target\avms-backend-0.1.0.jar
  echo         Build it once with:  cd backend-java ^&^& mvn -B package -DskipTests
  pause
  exit /b 1
)
if not exist "%ROOT%\frontend\node_modules" (
  echo Frontend dependencies missing - running 'npm install' first ^(one time only^)...
  pushd "%ROOT%\frontend"
  call npm install
  if errorlevel 1 (
    echo [ERROR] 'npm install' failed. Fix the errors above and retry.
    popd
    pause
    exit /b 1
  )
  popd
)
if not exist "%ROOT%\frontend\.env" (
  echo VITE_API_URL=http://localhost:8081/api>"%ROOT%\frontend\.env"
  echo Created frontend\.env ^(VITE_API_URL=http://localhost:8081/api^)
)
echo Pre-flight checks passed.
echo.

REM ---------- 1/3 Database ----------
echo [1/3] Checking Oracle database ^(avms@localhost:1521/orclpdb^)...
echo exit | sqlplus -L -S avms/avms@localhost:1521/orclpdb >nul 2>nul
if errorlevel 1 (
  echo   Database not reachable - attempting to start listener and database...
  lsnrctl start >nul 2>&1
  (echo startup & echo exit) | sqlplus -S / as sysdba >nul 2>&1
  (echo alter pluggable database orclpdb open; & echo exit) | sqlplus -S / as sysdba >nul 2>&1
  %SLEEP% /t 5 /nobreak >nul
  echo exit | sqlplus -L -S avms/avms@localhost:1521/orclpdb >nul 2>nul
  if errorlevel 1 (
    echo [ERROR] Could not reach Oracle. Start it manually with 'lsnrctl start'
    echo         then as SYSDBA run: startup; alter pluggable database orclpdb open;
    pause
    exit /b 1
  )
)
echo   Database is UP.
echo.

REM ---------- 2/3 Backend ----------
echo [2/3] Checking backend http://localhost:8081 ...
%CURL% -s -f --connect-timeout 3 -o nul http://localhost:8081/api/health/ >nul 2>nul
if errorlevel 1 (
  echo   Starting backend in a new window...
  start "AVMS Backend" /d "%ROOT%\backend-java" cmd /k "set SPRING_PROFILES_ACTIVE=dev&& set DB_URL=jdbc:oracle:thin:@localhost:1521/orclpdb&& set DB_USER=avms&& set DB_PASSWORD=avms&& set JWT_SECRET=dev-only-insecure-secret-do-not-use-in-prod-0123456789&& set JWT_ACCESS_MINUTES=30&& set JWT_REFRESH_DAYS=7&& set CORS_ORIGINS=http://localhost:5173,http://localhost:5174&& java -Duser.timezone=UTC -jar target\avms-backend-0.1.0.jar --server.port=8081"
  echo   Waiting for backend to become healthy ^(up to 2 min^)...
  set "BACKEND_OK="
  for /l %%i in (1,1,24) do (
    %CURL% -s -f --connect-timeout 3 -o nul http://localhost:8081/api/health/ >nul 2>nul
    if not errorlevel 1 (
      set "BACKEND_OK=1"
      goto backend_ready
    )
    %SLEEP% /t 5 /nobreak >nul
  )
  :backend_ready
  if not defined BACKEND_OK (
    echo [ERROR] Backend did not become healthy. See the 'AVMS Backend' window for errors.
    pause
    exit /b 1
  )
)
%CURL% -s --connect-timeout 3 http://localhost:8081/api/health/
echo.
echo   Backend is UP.
echo.

REM ---------- 3/3 Frontend ----------
echo [3/3] Checking frontend http://localhost:5173 ...
%CURL% -s -f --connect-timeout 3 -o nul http://localhost:5173/ >nul 2>nul
if errorlevel 1 (
  echo   Starting frontend in a new window...
  start "AVMS Frontend" /d "%ROOT%\frontend" cmd /k "npm run dev"
  echo   Waiting for frontend ^(up to 1 min^)...
  set "FRONTEND_OK="
  for /l %%i in (1,1,12) do (
    %CURL% -s -f --connect-timeout 3 -o nul http://localhost:5173/ >nul 2>nul
    if not errorlevel 1 (
      set "FRONTEND_OK=1"
      goto frontend_ready
    )
    %SLEEP% /t 5 /nobreak >nul
  )
  :frontend_ready
  if not defined FRONTEND_OK (
    echo [ERROR] Frontend did not respond. See the 'AVMS Frontend' window for errors.
    pause
    exit /b 1
  )
)
echo   Frontend is UP.
echo.

echo ============================================
echo  ALL RUNNING
echo    App:     http://localhost:5173/
echo    API:     http://localhost:8081/api/health/
echo    Swagger: http://localhost:8081/api/docs.html
echo  Run stop-avms.bat to stop backend + frontend.
echo ============================================
if "%NOBROWSER%"=="" start "" http://localhost:5173/
pause
