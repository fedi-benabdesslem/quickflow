# API Reference

Base URL: `http://localhost:8080`

All protected endpoints require `Authorization: Bearer <accessToken>` header.  
All endpoints return JSON. Errors use `{ "error": "message" }` format.

---

## Public Endpoints (no JWT required)

### `POST /api/auth/signup`
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

### `POST /api/auth/login`
Authenticate with email/password.

| Field | Type | Required |
|-------|------|----------|
| `email` | string | ✅ |
| `password` | string | ✅ |

**Success (200)**: Same as signup.  
**MFA Required (200)**: `{ "mfaRequired": true, "mfaToken": "..." }`  
**Errors**: `401` invalid credentials, `429` rate limited

---

### `POST /api/auth/verify-mfa`
Complete MFA verification.

| Field | Type | Required |
|-------|------|----------|
| `mfaToken` | string | ✅ |
| `code` | string | ✅ |

**Success (200)**: `{ "accessToken": "..." }`

---

### `POST /api/auth/refresh`
Refresh the access token using httpOnly cookie. No body required.

**Success (200)**: `{ "accessToken": "..." }` + new cookie  
**Errors**: `401` invalid/expired refresh token

---

### `POST /api/auth/forgot-password`
Send a password reset email.

| Field | Type |
|-------|------|
| `email` | string |

**Success (200)**: `{ "message": "If the email exists, a reset link was sent." }`  
Always returns 200 to prevent email enumeration.

---

### `POST /api/auth/reset-password`

| Field | Type |
|-------|------|
| `token` | string |
| `newPassword` | string |

**Success (200)**: `{ "message": "Password updated." }`

---

### `POST /api/auth/verify-email` / `POST /api/auth/send-verification`
Email verification flow. See Auth Architecture doc for details.

---

## OAuth2 Login

### `GET /oauth2/authorization/google`
### `GET /oauth2/authorization/microsoft`
Redirects browser to provider. After auth: `{FRONTEND_URL}/auth/callback?token=<JWT>`

---

## Protected Endpoints (JWT required)

### `POST /api/auth/logout`
**Success (200)**: `{ "message": "Logged out." }` + clears cookie

### `GET /api/auth/me`
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

### `GET /api/auth/providers`
`{ "providers": ["google", "microsoft"] }`

### `GET /api/auth/sessions`
```json
{
  "sessions": [
    { "id": "...", "deviceInfo": "Chrome/Win", "ipAddress": "1.2.3.4", "createdAt": "..." }
  ]
}
```

### `DELETE /api/auth/sessions/{id}`
`{ "message": "Session revoked." }`

---

## OAuth Account Linking

### `GET /api/auth/link/{provider}`
Get OAuth authorization URL for linking. Provider = `google` or `microsoft`.

**Success (200)**: `{ "status": "success", "authorizationUrl": "https://..." }`

### `GET /api/auth/link/callback?code=<code>&state=<state>`
OAuth callback — called by the provider redirect. Stores tokens in `AuthConnection`.

### `DELETE /api/auth/link`
Unlink all linked OAuth providers.

**Success (200)**: `{ "status": "success", "message": "Provider unlinked successfully." }`

---

## SMTP Configuration (`/api/user/smtp`)

### `POST /api/user/smtp/configure`
Validate and store an SMTP app-specific password.

| Field | Type | Description |
|-------|------|-------------|
| `appPassword` | string | The app-specific password from the email provider |

**Success (200)**: `{ "success": true, "message": "SMTP configured successfully." }`

### `GET /api/user/smtp/status`
Get SMTP config status and hosting provider detection result.

**Success (200)**:
```json
{
  "configured": true,
  "hostingProvider": "gmail",
  "smtpSupported": true,
  "action": "configured"
}
```

### `POST /api/user/smtp/test`
Send a test email to the user's own address to verify SMTP config.

**Success (200)**: `{ "success": true, "message": "Test email sent." }`

### `DELETE /api/user/smtp/config`
Remove SMTP configuration.

