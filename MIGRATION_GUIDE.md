# Migration Guide — Supabase to Self-Hosted Auth

> **Status: Migration Complete.**
> This guide applies to upgrading an **existing Supabase-based instance** to the self-hosted Spring Security + JWT system. New installations do not need to follow this guide — the new system is the default.

## Overview

This migration replaces Supabase Auth with a self-hosted Spring Security + JWT system. User data and OAuth connections are preserved via the `DataMigrationService`.

## Prerequisites

- MongoDB running and accessible
- Google OAuth credentials (client ID + secret)
- Resend API key for transactional emails

## Step 1: Configure Environment

### Backend (`application.properties`)

```properties
# JWT
jwt.secret=<YOUR_256_BIT_SECRET>
jwt.access-token.expiration=900000
jwt.refresh-token.expiration=604800000

# URLs
app.frontend.url=http://localhost:5173
app.backend.url=http://localhost:8080

# OAuth2 — Google
spring.security.oauth2.client.registration.google.client-id=<GOOGLE_CLIENT_ID>
spring.security.oauth2.client.registration.google.client-secret=<GOOGLE_CLIENT_SECRET>
spring.security.oauth2.client.registration.google.scope=openid,email,profile,\
  https://www.googleapis.com/auth/gmail.send,\
  https://www.googleapis.com/auth/gmail.readonly,\
  https://www.googleapis.com/auth/contacts.readonly

# Resend (transactional emails)
resend.api.key=<RESEND_API_KEY>
resend.from.email=noreply@yourdomain.com

# Encryption key for OAuth tokens at rest
encryption.secret.key=<32_BYTE_BASE64_KEY>
```

### Frontend (`.env`)

```env
VITE_BACKEND_URL=http://localhost:8080
```

> **Remove** `VITE_SUPABASE_URL` and `VITE_SUPABASE_ANON_KEY` — Supabase is no longer used.

## Step 2: OAuth Redirect URIs

Update your OAuth provider settings:

| Provider | Redirect URI |
|----------|-------------|
| Google (login) | `{BACKEND_URL}/login/oauth2/code/google` |
| Microsoft (login) | `{BACKEND_URL}/login/oauth2/code/microsoft` |
| Account Linking | `{BACKEND_URL}/api/auth/link/callback` |

## Step 3: Generate JWT Secret

```bash
openssl rand -base64 32
```

Set this as `jwt.secret` in `application.properties`.

## Step 4: Run Data Migration

The `DataMigrationService` converts existing `UserToken` documents into the new `User` model. Trigger it **once** after deploying on an existing instance:

```bash
# Via HTTP (if you expose the admin endpoint temporarily)
curl -X POST http://localhost:8080/api/admin/migrate

# Or trigger automatically at startup with --migrate flag
mvn spring-boot:run -Dspring-boot.run.arguments=--migrate
```

The migration:
- Creates `User` documents from existing `UserToken` documents
- Preserves OAuth tokens as `AuthConnection` entries (re-encrypted with AES-256)
- Preserves SMTP configuration
- Skips users that already exist in the `users` collection
- Keeps legacy `UserToken` documents as backup in `user_tokens` collection

## Step 5: Remove Supabase

After verifying the migration:

1. ✅ Remove `@supabase/supabase-js` from frontend `package.json`
2. ✅ Delete `frontend/src/lib/supabase.ts`
3. ✅ Delete `SupabaseJwtFilter.java`
4. ✅ Remove Supabase environment variables from all configs and deployment pipelines

## Step 6: Verify

```bash
# Backend — should compile with no errors
cd backend && mvn compile

# Frontend — should build with no errors
cd frontend && npm run build

# Check no Supabase references remain
grep -r "supabase" frontend/src/          # should be empty
grep -r "SupabaseJwt" backend/src/main/   # should be empty
```

## Breaking Changes

| Change | Impact |
|--------|--------|
| All sessions invalidated | Users must log in again |
| JWT format changed | Old JWTs will not validate |
| `supabaseId` → `userId` | Internal only, no API impact |
| Refresh tokens in httpOnly cookies | Frontend cannot access refresh tokens directly |
| `UserToken` legacy fallback removed | Services now use `User.authConnections` exclusively |

## Rollback Plan

> **Warning:** Legacy `UserToken` fallback code has been removed from all services. Rolling back requires reverting to a prior commit.

1. Legacy `UserToken` documents remain in `user_tokens` collection as backup
2. Re-add `SupabaseJwtFilter` and revert `SecurityConfig` from git history
3. The original `TokenStorageService` checked `user_tokens` first — restore from git if needed
