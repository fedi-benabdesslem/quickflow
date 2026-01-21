# QuickFlow Setup Guide

Welcome to the QuickFlow developer setup. Follow these instructions to get the full stack running on your local machine.

## 📋 Prerequisites

Ensure you have the following installed:

1.  **Node.js**: v18 or higher ([Download](https://nodejs.org/))
2.  **Java**: JDK 17 or higher ([Download](https://adoptium.net/))
3.  **MongoDB**: Local instance or Docker container ([Download](https://www.mongodb.com/try/download/community))
4.  **Ollama**: For AI capabilities ([Download](https://ollama.ai/))
5.  **Git**: For version control

---

## 🛠️ Step 1: Clone the Repository

```bash
git clone https://github.com/TaherBenAfia/quickflow.git
cd quickflow
```

---

## ☁️ Step 2: Configure Supabase (Auth)

You have two options for authentication:

### Option A: Quick Start (Use Defaults)
The project comes pre-configured with a demo Supabase project. **You can skip this step** and run the application immediately. 
*   **Pros**: Instant setup, no configuration needed.
*   **Cons**: Shared data environment with other developers.

### Option B: Custom Setup (Recommended)
To have your own private data and user management, follow these steps:

1.  **Create Project**: Go to [supabase.com](https://supabase.com) and create a new project.
2.  **Get Credentials**:
    *   Go to **Project Settings > API**.
    *   Copy: `Project URL`, `anon public` key, and `service_role` secret (JWT).
3.  **Configure Frontend**:
    *   Create a `.env` file in the `frontend/` directory.
    *   Add your keys:
        ```env
        VITE_SUPABASE_URL=your_project_url
        VITE_SUPABASE_ANON_KEY=your_anon_key
        ```
4.  **Configure Backend**:
    *   The backend needs the JWT secret to verify tokens.
    *   Set the `SUPABASE_JWT_SECRET` environment variable (see Step 4).
5.  **Configure Redirects**:
    *   Go to **Authentication > URL Configuration** in Supabase.
    *   Add `http://localhost:5173` to your **Site URL** and **Redirect URLs**.

---

## 🤖 Step 3: Configure Ollama (AI)

QuickFlow relies on the `mistral-nemo` model for generating summaries.

1.  Start the Ollama application.
2.  Pull the required model:
    ```bash
    ollama pull mistral-nemo
    ```
3.  Verify it's running:
    ```bash
    curl http://localhost:11434/api/tags
    ```

---

## ⚙️ Step 4: Backend Setup (Spring Boot)

The backend requires several environment variables to function correctly.

### Environment Variables

You can set these in your IDE (IntelliJ/Eclipse) or export them in your terminal session before running the app.

| Variable | Description | Example |
| :--- | :--- | :--- |
| `SUPABASE_JWT_SECRET` | Secret to verify Supabase tokens. Found in Supabase **Project Settings > API > JWT Settings**. | `your-super-long-jwt-secret` |
| `MAIL_USERNAME` | (Optional) Gmail address for sending emails. | `user@gmail.com` |
| `MAIL_PASSWORD` | (Optional) App Password for the Gmail account. | `xxxx xxxx xxxx xxxx` |

### Running the Backend

1.  Navigate to the backend directory:
    ```bash
    cd backend
    ```
2.  Run with Maven wrapper:
    ```bash
    ./mvnw spring-boot:run
    ```
    *(Note: On Windows, you can just use `mvnw spring-boot:run` or open the project in IntelliJ IDEA).*

The server will start on `http://localhost:8080`.

---

## 💻 Step 5: Frontend Setup (React/Vite)

1.  Navigate to the frontend directory:
    ```bash
    cd frontend
    ```
2.  Install dependencies:
    ```bash
    npm install
    ```
3.  Create a `.env` file in the `frontend` root (or copy `.env.example` if it exists) and add your Supabase keys:
    ```env
    VITE_SUPABASE_URL=your_project_url_from_step_2
    VITE_SUPABASE_ANON_KEY=your_anon_key_from_step_2
    ```
    *Note: If you don't create a .env file, you may need to manually update `src/lib/supabase.ts`.*

4.  Start the development server:
    ```bash
    npm run dev
    ```

The app will be available at [`http://localhost:5173`](http://localhost:5173).

---

## 🔧 Troubleshooting

### Backend fails to start with `Could not resolve placeholder`
This means you are missing required environment variables. Ensure `SUPABASE_JWT_SECRET` is set. If you don't need email functionality immediately, you can temporarily set dummy values for `MAIL_USERNAME` and `MAIL_PASSWORD`.

### "Ollama connection refused"
Ensure Ollama is running in the background. You should see an Ollama icon in your system tray. By default, it runs on port `11434`.

### Login redirects to a 404 or fails
Check your Supabase **Redirect URLs**. It MUST match exactly where your frontend is running (usually `http://localhost:5173` or `http://localhost:5173/auth/callback` depending on implementation).

### MongoDB Connection Error
Ensure your local MongoDB service is active.
- **Windows**: Check Services (`services.msc`) -> MongoDB Server.
- **Docker**: Run `docker ps` to ensure the container is up.
