# ===================================================================
#  QuickFlow - Full Stack Power-On Script (Windows)
# ===================================================================

[Console]::OutputEncoding = [System.Text.Encoding]::UTF8
$ErrorActionPreference = "Continue"
$ROOT = $PSScriptRoot

# -- Colors and helpers --------------------------------------------
function Write-Banner {
    param($msg)
    Write-Host ""
    Write-Host ("=" * 56) -ForegroundColor Cyan
    Write-Host "  $msg" -ForegroundColor Cyan
    Write-Host ("=" * 56) -ForegroundColor Cyan
}

function Write-Step { param($msg) Write-Host "  >> $msg" -ForegroundColor Yellow }
function Write-Ok { param($msg) Write-Host "  [OK] $msg" -ForegroundColor Green }
function Write-Err { param($msg) Write-Host "  [ERROR] $msg" -ForegroundColor Red }
function Write-Info { param($msg) Write-Host "  [INFO] $msg" -ForegroundColor Gray }

# -- Cleanup on exit -----------------------------------------------
$script:bgJobs = @()

function Stop-AllServices {
    Write-Host ""
    Write-Banner "Shutting down QuickFlow..."
    foreach ($j in $script:bgJobs) {
        if ($j -and !$j.HasExited) {
            Write-Step "Stopping PID $($j.Id)..."
            Stop-Process -Id $j.Id -Force -ErrorAction SilentlyContinue
        }
    }
    Write-Ok "All services stopped."
}

trap { Stop-AllServices; break }

# -- Health check helper -------------------------------------------
function Wait-ForHealth {
    param(
        [string]$Url,
        [string]$ServiceName,
        [int]$TimeoutSeconds = 30,
        [int]$IntervalMs = 1000
    )
    $deadline = (Get-Date).AddSeconds($TimeoutSeconds)
    while ((Get-Date) -lt $deadline) {
        try {
            $response = Invoke-RestMethod -Uri $Url -Method Get -TimeoutSec 3 -ErrorAction Stop
            return $true
        }
        catch {
            Start-Sleep -Milliseconds $IntervalMs
        }
    }
    return $false
}

# ==================================================================
Write-Banner "QuickFlow - Starting All Services"
Write-Host ""

# -- 1. Transcription Service -------------------------------------
Write-Banner "1/4  Transcription Service"

$tsDir = Join-Path $ROOT "transcription-service"
$venvPython = Join-Path $tsDir "venv\Scripts\python.exe"
$mainPy = Join-Path $tsDir "main.py"

if (-not (Test-Path $venvPython)) {
    Write-Err "[TRANSCRIPTION SERVICE] Virtual environment not found at: $venvPython"
    Write-Err "Run:  cd transcription-service && python -m venv venv && venv\Scripts\pip install -r requirements.txt"
    exit 1
}
if (-not (Test-Path $mainPy)) {
    Write-Err "[TRANSCRIPTION SERVICE] main.py not found at: $mainPy"
    exit 1
}

Write-Step "Starting transcription service..."
$tsProcess = Start-Process -FilePath $venvPython -ArgumentList $mainPy `
    -WorkingDirectory $tsDir -PassThru -NoNewWindow -RedirectStandardError (Join-Path $tsDir "ts_stderr.log")
$script:bgJobs += $tsProcess

Write-Info "Waiting for transcription service to become healthy (port 8001)..."
Start-Sleep -Seconds 5

$tsHealthy = Wait-ForHealth -Url "http://localhost:8001/health" -ServiceName "Transcription Service" -TimeoutSeconds 30
if ($tsHealthy) {
    Write-Ok "Transcription Service is running  ->  http://localhost:8001"
}
else {
    Write-Err "[TRANSCRIPTION SERVICE] Failed to start! Check transcription-service/ts_stderr.log"
    Write-Err "Health endpoint http://localhost:8001/health did not respond within 30s."
    Stop-AllServices
    exit 1
}

# -- 2. Backend (Spring Boot) -------------------------------------
Write-Banner "2/4  Backend Server"

$backendDir = Join-Path $ROOT "backend"
if (-not (Test-Path (Join-Path $backendDir "pom.xml"))) {
    Write-Err "[BACKEND] pom.xml not found in: $backendDir"
    Stop-AllServices
    exit 1
}

Write-Step "Starting Spring Boot backend..."
$mvnCmd = "mvn"
try {
    $null = Get-Command $mvnCmd -ErrorAction Stop
}
catch {
    $mvnw = Join-Path $backendDir "mvnw.cmd"
    if (Test-Path $mvnw) { $mvnCmd = $mvnw }
    else {
        Write-Err "[BACKEND] Maven (mvn) not found in PATH and no mvnw.cmd wrapper found."
        Stop-AllServices
        exit 1
    }
}

$backendProcess = Start-Process -FilePath $mvnCmd -ArgumentList "spring-boot:run" `
    -WorkingDirectory $backendDir -PassThru -NoNewWindow -RedirectStandardError (Join-Path $backendDir "backend_stderr.log")
