# Production Security Checklist

## Overview
This document describes the required environment variables for QuickFlow's self-hosted JWT + OAuth2 authentication system and lists all security steps needed before deploying to production.

> **Note:** All secrets have been removed from `application.properties`. Every sensitive value is now read exclusively from environment variables. The properties listed below must be set in your deployment environment (shell, `.env` file not committed to git, Docker secret, or a secrets manager).

---

## 1. Required Environment Variables

### 1.1 Gemini API Key
- **Environment variable:** `GEMINI_API_KEY`
- **Property:** `spring.ai.openai.api-key=${GEMINI_API_KEY:}`
- **Risk:** API abuse, unexpected billing charges
- **Action:** Obtain a new key from [Google AI Studio](https://aistudio.google.com/), set the variable, revoke any previously committed key

### 1.2 JWT Signing Secret
- **Environment variable:** `JWT_SECRET`
- **Property:** `app.jwt.secret=${JWT_SECRET}` (no fallback — application will not start if missing)
- **Risk:** JWT forgery, authentication bypass
- **Action:** Generate a new 256-bit secret and rotate the previously committed key:
  ```bash
  openssl rand -base64 32   # generate new JWT_SECRET
  ```

### 1.3 Token Encryption Key (AES-256)
- **Environment variable:** `TOKEN_ENCRYPTION_KEY`
- **Property:** `token.encryption.key=${TOKEN_ENCRYPTION_KEY}` (no fallback — application will not start if missing)
- **Risk:** Decryption of stored OAuth access/refresh tokens; account takeover
- **Action:** Generate a new 32-byte key and rotate the previously committed key:
  ```bash
  openssl rand -base64 32   # generate new TOKEN_ENCRYPTION_KEY
  ```

### 1.4 Resend Email API Key
- **Environment variable:** `RESEND_API_KEY`
- **Property:** `app.resend.api-key=${RESEND_API_KEY:}`
- **Risk:** Sending emails on behalf of your domain, billing abuse
- **Action:** Revoke the previously committed key at [resend.com](https://resend.com) and issue a new one

### 1.5 Google OAuth Client ID & Secret
- **Environment variables:** `GOOGLE_CLIENT_ID`, `GOOGLE_CLIENT_SECRET`
- **Properties:** `google.oauth.client-id`, `google.oauth.client-secret`
- **Risk:** OAuth credential impersonation, unauthorised login
- **Action:** Revoke the previously committed client secret in [Google Cloud Console](https://console.cloud.google.com/) and create new credentials for the production redirect URI

### 1.6 Microsoft OAuth Configuration (optional)
- **Environment variables:** `MICROSOFT_CLIENT_ID`, `MICROSOFT_CLIENT_SECRET`, `MICROSOFT_TENANT_ID`
- **Properties:** `microsoft.oauth.client-id`, `microsoft.oauth.client-secret`, `microsoft.oauth.tenant-id`
- **Status:** ✅ Properly configured (no hardcoded values; empty defaults are safe when Microsoft OAuth is not in use)
- **Action:** Set the variables when Microsoft OAuth is enabled in production

### 1.7 Application URLs
- **Environment variables:** `FRONTEND_URL`, `BACKEND_URL`
- **Properties:** `app.frontend.url`, `app.backend.url`
- **Status:** Defaults to `localhost` — must be overridden for production

### 1.8 MongoDB URI
- **Environment variable:** `MONGODB_URI`
- **Property:** `spring.data.mongodb.uri=${MONGODB_URI:mongodb://localhost:27017/electrodb}`
- **Action:** Supply a connection string with authentication for production (do not use the unauthenticated localhost default)

### 1.9 Ngrok Domain (development scripts only)
- **Environment variable:** `NGROK_DOMAIN`
- **Used by:** `power_on.sh`, `power_on.ps1`
- **Risk:** The domain is tied to a specific developer's ngrok account; hardcoding it would expose account identity
- **Action:** Each developer sets their own static domain:
  ```bash
  export NGROK_DOMAIN=your-domain.ngrok-free.dev
  ```

---

## 2. Git History — Credential Rotation Required

The following secrets were committed in earlier commits of this branch and **must be rotated/revoked immediately**, even though they have since been removed from the file:

| Secret | Type | Committed in |
|--------|------|--------------|
| `AIzaSy…Hi8Q` | Gemini API key | `application.properties` (commit `5728938`) |
| `AcdmhqxqAg2T…` | JWT signing secret | `application.properties` (commit `5728938`) |
| `GmXA9DC0KtUL…` | AES-256 encryption key | `application.properties` (commit `5728938`) |
| `re_MTesD6…` | Resend email API key | `application.properties` (commit `5728938`) |
| `GOCSPX-wOJlO…` | Google OAuth client secret | `application.properties` (commit `5728938`) |
| `253550165766-…` | Google OAuth client ID | `application.properties` (commit `5728938`) |
| `39r0…` | Ngrok authtoken | `SETUP.md` (commit `83af87f`) |

> These values are still visible in git history. Run `git filter-repo` or BFG Repo Cleaner to scrub history, then force-push and rotate each credential.

---

## 3. Secret Externalization Methods

### Option A: Environment Variables (Simplest)
```bash
# Add to your deployment environment or a local .env file (never committed to git)
export JWT_SECRET=...
export TOKEN_ENCRYPTION_KEY=...
export GEMINI_API_KEY=...
export RESEND_API_KEY=...
export GOOGLE_CLIENT_ID=...
export GOOGLE_CLIENT_SECRET=...
export MICROSOFT_CLIENT_ID=...
export MICROSOFT_CLIENT_SECRET=...
export MICROSOFT_TENANT_ID=...
export MONGODB_URI=mongodb+srv://user:pass@cluster/db
export FRONTEND_URL=https://app.example.com
export BACKEND_URL=https://api.example.com
```

### Option B: Spring Boot Profile-Based Configuration
Create `application-prod.properties` (not committed to git, listed in `.gitignore`):
```properties
app.jwt.secret=${JWT_SECRET}
token.encryption.key=${TOKEN_ENCRYPTION_KEY}
spring.ai.openai.api-key=${GEMINI_API_KEY}
app.resend.api-key=${RESEND_API_KEY}
google.oauth.client-id=${GOOGLE_CLIENT_ID}
google.oauth.client-secret=${GOOGLE_CLIENT_SECRET}
spring.data.mongodb.uri=${MONGODB_URI}
```
Run with: `java -jar app.jar --spring.profiles.active=prod`

### Option C: Docker Secrets / Kubernetes Secrets
```yaml
# docker-compose.yml
services:
  backend:
    environment:
      - JWT_SECRET_FILE=/run/secrets/jwt_secret
      - TOKEN_ENCRYPTION_KEY_FILE=/run/secrets/token_key
    secrets:
      - jwt_secret
      - token_key
secrets:
  jwt_secret:
    file: ./secrets/jwt_secret.txt
  token_key:
    file: ./secrets/token_key.txt
```

### Option D: HashiCorp Vault / AWS Secrets Manager
For enterprise deployments, integrate with a dedicated secrets manager:
```properties
# Spring Cloud Vault
spring.cloud.vault.uri=https://vault.example.com
spring.cloud.vault.authentication=token
```

---

## 4. Pre-Production Checklist

### Credential Rotation (URGENT — values appeared in git history)
- [ ] Revoke and reissue Gemini API key
- [ ] Generate a new JWT signing secret (`openssl rand -base64 32`)
- [ ] Generate a new AES-256 token encryption key (`openssl rand -base64 32`)
- [ ] Revoke and reissue Resend email API key
- [ ] Revoke and reissue Google OAuth client secret (create new production credentials)
- [ ] Revoke leaked ngrok authtoken in the ngrok dashboard

### Configuration
- [ ] Set all required environment variables listed in Section 1
- [ ] Verify `application.properties` contains NO plaintext secrets (`grep -v '\${' application.properties`)
- [ ] Add `application-prod.properties` and `.env` to `.gitignore`
- [ ] Update CORS origins from `localhost:5173` to production domain
- [ ] Enable HTTPS/TLS for all endpoints

### Infrastructure
- [ ] Enable MongoDB authentication and restrict network access
- [ ] Set `logging.level.org.springframework.data.mongodb.core.MongoTemplate` to `WARN` or `ERROR`
- [ ] Run `git filter-repo` or BFG to scrub leaked secrets from git history, then force-push
- [ ] Test the application with all externalized secrets before go-live

---

## 5. Additional Security Recommendations

1. **CORS Configuration**: Update `WebConfig.java` and `SecurityConfig.java` to use the production domain instead of `localhost:5173`
2. **CSRF Protection**: Re-evaluate CSRF disable; consider enabling for browser-based requests
3. **Rate Limiting**: Rate limiting is already applied to auth endpoints; verify limits are appropriate for production traffic
4. **HTTPS**: Enforce HTTPS in production (add `server.ssl.*` properties or terminate at a reverse proxy)
5. **MongoDB Security**: Enable MongoDB authentication and restrict network access
6. **Logging**: Replace `System.out.println` calls with SLF4J/Logback throughout the codebase
7. **Dependency Scanning**: Run OWASP dependency-check regularly
8. **Session Management**: Verify JWT access token (15 min) and refresh token (30 day) expiration settings are appropriate for your use case

