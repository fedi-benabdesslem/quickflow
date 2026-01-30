#!/bin/bash

# QuickFlow Security Scanning Script
# This script runs comprehensive Trivy security scans locally
# Usage: ./scripts/security-scan.sh [options]
#
# Options:
#   --quick        Run quick scan (vulnerabilities only)
#   --full         Run full scan (all checks)
#   --backend      Scan backend only
#   --frontend     Scan frontend only
#   --report       Generate HTML reports
#   --ci           CI mode (fail on HIGH/CRITICAL)

set -e  # Exit on error

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
MAGENTA='\033[0;35m'
CYAN='\033[0;36m'
NC='\033[0m' # No Color

# Configuration
REPORTS_DIR="security-reports"
TIMESTAMP=$(date +"%Y%m%d_%H%M%S")
SCAN_MODE="full"
TARGET="all"
GENERATE_REPORT=false
CI_MODE=false

# Parse arguments
while [[ $# -gt 0 ]]; do
    case $1 in
        --quick)
            SCAN_MODE="quick"
            shift
            ;;
        --full)
            SCAN_MODE="full"
            shift
            ;;
        --backend)
            TARGET="backend"
            shift
            ;;
        --frontend)
            TARGET="frontend"
            shift
            ;;
        --report)
            GENERATE_REPORT=true
            shift
            ;;
        --ci)
            CI_MODE=true
            shift
            ;;
        *)
            echo "Unknown option: $1"
            echo "Usage: $0 [--quick|--full] [--backend|--frontend] [--report] [--ci]"
            exit 1
            ;;
    esac
done

# Create reports directory
mkdir -p "$REPORTS_DIR"

# Print header
echo -e "${CYAN}╔════════════════════════════════════════════════════════════╗${NC}"
echo -e "${CYAN}║       QuickFlow Security Scanning with Trivy              ║${NC}"
echo -e "${CYAN}╚════════════════════════════════════════════════════════════╝${NC}"
echo ""
echo -e "${BLUE}Mode:${NC} $SCAN_MODE"
echo -e "${BLUE}Target:${NC} $TARGET"
echo -e "${BLUE}Report:${NC} $GENERATE_REPORT"
echo -e "${BLUE}CI Mode:${NC} $CI_MODE"
echo -e "${BLUE}Timestamp:${NC} $TIMESTAMP"
echo ""

# Check if Trivy is installed
if ! command -v trivy &> /dev/null; then
    echo -e "${RED}✗ Trivy is not installed!${NC}"
    echo ""
    echo "Please install Trivy:"
    echo "  macOS:   brew install aquasecurity/trivy/trivy"
    echo "  Linux:   See https://aquasecurity.github.io/trivy/latest/getting-started/installation/"
    echo "  Windows: choco install trivy"
    exit 1
fi

echo -e "${GREEN}✓ Trivy installed: $(trivy --version | head -n1)${NC}"
echo ""

# Function to run vulnerability scan
run_vuln_scan() {
    local target=$1
    local name=$2
    
    echo -e "${MAGENTA}═══════════════════════════════════════════════════════════${NC}"
    echo -e "${MAGENTA}  Vulnerability Scan: $name${NC}"
    echo -e "${MAGENTA}═══════════════════════════════════════════════════════════${NC}"
    echo ""
    
    local exit_code_flag="0"
    if [ "$CI_MODE" = true ]; then
        exit_code_flag="1"
    fi
    
    # JSON output for processing
    trivy fs \
        --severity HIGH,CRITICAL \
        --format json \
        --output "$REPORTS_DIR/${name}-vulnerabilities-${TIMESTAMP}.json" \
        "$target" || true
    
    # Table output for viewing
    echo ""
    trivy fs \
        --severity HIGH,CRITICAL \
        --format table \
        --exit-code "$exit_code_flag" \
        "$target"
    
    local scan_result=$?
    
    if [ $scan_result -eq 0 ]; then
        echo -e "${GREEN}✓ No HIGH or CRITICAL vulnerabilities found in $name${NC}"
    else
        echo -e "${RED}✗ Vulnerabilities found in $name${NC}"
        if [ "$CI_MODE" = true ]; then
            return 1
        fi
    fi
    
    echo ""
    return 0
}

# Function to run secret scan
run_secret_scan() {
    echo -e "${MAGENTA}═══════════════════════════════════════════════════════════${NC}"
    echo -e "${MAGENTA}  Secret Scanning${NC}"
    echo -e "${MAGENTA}═══════════════════════════════════════════════════════════${NC}"
    echo ""
    
    local exit_code_flag="0"
    if [ "$CI_MODE" = true ]; then
        exit_code_flag="1"
    fi
    
    # JSON output
    trivy fs \
        --scanners secret \
        --format json \
        --output "$REPORTS_DIR/secrets-scan-${TIMESTAMP}.json" \
        . || true
    
    # Table output
    echo ""
    trivy fs \
        --scanners secret \
        --format table \
        --exit-code "$exit_code_flag" \
        .
    
    local scan_result=$?
    
    if [ $scan_result -eq 0 ]; then
        echo -e "${GREEN}✓ No secrets detected${NC}"
    else
        echo -e "${RED}✗ Secrets detected in repository!${NC}"
        if [ "$CI_MODE" = true ]; then
            return 1
        fi
    fi
    
    echo ""
    return 0
}

