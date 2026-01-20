# Google OAuth Configuration for Keycloak

This guide provides step-by-step instructions for setting up Google Sign-In with Keycloak.

---

## Prerequisites

- Google account
- Keycloak running on http://localhost:8180
- Admin access to Keycloak

---

## Step 1: Create Google Cloud Project

1. **Go to Google Cloud Console**
   - URL: https://console.cloud.google.com/

2. **Create a new project**
   - Click the project dropdown at the top
   - Click **"New Project"**
   - Project name: `QuickFllow` (or any name you prefer)
   - Organization: Leave as default or select your organization
   - Click **"Create"**

3. **Wait for project creation**
   - You'll see a notification when the project is ready
   - Select the newly created project from the dropdown

---

## Step 2: Enable Google+ API

Google Sign-In requires the Google+ API to be enabled.

1. **Navigate to APIs & Services**
   - In the left sidebar, click **"APIs & Services"** → **"Library"**

2. **Search for Google+ API**
   - In the search bar, type: `Google+ API`
   - Click on **"Google+ API"** from the results

3. **Enable the API**
   - Click the **"Enable"** button
   - Wait for activation (takes a few seconds)

---

## Step 3: Configure OAuth Consent Screen

Before creating credentials, you need to configure the OAuth consent screen.

1. **Go to OAuth consent screen**
   - Left sidebar: **"APIs & Services"** → **"OAuth consent screen"**

2. **Choose user type**
   - Select **"External"** (for testing with any Google account)
   - Click **"Create"**

3. **Fill in App Information**
   - **App name:** `Chronoshift`
   - **User support email:** Your email address
   - **App logo:** (Optional) Upload your app logo
   - **Application home page:** `http://localhost:4200` (for development)
   - **Authorized domains:** Leave empty for local development

4. **Developer contact information**
   - **Email addresses:** Your email address

5. **Click "Save and Continue"**

6. **Scopes (Step 2)**
   - Click **"Add or Remove Scopes"**
   - Add these scopes:
     - `userinfo.email`
     - `userinfo.profile`
     - `openid`
   - Click **"Update"**
   - Click **"Save and Continue"**

7. **Test users (Step 3)**
   - Click **"Add Users"**
   - Add your email address and any other test users
   - Click **"Add"**
   - Click **"Save and Continue"**

8. **Summary (Step 4)**
   - Review your settings
   - Click **"Back to Dashboard"**

---

## Step 4: Create OAuth 2.0 Credentials

1. **Go to Credentials**
   - Left sidebar: **"APIs & Services"** → **"Credentials"**

2. **Create credentials**
   - Click **"+ CREATE CREDENTIALS"** at the top
   - Select **"OAuth client ID"**

3. **Configure OAuth client**
   - **Application type:** Select **"Web application"**
   - **Name:** `Chronoshift Keycloak` (or any descriptive name)

4. **Authorized JavaScript origins**
   - Click **"+ Add URI"**
   - Add: `http://localhost:8180`

5. **Authorized redirect URIs**
   - Click **"+ Add URI"**
   - Add: `http://localhost:8180/realms/quickflow-realm/broker/google/endpoint`
   
   ⚠️ **Important:** This URI must EXACTLY match what Keycloak expects. Pay attention to:
   - No trailing slash
   - Correct realm name (`quickflow-realm`)
   - Correct path (`/broker/google/endpoint`)

6. **Create the client**
   - Click **"Create"**

7. **Save your credentials**
   - A popup will show your credentials
   - **Client ID:** Copy this (looks like: `123456789-abc...xyz.apps.googleusercontent.com`)
   - **Client Secret:** Copy this (looks like: `GOCSPX-...`)
   - Click **"OK"**

   ⚠️ **Important:** Save these credentials securely. You'll need them in the next step.

---

## Step 5: Configure Keycloak Identity Provider

1. **Access Keycloak Admin Console**
   - URL: http://localhost:8180
   - Username: `admin`
   - Password: `admin`

