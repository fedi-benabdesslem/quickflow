#!/bin/bash

# QuickFlow Pre-commit Hook - Secret Scanning
# This hook runs Trivy secret scanning before each commit
# To install: copy this file to .git/hooks/pre-commit and make it executable
#   cp scripts/pre-commit-hook.sh .git/hooks/pre-commit
#   chmod +x .git/hooks/pre-commit

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

echo "🔍 Running pre-commit security checks..."

# Check if Trivy is installed
if ! command -v trivy &> /dev/null; then
    echo -e "${YELLOW}⚠️  Trivy not installed - skipping secret scan${NC}"
    echo "   Install with: brew install aquasecurity/trivy/trivy"
    exit 0
fi

# Get list of files to be committed
FILES=$(git diff --cached --name-only --diff-filter=ACM)

if [ -z "$FILES" ]; then
    echo -e "${GREEN}✓ No files to scan${NC}"
    exit 0
fi

echo "📄 Scanning staged files for secrets..."

# Create temporary directory for staged files
TEMP_DIR=$(mktemp -d)
trap "rm -rf $TEMP_DIR" EXIT

# Copy staged files to temp directory maintaining structure
for FILE in $FILES; do
    if [ -f "$FILE" ]; then
        mkdir -p "$TEMP_DIR/$(dirname "$FILE")"
        cp "$FILE" "$TEMP_DIR/$FILE"
    fi
done

# Run Trivy secret scan on temp directory
SCAN_OUTPUT=$(trivy fs --scanners secret --quiet --exit-code 1 "$TEMP_DIR" 2>&1)
SCAN_RESULT=$?

if [ $SCAN_RESULT -ne 0 ]; then
    echo -e "${RED}✗ Secret scanning failed!${NC}"
    echo ""
    echo "$SCAN_OUTPUT"
    echo ""
    echo -e "${RED}┌────────────────────────────────────────────────────┐${NC}"
    echo -e "${RED}│  🚨 SECRETS DETECTED IN STAGED FILES 🚨           │${NC}"
    echo -e "${RED}└────────────────────────────────────────────────────┘${NC}"
    echo ""
    echo "Please remove the secrets before committing."
    echo ""
    echo "Options:"
    echo "  1. Remove the secrets and commit again"
    echo "  2. Add false positives to .trivyignore"
    echo "  3. Use environment variables instead"
    echo ""
    echo "To skip this check (NOT RECOMMENDED):"
    echo "  git commit --no-verify"
    echo ""
    exit 1
fi

echo -e "${GREEN}✓ No secrets detected${NC}"

# Optional: Run quick vulnerability check on changed package files
if echo "$FILES" | grep -qE "(package\.json|package-lock\.json|pom\.xml)"; then
    echo "📦 Dependency files changed - running quick vulnerability scan..."
    
    if echo "$FILES" | grep -q "package"; then
        trivy fs --quiet --exit-code 0 --severity CRITICAL frontend/ || true
    fi
    
    if echo "$FILES" | grep -q "pom.xml"; then
        trivy fs --quiet --exit-code 0 --severity CRITICAL backend/ || true
    fi
fi

echo -e "${GREEN}✓ Pre-commit checks passed${NC}"
exit 0
