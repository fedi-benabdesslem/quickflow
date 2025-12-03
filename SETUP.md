# Chronoshift - Development Setup Guide

## Prerequisites

- **Java 17** or higher
- **Node.js 18+** and npm
- **Docker Desktop** (for Keycloak)
- **MongoDB** (local or Docker)
- **Ollama** (for AI features)

---

## Quick Start

### 1. Clone the Repository

```bash
git clone https://github.com/TaherBenAfia/quickflow.git
cd quickflow
```

### 2. Set Up Environment Variables

#### Backend Email Configuration

The application uses Gmail SMTP for sending emails. You need to configure your Gmail credentials as environment variables.

**IntelliJ IDEA:**
1. Open Run/Debug Configurations (top right dropdown → Edit Configurations)
2. Select your Spring Boot application
3. Find "Environment variables" field
4. Click the folder icon 📁
5. Add these variables:
   ```
   MAIL_USERNAME=your-email@gmail.com
   MAIL_PASSWORD=your-gmail-app-password
   ```
6. Click OK

**Get Gmail App Password:**
1. Go to https://myaccount.google.com/apppasswords
2. Sign in with your Gmail account
3. App name: `Chronoshift`
4. Click **Create**
5. Copy the 16-character password (format: `xxxx xxxx xxxx xxxx`)
6. Use this as `MAIL_PASSWORD` (remove spaces)

**Alternative (System Environment Variables):**

Windows PowerShell:
```powershell
$env:MAIL_USERNAME="your-email@gmail.com"
$env:MAIL_PASSWORD="your-gmail-app-password"
```

Linux/Mac:
```bash
export MAIL_USERNAME="your-email@gmail.com"
export MAIL_PASSWORD="your-gmail-app-password"
```

### 3. Start MongoDB

**Option A: Local MongoDB**
```bash
mongod --dbpath /path/to/data
```

**Option B: Docker**
```bash
docker run -d -p 27017:27017 --name mongodb mongo:latest
```

### 4. Start Ollama (AI Service)

**Download and install:** https://ollama.ai

**Pull the model:**
```bash
ollama pull mistral-nemo
```

**Verify it's running:**
```bash
curl http://localhost:11434/api/tags
```

### 5. Start Keycloak and Import Realm

**Step 1: Start Keycloak**

```bash
docker run -d \
  --name keycloak \
  -p 8180:8080 \
  -e KEYCLOAK_ADMIN=admin \
  -e KEYCLOAK_ADMIN_PASSWORD=admin \
  quay.io/keycloak/keycloak:latest \
  start-dev
```

**Wait for Keycloak to start** (takes about 30 seconds). Check with:
```bash
curl http://localhost:8180
```

**Step 2: Import Pre-configured Realm**

1. **Access Keycloak Admin Console:**
   - URL: http://localhost:8180
   - Username: `admin`
   - Password: `admin`

2. **Import Realm:**
   - Hover over the realm dropdown (top-left corner, currently shows "master")
   - Click **"Create Realm"**
   - Click **"Browse"** button
   - Select `keycloak/quickflow-realm.json` from the project directory
   - Click **"Create"**

3. **Verify Import:**
   - Realm `quickflow-realm` should now appear in the dropdown
   - Select it
   - Go to **Clients** → Verify `quickflow-frontend` exists
   - Check **Valid redirect URIs** includes: `http://localhost:4200/*`

**Step 3: Configure Google Sign-In**

The realm import includes the basic structure, but you need to add YOUR OWN Google OAuth credentials:

1. Follow [GOOGLE_OAUTH_SETUP.md](./GOOGLE_OAUTH_SETUP.md) to:
   - Create your Google Cloud project
   - Get your Client ID and Client Secret
   
2. In Keycloak:
   - Go to **Identity Providers** → Click **"Add provider"** → Select **"Google"**
   - Configure with your credentials (see GOOGLE_OAUTH_SETUP.md Step 5)

**Note:** Each developer must use their own Google OAuth credentials for security reasons.

### 6. Start Backend

```bash
cd backend
mvn clean install
mvn spring-boot:run
```

**Verify backend is running:**
```bash
curl http://localhost:8080/api/health
```

### 7. Start Frontend

```bash
cd frontend
npm install
npm start
```

**Access the application:**
- URL: http://localhost:4200
- The app will automatically redirect to Keycloak for login

---

## Development Modes

### Keycloak Mode (Default)
- Full authentication with Keycloak
- Google Sign-In enabled
- JWT token-based security

### Dev Mode (No Auth)
To disable Keycloak for local development:

1. Backend: Use `dev` profile
   ```bash
   mvn spring-boot:run -Dspring-boot.run.profiles=dev
   ```

2. Frontend: Use `environment.ts` instead of `environment.keycloak.ts`

---

## Project Structure

```
quickflow/
├── backend/                 # Spring Boot application
│   ├── src/main/java/
│   │   └── com/ai/application/
│   │       ├── Controllers/  # REST API endpoints
│   │       ├── Services/     # Business logic
│   │       ├── Config/       # Security & app config
│   │       └── model/        # Entities & DTOs
│   └── src/main/resources/
│       └── application.properties
│
├── frontend/                # Angular application
│   ├── src/app/
│   │   ├── components/      # UI components
│   │   ├── services/        # API & auth services
│   │   └── auth.config.ts   # Keycloak configuration
│   └── src/environments/    # Environment configs
│
└── GOOGLE_OAUTH_SETUP.md   # Google OAuth setup guide
```

---

## Troubleshooting

### Backend won't start - Email configuration error
**Error:** `Could not resolve placeholder 'MAIL_PASSWORD'`

**Solution:** Make sure you've set the environment variables `MAIL_USERNAME` and `MAIL_PASSWORD`

### Frontend shows "Invalid redirect URI"
**Solution:** Check that Keycloak client redirect URIs include:
- `http://localhost:4200/*`
- `http://localhost:4200`

### Google Sign-In doesn't work
**Solution:** 
1. Verify Google OAuth credentials are correctly configured in Keycloak
2. Check redirect URI in Google Cloud Console matches Keycloak's
3. See [GOOGLE_OAUTH_SETUP.md](./GOOGLE_OAUTH_SETUP.md) for detailed troubleshooting

### MongoDB connection failed
**Solution:** 
- Verify MongoDB is running: `mongosh` or check Docker container
- Check connection string in `application.properties`

### Ollama not responding
**Solution:**
- Verify Ollama is running: `ollama list`
- Check if model is pulled: `ollama pull mistral-nemo`
- Restart Ollama service

---

## Additional Resources

- [Keycloak Documentation](https://www.keycloak.org/documentation)
- [Angular OAuth2 OIDC](https://github.com/manfredsteyer/angular-oauth2-oidc)
- [Spring Security OAuth2](https://spring.io/guides/tutorials/spring-boot-oauth2/)

---

## Support

For issues or questions, please contact the development team or create an issue in the repository.
