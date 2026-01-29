# Security Section - Add to README.md

Add this section to your README.md after the "Getting Started" section:

---

## 🔒 Security

QuickFlow uses [Trivy](https://aquasecurity.github.io/trivy/) for comprehensive security scanning.

### Quick Security Check

```bash
# Install Trivy (macOS)
brew install aquasecurity/trivy/trivy

# Run security scan
./scripts/security-scan.sh --full --report
```

### Features

- **Automated Vulnerability Scanning** - Detects CVEs in Java and Node.js dependencies
- **Secret Detection** - Prevents accidental commit of API keys, tokens, and credentials
- **Configuration Auditing** - Checks for security misconfigurations
- **CI/CD Integration** - Automated scans on every push and pull request
- **Pre-commit Hooks** - Local scanning before commits

### Documentation

- **[Quick Start Guide](./TRIVY-QUICKSTART.md)** - Get started in 5 minutes
- **[Complete Integration Guide](./TRIVY-INTEGRATION.md)** - Full documentation
- **[Integration Checklist](./TRIVY-CHECKLIST.md)** - Step-by-step setup

### Security Workflow

1. **Local Development**
   ```bash
   # Pre-commit hook runs automatically on commit
   git commit -m "your changes"
   ```

2. **Pull Requests**
   - Automated scans run on every PR
   - Results appear in GitHub Security tab

3. **Regular Monitoring**
   - Daily automated scans at 2 AM UTC
   - Email notifications for new vulnerabilities

### Reporting Security Issues

Found a security issue? Please email: fadib.abdesslem204@gmail.com

**Do not** create public GitHub issues for security vulnerabilities.

---

## Updated Roadmap Section

Update the Roadmap section to include:

```markdown
## 🗺️ Roadmap

We are constantly improving QuickFlow. Here's what's coming next:
- [x] **Contact Autocomplete**: Smart suggestions for participants and recipients.
- [x] **Security Scanning**: Automated vulnerability detection with Trivy.
- [ ] **Calendar Integration**: Sync meetings directly from Google/Outlook Calendar.
- [ ] **Voice Mode**: Real-time transcription and summarization.
- [ ] **Team Workspaces**: Share templates and minutes with your team.
- [ ] **Docker Deployment**: Containerized deployment with security scanning.
```

---

## Badge to Add at Top of README

Add these badges after the title:

```markdown
![Security Scan](https://github.com/YOUR_USERNAME/quickflow/actions/workflows/trivy-security-scan.yml/badge.svg)
![License](https://img.shields.io/badge/License-MIT-blue.svg)
![Java](https://img.shields.io/badge/Java-17+-orange.svg)
![Node](https://img.shields.io/badge/Node-18+-green.svg)
```

Replace `YOUR_USERNAME` with your actual GitHub username.