# Function to run config scan
run_config_scan() {
    echo -e "${MAGENTA}═══════════════════════════════════════════════════════════${NC}"
    echo -e "${MAGENTA}  Configuration Scan${NC}"
    echo -e "${MAGENTA}═══════════════════════════════════════════════════════════${NC}"
    echo ""
    
    # JSON output
    trivy config \
        --format json \
        --output "$REPORTS_DIR/config-scan-${TIMESTAMP}.json" \
        . || true
    
    # Table output
    echo ""
    trivy config \
        --format table \
        --exit-code 0 \
        .
    
    echo -e "${GREEN}✓ Configuration scan completed${NC}"
    echo ""
}

# Function to run license scan
run_license_scan() {
    echo -e "${MAGENTA}═══════════════════════════════════════════════════════════${NC}"
    echo -e "${MAGENTA}  License Compliance Scan${NC}"
    echo -e "${MAGENTA}═══════════════════════════════════════════════════════════${NC}"
    echo ""
    
    trivy fs \
        --scanners license \
        --severity UNKNOWN,HIGH,CRITICAL \
        --format table \
        .
    
    echo -e "${GREEN}✓ License scan completed${NC}"
    echo ""
}

# Function to generate HTML report
generate_html_report() {
    local target=$1
    local name=$2
    
    echo -e "${BLUE}Generating HTML report for $name...${NC}"
    
    trivy fs \
        --format template \
        --template "@contrib/html.tpl" \
        --output "$REPORTS_DIR/${name}-report-${TIMESTAMP}.html" \
        "$target" || true
    
    echo -e "${GREEN}✓ HTML report: $REPORTS_DIR/${name}-report-${TIMESTAMP}.html${NC}"
}

# Main execution
SCAN_FAILED=false

# Backend scan
if [ "$TARGET" = "all" ] || [ "$TARGET" = "backend" ]; then
    if ! run_vuln_scan "backend" "backend"; then
        SCAN_FAILED=true
    fi
    
    if [ "$GENERATE_REPORT" = true ]; then
        generate_html_report "backend" "backend"
    fi
fi

# Frontend scan
if [ "$TARGET" = "all" ] || [ "$TARGET" = "frontend" ]; then
    if ! run_vuln_scan "frontend" "frontend"; then
        SCAN_FAILED=true
    fi
    
    if [ "$GENERATE_REPORT" = true ]; then
        generate_html_report "frontend" "frontend"
    fi
fi

# Full scan additional checks
if [ "$SCAN_MODE" = "full" ]; then
    if ! run_secret_scan; then
        SCAN_FAILED=true
    fi
    
    run_config_scan
    
    run_license_scan
fi

# Summary
echo -e "${CYAN}═══════════════════════════════════════════════════════════${NC}"
echo -e "${CYAN}  Scan Summary${NC}"
echo -e "${CYAN}═══════════════════════════════════════════════════════════${NC}"
echo ""

if [ "$SCAN_FAILED" = true ]; then
    echo -e "${RED}✗ Security issues detected!${NC}"
    echo ""
    echo "Review the findings above and:"
    echo "  1. Fix HIGH and CRITICAL vulnerabilities immediately"
    echo "  2. Remove any detected secrets"
    echo "  3. Update vulnerable dependencies"
    echo "  4. Review configuration issues"
    echo ""
    echo "Reports saved in: $REPORTS_DIR/"
    echo ""
    
    if [ "$CI_MODE" = true ]; then
        exit 1
    fi
else
    echo -e "${GREEN}✓ All scans passed!${NC}"
    echo ""
fi

# Display report locations
if [ "$GENERATE_REPORT" = true ]; then
    echo "HTML Reports:"
    ls -lh "$REPORTS_DIR"/*-report-${TIMESTAMP}.html 2>/dev/null || true
    echo ""
fi

echo "JSON Scan Results:"
ls -lh "$REPORTS_DIR"/*-${TIMESTAMP}.json 2>/dev/null || true
echo ""

echo -e "${BLUE}To view HTML reports:${NC}"
echo "  open $REPORTS_DIR/*-report-${TIMESTAMP}.html"
echo ""

echo -e "${BLUE}To view JSON results:${NC}"
echo "  cat $REPORTS_DIR/*-${TIMESTAMP}.json | jq"
echo ""

echo -e "${GREEN}Scan completed at $(date)${NC}"
