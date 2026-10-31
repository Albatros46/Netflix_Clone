<#
.SYNOPSIS
  start-all.ps1 ile başlatılan her şeyi durdurur. -KeepInfra: PostgreSQL ve Mailpit çalışmaya devam eder.
#>
param([switch]$KeepInfra)
$root = $PSScriptRoot
$pidFile = Join-Path $root 'logs\pids.txt'

if (Test-Path $pidFile) {
    foreach ($line in Get-Content $pidFile) {
        $name, $id = $line -split ' '
        # /T alt süreçleri de durdurur (npm -> node, ...).
        taskkill /PID $id /T /F 2>$null | Out-Null
        Write-Host "durduruldu: $name"
    }
    Remove-Item $pidFile
} else {
    Write-Host 'Çalışan süreç kaydı yok (logs\pids.txt bulunamadı).'
}

if (-not $KeepInfra) {
    Push-Location $root
    docker compose stop postgres mailpit
    Pop-Location
}
