# ============================================================
#  Integrity Family — Instalar tarea programada de backup nocturno
#
#  Ejecutar UNA SOLA VEZ como Administrador:
#    powershell -ExecutionPolicy Bypass -File scripts\install-backup-task.ps1
#
#  Para desinstalar:
#    powershell -ExecutionPolicy Bypass -File scripts\install-backup-task.ps1 -Uninstall
# ============================================================
param(
    [switch]$Uninstall,
    [string]$Hour = "02",
    [string]$Minute = "00"
)

$TaskName    = "IntegrityFamily-MySQL-Backup"
$ScriptPath  = Join-Path $PSScriptRoot "backup-nightly.ps1"
$ProjectRoot = Split-Path -Parent $PSScriptRoot

# ── Verificar permisos de administrador ─────────────────────
$isAdmin = ([Security.Principal.WindowsPrincipal][Security.Principal.WindowsIdentity]::GetCurrent()
           ).IsInRole([Security.Principal.WindowsBuiltInRole]::Administrator)

if (-not $isAdmin) {
    Write-Host ""
    Write-Host "ERROR: Este script requiere permisos de Administrador." -ForegroundColor Red
    Write-Host ""
    Write-Host "Ejecuta desde PowerShell (Administrador):" -ForegroundColor Yellow
    Write-Host "  powershell -ExecutionPolicy Bypass -File `"$PSCommandPath`"" -ForegroundColor Cyan
    Write-Host ""
    exit 1
}

# ── Desinstalar ──────────────────────────────────────────────
if ($Uninstall) {
    if (Get-ScheduledTask -TaskName $TaskName -ErrorAction SilentlyContinue) {
        Unregister-ScheduledTask -TaskName $TaskName -Confirm:$false
        Write-Host "Tarea '$TaskName' eliminada." -ForegroundColor Green
    } else {
        Write-Host "Tarea '$TaskName' no encontrada." -ForegroundColor Yellow
    }
    exit 0
}

# ── Verificar que el script existe ──────────────────────────
if (-not (Test-Path $ScriptPath)) {
    Write-Host "ERROR: No se encontró $ScriptPath" -ForegroundColor Red
    exit 1
}

Write-Host ""
Write-Host "Integrity Family — Instalando tarea de backup nocturno" -ForegroundColor Cyan
Write-Host "  Script  : $ScriptPath"
Write-Host "  Horario : Diariamente a las $($Hour):$($Minute)"
Write-Host ""

# ── Crear directorio de backups ──────────────────────────────
$BackupDir = Join-Path $ProjectRoot "backups"
if (-not (Test-Path $BackupDir)) {
    New-Item -ItemType Directory -Path $BackupDir -Force | Out-Null
    Write-Host "  Directorio creado: $BackupDir"
}

# ── Configurar la tarea ──────────────────────────────────────
$action = New-ScheduledTaskAction `
    -Execute "powershell.exe" `
    -Argument "-NonInteractive -ExecutionPolicy Bypass -File `"$ScriptPath`""

$trigger = New-ScheduledTaskTrigger `
    -Daily `
    -At "$($Hour):$($Minute)"

$settings = New-ScheduledTaskSettingsSet `
    -ExecutionTimeLimit (New-TimeSpan -Minutes 30) `
    -RestartCount 2 `
    -RestartInterval (New-TimeSpan -Minutes 5) `
    -StartWhenAvailable `
    -RunOnlyIfNetworkAvailable:$false

$principal = New-ScheduledTaskPrincipal `
    -UserId "SYSTEM" `
    -LogonType ServiceAccount `
    -RunLevel Highest

# Eliminar si ya existía
if (Get-ScheduledTask -TaskName $TaskName -ErrorAction SilentlyContinue) {
    Unregister-ScheduledTask -TaskName $TaskName -Confirm:$false
    Write-Host "  Tarea anterior eliminada."
}

Register-ScheduledTask `
    -TaskName $TaskName `
    -Action $action `
    -Trigger $trigger `
    -Settings $settings `
    -Principal $principal `
    -Description "Backup nocturno de MySQL para Integrity Family. Genera dump comprimido en $BackupDir" | Out-Null

Write-Host ""
Write-Host "Tarea programada instalada correctamente." -ForegroundColor Green
Write-Host ""

# ── Verificar ────────────────────────────────────────────────
$task = Get-ScheduledTask -TaskName $TaskName
Write-Host "Estado de la tarea:"
Write-Host "  Nombre  : $($task.TaskName)"
Write-Host "  Estado  : $($task.State)"
Write-Host "  Trigger : $((Get-ScheduledTaskInfo -TaskName $TaskName).NextRunTime)"
Write-Host ""
Write-Host "Comandos útiles:" -ForegroundColor Yellow
Write-Host "  # Ver próxima ejecución"
Write-Host "  Get-ScheduledTaskInfo -TaskName '$TaskName' | Select NextRunTime, LastRunTime, LastTaskResult"
Write-Host ""
Write-Host "  # Ejecutar ahora (para probar)"
Write-Host "  Start-ScheduledTask -TaskName '$TaskName'"
Write-Host ""
Write-Host "  # Ver log"
Write-Host "  Get-Content '$BackupDir\backup-nightly.log' -Tail 30"
Write-Host ""
Write-Host "  # Desinstalar"
Write-Host "  powershell -ExecutionPolicy Bypass -File `"$PSCommandPath`" -Uninstall"
