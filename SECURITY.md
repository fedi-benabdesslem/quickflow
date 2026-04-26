# Security Policy

## Authentication Security

### Password Storage
- **Algorithm**: bcrypt with cost factor 12
- **Enforcement**: Minimum 8 characters required at signup

### JWT Tokens
- **Algorithm**: HMAC-SHA256
- **Access token lifetime**: 15 minutes
- **Refresh token lifetime**: 30 days
- **Secret**: 256-bit, configured via `app.jwt.secret` property
- **Claims**: `sub` (userId), `email`, `role`, `iat`, `exp`

### Refresh Tokens
- **Storage (server)**: bcrypt hash in `sessions` collection
- **Storage (client)**: httpOnly, Secure, SameSite=None cookie (required for cross-origin OAuth flows)
- **Rotation**: New refresh token issued on each refresh
- **Revocation**: Old session invalidated on rotation

### OAuth Tokens
- **Encryption**: AES-256 at rest in MongoDB (`AuthConnection` embedded in `User`)
- **Key**: `token.encryption.key` in application properties (set via `TOKEN_ENCRYPTION_KEY` env var)
- **Scopes requested**: `openid`, `email`, `profile`, `gmail.send`, `gmail.readonly`, `contacts.readonly` (Google); `Mail.Send`, `User.Read`, `Contacts.Read` (Microsoft)

## Rate Limiting

In-memory rate limiter per IP address:

| Endpoint | Requests | Window |
|----------|----------|--------|
| `/api/auth/login` | 5 | 60 seconds |
| `/api/auth/signup` | 3 | 60 seconds |
| `/api/auth/forgot-password` | 3 | 60 seconds |
| `/api/auth/send-verification` | 3 | 60 seconds |

## HTTP Security Headers

Applied to all responses via `SecurityHeadersFilter`:

| Header | Value |
|--------|-------|
| `X-Content-Type-Options` | `nosniff` |
| `X-Frame-Options` | `DENY` |
| `X-XSS-Protection` | `1; mode=block` |
| `Referrer-Policy` | `strict-origin-when-cross-origin` |
| `Cache-Control` | `no-store` (auth endpoints) |

## CORS Configuration

- **Allowed origins**: Configured per-environment (`app.frontend.url`)
- **Credentials**: Allowed (for httpOnly cookies)
- **Allowed methods**: GET, POST, PUT, DELETE, OPTIONS
- **Exposed headers**: None (tokens sent via JSON body or cookies)

## MFA (Multi-Factor Authentication)

- **Type**: TOTP (Time-based One-Time Password)
- **Secret storage**: AES-256 encrypted
- **Recovery codes**: bcrypt hashed, single-use
- **Status**: Infrastructure in place, UI pending

## Data Protection

### Encrypted at Rest (AES-256)
- OAuth access tokens and refresh tokens (`User.authConnections`)
- SMTP app-specific passwords (`User.smtpPasswordEncrypted`)
- MFA TOTP secrets (`User.mfaSecretEncrypted`)

### Hashed (Irreversible)
- User passwords (bcrypt, cost 12)
- Refresh tokens (bcrypt, server-side in `sessions`)
- MFA recovery codes (bcrypt)

### Not Stored
- Plain-text passwords (never stored)
- Plain-text OAuth tokens (always encrypted before persistence)

## Vulnerability Reporting

If you discover a security vulnerability, please report it responsibly:
1. **Do not** open a public issue
2. Contact the maintainers directly
3. Provide detailed reproduction steps
4. Allow reasonable time for a fix before disclosure
