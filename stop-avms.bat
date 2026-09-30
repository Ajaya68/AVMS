@echo off
setlocal EnableExtensions
title AVMS Stop
cd /d "%~dp0"

echo Stopping AVMS backend + frontend...

if not defined SystemRoot set "SystemRoot=C:\Windows"
set "POWERSHELL=powershell"
if exist "%SystemRoot%\System32\WindowsPowerShell\v1.0\powershell.exe" set "POWERSHELL=%SystemRoot%\System32\WindowsPowerShell\v1.0\powershell.exe"
set "TASKKILL=taskkill"
if exist "%SystemRoot%\System32\taskkill.exe" set "TASKKILL=%SystemRoot%\System32\taskkill.exe"

"%POWERSHELL%" -NoProfile -ExecutionPolicy Bypass -Command "Get-CimInstance Win32_Process | Where-Object { $_.CommandLine -like '*avms-backend*' } | ForEach-Object { Stop-Process -Id $_.ProcessId -Force; Write-Output ('Stopped backend PID ' + $_.ProcessId) }"
"%POWERSHELL%" -NoProfile -ExecutionPolicy Bypass -Command "Get-CimInstance Win32_Process | Where-Object { $_.CommandLine -like '*vite*' } | ForEach-Object { Stop-Process -Id $_.ProcessId -Force; Write-Output ('Stopped frontend PID ' + $_.ProcessId) }"

"%TASKKILL%" /FI "WINDOWTITLE eq AVMS Backend*" >nul 2>&1
"%TASKKILL%" /FI "WINDOWTITLE eq AVMS Frontend*" >nul 2>&1

echo Done. Oracle database left running on purpose.
pause
