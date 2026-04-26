# QuickFlow Setup Guide

Welcome to QuickFlow! This guide will help you get the application running on your machine.

## 📋 Prerequisites

Ensure you have the following installed:
1. **Node.js**: v18+ ([Download](https://nodejs.org/))
2. **Java**: JDK 17+ ([Download](https://adoptium.net/))
3. **MongoDB**: Local instance running on port 27017 ([Download](https://www.mongodb.com/try/download/community))
4. **Ollama**: For local AI capabilities ([Download](https://ollama.ai/))
   - *Run `ollama pull mistral-nemo` after installing.*
5. **Python**: 3.10+ with `pip` ([Download](https://www.python.org/downloads/))
   - Required for the transcription service.
6. **Ngrok**: For cross-origin OAuth cookie support in development ([Download](https://ngrok.com/download))
   - Required to receive httpOnly refresh token cookies when the OAuth provider redirects back.

---

## ⚡ Quick Start (Recommended)

QuickFlow comes pre-configured with the necessary credentials for Contributor access. You typically **do not** need to configure OAuth or Google Cloud manually.

### 1. Clone the Repository
```bash
git clone https://github.com/TaherBenAfia/quickflow.git
cd quickflow
```

### 2. Set Up Ngrok

Ngrok is required so that the OAuth redirect and httpOnly cookie flow works correctly in development (cross-origin between `ngrok-free.dev` → `localhost`).

#### Install Ngrok
Download and install from [ngrok.com/download](https://ngrok.com/download), then add it to your PATH.

#### Add Auth Token
```bash
ngrok config add-authtoken YOUR_NGROK_AUTHTOKEN
```

#### Run Ngrok (keep this terminal open)
```bash
export NGROK_DOMAIN=your-domain.ngrok-free.dev   # set to your own ngrok static domain
ngrok http --url=$NGROK_DOMAIN 8080
```

This exposes your local backend at `https://your-domain.ngrok-free.dev`.

> **Note:** The ngrok tunnel must be running before you attempt any OAuth login (Google/Microsoft sign-in).
> Set the same domain in `BACKEND_URL` (environment variable for `application.properties`).

---

### 3. Start the Transcription Service

> **Note:** If starting the transcription service for the first time, you need to install dependencies. See [transcription-service/README.md](./transcription-service/README.md) for full details.

```bash
cd transcription-service
python -m venv venv
venv\Scripts\activate        # Windows
# source venv/bin/activate   # Linux/Mac
venv\Scripts\python main.py  # Windows
# python main.py             # Linux/Mac
```

The transcription service starts on `http://localhost:8001`.

### 4. Start the Backend
Open a new terminal in the `backend` folder:
```bash
cd backend
mvn spring-boot:run
# or on Mac/Linux: ./mvnw spring-boot:run
```

*The server will start on `http://localhost:8080`.*

### 5. Start the Frontend
Open a new terminal in the `frontend` folder:
```bash
cd frontend
npm install
npm run dev
```

*The app will open at `http://localhost:5173`.*

> **Note:** The project is in dev phase. To log in with your Google account you must contact the developer so your email is added to the test-user allowlist in Google Cloud Console.
>
> You can contact the host at [fadib.abdesslem204@gmail.com](mailto:fadib.abdesslem204@gmail.com)

---

## 🛠️ Advanced: Self-Hosting

If you wish to host your own instance of QuickFlow with your own credentials, follow these additional steps.

### 1. Generate a JWT Secret
```bash
openssl rand -base64 32
```
Set this as `jwt.secret` in `backend/src/main/resources/application.properties`.

### 2. Google Cloud Setup (Login + Email + Contacts)
1. Create a project in [Google Cloud Console](https://console.cloud.google.com/).
2. Enable the **Gmail API** and **People API** (for contacts sync).
3. Configure the **OAuth Consent Screen** with scopes:
   - `openid`, `email`, `profile`
   - `https://www.googleapis.com/auth/gmail.send`
   - `https://www.googleapis.com/auth/gmail.readonly`
   - `https://www.googleapis.com/auth/contacts.readonly`
4. Create **OAuth 2.0 Credentials** (Web Client).
   - Authorized Redirect URI: `http://localhost:8080/login/oauth2/code/google`
   - For account linking: `http://localhost:8080/api/auth/link/callback`
5. Update `application.properties`:
   ```properties
   spring.security.oauth2.client.registration.google.client-id=YOUR_CLIENT_ID
   spring.security.oauth2.client.registration.google.client-secret=YOUR_CLIENT_SECRET
   spring.security.oauth2.client.registration.google.scope=openid,email,profile,\
     https://www.googleapis.com/auth/gmail.send,\
     https://www.googleapis.com/auth/gmail.readonly,\
     https://www.googleapis.com/auth/contacts.readonly
   ```

### 3. Microsoft Azure Setup (Optional — Outlook + Contacts)
1. Register an app in [Azure AD](https://portal.azure.com/).
2. Add permissions: `Mail.Send`, `User.Read`, `Contacts.Read`, `offline_access`.
3. Set redirect URI: `http://localhost:8080/login/oauth2/code/microsoft`
4. Update `application.properties` with Client ID, Secret, and Tenant ID.

### 4. Resend (Transactional Emails — Password Reset, Verification)
1. Create an account at [resend.com](https://resend.com).
2. Update `application.properties`:
   ```properties
   resend.api.key=YOUR_RESEND_API_KEY
   resend.from.email=noreply@yourdomain.com
   ```

### 5. Frontend Environment
Update `frontend/.env`:
```env
VITE_BACKEND_URL=http://localhost:8080
```

---

## ❓ Troubleshooting

| Problem | Solution |
|---------|----------|
| **Ollama Connection Refused** | Ensure Ollama is running. Use `curl http://localhost:11434` to verify. |
| **MongoDB Error** | Ensure `mongod` is running: `mongod --dbpath /your/db/path`. |
| **OAuth Loop / Login Fails** | Ensure ngrok is running and the backend URL in `application.properties` matches the ngrok domain. |
| **"Permission Denied" for Contacts** | Re-login with Google after granting `contacts.readonly` scope. |
| **Transcription service won't start** | Check Python version (3.10+), ensure all dependencies installed via `pip install -r requirements.txt` inside the venv. |
| **Email sending fails** | Make sure the user has linked their Google/Microsoft account via the OAuth Linking flow, or configured an SMTP app password. |
