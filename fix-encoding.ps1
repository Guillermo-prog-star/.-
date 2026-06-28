# fix-encoding.ps1
# Corrige triple-encoding Windows-1252 en archivos Java.
# Cadena original paso por: UTF-8 -> W1252-read -> UTF8-save -> W1252-read -> UTF8-save
# Para revertir: aplicar dos veces (UTF8-decode -> W1252-encode -> UTF8-decode -> W1252-encode -> UTF8-decode)

$files = @(
    "backend\src\main\java\com\integrityfamily\common\config\DataSeeder.java",
    "backend\src\main\java\com\integrityfamily\risk\service\RiskService.java",
    "backend\src\main\java\com\integrityfamily\ai\service\AiInferenceService.java",
    "backend\src\main\java\com\integrityfamily\common\config\RestTemplateConfig.java",
    "backend\src\main\java\com\integrityfamily\chat\controller\ChatController.java",
    "backend\src\main\java\com\integrityfamily\ai\service\AiService.java",
    "backend\src\main\java\com\integrityfamily\analytics\dto\DashboardSummaryResponse.java",
    "backend\src\main\java\com\integrityfamily\analytics\controller\AnalyticsController.java",
    "backend\src\main\java\com\integrityfamily\family\service\FamilyService.java",
    "backend\src\main\java\com\integrityfamily\reports\service\ReportService.java",
    "backend\src\main\java\com\integrityfamily\analytics\service\RiskSnapshotService.java",
    "backend\src\main\java\com\integrityfamily\simulation\service\BetaLauncherService.java",
    "backend\src\main\java\com\integrityfamily\admin\service\SecurityWatchdogService.java",
    "backend\src\main\java\com\integrityfamily\auth\dto\RegisterFamilyRequest.java",
    "backend\src\main\java\com\integrityfamily\risk\controller\RiskController.java",
    "backend\src\main\java\com\integrityfamily\common\security\SecurityValidator.java",
    "backend\src\main\java\com\integrityfamily\analytics\service\AnalyticsService.java",
    "backend\src\main\java\com\integrityfamily\checklist\service\ChecklistService.java",
    "backend\src\main\java\com\integrityfamily\ai\service\ClaudeAiService.java",
    "backend\src\main\java\com\integrityfamily\auth\service\MasterCredentialService.java",
    "backend\src\main\java\com\integrityfamily\analytics\service\AdminAnalyticsService.java",
    "backend\src\main\java\com\integrityfamily\report\service\VoiceAnalyticsService.java",
    "backend\src\main\java\com\integrityfamily\report\service\AutomatedReportingService.java",
    "backend\src\main\java\com\integrityfamily\simulation\service\SentinelSimulationService.java",
    "backend\src\main\java\com\integrityfamily\risk\service\CrisisService.java",
    "backend\src\main\java\com\integrityfamily\assessment\service\EvaluationScoringService.java",
    "backend\src\main\java\com\integrityfamily\simulation\service\CrisisSimulationService.java",
    "backend\src\main\java\com\integrityfamily\simulation\service\TrendSimulationService.java",
    "backend\src\main\java\com\integrityfamily\admin\service\WatchdogIntegrityService.java",
    "backend\src\main\java\com\integrityfamily\simulation\service\AlphaLaunchService.java",
    "backend\src\main\java\com\integrityfamily\security\SecurityExceptionHandler.java",
    "backend\src\main\java\com\integrityfamily\security\AuthExceptionHandler.java",
    "backend\src\main\java\com\integrityfamily\reports\service\ExcelExportService.java",
    "backend\src\main\java\com\integrityfamily\report\service\PdfReportService.java",
    "backend\src\main\java\com\integrityfamily\report\controller\ReportController.java",
    "backend\src\main\java\com\integrityfamily\common\service\BackupService.java",
    "backend\src\main\java\com\integrityfamily\config\exception\GlobalExceptionHandler.java",
    "backend\src\main\java\com\integrityfamily\common\dto\ApiResponse.java",
    "backend\src\main\java\com\integrityfamily\auth\service\AccountLockService.java",
    "backend\src\main\java\com\integrityfamily\auth\exception\InvalidResetTokenException.java",
    "backend\src\main\java\com\integrityfamily\auth\exception\InvalidCredentialsException.java",
    "backend\src\main\java\com\integrityfamily\auth\exception\AccountLockedException.java",
    "backend\src\main\java\com\integrityfamily\auth\dto\ResetPasswordRequest.java",
    "backend\src\main\java\com\integrityfamily\auth\dto\RegisterRequest.java",
    "backend\src\main\java\com\integrityfamily\auth\dto\LoginRequest.java",
    "backend\src\main\java\com\integrityfamily\auth\dto\AuthResponse.java",
    "backend\src\main\java\com\integrityfamily\analytics\controller\AuditController.java",
    "backend\src\main\java\com\integrityfamily\ai\service\SonicIntegrationTest.java"
)

$w1252    = [System.Text.Encoding]::GetEncoding(1252)
$utf8     = [System.Text.Encoding]::UTF8
$utf8_nobom = New-Object System.Text.UTF8Encoding($false)

$fixed_count = 0
$errors = 0

foreach ($rel in $files) {
    $path = "C:\Proyectos\if-full\$rel"
    if (-not (Test-Path $path)) {
        Write-Host "  SKIP (no existe): $rel" -ForegroundColor Yellow
        continue
    }

    try {
        $original_bytes = [System.IO.File]::ReadAllBytes($path)
        $s2 = $utf8.GetString($original_bytes)

        # Paso 1: revertir segunda pasada W1252
        $b1     = $w1252.GetBytes($s2)
        $s1     = $utf8.GetString($b1)

        # Paso 2: revertir primera pasada W1252
        $b0     = $w1252.GetBytes($s1)
        $fixed  = $utf8.GetString($b0)

        if ($fixed -ne $s2) {
            $out_bytes = $utf8_nobom.GetBytes($fixed)
            [System.IO.File]::WriteAllBytes($path, $out_bytes)
            Write-Host "  OK: $rel" -ForegroundColor Green
            $fixed_count++
        } else {
            Write-Host "  SIN CAMBIOS: $rel" -ForegroundColor Gray
        }
    } catch {
        Write-Host "  ERROR en ${rel}: $_" -ForegroundColor Red
        $errors++
    }
}

Write-Host ""
Write-Host "Resultado: $fixed_count archivos corregidos, $errors errores." -ForegroundColor Cyan
