# ============================================================
#  Integrity Family — Backup nocturno MySQL
#  Ejecutado por Windows Task Scheduler cada noche a las 02:00
#  NO ejecutar manualmente; usar backup-mysql.sh para backups manuales
# ============================================================

$ProjectRoot = Split-Path -Parent $PSScriptRoot
$BackupDir   = Join-Path $ProjectRoot "backups"
$LogFile     = Join-Path $BackupDir "backup-nightly.log"
$BashScript  = Join-Path $PSScriptRoot "backup-mysql.sh"

# Crear directorio de backups si no existe
if (-not (Test-Path $BackupDir)) {
    New-Item -ItemType Directory -Path $BackupDir -Force | Out-Null
}

function Write-Log {
    param([string]$Message, [string]$Level = "INFO")
    $Timestamp = Get-Date -Format "yyyy-MM-dd HH:mm:ss"
    $Line = "[$Timestamp] [$Level] $Message"
    Add-Content -Path $LogFile -Value $Line -Encoding UTF8
    Write-Host $Line
}

Write-Log "=== Backup nocturno iniciado ==="

# ── Verificar que Docker está corriendo ─────────────────────
try {
    $dockerStatus = docker ps --format "{{.Names}}" 2>&1
    if ($LASTEXITCODE -ne 0) {
        Write-Log "Docker no está activo. Abortando backup." "ERROR"
        exit 1
    }
} catch {
    Write-Log "Docker no encontrado: $_" "ERROR"
    exit 1
}

# ── Verificar que el contenedor MySQL está corriendo ─────────
$dbRunning = docker ps --format "{{.Names}}" | Where-Object { $_ -eq "integrity-db" }
if (-not $dbRunning) {
    Write-Log "Contenedor 'integrity-db' no está activo. Backup omitido." "WARN"
    exit 0
}

# ── Buscar bash (Git Bash o WSL) ────────────────────────────
$BashExe = $null
$candidates = @(
    "C:\Program Files\Git\bin\bash.exe",
    "C:\Program Files (x86)\Git\bin\bash.exe",
    "$env:LOCALAPPDATA\Programs\Git\bin\bash.exe"
)
foreach ($c in $candidates) {
    if (Test-Path $c) { $BashExe = $c; break }
}
if (-not $BashExe) {
    # Intentar bash del PATH (WSL)
    $bashCmd = Get-Command bash -ErrorAction SilentlyContinue
    if ($bashCmd) { $BashExe = $bashCmd.Source }
}
if (-not $BashExe) {
    Write-Log "No se encontró bash (Git Bash ni WSL). Instala Git for Windows." "ERROR"
    exit 1
}

Write-Log "Usando bash: $BashExe"

# ── Convertir ruta Windows → Unix para Git Bash ─────────────
$BashScriptUnix = $BashScript -replace '\\', '/' -replace '^([A-Z]):', { "/$(([string]$args[0]).ToLower()[0])" }

# ── Ejecutar backup ──────────────────────────────────────────
Write-Log "Ejecutando: $BashScriptUnix --compress --keep 14"

$proc = Start-Process -FilePath $BashExe `
    -ArgumentList "--login", "-c", "`"$BashScriptUnix --compress --keep 14`"" `
    -Wait -PassThru -NoNewWindow `
    -RedirectStandardOutput (Join-Path $BackupDir "backup-stdout.tmp") `
    -RedirectStandardError  (Join-Path $BackupDir "backup-stderr.tmp")

# Volcar stdout/stderr al log
if (Test-Path (Join-Path $BackupDir "backup-stdout.tmp")) {
    Get-Content (Join-Path $BackupDir "backup-stdout.tmp") | ForEach-Object { Write-Log $_ }
    Remove-Item (Join-Path $BackupDir "backup-stdout.tmp") -Force
}
if (Test-Path (Join-Path $BackupDir "backup-stderr.tmp")) {
    $errContent = Get-Content (Join-Path $BackupDir "backup-stderr.tmp") -Raw
    if ($errContent.Trim()) { Write-Log $errContent.Trim() "WARN" }
    Remove-Item (Join-Path $BackupDir "backup-stderr.tmp") -Force
}

if ($proc.ExitCode -eq 0) {
    Write-Log "Backup completado exitosamente." "INFO"

    # Listar backups actuales
    $backups = Get-ChildItem -Path $BackupDir -Filter "if_backup_*.sql.gz" |
               Sort-Object LastWriteTime -Descending
    Write-Log "Backups disponibles: $($backups.Count)"
    $backups | ForEach-Object {
        $size = "{0:N1} MB" -f ($_.Length / 1MB)
        Write-Log "  $($_.Name)  ($size)"
    }
} else {
    Write-Log "ERROR en backup. Exit code: $($proc.ExitCode)" "ERROR"
    exit $proc.ExitCode
}

Write-Log "=== Backup nocturno finalizado ==="

# ── Rotar log si supera 500 KB ──────────────────────────────
$logSize = (Get-Item $LogFile).Length
if ($logSize -gt 500KB) {
    $archive = $LogFile -replace '\.log$', "-$(Get-Date -Format 'yyyyMMdd').log"
    Move-Item $LogFile $archive -Force
    Write-Host "[$(Get-Date -Format 'yyyy-MM-dd HH:mm:ss')] Log rotado a: $archive"
}
