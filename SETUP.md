# QuickFlow Setup Guide

Welcome to QuickFlow! This guide will help you get the application running on your machine.

## 📋 Prerequisites

Ensure you have the following installed:
1. **Node.js**: v18+ ([Download](https://nodejs.org/))
2. **Java**: JDK 17+ ([Download](https://adoptium.net/))
3. **MongoDB**: Local instance running on port 27017 ([Download](https://www.mongodb.com/try/download/community))
4. **Ollama**: For local AI capabilities ([Download](https://ollama.ai/))
   - *Run `ollama pull mistral-nemo` after installing.*

---

## ⚡ Quick Start (Recommended)

QuickFlow comes pre-configured with the necessary credentials for Contributor access. You typically **do not** need to configure Supabase or Google Cloud manually.

### 1. Clone the Repository
```bash
git clone https://github.com/TaherBenAfia/quickflow.git
cd quickflow
```

### 2. Start the Backend
Open a terminal in the `backend` folder:
```bash
cd backend
# Windows
mvnw spring-boot:run
# Mac/Linux
./mvnw spring-boot:run
```
*The server will start on `http://localhost:8069`.*

### 3. Start the Frontend
Open a new terminal in the `frontend` folder:
```bash
cd frontend
npm install
npm run dev
```
*The app will open at `http://localhost:5173`.*

> **Note:** If you encounter authentication or email sending errors, please ensure you have the latest version of the `.env` and `application.properties` files from the repository, which contain the shared credentials.

> **Note:** The project is in dev phase, so for logging in with your gmail in the app, you must contact the developer (host) of the Supabase/Google Cloud app so he can add your email address to the test users list . ( **!** if you're planning to self-host your own app, you don't need to contact anyone. )

>You can contact the host at [fadib.abdesslem204@gmail.com](mailto:[fadib.abdesslem204@gmail.com])

---

## 🛠️ Advanced: Self-Hosting

If you wish to host your own instance of QuickFlow with your own data and API keys, follow these additional steps.

### 1. Supabase Setup (Auth)
1. Create a project at [supabase.com](https://supabase.com).
2. Go to **Authentication > URL Configuration** and add `http://localhost:5173` to Redirect URLs.
3. Get your `Project URL` and `Anon Key` from **Settings > API**.
4. Update `frontend/.env`:
   ```env
   VITE_SUPABASE_URL=your_project_url
   VITE_SUPABASE_ANON_KEY=your_anon_key
   ```
5. Get your `JWT Secret` from **Settings > API > JWT Settings** and update `backend/src/main/resources/application.properties`:
   ```properties
   supabase.jwt.secret=your_jwt_secret
   ```

### 2. Google Cloud Setup (Email)
1. Create a project in [Google Cloud Console](https://console.cloud.google.com/).
2. Enable **Gmail API**.
3. Configure **OAuth Consent Screen** with scopes: `gmail.send`, `email`, `profile`.
4. Create **OAuth 2.0 Credentials** (Web Client).
   - Authorized Redirect URI: `https://<your-supabase-project>.supabase.co/auth/v1/callback`
5. Add these credentials to your Supabase Auth Providers (Google) to enable "Sign in with Google".
6. For backend email sending (token refresh flow), update `application.properties`:
   ```properties
   google.client.id=your_client_id
   google.client.secret=your_client_secret
   ```

### 3. Microsoft Azure Setup (Optional)
1. Register an app in Azure AD.
2. Add `Mail.Send` and `offline_access` permissions.
3. Update `application.properties` with Client ID, Secret, and Tenant ID.

---

## ❓ Troubleshooting

- **Ollama Connection Refused**: Make sure Ollama is running in the system tray. Use `curl http://localhost:11434` to verify.
- **MongoDB Error**: Ensure `mongod` service is running.
- **Login Failed**: If using Quick Start, ensure you are not overriding the `.env` file with invalid local keys.
