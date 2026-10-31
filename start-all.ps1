<#
.SYNOPSIS
  Startet PostgreSQL + Mailpit (Docker), das Spring-Boot-Backend und das React-Frontend im Hintergrund.
.EXAMPLE
  .\start-all.ps1                # bauen, dann alles starten
  .\start-all.ps1 -SkipBuild     # mit der vorhandenen app.jar starten
  .\start-all.ps1 -NoFrontend    # nur Backend
  .\stop-all.ps1                 # alles wieder stoppen
#>
param(
    [switch]$SkipBuild,
    [switch]$NoFrontend
)
$ErrorActionPreference = 'Stop'
$root = $PSScriptRoot
$backendDir = Join-Path $root 'com.NetflixClone'
$frontendDir = Join-Path $root 'frontend'
$logs = Join-Path $root 'logs'
$pidFile = Join-Path $logs 'pids.txt'
$backendPort = 8086
$frontendPort = 5175

function Test-Docker {
    # PowerShell 5.1: ErrorActionPreference=Stop iken docker'ın stderr çıktısı script'i durdururdu
    $old = $ErrorActionPreference
    $ErrorActionPreference = 'Continue'
    try { docker info *> $null; return ($LASTEXITCODE -eq 0) } finally { $ErrorActionPreference = $old }
}

if (Test-Path $pidFile) {
    Write-Host 'Uygulama zaten çalışıyor görünüyor. Önce .\stop-all.ps1 çalıştırın.' -ForegroundColor Yellow
    exit 1
}

$ports = @($backendPort)
if (-not $NoFrontend) { $ports += $frontendPort }
$busy = foreach ($port in $ports) {
    $c = Get-NetTCPConnection -LocalPort $port -State Listen -ErrorAction SilentlyContinue | Select-Object -First 1
    if ($c) { "    :$port  ->  $((Get-Process -Id $c.OwningProcess -ErrorAction SilentlyContinue).ProcessName) (pid $($c.OwningProcess))" }
}
if ($busy) {
    Write-Host 'Bu portlar başka bir program tarafından kullanılıyor:' -ForegroundColor Red
    $busy | ForEach-Object { Write-Host $_ -ForegroundColor Red }
    exit 1
}

New-Item -ItemType Directory -Force $logs | Out-Null
Push-Location $root
try {
    Write-Host '1/4 Docker: PostgreSQL (5433) ve Mailpit (8027) başlatılıyor...' -ForegroundColor Cyan
    if (-not (Test-Docker)) {
        $dockerDesktop = 'C:\Program Files\Docker\Docker\Docker Desktop.exe'
        if (-not (Test-Path $dockerDesktop)) { throw 'Docker çalışmıyor ve Docker Desktop bulunamadı.' }
        Write-Host '    Docker Desktop başlatılıyor...' -ForegroundColor DarkGray
        Start-Process $dockerDesktop
        $deadline = (Get-Date).AddMinutes(3)
        do { Start-Sleep -Seconds 3; $up = Test-Docker } while (-not $up -and (Get-Date) -lt $deadline)
        if (-not $up) { throw 'Docker Desktop 3 dakika içinde hazır olmadı.' }
    }
    docker compose up -d --wait postgres mailpit
    if ($LASTEXITCODE -ne 0) { throw 'docker compose başarısız.' }

    $jar = Join-Path $backendDir 'target\app.jar'
    if (-not $SkipBuild) {
        Write-Host '2/4 Maven ile derleniyor...' -ForegroundColor Cyan
        Push-Location $backendDir
        mvn -q -B package -DskipTests
        $code = $LASTEXITCODE
        Pop-Location
        if ($code -ne 0) { throw 'Maven derlemesi başarısız.' }
    } else {
        Write-Host '2/4 Derleme atlandı (-SkipBuild).' -ForegroundColor Cyan
    }
    if (-not (Test-Path $jar)) { throw "$jar yok. -SkipBuild olmadan çalıştırın." }

    Write-Host '3/4 Backend ve frontend başlatılıyor...' -ForegroundColor Cyan
    # Çalışma klasörü com.NetflixClone: yüklenen dosyalar com.NetflixClone\upload\ altına gider
    $backend = Start-Process java -ArgumentList '-jar', 'target\app.jar' -WorkingDirectory $backendDir -WindowStyle Hidden -PassThru `
        -RedirectStandardOutput (Join-Path $logs 'backend.log') -RedirectStandardError (Join-Path $logs 'backend.err.log')
    Add-Content $pidFile "backend $($backend.Id)"

    if (-not $NoFrontend) {
        if (-not (Test-Path (Join-Path $frontendDir 'node_modules'))) {
            Write-Host '    npm install...' -ForegroundColor DarkGray
            Push-Location $frontendDir; npm install --no-audit --no-fund; Pop-Location
        }
        $p = Start-Process npm.cmd -ArgumentList 'run', 'dev' -WorkingDirectory $frontendDir -WindowStyle Hidden -PassThru `
            -RedirectStandardOutput (Join-Path $logs 'frontend.log') -RedirectStandardError (Join-Path $logs 'frontend.err.log')
        Add-Content $pidFile "frontend $($p.Id)"
    }

    Write-Host '4/4 Backend hazır olması bekleniyor...' -ForegroundColor Cyan
    $deadline = (Get-Date).AddMinutes(2)
    $ready = $false
    while (-not $ready -and -not $backend.HasExited -and (Get-Date) -lt $deadline) {
        try { $ready = (Invoke-RestMethod "http://localhost:$backendPort/actuator/health" -TimeoutSec 2).status -eq 'UP' } catch { }
        if (-not $ready) { Start-Sleep -Seconds 2 }
    }
    if ($ready) { Write-Host "    backend   :$backendPort  hazır" -ForegroundColor Green }
    else { Write-Host "    backend   :$backendPort  BAŞLAMADI -> logs\backend.log" -ForegroundColor Red }

    Write-Host ''
    if (-not $NoFrontend) { Write-Host "Arayüz:      http://localhost:$frontendPort" }
    Write-Host '             admin@netflix.local / admin123   |   user@netflix.local / user123'
    Write-Host "Backend:     http://localhost:$backendPort"
    Write-Host 'PostgreSQL:  localhost:5433/netflix_clone  (netflix / netflix)'
    Write-Host 'E-postalar:  http://localhost:8027'
    Write-Host 'Loglar:      logs\   |   Durdurmak için: .\stop-all.ps1'
} finally {
    Pop-Location
}
