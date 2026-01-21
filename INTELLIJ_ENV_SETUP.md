# IntelliJ IDEA - Environment Variables Setup

This guide shows you how to set up environment variables in IntelliJ IDEA for the QuickFlow backend.

---

## Step 1: Open Run/Debug Configurations

1. **Locate the run configuration dropdown**
   - Look at the top-right of IntelliJ IDEA
   - You'll see a dropdown with your application name (e.g., `AiApplication`)

2. **Open Edit Configurations**
   - Click the dropdown
   - Select **"Edit Configurations..."**
   - Or use keyboard shortcut: `Alt+Shift+F10` → `0` (Windows/Linux) or `Ctrl+Option+R` → `0` (Mac)

---

## Step 2: Add Environment Variables

1. **Select your Spring Boot configuration**
   - In the left panel, find your application under **"Spring Boot"**
   - Click on it (e.g., `AiApplication`)

2. **Find Environment Variables field**
   - Scroll down in the configuration panel
   - Look for **"Environment variables"** field
   - It should be empty or show existing variables

3. **Open the Environment Variables dialog**
   - Click the **folder icon** 📁 next to the field
   - Or click directly in the field and press `Shift+Enter`

4. **Add the variables**
   - Click the **"+"** button to add a new variable
   - Add these two variables:

   ```
   Name: MAIL_USERNAME
   Value: your-email@gmail.com
   ```

   ```
   Name: MAIL_PASSWORD
   Value: your-gmail-app-password
   ```

   **Important:** Replace with your actual Gmail credentials!

5. **Click OK**
   - Click **"OK"** in the Environment Variables dialog
   - Click **"OK"** or **"Apply"** in the Run/Debug Configurations dialog

---

## Step 3: Get Your Gmail App Password

1. **Go to Google Account**
   - URL: https://myaccount.google.com/apppasswords
   - Sign in with your Gmail account

2. **Create App Password**
   - App name: `Chronoshift` (or any name)
   - Click **"Create"**

3. **Copy the password**
   - You'll see a 16-character password (format: `xxxx xxxx xxxx xxxx`)
   - Copy it
   - **Remove the spaces** when pasting into IntelliJ
   - Example: `abcd efgh ijkl mnop` → `abcdefghijklmnop`

4. **Use in IntelliJ**
   - Paste this password as the value for `MAIL_PASSWORD`

---

## Step 4: Verify Setup

1. **Run your application**
   - Click the **green play button** ▶️ in IntelliJ
   - Or press `Shift+F10`

2. **Check the logs**
   - Look for successful startup messages
   - No errors about `MAIL_PASSWORD` or `MAIL_USERNAME`

3. **Test email sending**
   - Use the application to send a test email
   - Check if it sends successfully

---

## Troubleshooting

### Error: "Could not resolve placeholder 'MAIL_PASSWORD'"

**Cause:** Environment variable not set or IntelliJ didn't pick it up.

**Solution:**
1. Verify you added the variables correctly
2. Make sure you clicked **"OK"** to save
3. Restart IntelliJ IDEA
4. Try running the application again

### Error: "Authentication failed"

**Cause:** Wrong Gmail password or not using App Password.

**Solution:**
1. Make sure you're using an **App Password**, not your regular Gmail password
2. Verify the App Password is correct (no spaces)
3. Check that 2-Step Verification is enabled on your Google account

### Variables not showing in the field

**Cause:** IntelliJ UI issue.

**Solution:**
1. Close and reopen the Run/Debug Configurations dialog
2. The variables should appear in the format: `MAIL_USERNAME=...;MAIL_PASSWORD=...`

---

## Alternative: System Environment Variables

If you prefer to set environment variables system-wide (not just in IntelliJ):

### Windows

**PowerShell:**
```powershell
[System.Environment]::SetEnvironmentVariable('MAIL_USERNAME', 'your-email@gmail.com', 'User')
[System.Environment]::SetEnvironmentVariable('MAIL_PASSWORD', 'your-app-password', 'User')
```

**Or via GUI:**
1. Press `Win + X` → **System**
2. Click **"Advanced system settings"**
3. Click **"Environment Variables"**
4. Under **"User variables"**, click **"New"**
5. Add `MAIL_USERNAME` and `MAIL_PASSWORD`
6. Restart IntelliJ

### Linux/Mac

**Add to `~/.bashrc` or `~/.zshrc`:**
```bash
export MAIL_USERNAME="your-email@gmail.com"
export MAIL_PASSWORD="your-app-password"
```

**Then reload:**
```bash
source ~/.bashrc  # or ~/.zshrc
```

**Restart IntelliJ** to pick up the new variables.

---

## Security Note

⚠️ **Never commit environment variables to Git!**

- Environment variables are stored locally in IntelliJ
- They are NOT committed to the repository
- Each developer must set their own credentials
- This keeps your Gmail password secure

---

## Next Steps

After setting up environment variables:
1. ✅ Run the backend application
2. ✅ Test email sending functionality
3. ✅ Continue with frontend setup (see [SETUP.md](./SETUP.md))

---

For more help, see:
- [SETUP.md](./SETUP.md) - Complete development setup
- [GOOGLE_OAUTH_SETUP.md](./GOOGLE_OAUTH_SETUP.md) - Google OAuth configuration
