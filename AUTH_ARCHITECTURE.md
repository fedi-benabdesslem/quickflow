# Authentication Architecture

## Overview

QuickFlow uses a **self-hosted Spring Security + JWT** authentication system. Users authenticate via email/password or OAuth2 (Google, Microsoft), receive a JWT access token + httpOnly refresh token cookie, and the frontend manages token lifecycle automatically.

## Architecture Diagram

```
┌─────────────────┐         ┌──────────────────────────────────┐
│   Frontend      │         │   Backend (Spring Boot)           │
│   (React/Vite)  │         │                                  │
│                 │         │  ┌────────────┐                  │
│  tokenManager ──┼─Bearer──┼──► JwtAuthFilter                │
│                 │         │  │  validates JWT                │
│  AuthContext ───┼─fetch───┼──► AuthController                │
│                 │         │  │  signup/login/refresh/logout   │
│                 │         │  │                                │
│                 │◄─cookie──┼──► OAuthSuccessHandler           │
│                 │         │  │  Google/Microsoft callback     │
│                 │         │  │                                │
│                 │         │  ┌────────────┐                  │
│                 │         │  │ TokenService │ JWT generation  │
│                 │         │  │ AuthService  │ Business logic  │
│                 │         │  └────────────┘                  │
│                 │         │                                  │
│                 │         │  ┌────────────┐                  │
│                 │         │  │ MongoDB     │                 │
│                 │         │  │ - users     │                 │
│                 │         │  │ - sessions  │                 │
│                 │         │  │ - user_tokens (legacy)        │
│                 │         │  └────────────┘                  │
└─────────────────┘         └──────────────────────────────────┘
```

## Token Lifecycle

| Token | Storage | Lifetime | Purpose |
|-------|---------|----------|---------|
| **Access Token** | In-memory + localStorage | 15 minutes | API authorization via `Authorization: Bearer <token>` |
| **Refresh Token** | httpOnly cookie | 7 days | Silent access token renewal |
| **Linking Token** | URL state param | 10 minutes | OAuth account linking HMAC-signed token |
| **Email Token** | Email link | 24 hours | Password reset / email verification |

## Authentication Flows

### Email/Password Login
1. Frontend `POST /api/auth/login` with `{email, password}`
2. Backend validates credentials, generates JWT + refresh token
3. Response: `{ accessToken }` + `Set-Cookie: refreshToken=...; HttpOnly; Secure`
4. Frontend stores access token via `tokenManager`

### OAuth2 Login (Google / Microsoft)
1. Frontend redirects to `{BACKEND}/oauth2/authorization/google`
2. Spring Security handles OAuth2 code exchange
3. `OAuthSuccessHandler` creates/finds user, generates JWT
4. Redirects to `{FRONTEND}/auth/callback?token=<JWT>`
5. `AuthCallbackPage` extracts token, stores via `tokenManager`

### Silent Token Refresh
1. `tokenManager` detects token expiring within 2 minutes
2. `POST /api/auth/refresh` with httpOnly cookie (no body needed)
3. Backend validates refresh token, rotates both tokens
4. Response: new access token + new refresh cookie

### Account Linking (for email-sending)
1. Authenticated user requests `GET /api/auth/link/google`
2. Backend generates HMAC-signed linking token, returns OAuth URL
3. User authorizes, callback stores tokens in `AuthConnection`
4. User can now send email via the linked provider

## Data Models

### User (MongoDB: `users`)
Primary identity model. Contains:
- **Identity**: email (unique), name, role
- **Local auth**: bcrypt password hash
- **OAuth connections**: List of `AuthConnection` embedded documents
- **MFA**: TOTP secret (encrypted), recovery codes (hashed)
- **Email config**: SMTP password (encrypted), detected hosting provider
- **Subscription**: plan, trial expiry, Stripe/Konnect IDs

### AuthConnection (embedded in User)
Represents one OAuth provider connection:
- `provider`: "google" or "microsoft"
- `connectionType`: "primary" (signup) or "linked" (added later)
- `accessTokenEncrypted`, `refreshTokenEncrypted`: AES-256 encrypted
- `providerEmail`, `grantedScopes`, `connectedAt`

### UserSession (MongoDB: `sessions`)
Server-side session tracking:
- `userId`, `refreshTokenHash` (bcrypt), `deviceInfo`, `ipAddress`
- TTL index for auto-expiry

## Security Measures

- **Passwords**: bcrypt with cost factor 12
- **JWT**: HMAC-SHA256, 256-bit secret
- **OAuth tokens**: AES-256 encryption at rest
- **Refresh tokens**: bcrypt-hashed server-side, httpOnly cookies
- **Rate limiting**: In-memory per-IP (login: 5/min, signup: 3/min)
- **Security headers**: X-Content-Type-Options, X-Frame-Options, X-XSS-Protection, Referrer-Policy
- **CORS**: Configured per-environment with credentials support