**Success (200)**: `{ "success": true }`

### `POST /api/user/smtp/skip`
Mark SMTP setup as skipped (user acknowledged but didn't configure).

**Success (200)**: `{ "success": true }`

---

## Voice Mode (`/api/minutes/voice`)

### `GET /api/minutes/voice/status`
Check if the transcription service is available.

**Success (200)**: `{ "available": true }`

### `POST /api/minutes/voice/transcribe`
Upload an audio file. Returns immediately with a `jobId`.

**Request**: `multipart/form-data` with `file` field.

**Success (200)**:
```json
{ "jobId": "abc123", "status": "processing", "audioDuration": 42.5 }
```

### `GET /api/minutes/voice/progress/{jobId}`
Poll job progress.

**Success (200)**:
```json
{ "jobId": "abc123", "status": "processing", "progress": 65, "stage": "diarizing" }
```

### `GET /api/minutes/voice/job/{jobId}`
Get final job status and result.

**Success (200)** (when completed):
```json
{
  "jobId": "abc123",
  "status": "completed",
  "segments": [{ "speaker": "SPEAKER_00", "start": 0.0, "end": 5.2, "text": "Hello..." }],
  "speakers": ["SPEAKER_00", "SPEAKER_01"],
  "fullText": "...",
  "processingTimeSeconds": 12.4
}
```

### `POST /api/minutes/voice/cancel/{jobId}`
Cancel a running job.

### `POST /api/minutes/voice/generate`
Generate meeting minutes from a completed transcript.

| Field | Description |
|-------|-------------|
| `transcript` | Full transcript text |
| `segments` | Array of speaker-labelled segments |

---

## Minutes (`/api/minutes`)

| Method | Endpoint | Description |
|---|---|---|
| `POST` | `/quick/extract` | Extract structured data from raw text |
| `POST` | `/quick/extract-file` | Extract from uploaded PDF/DOCX |
| `POST` | `/quick/generate` | Generate minutes from extracted data |
| `POST` | `/structured/generate` | Generate minutes from detailed structured form |
| `POST` | `/send` | Send minutes email with PDF |

---

## Contacts (`/api/contacts`)

| Method | Endpoint | Description |
|---|---|---|
| `GET` | `/` | List all contacts |
| `GET` | `/search?q=&limit=` | Autocomplete search |
| `GET` | `/recent?limit=` | Recently used contacts |
| `POST` | `/sync` | Sync from Google/Microsoft |
| `GET` | `/sync/status` | Sync status + last timestamp |
| `POST` | `/{id}/usage` | Increment usage counter |
| `PUT` | `/{id}/favorite` | Toggle favorite |

---

## Email (`/api/email`)

| Method | Endpoint | Description |
|---|---|---|
| `POST` | `/send` | Generate AI email draft |
| `POST` | `/send-final` | Send via Gmail/Outlook/SMTP |

---

## Meeting Templates (`/api/meeting-templates`)

| Method | Endpoint | Description |
|---|---|---|
| `POST` | `/` | Create template |
| `GET` | `/` | List templates |
| `GET` | `/{id}` | Get template |
| `PUT` | `/{id}` | Update template |
| `DELETE` | `/{id}` | Delete template |
| `POST` | `/{id}/track-usage` | Increment usage |

---

## PDF (`/api/pdf`)

| Method | Endpoint | Description |
|---|---|---|
| `POST` | `/generate` | Generate PDF from HTML |
| `GET` | `/preview/{id}` | Stream inline |
| `GET` | `/download/{id}` | Download attachment |

---

## System

| Method | Endpoint | Auth | Description |
|---|---|---|---|
| `GET` | `/api/health` | Public | Liveness probe |

---

## Rate Limits

| Endpoint | Limit |
|----------|-------|
| `/api/auth/login` | 5 requests/minute per IP |
| `/api/auth/signup` | 3 requests/minute per IP |
| `/api/auth/forgot-password` | 3 requests/minute per IP |
| `/api/auth/send-verification` | 3 requests/minute per IP |
