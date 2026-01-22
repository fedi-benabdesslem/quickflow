# QuickFlow

> **Intelligent Meetings, Effortless Emails.**
> Transform your workflow with AI-powered meeting summaries and instant email drafting.

<img src="frontend/public/banner.png" alt="QuickFlow Banner" width="100%">
<!-- Placeholder image for now, user can replace later -->

## Overview

**QuickFlow** is a modern productivity suite designed to reclaim your time. Leveraging the power of local LLMs and a sleek, mesmerizing UI, it acts as your personal executive assistant.

Key capabilities include:
- **🎙️ Smart Summaries**: Automatically digest meeting notes into actionable insights.
- **✉️ Instant Drafts**: Generate professional emails in seconds based on brief prompts.
- **📧 Email Sending**: Send emails directly from your Gmail or Outlook account via OAuth.
- **📄 PDF Generation**: Create professional PDF attachments for meeting minutes.
- **🛡️ Privacy First**: Built with Supabase Auth and strictly local AI processing options (Ollama).
- **🌌 Immersive UX**: A deep "Dark Nebula" aesthetic that makes work feel like play.

## Tech Stack

Built with cutting-edge technologies for performance and scalability:

- **Frontend**: [React 18](https://react.dev/), [TypeScript](https://www.typescriptlang.org/), [Vite](https://vitejs.dev/), [Tailwind CSS](https://tailwindcss.com/)
- **Backend**: [Spring Boot 3.3](https://spring.io/projects/spring-boot), [Spring AI](https://spring.io/projects/spring-ai)
- **Database**: [MongoDB](https://www.mongodb.com/)
- **AI Engine**: [Ollama](https://ollama.ai/) (Mistral-Nemo)
- **Auth**: [Supabase](https://supabase.com/)
- **Email APIs**: Gmail API, Microsoft Graph API

## Prerequisites

Before running QuickFlow, ensure you have:

1. **Node.js** (v18+) and **npm**
2. **Java 17+** and **Maven**
3. **MongoDB** (local or Atlas)
4. **Ollama** with Mistral-Nemo model installed
5. **Supabase** project configured

### Google Cloud Setup (Required for Email Sending)

To enable email sending via Gmail, you must configure Google Cloud:

1. **Create a Google Cloud Project** at [console.cloud.google.com](https://console.cloud.google.com/)
2. **Enable the Gmail API**:
   - Navigate to **APIs & Services** → **Library**
   - Search for "Gmail API"
   - Click **Enable**
3. **Configure OAuth Consent Screen**:
   - Go to **APIs & Services** → **OAuth consent screen**
   - Add scopes: `gmail.send`, `email`, `profile`, `openid`
4. **Create OAuth Credentials**:
   - Go to **APIs & Services** → **Credentials**
   - Create an **OAuth 2.0 Client ID** (Web application)
   - Add authorized redirect URIs for Supabase

> **⚠️ Important**: The Gmail API must be enabled in your Google Cloud project, or email sending will fail with a 500 error.

### Microsoft Azure Setup (Optional - for Outlook)

For Microsoft/Outlook email sending:

1. Register an app in **Azure AD**
2. Add **Mail.Send** and **offline_access** permissions
3. Configure redirect URIs for Supabase

## Environment Variables

### Backend (`backend/src/main/resources/application.properties`)

```properties
# MongoDB
spring.data.mongodb.uri=mongodb://localhost:27017/quickflow

# Supabase
supabase.jwt.secret=your-supabase-jwt-secret

# Token Encryption
token.encryption.key=your-32-character-encryption-key

# Google OAuth (for token refresh)
google.client.id=your-google-client-id
google.client.secret=your-google-client-secret

# Microsoft OAuth (optional)
microsoft.client.id=your-azure-client-id
microsoft.client.secret=your-azure-client-secret
microsoft.tenant.id=your-azure-tenant-id

# Ollama
spring.ai.ollama.base-url=http://localhost:11434
spring.ai.ollama.chat.model=mistral-nemo
```

### Frontend (`.env`)

```env
VITE_SUPABASE_URL=https://your-project.supabase.co
VITE_SUPABASE_ANON_KEY=your-supabase-anon-key
VITE_API_URL=http://localhost:8080
```

## Getting Started

### 1. Clone and Install

```bash
# Clone the repository
git clone https://github.com/your-username/quickflow.git
cd quickflow

# Install frontend dependencies
cd frontend
npm install

# Install backend dependencies
cd ../backend
mvn install
```

### 2. Start Services

```bash
# Start Ollama (in separate terminal)
ollama run mistral-nemo

# Start MongoDB (if local)
mongod

# Start backend (in backend directory)
mvn spring-boot:run

# Start frontend (in frontend directory)
npm run dev
```

### 3. Access the App

Open [http://localhost:5173](http://localhost:5173) in your browser.

## Features

### 📝 AI Meeting Minutes
- Fill in meeting details (date, attendees, topics, action items)
- AI generates professional meeting minutes
- Edit with rich text editor
- Generate PDF and send via email

### ✉️ AI Email Writer
- Describe your message casually
- AI drafts a professional email with subject
- Edit and send directly from your Gmail/Outlook

## License

[MIT](./LICENSE)

