# QuickFlow

> **Intelligent Meetings, Effortless Emails.**
> 
> Transform your workflow with AI-powered meeting summaries, structured minutes, and instant email drafting.

<img src="frontend/public/banner.png" alt="QuickFlow Banner" width="100%">
<!-- Placeholder image for now, user can replace later -->

## Overview

**QuickFlow** is a modern productivity suite designed to act as your personal executive assistant. Leveraging local LLMs and a premium "Dark Nebula" aesthetic, it streamlines your daily communication tasks while keeping your data private.

## ✨ Features

### 📝 AI Meeting Minutes
- **Voice Mode**: **NEW!** Upload audio recordings for automated transcription, speaker identification, and minute generation.
- **Structured Mode**: Fill in meeting details (attendees, agenda, decisions) and generate formal minutes (PV).
- **Quick Mode**: Paste rough notes and let AI extract the structure automatically.
- **Template System**: Save and reuse meeting structures for recurring syncs.
- **PDF Generation**: Export professional PDF reports in one click.
- **Smart Contact Matching**: Extracted participant names are auto-matched to your contacts with emails.

### ✉️ AI Email Writer
- **Smart Drafting**: Describe your intent ("ask for a budget increase") and get a polished email.
- **Tone Adjustment**: Switch between Formal, Casual, or Urgent tones instantly.
- **Direct Sending**: Integrated with Gmail and Outlook for seamless delivery.
- **Contact Autocomplete**: Search and select recipients from your synced contacts.
- **Manual Recipient Entry**: Support for adding any email address, even if not in contacts.

### 👥 Contact Management
- **Cloud Sync**: Import contacts from Google or Microsoft accounts.
- **Smart Search**: Autocomplete dropdown across all forms (participants, recipients).
- **Favorites & Usage**: Mark favorites and track frequently used contacts.
- **QuickFlow Detection**: See which contacts also use QuickFlow.
- **Manual Entry**: Type any name or email and press Enter to add them, even if not in your synced list.

## 🛠️ Tech Stack

Built with cutting-edge technologies for performance and experience:

- **Frontend**: [React 18](https://react.dev/), [TypeScript](https://www.typescriptlang.org/), [Vite](https://vitejs.dev/), [Tailwind CSS](https://tailwindcss.com/), [Framer Motion](https://www.framer.com/motion/)
- **Backend**: [Spring Boot 3.3](https://spring.io/projects/spring-boot), [Spring AI](https://spring.io/projects/spring-ai)
- **Database**: [MongoDB](https://www.mongodb.com/)
- **AI Engine**: [Ollama](https://ollama.ai/) (Local Mistral-Nemo model)
- **Auth**: [Supabase](https://supabase.com/)

> **[Click here for the full Technical report](./tech-report.md)**

## 🚀 Getting Started

QuickFlow is designed to be easy to set up.

👉 **[Click here for the Setup Guide](./SETUP.md)**

## 🗺️ Roadmap

We are constantly improving QuickFlow. Here's what's coming next:
- [x] **Contact Autocomplete**: Smart suggestions for participants and recipients.
- [ ] **Calendar Integration**: Sync meetings directly from Google/Outlook Calendar.
- [x] **Voice Mode**: Real-time transcription and summarization.
- [ ] **Team Workspaces**: Share templates and minutes with your team.
## ⚙️ Automated Tests
Built-in automated tests in the backend and frontend to ensure reliability and stability.

👉 **[Click here for the Test Guide](./tests.md)**
