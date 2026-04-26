#!/usr/bin/env bash
# ═══════════════════════════════════════════════════════════════
#  QuickFlow — Full Stack Power-On Script (Linux / macOS)
# ═══════════════════════════════════════════════════════════════
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

# ── Colors & helpers ──────────────────────────────────────────
C_CYAN='\033[0;36m'
C_GREEN='\033[0;32m'
C_YELLOW='\033[1;33m'
C_RED='\033[0;31m'
C_GRAY='\033[0;90m'
C_WHITE='\033[1;37m'
C_RESET='\033[0m'

banner()  { echo -e "\n${C_CYAN}╔══════════════════════════════════════════════════╗${C_RESET}"; echo -e "${C_CYAN}║  $1${C_RESET}"; echo -e "${C_CYAN}╚══════════════════════════════════════════════════╝${C_RESET}"; }
step()    { echo -e "  ${C_YELLOW}► $1${C_RESET}"; }
ok()      { echo -e "  ${C_GREEN}✔ $1${C_RESET}"; }
err()     { echo -e "  ${C_RED}✖ $1${C_RESET}"; }
info()    { echo -e "  ${C_GRAY}ℹ $1${C_RESET}"; }

# ── PID tracking for cleanup ─────────────────────────────────
PIDS=()

cleanup() {
    echo ""
    banner "Shutting down QuickFlow..."
    for pid in "${PIDS[@]}"; do
        if kill -0 "$pid" 2>/dev/null; then
            step "Stopping PID $pid..."
            kill "$pid" 2>/dev/null || true
            wait "$pid" 2>/dev/null || true
        fi
    done
    ok "All services stopped."
    exit 0
}

trap cleanup SIGINT SIGTERM EXIT

# ── Health check helper ───────────────────────────────────────
wait_for_health() {
    local url="$1"
    local name="$2"
    local timeout="${3:-30}"
    local deadline=$((SECONDS + timeout))

    while [ $SECONDS -lt $deadline ]; do
        if curl -sf --max-time 3 "$url" > /dev/null 2>&1; then
            return 0
        fi
        sleep 1
    done
    return 1
}

# ══════════════════════════════════════════════════════════════
banner "QuickFlow — Starting All Services"
echo ""

# ── 1. Transcription Service ─────────────────────────────────
banner "1/4  Transcription Service"

TS_DIR="$ROOT/transcription-service"
VENV_PYTHON="$TS_DIR/venv/bin/python"
MAIN_PY="$TS_DIR/main.py"

if [ ! -f "$VENV_PYTHON" ]; then
    err "[TRANSCRIPTION SERVICE] Virtual environment not found at: $VENV_PYTHON"
    err "Run:  cd transcription-service && python3 -m venv venv && venv/bin/pip install -r requirements.txt"
    exit 1
fi
if [ ! -f "$MAIN_PY" ]; then
    err "[TRANSCRIPTION SERVICE] main.py not found at: $MAIN_PY"
    exit 1
fi

step "Starting transcription service..."
cd "$TS_DIR"
"$VENV_PYTHON" "$MAIN_PY" > /dev/null 2>"$TS_DIR/ts_stderr.log" &
TS_PID=$!
PIDS+=("$TS_PID")
cd "$ROOT"

info "Waiting for transcription service to become healthy (port 8001)..."
sleep 5

if wait_for_health "http://localhost:8001/health" "Transcription Service" 30; then
    ok "Transcription Service is running  →  http://localhost:8001"
else
    err "[TRANSCRIPTION SERVICE] Failed to start! Check transcription-service/ts_stderr.log"
    err "Health endpoint http://localhost:8001/health did not respond within 30s."
    exit 1
fi

# ── 2. Backend (Spring Boot) ─────────────────────────────────
banner "2/4  Backend Server"

BACKEND_DIR="$ROOT/backend"
if [ ! -f "$BACKEND_DIR/pom.xml" ]; then
    err "[BACKEND] pom.xml not found in: $BACKEND_DIR"
    exit 1
fi

step "Starting Spring Boot backend..."
MVN_CMD="mvn"
if ! command -v mvn &> /dev/null; then
    if [ -f "$BACKEND_DIR/mvnw" ]; then
        MVN_CMD="$BACKEND_DIR/mvnw"
        chmod +x "$MVN_CMD"
    else
        err "[BACKEND] Maven (mvn) not found in PATH and no mvnw wrapper found."
        exit 1
    fi
fi

cd "$BACKEND_DIR"
$MVN_CMD spring-boot:run > /dev/null 2>"$BACKEND_DIR/backend_stderr.log" &
BE_PID=$!
PIDS+=("$BE_PID")
cd "$ROOT"

