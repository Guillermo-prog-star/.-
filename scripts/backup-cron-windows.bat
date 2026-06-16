@echo off
REM ============================================================
REM  Integrity Family — Backup automático (Windows)
REM
REM  Para programar ejecución diaria en Windows:
REM    1. Abre "Programador de tareas" (Task Scheduler)
REM    2. Crea una tarea básica que ejecute este .bat
REM    3. Programa: diariamente a las 02:00 AM
REM
REM  O desde PowerShell (como Administrador):
REM    $action = New-ScheduledTaskAction -Execute 'C:\Proyectos\if-full\scripts\backup-cron-windows.bat'
REM    $trigger = New-ScheduledTaskTrigger -Daily -At 2am
REM    Register-ScheduledTask -TaskName 'IF-MySQL-Backup' -Action $action -Trigger $trigger -RunLevel Highest
REM ============================================================

SET SCRIPT_DIR=%~dp0
SET LOG_FILE=%SCRIPT_DIR%..\backups\backup.log

echo [%DATE% %TIME%] Iniciando backup >> "%LOG_FILE%"

REM Ejecutar backup via Git Bash / WSL
bash "%SCRIPT_DIR%backup-mysql.sh" --compress --keep 14 >> "%LOG_FILE%" 2>&1

IF %ERRORLEVEL% EQU 0 (
  echo [%DATE% %TIME%] Backup exitoso >> "%LOG_FILE%"
) ELSE (
  echo [%DATE% %TIME%] ERROR en backup - revisar log >> "%LOG_FILE%"
)
