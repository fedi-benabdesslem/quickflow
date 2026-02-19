# Authentication Architecture

## Overview

QuickFlow uses a **self-hosted Spring Security + JWT** authentication system. Users authenticate via email/password or OAuth2 (Google, Microsoft), receive a JWT access token + httpOnly refresh token cookie, and the frontend manages token lifecycle automatically.

## Architecture Diagram

```
┌───────────────────────┐         ┌──────────────────────────────────────┐
│   Frontend            │         │   Backend (Spring Boot)               │
│   (React/Vite)        │         │                                      │
│                       │         │  ┌─────────────┐                    │
│  tokenManager.ts ─────┼─Bearer──┼─►│ JwtAuthFilter│                   │
│                       │         │  │ validates JWT│                    │
│  AuthContext ─────────┼─fetch───┼─►│ AuthController                   │
│                       │         │  │ signup/login/refresh/logout       │
│                       │         │  │                                   │
│                       │◄─cookie─┼──│ OAuthSuccessHandler               │
│                       │         │  │ Google/Microsoft callback          │
│                       │         │  │                                   │
│                       │         │  │ OAuthLinkingController            │
│                       │         │  │ link Google/MS to email account   │
│                       │         │  │                                   │
│                       │         │  ┌─────────────┐                    │
│                       │         │  │ TokenService │ JWT generation     │
│                       │         │  │ AuthService  │ Business logic     │
│                       │         │  └─────────────┘                    │
│                       │         │                                      │
│                       │         │  ┌─────────────┐                    │
│                       │         │  │ MongoDB      │                    │
│                       │         │  │ - users      │                    │
│                       │         │  │ - sessions   │                    │
│                       │         │  └─────────────┘                    │
└───────────────────────┘         └──────────────────────────────────────┘
```

## Token Lifecycle

| Token | Storage | Lifetime | Purpose |
|-------|---------|----------|---------|
| **Access Token** | In-memory + localStorage (`qf_access_token`) | 15 minutes | API authorization via `Authorization: Bearer <token>` |
| **Refresh Token** | httpOnly cookie (`SameSite=None; Secure`) | 7 days | Silent access token renewal |
| **Linking Token** | URL state param (HMAC-signed) | 10 minutes | OAuth account linking security token |
| **Email Token** | Email link | 24 hours | Password reset / email verification |

> **Ngrok note**: The refresh cookie requires `SameSite=None; Secure` (HTTPS). In development, ngrok provides the required HTTPS tunnel so the browser accepts the cookie from the backend domain.

## Authentication Flows

### Email/Password Login
1. Frontend `POST /api/auth/login` with `{email, password}`
2. Backend validates credentials, generates JWT + refresh token
3. Response: `{ accessToken }` + `Set-Cookie: refreshToken=...; HttpOnly; Secure; SameSite=None`
4. Frontend stores access token via `tokenManager`

### OAuth2 Login (Google / Microsoft)
1. Frontend redirects to `{BACKEND}/oauth2/authorization/google`
2. Spring Security handles OAuth2 code exchange
3. `OAuthSuccessHandler` creates/finds user, stores tokens in `AuthConnection`, generates JWT
4. Redirects to `{FRONTEND}/auth/callback?token=<JWT>`
5. `AuthCallbackPage` extracts token, stores via `tokenManager`

### Silent Token Refresh
1. `tokenManager` detects token expiring within 2 minutes
2. `POST /api/auth/refresh` with httpOnly cookie (no body needed)
3. Backend validates refresh token, rotates both tokens
4. Response: new access token + new refresh cookie

### Account Linking (for email-sending)
1. Authenticated user requests `GET /api/auth/link/google`
2. Backend generates HMAC-signed state token, returns OAuth authorization URL
3. User authorizes — Google redirects to `/api/auth/link/callback`
4. Backend stores encrypted tokens in `User.authConnections`
5. User can now send email via the linked provider (Gmail API / MS Graph)

### SMTP Configuration (alternative to OAuth linking)
For users whose provider isn't Google or Microsoft:
1. `DomainDetectionService` performs DNS MX lookup on user's email domain
2. Detects known SMTP provider (Gmail, Outlook, Yahoo, etc.)
3. User generates an app-specific password in their email provider settings
4. `POST /api/user/smtp/configure` — password is AES-256 encrypted and stored
5. `EmailProviderService` routes email through `SmtpEmailService` (STARTTLS)

## Data Models

### User (MongoDB: `users`)
Primary identity model. Contains:
- **Identity**: email (unique), name, role
- **Local auth**: bcrypt password hash (null for OAuth-only users)
- **OAuth connections**: `List<AuthConnection>` embedded documents
- **MFA**: TOTP secret (AES-encrypted), recovery codes (bcrypt-hashed)
- **Email config**: SMTP app password (AES-encrypted), detected hosting provider
- **Subscription**: plan, trial expiry

### AuthConnection (embedded in User)
Represents one OAuth provider connection:
- `provider`: `"google"` or `"microsoft"`
- `connectionType`: `"primary"` (signed up via OAuth) or `"linked"` (linked later)
- `accessTokenEncrypted`, `refreshTokenEncrypted`: AES-256 encrypted
- `providerEmail`, `grantedScopes`, `connectedAt`

### UserSession (MongoDB: `sessions`)
Server-side session tracking:
- `userId`, `refreshTokenHash` (bcrypt), `deviceInfo`, `ipAddress`
- TTL index for auto-expiry

## Security Measures

- **Passwords**: bcrypt with cost factor 12
- **JWT**: HMAC-SHA256, 256-bit secret (`jwt.secret`)
- **OAuth tokens**: AES-256 encryption at rest (`encryption.secret.key`)
- **Refresh tokens**: bcrypt-hashed server-side, httpOnly cookies
- **Rate limiting**: In-memory per-IP (login: 5/min, signup: 3/min)
- **Security headers**: X-Content-Type-Options, X-Frame-Options: DENY, X-XSS-Protection, Referrer-Policy
- **CORS**: Configured per-environment with credentials support
