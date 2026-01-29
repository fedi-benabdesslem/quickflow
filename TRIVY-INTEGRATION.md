# Trivy Security Scanning Integration Guide

> **Comprehensive security scanning for QuickFlow using Aqua Security's Trivy**

## 📋 Table of Contents

1. [Overview](#overview)
2. [Prerequisites](#prerequisites)
3. [Installation](#installation)
4. [GitHub Actions Integration](#github-actions-integration)
5. [Local Development Scanning](#local-development-scanning)
6. [Docker Image Scanning](#docker-image-scanning)
7. [Configuration Files](#configuration-files)
8. [Best Practices](#best-practices)
9. [Troubleshooting](#troubleshooting)

---

## Overview

Trivy is a comprehensive security scanner that will help detect:
- **Vulnerabilities** in dependencies (Java/Maven, Node.js/npm)
- **Misconfigurations** in configuration files
- **Secrets** accidentally committed to the repository
- **License compliance** issues
- **Docker image** vulnerabilities (when containerized)

For QuickFlow, we'll scan:
- Backend Java dependencies (Maven)
- Frontend Node.js dependencies (npm)
- Configuration files (YAML, JSON, properties)
- Source code for hardcoded secrets
- Future Docker images

---

## Prerequisites

- Git repository (GitHub recommended for Actions)
- GitHub account with repo access
- Local machine with internet connection
- Admin access to repository settings

---

## Installation

### Local Installation

#### MacOS
```bash
brew install aquasecurity/trivy/trivy
```

#### Linux
```bash
# Debian/Ubuntu
sudo apt-get install wget apt-transport-https gnupg lsb-release
wget -qO - https://aquasecurity.github.io/trivy-repo/deb/public.key | gpg --dearmor | sudo tee /usr/share/keyrings/trivy.gpg > /dev/null
echo "deb [signed-by=/usr/share/keyrings/trivy.gpg] https://aquasecurity.github.io/trivy-repo/deb $(lsb_release -sc) main" | sudo tee -a /etc/apt/sources.list.d/trivy.list
sudo apt-get update
sudo apt-get install trivy

# RHEL/CentOS
sudo rpm -ivh https://github.com/aquasecurity/trivy/releases/download/v0.48.0/trivy_0.48.0_Linux-64bit.rpm
```

#### Windows
```powershell
# Using Chocolatey
choco install trivy

# Or download from releases
# https://github.com/aquasecurity/trivy/releases
```

### Verify Installation
```bash
trivy version
```

---

## GitHub Actions Integration

Create a complete CI/CD pipeline with automated security scanning on every push and pull request.

### 1. Create GitHub Actions Workflow

Create `.github/workflows/trivy-security-scan.yml`:

```yaml
name: Trivy Security Scan

on:
  push:
    branches: [ main, develop ]
  pull_request:
    branches: [ main, develop ]
  schedule:
    # Run daily at 2 AM UTC to catch newly disclosed CVEs
    - cron: '0 2 * * *'

jobs:
  trivy-backend-scan:
    name: Trivy Backend (Java/Maven) Scan
    runs-on: ubuntu-latest
    permissions:
      contents: read
      security-events: write
      
    steps:
      - name: Checkout code
        uses: actions/checkout@v4

      - name: Run Trivy vulnerability scanner on backend
        uses: aquasecurity/trivy-action@master
        with:
          scan-type: 'fs'
          scan-ref: './backend'
          format: 'sarif'
          output: 'trivy-backend-results.sarif'
          severity: 'CRITICAL,HIGH,MEDIUM'
          vuln-type: 'os,library'
          exit-code: '0'  # Don't fail the build yet

      - name: Upload Trivy results to GitHub Security tab
        uses: github/codeql-action/upload-sarif@v3
        if: always()
        with:
          sarif_file: 'trivy-backend-results.sarif'
          category: 'trivy-backend'

      - name: Run Trivy and fail on HIGH/CRITICAL
        uses: aquasecurity/trivy-action@master
        with:
          scan-type: 'fs'
          scan-ref: './backend'
          format: 'table'
          severity: 'CRITICAL,HIGH'
          exit-code: '1'  # Fail if HIGH or CRITICAL found

  trivy-frontend-scan:
    name: Trivy Frontend (Node.js/npm) Scan
    runs-on: ubuntu-latest
    permissions:
      contents: read
      security-events: write
      
    steps:
      - name: Checkout code
        uses: actions/checkout@v4

      - name: Run Trivy vulnerability scanner on frontend
        uses: aquasecurity/trivy-action@master
        with:
          scan-type: 'fs'
          scan-ref: './frontend'
          format: 'sarif'
          output: 'trivy-frontend-results.sarif'
          severity: 'CRITICAL,HIGH,MEDIUM'
          vuln-type: 'library'
          exit-code: '0'

      - name: Upload Trivy results to GitHub Security tab
        uses: github/codeql-action/upload-sarif@v3
        if: always()
        with:
          sarif_file: 'trivy-frontend-results.sarif'
          category: 'trivy-frontend'

      - name: Run Trivy and fail on HIGH/CRITICAL
        uses: aquasecurity/trivy-action@master
        with:
          scan-type: 'fs'
          scan-ref: './frontend'
          format: 'table'
          severity: 'CRITICAL,HIGH'
          exit-code: '1'

  trivy-config-scan:
    name: Trivy Configuration Scan
    runs-on: ubuntu-latest
    permissions:
      contents: read
      security-events: write
      
    steps:
      - name: Checkout code
        uses: actions/checkout@v4

      - name: Run Trivy misconfiguration scanner
        uses: aquasecurity/trivy-action@master
        with:
          scan-type: 'config'
          scan-ref: '.'
          format: 'sarif'
          output: 'trivy-config-results.sarif'
          exit-code: '0'

      - name: Upload Trivy config results
        uses: github/codeql-action/upload-sarif@v3
        if: always()
        with:
          sarif_file: 'trivy-config-results.sarif'
          category: 'trivy-config'

      - name: Run Trivy config scan (fail on issues)
        uses: aquasecurity/trivy-action@master
        with:
          scan-type: 'config'
          scan-ref: '.'
          format: 'table'
          severity: 'CRITICAL,HIGH'
          exit-code: '1'

  trivy-secret-scan:
    name: Trivy Secret Scanning
    runs-on: ubuntu-latest
    permissions:
      contents: read
      security-events: write
      
    steps:
      - name: Checkout code
        uses: actions/checkout@v4

      - name: Run Trivy secret scanner
        uses: aquasecurity/trivy-action@master
        with:
          scan-type: 'fs'
          scan-ref: '.'
          scanners: 'secret'
          format: 'sarif'
          output: 'trivy-secret-results.sarif'
          exit-code: '0'

      - name: Upload Trivy secret results
        uses: github/codeql-action/upload-sarif@v3
        if: always()
        with:
          sarif_file: 'trivy-secret-results.sarif'
          category: 'trivy-secrets'

      - name: Run Trivy secret scan (fail on findings)
        uses: aquasecurity/trivy-action@master
        with:
          scan-type: 'fs'
          scan-ref: '.'
          scanners: 'secret'
          format: 'table'
          exit-code: '1'  # Always fail if secrets found
```

### 2. Enable GitHub Security Features

1. Go to your repository **Settings**
2. Navigate to **Security > Code security and analysis**
3. Enable:
   - ✅ Dependency graph
   - ✅ Dependabot alerts
   - ✅ Dependabot security updates
   - ✅ Secret scanning (if available for your plan)

### 3. View Scan Results

After pushing the workflow:
1. Go to **Actions** tab to see workflow runs
2. Go to **Security > Code scanning alerts** to see Trivy findings
3. Each vulnerability will show:
   - Severity level
   - Affected package/file
   - CVE details
   - Remediation advice

---

## Local Development Scanning

Run Trivy scans locally before pushing code.

### Quick Scan Commands

#### Scan Backend (Java/Maven)
```bash
# Basic scan
trivy fs backend/

# Detailed scan with severity filtering
trivy fs --severity HIGH,CRITICAL backend/

# Generate HTML report
trivy fs --format template --template "@contrib/html.tpl" -o backend-report.html backend/

# Scan specific pom.xml
trivy fs --scanners vuln backend/pom.xml
```

#### Scan Frontend (Node.js/npm)
```bash
# Basic scan
trivy fs frontend/

# Scan with JSON output
trivy fs --format json -o frontend-scan.json frontend/

# Only scan package-lock.json
trivy fs --scanners vuln frontend/package-lock.json

# Scan ignoring dev dependencies
trivy fs --skip-dirs node_modules frontend/
```

#### Scan for Secrets
```bash
# Scan entire repository for secrets
trivy fs --scanners secret .

# Scan specific directories
trivy fs --scanners secret backend/src/
trivy fs --scanners secret frontend/src/

# Ignore specific patterns
trivy fs --scanners secret --skip-files "*.test.js,*.md" .
```

#### Scan for Misconfigurations
```bash
# Scan configuration files
trivy config .

# Scan specific config files
trivy config backend/src/main/resources/application.properties
trivy config frontend/vite.config.ts
```

### Create Local Scan Script

Create `scripts/security-scan.sh`:

```bash
#!/bin/bash

echo "🔍 Running QuickFlow Security Scans..."
echo ""

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Create reports directory
mkdir -p security-reports

echo "📦 Scanning Backend Dependencies..."
trivy fs --severity HIGH,CRITICAL \
  --format json \
  -o security-reports/backend-vulnerabilities.json \
  backend/

if [ $? -eq 0 ]; then
  echo -e "${GREEN}✓ Backend scan completed${NC}"
else
  echo -e "${RED}✗ Backend scan found issues${NC}"
fi

echo ""
echo "🎨 Scanning Frontend Dependencies..."
trivy fs --severity HIGH,CRITICAL \
  --format json \
  -o security-reports/frontend-vulnerabilities.json \
  frontend/

if [ $? -eq 0 ]; then
  echo -e "${GREEN}✓ Frontend scan completed${NC}"
else
  echo -e "${RED}✗ Frontend scan found issues${NC}"
fi

echo ""
echo "🔐 Scanning for Secrets..."
trivy fs --scanners secret \
  --format json \
  -o security-reports/secrets-scan.json \
  .

if [ $? -eq 0 ]; then
  echo -e "${GREEN}✓ No secrets found${NC}"
else
  echo -e "${RED}✗ Secrets detected!${NC}"
fi

echo ""
echo "⚙️  Scanning Configurations..."
trivy config \
  --format json \
  -o security-reports/config-scan.json \
  .

if [ $? -eq 0 ]; then
  echo -e "${GREEN}✓ Configuration scan completed${NC}"
else
  echo -e "${YELLOW}⚠ Configuration issues found${NC}"
fi

echo ""
echo "📊 Generating HTML Reports..."
trivy fs --format template --template "@contrib/html.tpl" \
  -o security-reports/backend-report.html \
  backend/

trivy fs --format template --template "@contrib/html.tpl" \
  -o security-reports/frontend-report.html \
  frontend/

echo ""
echo -e "${GREEN}✓ All scans completed!${NC}"
echo "📁 Reports saved to: security-reports/"
echo ""
echo "To view reports:"
echo "  open security-reports/backend-report.html"
echo "  open security-reports/frontend-report.html"
```

Make it executable:
```bash
chmod +x scripts/security-scan.sh
```

Run it:
```bash
./scripts/security-scan.sh
```

### Add to Git Hooks (Pre-commit)

Create `.git/hooks/pre-commit`:

```bash
#!/bin/bash

echo "Running Trivy security scan before commit..."

# Scan for secrets (strict)
trivy fs --scanners secret --exit-code 1 --quiet . 2>&1 | grep -v "Trivy skipped"

if [ $? -ne 0 ]; then
  echo "❌ Security scan failed! Secrets detected."
  echo "Please remove secrets before committing."
  exit 1
fi

echo "✅ Security scan passed!"
exit 0
```

Make it executable:
```bash
chmod +x .git/hooks/pre-commit
```

---

## Docker Image Scanning

When you containerize QuickFlow, scan Docker images before deployment.

### Dockerfile Security Scanning

Create `backend/Dockerfile` (example):
```dockerfile
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

COPY target/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
```

Create `frontend/Dockerfile` (example):
```dockerfile
FROM node:18-alpine as build

WORKDIR /app

COPY package*.json ./
RUN npm ci

COPY . .
RUN npm run build

FROM nginx:alpine
COPY --from=build /app/dist /usr/share/nginx/html
EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]
```

### Scan Docker Images

```bash
# Build images
docker build -t quickflow-backend:latest ./backend
docker build -t quickflow-frontend:latest ./frontend

# Scan backend image
trivy image quickflow-backend:latest

# Scan frontend image  
trivy image quickflow-frontend:latest

# Scan with severity filter
trivy image --severity HIGH,CRITICAL quickflow-backend:latest

# Generate report
trivy image --format json -o backend-image-scan.json quickflow-backend:latest
```

### Add Docker Scanning to GitHub Actions

Add to `.github/workflows/trivy-security-scan.yml`:

```yaml
  trivy-docker-scan:
    name: Trivy Docker Image Scan
    runs-on: ubuntu-latest
    if: github.event_name == 'push' && github.ref == 'refs/heads/main'
    permissions:
      contents: read
      security-events: write
      
    steps:
      - name: Checkout code
        uses: actions/checkout@v4

      - name: Set up Docker Buildx
        uses: docker/setup-buildx-action@v3

      - name: Build Backend Docker image
        run: |
          cd backend
          docker build -t quickflow-backend:${{ github.sha }} .

      - name: Run Trivy on Backend image
        uses: aquasecurity/trivy-action@master
        with:
          image-ref: 'quickflow-backend:${{ github.sha }}'
          format: 'sarif'
          output: 'trivy-backend-image.sarif'

      - name: Upload Backend image results
        uses: github/codeql-action/upload-sarif@v3
        with:
          sarif_file: 'trivy-backend-image.sarif'
          category: 'trivy-backend-image'

      - name: Build Frontend Docker image
        run: |
          cd frontend
          docker build -t quickflow-frontend:${{ github.sha }} .

      - name: Run Trivy on Frontend image
        uses: aquasecurity/trivy-action@master
        with:
          image-ref: 'quickflow-frontend:${{ github.sha }}'
          format: 'sarif'
          output: 'trivy-frontend-image.sarif'

      - name: Upload Frontend image results
        uses: github/codeql-action/upload-sarif@v3
        with:
          sarif_file: 'trivy-frontend-image.sarif'
          category: 'trivy-frontend-image'
```

---

## Configuration Files

### Trivy Configuration File

Create `trivy.yaml` in project root:

```yaml
# Trivy configuration file for QuickFlow
# Place in project root

# Scan settings
scan:
  # File patterns to skip
  skip-files:
    - "**/*.md"
    - "**/*.txt"
    - "**/test/**"
    - "**/tests/**"
    - "**/__tests__/**"
    - "**/node_modules/**"
    - "**/target/**"
    - "**/dist/**"
    - "**/build/**"
  
  # Directories to skip
  skip-dirs:
    - "node_modules"
    - "target"
    - "dist"
    - "build"
    - ".git"
    - ".github"
    - "security-reports"

# Vulnerability settings
vulnerability:
  # Severity levels to detect
  severity:
    - CRITICAL
    - HIGH
    - MEDIUM
  
  # Vulnerability types
  type:
    - os
    - library

# Secret scanning settings
secret:
  # Secret types to detect
  config: "trivy-secret.yaml"

# Misconfiguration settings
misconfiguration:
  # Configuration scanners to use
  scanners:
    - yaml
    - json
    - dockerfile
    - terraform

# Output settings
format: table
output: ""

# Database settings
db:
  # Skip updating vulnerability database (use for faster scans in CI)
  skip-update: false
  
  # No progress bar in CI
  no-progress: false

# Cache settings
cache:
  # Cache directory
  dir: "/tmp/trivy"
```

### Secret Scanning Configuration

Create `trivy-secret.yaml`:

```yaml
# Custom secret scanning rules for QuickFlow

# Disable built-in rules that cause false positives
disabled-rules:
  - generic-api-key  # Often triggers on example values

# Custom secret patterns
custom-patterns:
  - id: supabase-key
    category: supabase
    title: Supabase Anon Key
    pattern: "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9\\.[a-zA-Z0-9_-]+\\.[a-zA-Z0-9_-]+"
    severity: HIGH
    
  - id: google-oauth-client-id
    category: google
    title: Google OAuth Client ID
    pattern: "[0-9]+-[a-zA-Z0-9_]+\\.apps\\.googleusercontent\\.com"
    severity: MEDIUM
    
  - id: mongodb-connection-string
    category: database
    title: MongoDB Connection String
    pattern: "mongodb(\\+srv)?://[^\\s]+"
    severity: CRITICAL

# Allowlist (patterns that are safe to ignore)
allowlist:
  - rule: generic-api-key
    patterns:
      - "EXAMPLE_API_KEY"
      - "your_api_key_here"
      - "YOUR_.*_KEY"
      - "test-api-key"
      - "fake-key-123"
```

### .trivyignore File

Create `.trivyignore` to suppress known false positives:

```
# QuickFlow Trivy Ignore File
# Use this to suppress false positives or accepted risks

# Example: Suppress specific CVEs that don't apply
# CVE-2023-12345

# Example: Suppress low severity issues in dev dependencies
# npm:dev-dependency-name

# Example: Suppress issues in test files
# test/**

# Add your exceptions below with comments explaining why:

# Known issue with Spring Boot 3.3 - fix coming in 3.3.1
# CVE-2024-XXXXX
```

---

## Best Practices

### 1. **Scan Regularly**
- Run Trivy scans before every commit (via git hooks)
- Run automated scans on every PR
- Run scheduled daily scans to catch new CVEs

### 2. **Prioritize Fixes**
```
Priority Order:
1. CRITICAL secrets in source code → Fix immediately
2. CRITICAL vulnerabilities with exploits → Fix within 24h
3. HIGH severity vulnerabilities → Fix within 1 week
4. MEDIUM severity → Fix in next sprint
5. LOW severity → Review quarterly
```

### 3. **Keep Dependencies Updated**

Backend (Maven):
```bash
# Check for updates
cd backend
mvn versions:display-dependency-updates
mvn versions:display-plugin-updates

# Update dependencies
mvn versions:use-latest-versions
```

Frontend (npm):
```bash
# Check for updates
cd frontend
npm outdated

# Update dependencies
npm update
npm audit fix

# For major version updates
npx npm-check-updates -u
npm install
```

### 4. **Document Exceptions**

When ignoring findings, always document why:
```yaml
# .trivyignore
CVE-2024-12345  # False positive - we don't use the vulnerable function
CVE-2024-67890  # Risk accepted - waiting for upstream fix in Q2
```

### 5. **Integrate with Development Workflow**

```bash
# Add to package.json scripts
{
  "scripts": {
    "security:scan": "trivy fs .",
    "security:fix": "npm audit fix && trivy fs .",
    "precommit": "npm run security:scan"
  }
}
```

### 6. **Monitor Trends**

Track metrics over time:
- Number of vulnerabilities per severity
- Time to remediation
- Repeat offenders (dependencies that frequently have issues)

---

## Troubleshooting

### Common Issues

#### 1. "Database Download Failed"
```bash
# Manually update Trivy database
trivy image --download-db-only

# Or use offline mode with cached DB
trivy --offline fs .
```

#### 2. "Too Many False Positives"
```bash
# Use .trivyignore file
echo "CVE-2024-XXXXX" >> .trivyignore

# Or suppress specific rules
trivy fs --ignore-unfixed .
```

#### 3. "Scan Takes Too Long"
```bash
# Skip database update
trivy --skip-db-update fs .

# Scan specific paths only
trivy fs backend/pom.xml frontend/package.json

# Use cache
trivy --cache-dir /tmp/trivy-cache fs .
```

#### 4. "GitHub Actions Rate Limited"
```yaml
# Add caching to workflow
- name: Cache Trivy DB
  uses: actions/cache@v3
  with:
    path: ~/.cache/trivy
    key: trivy-db-${{ github.run_id }}
    restore-keys: trivy-db-
```

#### 5. "Secret Scan False Positives"
```bash
# Create custom secret rules
trivy fs --secret-config trivy-secret.yaml .

# Use allowlist
trivy fs --secret-config trivy-secret.yaml \
  --ignore-policy .trivyignore .
```

### Debug Mode

Run Trivy with debug output:
```bash
trivy --debug fs .
```

### Clear Cache

If experiencing issues:
```bash
trivy clean --all
rm -rf ~/.cache/trivy
```

---

## Integration Checklist

- [ ] Trivy installed locally
- [ ] `.github/workflows/trivy-security-scan.yml` created
- [ ] `trivy.yaml` configuration created
- [ ] `trivy-secret.yaml` secret rules created
- [ ] `.trivyignore` file created
- [ ] Pre-commit hook configured
- [ ] `security-scan.sh` script created
- [ ] GitHub Security features enabled
- [ ] Team trained on interpreting results
- [ ] Process defined for handling findings
- [ ] Documentation updated

---

## Additional Resources

- **Trivy Documentation**: https://aquasecurity.github.io/trivy/
- **GitHub Actions**: https://github.com/aquasecurity/trivy-action
- **CVE Database**: https://cve.mitre.org/
- **OWASP Top 10**: https://owasp.org/Top10/
- **Spring Boot Security**: https://spring.io/guides/topicals/spring-security-architecture
- **React Security**: https://react.dev/learn/security

---

## Support

For issues or questions:
1. Check Trivy GitHub Issues: https://github.com/aquasecurity/trivy/issues
2. Review this documentation
3. Contact security team at: fadib.abdesslem204@gmail.com

---

**Last Updated**: January 29, 2026  
**Version**: 1.0  
**Maintainer**: QuickFlow Security Team
