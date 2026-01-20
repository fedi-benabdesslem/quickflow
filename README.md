# QuickFlow

AI-powered meeting summaries and email drafting application.

## Tech Stack

- **Frontend**: React + TypeScript + Vite + Tailwind CSS
- **Backend**: Spring Boot 3.3 + MongoDB + Spring AI (Ollama)
- **Authentication**: Supabase Auth

## Prerequisites

- Node.js 18+
- Java 17+
- MongoDB (running on `localhost:27017`)
- Ollama with `mistral-nemo` model

## Quick Start

### 1. Configure Supabase

Create a Supabase project at [supabase.com](https://supabase.com) and get your credentials.

**Frontend** (`frontend/src/lib/supabase.ts`):
```typescript
const supabaseUrl = 'YOUR_SUPABASE_URL'
const supabaseAnonKey = 'YOUR_SUPABASE_ANON_KEY'
```

**Backend** - Set environment variable:
```bash
export SUPABASE_JWT_SECRET=your_jwt_secret
```

Or update `backend/src/main/resources/application.properties`:
```properties
supabase.jwt.secret=your_jwt_secret
```

### 2. Configure Email (Optional)

Set environment variables for SMTP:
```bash
export MAIL_USERNAME=your_email@gmail.com
export MAIL_PASSWORD=your_app_password
```

### 3. Start Backend

```bash
cd backend
./mvnw spring-boot:run
```

### 4. Start Frontend

```bash
cd frontend
npm install
npm run dev
```

Open [http://localhost:5173](http://localhost:5173)

## Project Structure

```
quickflow/
├── frontend/           # React + Vite application
│   ├── src/
│   │   ├── components/ # Reusable components
│   │   ├── contexts/   # Auth & Review contexts
│   │   ├── lib/        # Supabase client & API service
│   │   ├── pages/      # Page components
│   │   └── types/      # TypeScript interfaces
│   └── ...
├── backend/            # Spring Boot application
│   └── src/main/java/com/ai/application/
│       ├── Config/     # Security & JWT filter
│       ├── Controllers/
│       ├── Services/
│       └── model/
└── README.md
```

## Features

- 🔐 Secure authentication with Supabase
- 📝 AI-generated meeting summaries
- ✉️ AI-powered email drafting
- 🎨 Modern "Dark Nebula" theme with glassmorphism
- ✨ Smooth animations with Framer Motion

## Configuration Placeholders

| Variable | Location | Description |
|----------|----------|-------------|
| `SUPABASE_URL` | Frontend | Your Supabase project URL |
| `SUPABASE_ANON_KEY` | Frontend | Supabase anonymous/public key |
| `SUPABASE_JWT_SECRET` | Backend | JWT secret from Supabase settings |

## License

MIT
