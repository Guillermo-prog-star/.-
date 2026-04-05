param (
    [Parameter(Mandatory=$true)]
    [int]$Port
)
Write-Host "Buscando proceso en el puerto: $Port..." -ForegroundColor Cyan
$connection = Get-NetTCPConnection -LocalPort $Port -ErrorAction SilentlyContinue
if ($null -ne $connection) {
    foreach ($conn in $connection) {
        $pId = $conn.OwningProcess
        try {
            $proc = Get-Process -Id $pId -ErrorAction Stop
            Write-Host "Deteniendo: $($proc.ProcessName) (PID: $pId)..." -ForegroundColor Yellow
            Stop-Process -Id $pId -Force
            Write-Host "Puerto $Port liberado." -ForegroundColor Green
        } catch {
            Write-Warning "No se pudo detener el PID $pId."
        }
    }
} else {
    Write-Host "El puerto $Port ya está libre." -ForegroundColor Gray
}
