# API Reference — Authentication Endpoints

Base URL: `/api/auth`

All endpoints return JSON. Errors use `{ "error": "message" }` format.

---

## Public Endpoints (no JWT required)

### `POST /signup`
Create a new account with email/password.

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `email` | string | ✅ | User's email address |
| `password` | string | ✅ | Min 8 characters |
| `name` | string | ❌ | Display name |

**Success (200)**:
```json
{ "accessToken": "eyJ...", "user": { "id": "...", "email": "...", "name": "..." } }
```
Sets `refreshToken` httpOnly cookie.

**Errors**: `409` email exists, `400` validation, `429` rate limited

---

### `POST /login`
Authenticate with email/password.

| Field | Type | Required |
|-------|------|----------|
| `email` | string | ✅ |
| `password` | string | ✅ |

**Success (200)**: Same as signup.  
**MFA Required (200)**: `{ "mfaRequired": true, "mfaToken": "..." }`  
**Errors**: `401` invalid credentials, `429` rate limited

---

### `POST /verify-mfa`
Complete MFA verification.

| Field | Type | Required |
|-------|------|----------|
| `mfaToken` | string | ✅ |
| `code` | string | ✅ |

**Success (200)**: `{ "accessToken": "..." }`

---

### `POST /refresh`
Refresh the access token using httpOnly cookie.

No body required. Refresh token sent automatically via cookie.

**Success (200)**: `{ "accessToken": "..." }` + new cookie  
**Errors**: `401` invalid/expired refresh token

---

### `POST /forgot-password`
Send a password reset email.

| Field | Type | Required |
|-------|------|----------|
| `email` | string | ✅ |

**Success (200)**: `{ "message": "If the email exists, a reset link was sent." }`  
Always returns 200 to prevent email enumeration.

---

### `POST /reset-password`
Reset password using token from email.

| Field | Type | Required |
|-------|------|----------|
| `token` | string | ✅ |
| `newPassword` | string | ✅ |

**Success (200)**: `{ "message": "Password updated." }`  
**Errors**: `400` invalid/expired token

---

### `POST /verify-email`
Verify email using token from verification email.

| Field | Type | Required |
|-------|------|----------|
| `token` | string | ✅ |

**Success (200)**: `{ "message": "Email verified." }`

---

### `POST /send-verification`
Resend the email verification link.

| Field | Type | Required |
|-------|------|----------|
| `email` | string | ✅ |

**Success (200)**: `{ "message": "Verification email sent." }`

---

## OAuth2 Login

### `GET /oauth2/authorization/google`
### `GET /oauth2/authorization/microsoft`
Initiates OAuth2 flow. Redirect the browser to this URL.  
After successful auth, redirects to: `{FRONTEND_URL}/auth/callback?token=<JWT>`

---

## Protected Endpoints (JWT required)

All require `Authorization: Bearer <accessToken>` header.

### `POST /logout`
Invalidate the current session.

**Success (200)**: `{ "message": "Logged out." }`  
Clears `refreshToken` cookie.

---

### `GET /me`
Get current user info.

**Success (200)**:
```json
{
  "id": "...",
  "email": "user@example.com",
  "name": "John",
  "role": "USER",
  "emailVerified": true,
  "providers": ["google"],
  "plan": "free_trial"
}
```

---

### `GET /providers`
List connected OAuth providers.

**Success (200)**: `{ "providers": ["google", "microsoft"] }`

---

### `GET /sessions`
List all active sessions.

**Success (200)**:
```json
{
  "sessions": [
    { "id": "...", "deviceInfo": "Chrome/Win", "ipAddress": "1.2.3.4", "createdAt": "..." }
  ]
}
```

---

### `DELETE /sessions/{id}`
Revoke a specific session.

**Success (200)**: `{ "message": "Session revoked." }`

---

## OAuth Account Linking

### `GET /link/{provider}`
Get OAuth authorization URL for linking. Provider: `google` or `microsoft`.

**Success (200)**: `{ "status": "success", "authorizationUrl": "https://..." }`

### `GET /link/callback`
OAuth callback for account linking (called by provider redirect, not frontend).

### `DELETE /link`
Unlink all linked OAuth providers.

**Success (200)**: `{ "status": "success", "message": "Provider unlinked successfully." }`

---

## Rate Limits

| Endpoint | Limit |
|----------|-------|
| `/login` | 5 requests/minute per IP |
| `/signup` | 3 requests/minute per IP |
| `/forgot-password` | 3 requests/minute per IP |
| `/send-verification` | 3 requests/minute per IP |
