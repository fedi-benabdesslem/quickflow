# Production Security Checklist

## Overview
This document lists all exposed secrets and credentials in the QuickFlow codebase that **MUST** be externalized before deploying to production. During development, these values are shared for contributor convenience, but they represent critical security risks in production.

---

## 1. Secrets in `backend/src/main/resources/application.properties`

### 1.1 Gemini API Key (OpenAI-compatible)
- **Property:** `spring.ai.openai.api-key`
- **Current Value:** Hardcoded Google AI API key
- **Risk:** API abuse, unexpected billing charges
- **Action:** Externalize via environment variable
  ```properties
  spring.ai.openai.api-key=${GEMINI_API_KEY}
  ```
  ```bash
  export GEMINI_API_KEY=your-production-api-key
  ```

### 1.2 Supabase JWT Secret
- **Property:** `supabase.jwt.secret`
- **Current Value:** Uses `${SUPABASE_JWT_SECRET:...}` with hardcoded fallback
- **Risk:** JWT token forgery, authentication bypass
- **Action:** Remove the hardcoded fallback in production
  ```properties
  supabase.jwt.secret=${SUPABASE_JWT_SECRET}
  ```
  ```bash
  export SUPABASE_JWT_SECRET=your-production-jwt-secret
  ```

### 1.3 Token Encryption Key
- **Property:** `token.encryption.key`
- **Current Value:** Uses `${TOKEN_ENCRYPTION_KEY:...}` with hardcoded fallback
- **Risk:** OAuth token decryption, account takeover
- **Action:** Remove the hardcoded fallback in production; generate a new key
  ```properties
  token.encryption.key=${TOKEN_ENCRYPTION_KEY}
  ```
  ```bash
  # Generate a new 32-byte key:
  openssl rand -base64 32
  export TOKEN_ENCRYPTION_KEY=your-new-base64-key
  ```

### 1.4 Google OAuth Client ID & Secret
- **Property:** `google.oauth.client-id`, `google.oauth.client-secret`
- **Current Value:** Uses `${GOOGLE_OAUTH_CLIENT_ID:...}` and `${GOOGLE_OAUTH_CLIENT_SECRET:...}` with hardcoded fallbacks
- **Risk:** OAuth credential theft, impersonation
- **Action:** Remove hardcoded fallbacks in production
  ```properties
  google.oauth.client-id=${GOOGLE_OAUTH_CLIENT_ID}
  google.oauth.client-secret=${GOOGLE_OAUTH_CLIENT_SECRET}
  ```
  ```bash
  export GOOGLE_OAUTH_CLIENT_ID=your-production-client-id
  export GOOGLE_OAUTH_CLIENT_SECRET=your-production-client-secret
  ```

### 1.5 Microsoft OAuth Configuration
- **Property:** `microsoft.oauth.client-id`, `microsoft.oauth.client-secret`, `microsoft.oauth.tenant-id`
- **Current Value:** Already externalized via `${MICROSOFT_CLIENT_ID:}` etc.
- **Status:** ✅ Properly configured (no hardcoded fallback)
- **Action:** Ensure environment variables are set in production

---

## 2. Secrets in Frontend Code

### 2.1 Supabase URL and Anonymous Key
- **File:** `frontend/src/lib/supabase.ts` (lines 9-10)
- **Current Value:** Hardcoded Supabase project URL and anonymous key
- **Risk:** The anon key is designed to be public (client-side), but the URL should be configurable
- **Note:** Supabase anon keys are safe for client-side use by design (Row Level Security enforces access control). However, for multi-environment deployment, externalize these:
  ```typescript
  const supabaseUrl = import.meta.env.VITE_SUPABASE_URL || 'https://your-project.supabase.co'
  const supabaseAnonKey = import.meta.env.VITE_SUPABASE_ANON_KEY || ''
  ```
  Then set in `.env.production`:
  ```
  VITE_SUPABASE_URL=https://your-production-project.supabase.co
  VITE_SUPABASE_ANON_KEY=your-production-anon-key
  ```