$script:bgJobs += $backendProcess

Write-Info "Waiting for backend to become healthy (port 8080)..."
Start-Sleep -Seconds 4

$beHealthy = Wait-ForHealth -Url "http://localhost:8080/api/health" -ServiceName "Backend" -TimeoutSeconds 60
if ($beHealthy) {
    Write-Ok "Backend Server is running  ->  http://localhost:8080"
}
else {
    Write-Err "[BACKEND] Failed to start! Check backend/backend_stderr.log"
    Write-Err "Health endpoint http://localhost:8080/api/health did not respond within 60s."
    Stop-AllServices
    exit 1
}

# -- 3. Ngrok Tunnel ----------------------------------------------
Write-Banner "3/4  Ngrok Tunnel"

$ngrokDomain = "undefinitively-dramatic-terrell.ngrok-free.dev"

try {
    $null = Get-Command ngrok -ErrorAction Stop
}
catch {
    Write-Err "[NGROK] ngrok is not installed or not in PATH."
    Write-Err "Install from https://ngrok.com/download and add to PATH."
    Stop-AllServices
    exit 1
}

Write-Step "Exposing backend through ngrok..."
$ngrokProcess = Start-Process -FilePath "ngrok" `
    -ArgumentList "http", "--url=$ngrokDomain", "8080" `
    -PassThru -NoNewWindow
$script:bgJobs += $ngrokProcess

Start-Sleep -Seconds 2
if ($ngrokProcess.HasExited) {
    Write-Err "[NGROK] ngrok exited unexpectedly. Is another ngrok instance already running?"
    Stop-AllServices
    exit 1
}

Write-Ok "Backend exposed to the web  ->  https://$ngrokDomain"

# -- 4. Frontend (Vite Dev Server) --------------------------------
Write-Banner "4/4  Frontend Dev Server"

$frontendDir = Join-Path $ROOT "frontend"
if (-not (Test-Path (Join-Path $frontendDir "package.json"))) {
    Write-Err "[FRONTEND] package.json not found in: $frontendDir"
    Stop-AllServices
    exit 1
}

if (-not (Test-Path (Join-Path $frontendDir "node_modules"))) {
    Write-Step "Installing frontend dependencies (npm install)..."
    $npmInstall = Start-Process -FilePath "npm" -ArgumentList "install" `
        -WorkingDirectory $frontendDir -PassThru -NoNewWindow -Wait
    if ($npmInstall.ExitCode -ne 0) {
        Write-Err "[FRONTEND] npm install failed."
        Stop-AllServices
        exit 1
    }
}

Write-Step "Starting Vite dev server..."
$frontendProcess = Start-Process -FilePath "npm" -ArgumentList "run", "dev" `
    -WorkingDirectory $frontendDir -PassThru -NoNewWindow
$script:bgJobs += $frontendProcess

Start-Sleep -Seconds 3
if ($frontendProcess.HasExited) {
    Write-Err "[FRONTEND] Vite dev server exited unexpectedly."
    Stop-AllServices
    exit 1
}

Write-Ok "Frontend running dev server  ->  http://localhost:5173/"

# -- Summary -------------------------------------------------------
Write-Host ""
Write-Host ("=" * 56) -ForegroundColor Green
Write-Host "       QuickFlow is fully operational!" -ForegroundColor Green
Write-Host ("-" * 56) -ForegroundColor Green
Write-Host "  Transcription :  http://localhost:8001" -ForegroundColor White
Write-Host "  Backend       :  http://localhost:8080" -ForegroundColor White
Write-Host "  Ngrok         :  https://$ngrokDomain" -ForegroundColor White
Write-Host "  Frontend      :  http://localhost:5173" -ForegroundColor White
Write-Host ("-" * 56) -ForegroundColor Green
Write-Host "  Press Ctrl+C to shut down all services" -ForegroundColor Yellow
Write-Host ("=" * 56) -ForegroundColor Green
Write-Host ""

# -- Keep alive until Ctrl+C --------------------------------------
try {
    while ($true) {
        foreach ($j in $script:bgJobs) {
            if ($j.HasExited) {
                $name = switch ($j.Id) {
                    $tsProcess.Id       { "TRANSCRIPTION SERVICE" }
                    $backendProcess.Id  { "BACKEND" }
                    $ngrokProcess.Id    { "NGROK" }
                    $frontendProcess.Id { "FRONTEND" }
                    default { "UNKNOWN (PID $($j.Id))" }
                }
                Write-Err "$name exited unexpectedly (exit code: $($j.ExitCode))"
            }
        }
        Start-Sleep -Seconds 5
    }
}
finally {
    Stop-AllServices
}