info "Waiting for backend to become healthy (port 8080)..."
sleep 4

if wait_for_health "http://localhost:8080/api/health" "Backend" 60; then
    ok "Backend Server is running  →  http://localhost:8080"
else
    err "[BACKEND] Failed to start! Check backend/backend_stderr.log"
    err "Health endpoint http://localhost:8080/api/health did not respond within 60s."
    exit 1
fi

# ── 3. Ngrok Tunnel ──────────────────────────────────────────
banner "3/4  Ngrok Tunnel"

NGROK_DOMAIN="${NGROK_DOMAIN:-}"

if [ -z "$NGROK_DOMAIN" ]; then
    err "[NGROK] NGROK_DOMAIN environment variable is not set."
    err "Set it to your ngrok static domain, e.g.:"
    err "  export NGROK_DOMAIN=your-domain.ngrok-free.dev"
    exit 1
fi

if ! command -v ngrok &> /dev/null; then
    err "[NGROK] ngrok is not installed or not in PATH."
    err "Install from https://ngrok.com/download and add to PATH."
    exit 1
fi

step "Exposing backend through ngrok..."
ngrok http --url="$NGROK_DOMAIN" 8080 > /dev/null 2>&1 &
NGROK_PID=$!
PIDS+=("$NGROK_PID")

sleep 2
if ! kill -0 "$NGROK_PID" 2>/dev/null; then
    err "[NGROK] ngrok exited unexpectedly. Is another ngrok instance already running?"
    exit 1
fi

ok "Backend exposed to the web  →  https://$NGROK_DOMAIN"

# ── 4. Frontend (Vite Dev Server) ────────────────────────────
banner "4/4  Frontend Dev Server"

FRONTEND_DIR="$ROOT/frontend"
if [ ! -f "$FRONTEND_DIR/package.json" ]; then
    err "[FRONTEND] package.json not found in: $FRONTEND_DIR"
    exit 1
fi

# Check node_modules
if [ ! -d "$FRONTEND_DIR/node_modules" ]; then
    step "Installing frontend dependencies (npm install)..."
    cd "$FRONTEND_DIR"
    npm install > /dev/null 2>&1
    if [ $? -ne 0 ]; then
        err "[FRONTEND] npm install failed."
        exit 1
    fi
    cd "$ROOT"
fi

step "Starting Vite dev server..."
cd "$FRONTEND_DIR"
npm run dev > /dev/null 2>&1 &
FE_PID=$!
PIDS+=("$FE_PID")
cd "$ROOT"

sleep 3
if ! kill -0 "$FE_PID" 2>/dev/null; then
    err "[FRONTEND] Vite dev server exited unexpectedly."
    exit 1
fi

ok "Frontend running dev server  →  http://localhost:5173/"

# ── Summary ──────────────────────────────────────────────────
echo ""
echo -e "${C_GREEN}╔══════════════════════════════════════════════════╗${C_RESET}"
echo -e "${C_GREEN}║         QuickFlow is fully operational!          ║${C_RESET}"
echo -e "${C_GREEN}╠══════════════════════════════════════════════════╣${C_RESET}"
echo -e "${C_WHITE}║  Transcription :  http://localhost:8001          ║${C_RESET}"
echo -e "${C_WHITE}║  Backend       :  http://localhost:8080          ║${C_RESET}"
echo -e "${C_WHITE}║  Ngrok         :  https://$NGROK_DOMAIN          ${C_RESET}"
echo -e "${C_WHITE}║  Frontend      :  http://localhost:5173          ║${C_RESET}"
echo -e "${C_GREEN}╠══════════════════════════════════════════════════╣${C_RESET}"
echo -e "${C_YELLOW}║  Press Ctrl+C to shut down all services          ║${C_RESET}"
echo -e "${C_GREEN}╚══════════════════════════════════════════════════╝${C_RESET}"
echo ""

# ── Keep alive until Ctrl+C — monitor processes ──────────────
while true; do
    for pid in "${PIDS[@]}"; do
        if ! kill -0 "$pid" 2>/dev/null; then
            case "$pid" in
                "$TS_PID")      err "TRANSCRIPTION SERVICE exited unexpectedly" ;;
                "$BE_PID")      err "BACKEND exited unexpectedly" ;;
                "$NGROK_PID")   err "NGROK exited unexpectedly" ;;
                "$FE_PID")      err "FRONTEND exited unexpectedly" ;;
                *)              err "Unknown process (PID $pid) exited unexpectedly" ;;
            esac
        fi
    done
    sleep 5
done
