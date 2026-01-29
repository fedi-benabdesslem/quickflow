# Trivy Quick Start Guide

Get started with Trivy security scanning in 5 minutes.

## 🚀 Quick Setup

### 1. Install Trivy

**macOS:**
```bash
brew install aquasecurity/trivy/trivy
```

**Linux:**
```bash
# Ubuntu/Debian
sudo apt-get install wget apt-transport-https gnupg
wget -qO - https://aquasecurity.github.io/trivy-repo/deb/public.key | gpg --dearmor | sudo tee /usr/share/keyrings/trivy.gpg > /dev/null
echo "deb [signed-by=/usr/share/keyrings/trivy.gpg] https://aquasecurity.github.io/trivy-repo/deb $(lsb_release -sc) main" | sudo tee -a /etc/apt/sources.list.d/trivy.list
sudo apt-get update
sudo apt-get install trivy
```

**Windows:**
```powershell
choco install trivy
```

### 2. Verify Installation
```bash
trivy version
```

### 3. Run Your First Scan

**Scan backend:**
```bash
trivy fs backend/
```

**Scan frontend:**
```bash
trivy fs frontend/
```

**Scan for secrets:**
```bash
trivy fs --scanners secret .
```

## 📋 Common Commands

### Development Workflow

**Before committing (quick check):**
```bash
trivy fs --scanners secret --quiet .
```

**Full local scan:**
```bash
./scripts/security-scan.sh --full --report
```

**Scan specific component:**
```bash
./scripts/security-scan.sh --backend --report
```

**CI-style scan (fail on issues):**
```bash
./scripts/security-scan.sh --full --ci
```

### View Results

**HTML report:**
```bash
trivy fs --format template --template "@contrib/html.tpl" -o report.html backend/
open report.html
```

**JSON for processing:**
```bash
trivy fs --format json -o results.json backend/
cat results.json | jq
```

**Table view (default):**
```bash
trivy fs --severity HIGH,CRITICAL backend/
```

## 🔧 Integration Setup

### 1. Add GitHub Actions Workflow

The workflow file is already created at `.github/workflows/trivy-security-scan.yml`.

Just commit and push:
```bash
git add .github/workflows/trivy-security-scan.yml
git commit -m "Add Trivy security scanning"
git push
```

### 2. Enable Pre-commit Hook

Install the pre-commit hook to scan before every commit:
```bash
chmod +x scripts/pre-commit-hook.sh
cp scripts/pre-commit-hook.sh .git/hooks/pre-commit
chmod +x .git/hooks/pre-commit
```

Test it:
```bash
git add README.md
git commit -m "test commit"
# Should see: "🔍 Running pre-commit security checks..."
```

### 3. Make Security Script Executable
```bash
chmod +x scripts/security-scan.sh
```

## 📊 Understanding Results

### Severity Levels

| Level | Description | Action |
|-------|-------------|--------|
| **CRITICAL** | Immediate exploitation possible | Fix within 24h |
| **HIGH** | Serious vulnerability | Fix within 1 week |
| **MEDIUM** | Moderate risk | Fix in next sprint |
| **LOW** | Minor issue | Review quarterly |

### Vulnerability Example
```
library-name 1.2.3 (package-lock.json)
══════════════════════════════════════
Total: 1 (HIGH: 1)

┌────────────┬─────────────────┬──────────┬────────┬───────────────────┬─────────────────┐
│  Library   │ Vulnerability   │ Severity │ Status │ Installed Version │ Fixed Version   │
├────────────┼─────────────────┼──────────┼────────┼───────────────────┼─────────────────┤
│ express    │ CVE-2024-12345  │ HIGH     │ fixed  │ 4.17.1            │ 4.17.3          │
└────────────┴─────────────────┴──────────┴────────┴───────────────────┴─────────────────┘
```

**Fix:** Update the package
```bash
cd frontend
npm install express@4.17.3
```

### Secret Detection Example
```
secret: generic-api-key (SECRET)
──────────────────────────────────
Path: backend/src/main/resources/application.properties
Line: 42
Match: google.api.key=AIzaSyDaGmWKa4JsXZ...
```

**Fix:** Move to environment variable
```properties
# application.properties
google.api.key=${GOOGLE_API_KEY}
```

## 🎯 Best Practices

### Daily Workflow

1. **Before starting work:**
   ```bash
   git pull
   ./scripts/security-scan.sh --quick
   ```

2. **Before committing:**
   ```bash
   # Pre-commit hook runs automatically
   git commit -m "your message"
   ```

3. **Weekly:**
   ```bash
   ./scripts/security-scan.sh --full --report
   # Review and address findings
   ```

### Handling False Positives

Add to `.trivyignore`:
```
# Example: Development dependency, not used in production
CVE-2024-12345 pkg:npm/dev-dependency

# Secret in documentation
secret:generic-api-key README.md
```

### Updating Dependencies

**Backend (Maven):**
```bash
cd backend
mvn versions:display-dependency-updates
mvn versions:use-latest-versions
```

**Frontend (npm):**
```bash
cd frontend
npm outdated
npm update
npm audit fix
```

## 📚 Next Steps

1. ✅ Install Trivy
2. ✅ Run first scan
3. ✅ Set up GitHub Actions
4. ✅ Install pre-commit hook
5. ✅ Review and address findings
6. 📖 Read full documentation: `TRIVY-INTEGRATION.md`

## 🆘 Troubleshooting

### "Database download failed"
```bash
trivy image --download-db-only
```

### "Too many false positives"
Edit `.trivyignore` and add exceptions with comments.

### "Scan too slow"
```bash
# Use cache
trivy --cache-dir ~/.cache/trivy fs .

# Skip unfixed
trivy --ignore-unfixed fs .
```

### "Pre-commit hook not working"
```bash
chmod +x .git/hooks/pre-commit
# Verify:
cat .git/hooks/pre-commit
```

## 📞 Support

- Full documentation: `TRIVY-INTEGRATION.md`
- Trivy docs: https://aquasecurity.github.io/trivy/
- GitHub Issues: https://github.com/aquasecurity/trivy/issues
- Project maintainer: fadib.abdesslem204@gmail.com

---

**Remember:** Security is a continuous process, not a one-time task. Run scans regularly! 🛡️