2. **Select your realm**
   - In the top-left dropdown, select **`quickflow-realm`**
   - (If the realm doesn't exist, create it first)

3. **Add Google Identity Provider**
   - Left sidebar: Click **"Identity providers"**
   - Click **"Add provider"** dropdown
   - Select **"Google"**

4. **Configure Google provider**
   
   **Basic Settings:**
   - **Alias:** `google` (must be exactly this, lowercase)
   - **Display name:** `Google` (optional, for UI display)
   - **Enabled:** Toggle **ON**

   **OAuth Settings:**
   - **Client ID:** Paste your Google Client ID
   - **Client Secret:** Paste your Google Client Secret

   **Advanced Settings:**
   - **Trust Email:** Toggle **ON** (automatically verify email from Google)
   - **First Login Flow:** Select `first broker login`
   - **Sync Mode:** `import` (default)

5. **Save the configuration**
   - Click **"Save"** at the bottom

6. **Copy the Redirect URI**
   - After saving, scroll to the top of the page
   - You'll see **"Redirect URI"** with a value like:
     ```
     http://localhost:8180/realms/quickflow-realm/broker/google/endpoint
     ```
   - **Verify** this matches what you entered in Google Cloud Console
   - If it doesn't match, go back to Google Cloud Console and update it

---

## Step 6: Test the Integration

1. **Logout from Keycloak** (if you're logged in)
   - Click your username → **"Sign out"**

2. **Go to your application**
   - URL: http://localhost:4200

3. **Click Login**
   - You should be redirected to Keycloak login page

4. **Verify Google button appears**
   - You should see a **"Sign in with Google"** button or link
   - If you don't see it, check the Keycloak configuration

5. **Test Google Sign-In**
   - Click **"Sign in with Google"**
   - Select your Google account
   - Grant permissions if prompted
   - You should be redirected back to your app, logged in

---

## Troubleshooting

### Error: "redirect_uri_mismatch"

**Cause:** The redirect URI in Google Cloud Console doesn't match Keycloak's redirect URI.

**Solution:**
1. Go to Keycloak → Identity Providers → Google
2. Copy the exact **Redirect URI** shown at the top
3. Go to Google Cloud Console → Credentials → Your OAuth client
4. Update **Authorized redirect URIs** to match exactly
5. Save and try again

### Error: "Access blocked: This app's request is invalid"

**Cause:** OAuth consent screen not properly configured or Google+ API not enabled.

**Solution:**
1. Verify Google+ API is enabled (Step 2)
2. Check OAuth consent screen is configured (Step 3)
3. Add your email as a test user in OAuth consent screen
4. Make sure app is in "Testing" mode (not "Production")

### Google button doesn't appear on login page

**Cause:** Keycloak Identity Provider not enabled or misconfigured.

**Solution:**
1. Go to Keycloak → Identity Providers
2. Verify **Google** provider exists
3. Check that **Enabled** toggle is ON
4. Verify **Alias** is exactly `google` (lowercase)
5. Clear browser cache and try again

### Error: "Invalid client credentials"

**Cause:** Client ID or Client Secret is incorrect.

**Solution:**
1. Go to Google Cloud Console → Credentials
2. Click on your OAuth client
3. Verify Client ID and Client Secret
4. Copy them again and update in Keycloak
5. Click **Save** in Keycloak

### Users can't sign in after Google authentication

**Cause:** Email verification or user creation issue.

**Solution:**
1. In Keycloak → Identity Providers → Google
2. Make sure **Trust Email** is ON
3. Check **First Login Flow** is set to `first broker login`
4. Go to Users in Keycloak and verify the user was created

### Error: "User not found" or "User disabled"

**Cause:** User account issues in Keycloak.

**Solution:**
1. Go to Keycloak → Users
2. Search for the user's email
3. Check if user exists and is **Enabled**
4. If user doesn't exist, try signing in with Google again
5. Check Keycloak logs for errors

---

## Production Considerations

When deploying to production, you'll need to:

1. **Update OAuth consent screen**
   - Change from "Testing" to "Production" mode
   - Submit for Google verification (if needed)

2. **Update redirect URIs**
   - Add your production domain to Google Cloud Console
   - Example: `https://yourdomain.com/realms/quickflow-realm/broker/google/endpoint`

3. **Use HTTPS**
   - Google requires HTTPS for production OAuth
   - Update all URIs to use `https://`

4. **Secure credentials**
   - Store Client ID and Secret in environment variables
   - Never commit credentials to Git

---

## Additional Resources

- [Google OAuth 2.0 Documentation](https://developers.google.com/identity/protocols/oauth2)
- [Keycloak Identity Brokering Guide](https://www.keycloak.org/docs/latest/server_admin/#_identity_broker)
- [OAuth Consent Screen Setup](https://support.google.com/cloud/answer/10311615)

---

## Support

If you encounter issues not covered in this guide, please:
1. Check Keycloak server logs
2. Check browser console for errors
3. Verify all URLs and credentials are correct
4. Contact the development team