### 2.2 Supabase URL and Anon Key in Backend JWT Filter
- **File:** `backend/src/main/java/com/ai/application/Config/SupabaseJwtFilter.java` (lines 35-38)
- **Current Value:** Hardcoded defaults in `@Value` annotations
- **Action:** Remove hardcoded defaults in production
  ```properties
  supabase.url=${SUPABASE_URL}
  supabase.anon.key=${SUPABASE_ANON_KEY}
  ```

---

## 3. Secret Externalization Methods

### Option A: Environment Variables (Simplest)
```bash
# Add to your deployment environment or .env file (not committed to git)
export GEMINI_API_KEY=...
export SUPABASE_JWT_SECRET=...
export TOKEN_ENCRYPTION_KEY=...
export GOOGLE_OAUTH_CLIENT_ID=...
export GOOGLE_OAUTH_CLIENT_SECRET=...
export MICROSOFT_CLIENT_ID=...
export MICROSOFT_CLIENT_SECRET=...
export MICROSOFT_TENANT_ID=...
export SUPABASE_URL=...
export SUPABASE_ANON_KEY=...
```

### Option B: Spring Boot Profile-Based Configuration
Create `application-prod.properties` (not committed to git):
```properties
spring.ai.openai.api-key=${GEMINI_API_KEY}
supabase.jwt.secret=${SUPABASE_JWT_SECRET}
token.encryption.key=${TOKEN_ENCRYPTION_KEY}
google.oauth.client-id=${GOOGLE_OAUTH_CLIENT_ID}
google.oauth.client-secret=${GOOGLE_OAUTH_CLIENT_SECRET}
```
Run with: `java -jar app.jar --spring.profiles.active=prod`

### Option C: Docker Secrets / Kubernetes Secrets
```yaml
# docker-compose.yml
services:
  backend:
    environment:
      - GEMINI_API_KEY_FILE=/run/secrets/gemini_api_key
    secrets:
      - gemini_api_key
secrets:
  gemini_api_key:
    file: ./secrets/gemini_api_key.txt
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

- [ ] Generate new API keys for production (do NOT reuse dev keys)
- [ ] Generate a new AES-256 encryption key for `TOKEN_ENCRYPTION_KEY`
- [ ] Create new Google OAuth credentials for production redirect URIs
- [ ] Create new Microsoft OAuth credentials for production
- [ ] Create a new Supabase project (or rotate keys) for production
- [ ] Set all secrets via environment variables or secret manager
- [ ] Remove hardcoded fallback values from `application.properties`
- [ ] Verify `application.properties` contains NO plaintext secrets
- [ ] Add `.env` and `application-prod.properties` to `.gitignore`
- [ ] Update CORS origins from `localhost:5173` to production domain
- [ ] Enable HTTPS/TLS for all endpoints
- [ ] Review and restrict MongoDB access (authentication + network binding)
- [ ] Set `logging.level.org.springframework.data.mongodb.core.MongoTemplate` to `WARN` or `ERROR`
- [ ] Rotate the Supabase JWT secret in the Supabase dashboard
- [ ] Test the application with all externalized secrets before go-live

---

## 5. Additional Security Recommendations

1. **CORS Configuration**: Update `WebConfig.java` and `SecurityConfig.java` to use production domain instead of `localhost:5173`
2. **CSRF Protection**: Re-evaluate CSRF disable; consider enabling for browser-based requests
3. **Rate Limiting**: Add rate limiting to authentication and email endpoints
4. **HTTPS**: Enforce HTTPS in production (add `server.ssl.*` properties)
5. **MongoDB Security**: Enable MongoDB authentication and restrict network access
6. **Logging**: Replace `System.out.println` with a proper logging framework (SLF4J/Logback) throughout the codebase
7. **Dependency Scanning**: Run OWASP dependency-check regularly
8. **Session Management**: Verify JWT token expiration settings are appropriate for production
