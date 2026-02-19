# QuickFlow

> **Intelligent Meetings, Effortless Emails.**
>
> Transform your workflow with AI-powered meeting summaries, structured minutes, and instant email drafting.

<img src="frontend/public/banner.png" alt="QuickFlow Banner" width="100%">

## Overview

**QuickFlow** is a modern productivity suite designed to act as your personal executive assistant. Leveraging local LLMs and a premium "Dark Nebula" aesthetic, it streamlines your daily communication tasks while keeping your data private.

---

## ✨ Features

### 📝 AI Meeting Minutes
- **Voice Mode**: Upload audio recordings for automated transcription, speaker identification, and minute generation.
- **Structured Mode**: Fill in meeting details (attendees, agenda, decisions) and generate formal minutes (PV).
- **Quick Mode**: Paste rough notes or upload a file and let AI extract the structure automatically.
- **Template System**: Save and reuse meeting structures for recurring syncs.
- **PDF Generation**: Export professional PDF reports in one click.
- **Smart Contact Matching**: Extracted participant names are auto-matched to your contacts with emails.

### ✉️ AI Email Writer
- **Smart Drafting**: Describe your intent ("ask for a budget increase") and get a polished email.
- **Tone Adjustment**: Switch between Formal, Casual, or Urgent tones instantly.
- **Direct Sending**: Integrated with Gmail, Outlook, and SMTP for seamless delivery.
- **Contact Autocomplete**: Search and select recipients from your synced contacts.
- **Manual Recipient Entry**: Add any email address, even if not in contacts.

### 👥 Contact Management
- **Cloud Sync**: Import contacts from Google or Microsoft accounts.
- **Smart Search**: Autocomplete dropdown across all forms (participants, recipients).
- **Favorites & Usage**: Mark favorites and track frequently used contacts.
- **QuickFlow Detection**: See which contacts also use QuickFlow.
- **Manual Entry**: Type any name or email and press Enter to add them instantly.

### 🔐 Authentication & Security
- **Email/Password**: Secure signup and login with bcrypt-hashed passwords.
- **OAuth2 Login**: Sign in with Google or Microsoft — tokens stored encrypted in MongoDB.
- **OAuth Account Linking**: Link a Google or Microsoft account to an email-only signup for email sending.
- **SMTP Fallback**: Users who don't use Google/Microsoft can configure an app-specific SMTP password.
- **MFA Ready**: TOTP infrastructure in place (UI pending).
- **Session Management**: Server-side sessions with automatic rotation and revocation.

---

## 🛠️ Tech Stack

Built with cutting-edge technologies for performance and experience:

- **Frontend**: [React 18](https://react.dev/), [TypeScript](https://www.typescriptlang.org/), [Vite](https://vitejs.dev/), [Tailwind CSS](https://tailwindcss.com/), [Framer Motion](https://www.framer.com/motion/)
- **Backend**: [Spring Boot 3.3](https://spring.io/projects/spring-boot), [Spring Security](https://spring.io/projects/spring-security), [Spring AI](https://spring.io/projects/spring-ai)
- **Database**: [MongoDB](https://www.mongodb.com/)
- **Auth**: Self-hosted Spring Security + JWT (email/password + OAuth2 via Google & Microsoft)
- **AI Engine**: [Ollama](https://ollama.ai/) (Local Mistral-Nemo model)
- **Transcription**: Python microservice (OpenAI Whisper + Pyannote)
- **Tunnel**: [Ngrok](https://ngrok.com/) for cross-origin OAuth flows in development

> **[Click here for the full Technical Report](./tech-report.md)**

---

## 🚀 Getting Started

👉 **[Click here for the Setup Guide](./SETUP.md)**

---

## 🗺️ Roadmap

- [x] **Contact Autocomplete**: Smart suggestions for participants and recipients.
- [x] **Voice Mode**: Audio transcription and summarization.
- [x] **OAuth Account Linking**: Link Google/Microsoft to email-only accounts for email sending.
- [x] **SMTP Support**: Email sending for non-OAuth users.
- [x] **Contact Sync**: Google Contacts & Microsoft contacts import.
- [ ] **Calendar Integration**: Sync meetings directly from Google/Outlook Calendar.
- [ ] **Team Workspaces**: Share templates and minutes with your team.
- [ ] **MFA UI**: Complete the TOTP multi-factor authentication experience.

---

## ⚙️ Automated Tests

Built-in automated tests in the backend and frontend to ensure reliability and stability.

👉 **[Click here for the Test Guide](./tests.md)**
