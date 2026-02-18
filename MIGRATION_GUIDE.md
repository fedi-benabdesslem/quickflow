# Migration Guide — Supabase to Self-Hosted Auth

## Overview

This guide covers migrating from Supabase Auth to the self-hosted Spring Security + JWT system. The migration preserves existing user data and OAuth connections.

## Prerequisites

- MongoDB running and accessible
- Google OAuth credentials (client ID + secret)
- Microsoft OAuth credentials (client ID + secret)
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
app.base-url=http://localhost:8080

# OAuth2 — Google
spring.security.oauth2.client.registration.google.client-id=<GOOGLE_CLIENT_ID>
spring.security.oauth2.client.registration.google.client-secret=<GOOGLE_CLIENT_SECRET>
spring.security.oauth2.client.registration.google.scope=email,profile,openid,https://www.googleapis.com/auth/gmail.send

# OAuth2 — Microsoft
spring.security.oauth2.client.registration.microsoft.client-id=<MICROSOFT_CLIENT_ID>
spring.security.oauth2.client.registration.microsoft.client-secret=<MICROSOFT_CLIENT_SECRET>
spring.security.oauth2.client.registration.microsoft.scope=email,profile,openid,Mail.Send,User.Read

# Resend (transactional emails)
resend.api.key=<RESEND_API_KEY>
resend.from.email=noreply@yourdomain.com
```

### Frontend (`.env`)

```
VITE_BACKEND_URL=http://localhost:8080
```

## Step 2: OAuth Redirect URIs

Update your OAuth provider settings:

| Provider | Redirect URI |
|----------|-------------|
| Google | `{BACKEND_URL}/login/oauth2/code/google` |
| Microsoft | `{BACKEND_URL}/login/oauth2/code/microsoft` |
| Account Linking | `{BACKEND_URL}/api/auth/link/callback` |

## Step 3: Generate JWT Secret

Generate a secure 256-bit secret:

```bash
openssl rand -base64 32
```

Set this as `jwt.secret` in `application.properties`.

## Step 4: Run Data Migration

The `DataMigrationService` converts existing `UserToken` documents into the new `User` model. Trigger it once after deploying:

```java
@RestController
@RequestMapping("/api/admin")
public class AdminController {
    
    private final DataMigrationService migrationService;
    
    @PostMapping("/migrate")
    public ResponseEntity<?> runMigration() {
        var result = migrationService.migrateAllUsers();
        return ResponseEntity.ok(result);
    }
}
```

Or trigger via a startup runner:

```java
@Component
public class MigrationRunner implements CommandLineRunner {
    
    private final DataMigrationService migrationService;
    
    @Override
    public void run(String... args) {
        if (Arrays.asList(args).contains("--migrate")) {
            migrationService.migrateAllUsers();
        }
    }
}
```

The migration:
- Creates `User` documents from `UserToken` documents
- Preserves OAuth tokens as `AuthConnection` entries
- Preserves SMTP configuration
- Skips users that already exist in the new collection
- Keeps legacy `UserToken` documents as backup

## Step 5: Remove Supabase

After verifying the migration:

1. Remove `@supabase/supabase-js` from frontend `package.json` ✅ (already done)
2. Delete `frontend/src/lib/supabase.ts` ✅ (already done)
3. Delete `SupabaseJwtFilter.java` ✅ (already done)
4. Remove Supabase environment variables from deployment configs

## Step 6: Verify

```bash
# Backend — should compile with no errors
cd backend && mvn compile

# Frontend — should build with no errors
cd frontend && npm run build

# Check no Supabase references remain
grep -r "supabase" frontend/src/          # should be empty
grep -r "supabaseId" backend/src/main/    # only in legacy UserToken/TokenStorageService
```

## Breaking Changes

| Change | Impact |
|--------|--------|
| All sessions invalidated | Users must log in again |
| JWT format changed | Old JWTs will not validate |
| `supabaseId` → `userId` | Internal only, no API impact |
| `Session.access_token` → `Session.accessToken` | Frontend type change |
| Refresh tokens in httpOnly cookies | Frontend cannot access refresh tokens |

## Rollback Plan

If issues arise:
1. Legacy `UserToken` documents are preserved in `user_tokens` collection
2. `TokenStorageService`, `GmailService`, `MicrosoftGraphService` still support legacy model
3. Re-add `SupabaseJwtFilter` and revert `SecurityConfig` if needed
