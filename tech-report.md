# QuickFlow: Deep Technical Audit Report

## 1. Project Architecture Overview

QuickFlow implements a modern client-server architecture designed for secure, AI-powered document generation and management.

-   **Frontend**: A responsive Single Page Application (SPA) built with React, delivering a "Dark Nebula" immersive experience.
-   **Backend**: A RESTful API service based on Spring Boot, managing business logic, AI integration, and database interactions.
-   **Database**: MongoDB for flexible, document-oriented data storage.
-   **AI Layer**: Local LLM inference via Ollama (Mistral-Nemo) for privacy-preserving content generation.
-   **Authentication**: Supabase Auth (JWT) for secure, managed user identity.
-   **External Integration**: Google (Gmail API) and Microsoft (Graph API) for email capabilities.
-   **Transcription Service**: Dedicated Python microservice using OpenAI Whisper and Pyannote for high-fidelity audio transcription and diarization.

---

## 2. Backend Analysis (`com.ai.application`)

The backend is structured as a layered Spring Boot application.

### 2.1 Core Application
-   **`AiApplication.java`**: The entry point. Bootstraps the Servlet container and Spring Context.

### 2.2 Controllers (API Layer)
REST endpoints that handle HTTP requests and responses.
-   **`AuthController`**: Manages user authentication flows.
-   **`BookmarkController`**: Manages user bookmarks for quick access to items.
-   **`ContactsController`**: Manages contact CRUD operations, sync with Google/Microsoft, and search/autocomplete.
-   **`EmailController`**: Handles general email sending operations via Gmail/Outlook.
-   **`GroupController`**: Manages user groups for organizing contacts and sharing.
-   **`HealthController`**: Provides a simple liveness probe endpoint (`/health`).
-   **`MeetingController`**: Core logic for meeting management (CRUD).
-   **`MeetingTemplateController`**: Manages the CRUD operations for user defined Meeting Templates.
-   **`MinutesController`**: Handles the specific generation/updating of meeting minutes documents.
-   **`MinutesEmailController`**: Specialized controller for sending specifically formatted meeting minutes via email.
-   **`PVController`**: Manages "Procès-Verbal" (formal minutes) generation logic.
-   **`PdfController`**: Endpoint for client-side requested PDF generation.
-   **`QuickModeController`**: Handles the unstructured-to-structured data transformation pipeline.
-   **`StructuredModeController`**: Handles validation and processing of structured form submissions.
-   **`TemplateController`**: (Legacy/Base) Generic template handling.
-   **`SupportController`**: Handles the "Tech Support" feature.
-   **`HistoryController`**: Manages user history data retrieval and deletion.
-   **`VoiceModeController`**: Manages the Voice Mode workflow: audio upload, transcription polling, and minute generation from transcripts.

### 2.3 Services (Business Logic)
Encapsulates the core business rules and integration logic.
-   **`ContactService`**: Manages contact CRUD, syncing with Google/Microsoft People APIs, search/autocomplete logic, and usage tracking.
-   **`EmailProviderService`**: Abstract factory/strategy pattern to switch between Gmail and Outlook providers efficiently.
-   **`EncryptionService`**: Handles symmetric encryption (AES) for sensitive tokens at rest.
-   **`FileProcessingService`**: Utilities for parsing uploaded files (PDF/DOCX) for Quick Mode.
-   **`GmailService`**: Implementation of Google Gmail API interactions (Draft creation, Sending).
-   **`GooglePeopleService`**: Fetches contacts from Google People API for sync operations.
-   **`GridFsService`**: manages large file storage in MongoDB (for potential attachment scaling).
-   **`HistoryService`**: Manages retrieval and deletion of user history records, ensuring data isolation.
-   **`LLMService`**: The bridge to the AI engine. Orchestrates prompts sent to Ollama/Spring AI.
-   **`MeetingTemplateService`**: Business logic for the Template System (Create, Read, Update, Delete).
-   **`MicrosoftGraphService`**: Implementation of Azure/Microsoft Graph API for Outlook and Contacts integration.
-   **`PdfGenerationService`**: Uses iText 7 to render HTML/Markdown content into professional PDF documents.
-   **`PdfService`**: Helper service for low-level PDF manipulation.
-   **`QuickFlowDetectionService`**: Detects if a contact uses QuickFlow by checking user database.
-   **`TemplateService`**: Manages the prompt templates used by the LLM.
-   **`TokenRefreshService`**: Background service (or on-demand) to handle OAuth2 token rotation for Google/Microsoft.
-   **`TokenStorageService`**: Securely stores and retrieves encrypted OAuth tokens from the database.
-   **`TranscriptionService`**: Orchestrates the communication with the Python Transcription Service (upload, polling, result parsing) and integrates with LLMService for minute generation.

### 2.4 Repositories (Data Access)
Interfaces extending `MongoRepository` for abstracted database operations.
-   **`ContactRepository`**: CRUD for synced contacts with support for search, favorites, and usage tracking.
-   **`EmailRepository`**: CRUD for email logs/metadata.
-   **`GeneratedOutputRepository`**: Stores history of AI-generated content.
-   **`MeetingRepository`**: Persistence for meeting records.
-   **`MeetingTemplateRepository`**: Persistence for user-defined templates.
-   **`TemplateRepository`**: Persistence for system prompt templates.
-   **`UserRepository`**: Stores user profiles and QuickFlow detection.
-   **`UserTokenRepository`**: Stores user-specific OAuth tokens (encrypted).

### 2.5 Security & Configuration
-   **`SupabaseJwtFilter`**: A custom Servlet Filter that intercepts requests, extracts the Bearer token, and verifies it against Supabase's public keys. It hydrates the Spring Security Context.
-   **`WebConfig`**: Global CORS configuration allowing requests from the frontend (`http://localhost:5173`) with credentials.

---

### 2.6 API Endpoints

A comprehensive reference of all available backend endpoints.

#### Authentication (`/api/auth`)
| Method | Endpoint | Description | Request Body |
| :--- | :--- | :--- | :--- |
| `POST` | `/store-tokens` | Stores OAuth tokens after frontend login. | `{ accessToken, refreshToken, provider, email }` |
| `GET` | `/email-capability` | Checks if user has stored credentials to send emails. | - |

#### Meeting Management (`/api/meeting`) 
> **Note:** Legacy endpoints for meeting management. New endpoints are in `/api/minutes`. those are not used in the frontend anymore but kept for backward compatibility.

| Method | Endpoint | Description | Request Body |
| :--- | :--- | :--- | :--- |
| `POST` | `/generate` | Generates a new meeting record from raw input. | `{ people, location, date, timeBegin, timeEnd, subject }` |
| `POST` | `/send-final` | Saves final meeting details and sends PDF via email if recipients are present. | `{ id, subject, content, recipients... }` |

#### Meeting Templates (`/api/meeting-templates`)
| Method | Endpoint | Description | Request Body |
| :--- | :--- | :--- | :--- |
| `POST` | `/` | Create a new user template. | `{ name, description, templateData }` |
| `GET` | `/` | List all templates for the logged-in user. | - |
| `GET` | `/{id}` | Get a specific template by ID. | - |
| `PUT` | `/{id}` | Update an existing template. | `{ name, description, templateData }` |
| `DELETE` | `/{id}` | Delete a template. | - |
| `POST` | `/{id}/track-usage` | Increment usage counter for a template. | - |

#### Meeting Minutes (`/api/minutes`)
| Method | Endpoint | Description | Request Body |
| :--- | :--- | :--- | :--- |
| `POST` | `/quick/extract` | Quick Mode: Extracts structured data from raw text. | `{ content, date, time }` |
| `POST` | `/quick/extract-file` | Quick Mode: Extracts structured data from uploaded file (PDF). | `MultipartFile` |
| `POST` | `/quick/generate` | Quick Mode: Generates final minutes from extracted data. | `{ data: ExtractedData }` |
| `POST` | `/structured/generate` | Structured Mode: Generates minutes from detailed form. | `StructuredModeRequest` |
| `POST` | `/send` | Sends generated minutes email with PDF attachment. | `MinutesEmailRequest` |
| `POST` | `/voice/cancel/{jobId}` | Cancels a running voice transcription job. | - |
| `POST` | `/draft/quick` | (Legacy) Save Quick Mode draft. | `Map` |
| `POST` | `/draft/structured`| (Legacy) Save Structured Mode draft. | `Map` |

#### Email (`/api/email`)
| Method | Endpoint | Description | Request Body |
| :--- | :--- | :--- | :--- |
| `POST` | `/send` | Generates an AI email draft. | `EmailRequest` |
| `POST` | `/send-final` | Sends the finalized email via Gmail/Outlook. | `{ id, subject, content, recipients }` |

#### Contacts (`/api/contacts`)
| Method | Endpoint | Description | Request Body |
| :--- | :--- | :--- | :--- |
| `GET` | `/` | Lists all contacts for user (with optional filter/source/sortBy). | - |
| `GET` | `/search` | Search contacts by query string for autocomplete. | `?q=&limit=` |
| `GET` | `/recent` | Get recently used contacts. | `?limit=` |
| `POST` | `/sync` | Sync contacts from Google/Microsoft provider. | - |
| `GET` | `/sync/status` | Get sync status and last sync timestamp. | - |
| `POST` | `/{id}/usage` | Increment usage counter for contact (used for sorting). | - |
| `PUT` | `/{id}/favorite` | Toggle favorite status for a contact. | - |

#### Bookmarks (`/api/bookmarks`)
| Method | Endpoint | Description | Request Body |
| :--- | :--- | :--- | :--- |
| `GET` | `/` | Get all bookmarks for the user. | - |
| `POST` | `/` | Add a new bookmark. | `{ itemId, type }` |
| `DELETE` | `/{itemId}` | Remove a bookmark. | - |

#### Groups (`/api/groups`)
| Method | Endpoint | Description | Request Body |
| :--- | :--- | :--- | :--- |
| `GET` | `/` | Get all groups for the user. | - |
| `POST` | `/` | Create a new group. | `{ name, description, memberIds }` |
| `GET` | `/{id}` | Get a specific group details. | - |
| `PUT` | `/{id}` | Update a group. | `{ name, description, memberIds }` |
| `DELETE` | `/{id}` | Delete a group. | - |
| `POST` | `/{id}/members` | Add members to a group. | `{ memberIds }` |
| `DELETE` | `/{id}/members/{memberId}` | Remove a member from a group. | - |
| `GET` | `/search` | Search groups by name. | `?q=&limit=` |
| `GET` | `/count` | Get total group count (for badges). | - |
#### PDF (`/api/pdf`)
| Method | Endpoint | Description | Request Body |
| :--- | :--- | :--- | :--- |
| `POST` | `/generate` | Generates a PDF file from HTML content. | `PdfGenerationRequest` |
| `GET` | `/preview/{id}` | Streams PDF for browser preview (inline). | - |
| `GET` | `/download/{id}` | Downloads PDF file (attachment). | - |

#### Tech Support (`/api/support/report`)
| Method | Endpoint | Description | Request Body |
| :--- | :--- | :--- | :--- |
| `POST` | `/send` | Sends a support email. | `SupportEmailRequest` |
#### History (`/api/history`)
| Method | Endpoint | Description | Request Body |
| :--- | :--- | :--- | :--- |
| `GET` | `/` | Retrieves user-specific history of generated content. | - |
| `DELETE` | `/{id}` | Deletes a history record. | - |

#### System (`/api`) 
| Method | Endpoint | Description | Request Body |
| :--- | :--- | :--- | :--- |
| `GET` | `/health` | Liveness probe to check if backend is running. | - |

---

### 2.7 Python Transcription Service (`transcription-service`)

A dedicated microservice optimized for GPU-accelerated audio processing.

#### Core Components
-   **`main.py`**: FastAPI application entry point defining endpoints (`/transcribe`, `/diarize`, `/health`, `/status/{job_id}`, `/cancel/{job_id}`, `/result/{job_id}`, `/metrics`).
-   **`transcription.py`**: Wraps `openai-whisper` for speech-to-text conversion. Handles model loading and VRAM management.
-   **`diarization.py`**: Wraps `pyannote.audio` for speaker identification. Segments audio by speaker turns.
-   **`job_manager.py`**: A robust async job queue using `asyncio.Semaphore` to limit concurrent GPU operations. Manages job lifecycle (Queued -> Processing -> Completed/Failed).
-   **`config.py`**: Centralized configuration management using environment variables.

#### Architecture
-   **Concurrency Control**: Uses a semaphore-based locking mechanism to ensure only `MAX_CONCURRENT_JOBS` (default: 1) run on the GPU at a time, preventing Out-Of-Memory (OOM) errors.
-   **Asynchronous Processing**: Long-running transcription tasks are offloaded to background tasks, returning a Job ID immediately for polling.
-   **Hardware Acceleration**: Automatically detects CUDA support and moves models to GPU. Falls back to CPU if unavailable (or on Windows for specific stability settings).

## 3. Frontend Analysis (`quickflow-frontend`)

A React application structured for component reusability and type safety.

### 3.1 Technology Stack
-   **Library**: React 18
-   **Build Tool**: Vite (for fast HMR and bundling).
-   **Language**: TypeScript (strict mode enabled).
-   **Styling**: Tailwind CSS (utility-first) + Custom CSS (`index.css`) for the "Nebula" theme.
-   **Animation**: Framer Motion (page transitions, micro-interactions).
-   **State Management**: React Context API (`AuthContext`, `ReviewContext`).

### 3.2 Key Pages (Routes)
-   **`HomePage`**: Dashboard with action cards (Create Minutes, Email, Templates, Contacts).
-   **`AuthPage`**: Login/Register interface integration with Supabase Auth UI.
-   **`ContactsPage`**: Contact management with sync, search, favorites, and QuickFlow user detection.
-   **`ModeSelectionPage`**: Choice between "Quick Mode" and "Structured Mode".
-   **`StructuredModePage`**: A complex form with dynamic fields (participants, agenda) and Template loading support.
-   **`QuickModePage`**: File upload and text area for raw note input.
-   **`QuickModeReviewPage`**: Intermediate step to review AI-extracted data with contact autocomplete for participants.
-   **`ContentEditorPage`**: Rich text editor (Quill) for polishing generated content.
-   **`TemplateManagementPage`**: CRUD interface for managing Meeting Templates.
-   **`EmailPage`**: Interface for the "AI Email Writer" feature with contact autocomplete recipients.
-   **`VoiceModePage`**: A wizard-style interface for the Voice Mode workflow (Upload -> Transcribe -> Review -> Generate).
-   **`TechSupportPage`**: Interface for the "Tech Support" feature with contact autocomplete recipients.
-   **`HistoryPage`**: Displays user-specific history of generated minutes and emails with delete functionality.

### 3.3 Core Components
-   **`NebulaBackground`**: A canvas-based animated background component creating the immersive star/nebula effect.
-   **`RichTextEditor`**: Wrapper around `react-quill` for WYSIWYG editing.
-   **`PdfPreview`**: real-time PDF rendering component using `@react-pdf/renderer`.
-   **`SaveTemplateModal` / `EditTemplateModal`**: Modals for the Template System interaction.
-   **`ContactAutocomplete`**: Smart search input with dropdown for selecting contacts from synced list.
-   **`PdfPreviewModal`**: A unified modal for previewing generated PDFs and initiating email flows.
-   **`RecipientSelectionModal`**: Modal for selecting meeting participants as email recipients with autocomplete.
-   **`UserAvatar`**: Dynamic avatar component with initials fallback and QuickFlow user indicator.

### 3.4 Lib & Integration
-   **`api.ts`**: Centralized Axios instance with interceptors for error handling and injecting the Supabase Authorization header.
-   **`supabase.ts`**: Singleton Supabase client configuration.

---

## 4. Protocols & Techniques

### 4.1 Authentication & Authorization
-   **Standard**: OAuth2 / OpenID Connect.
-   **Implementation**: Users sign in via Supabase (Frontend). The resulting JWT (JSON Web Token) is sent in the `Authorization: Bearer` header. The Backend validates the signature of this token on every protected request.

### 4.2 Data Transport
-   **Protocol**: HTTPS (recommended for prod) / HTTP (local).
-   **Format**: JSON (JavaScript Object Notation) for all API payloads.
-   **Pattern**: REST (Representational State Transfer).

### 4.3 AI & Logic
-   **Technique**: RAG-lite (Retrieval Augmented Generation concepts used in context injection).
-   **Prompt Engineering**: The `LLMService` constructs sophisticated prompts effectively instructing the model to act as a "Professional Secretary", enforcing strict output formats (JSON/Markdown) for parsing.

### 4.4 External APIs
-   **Google Gmail API**: Used via `google-api-client`. Requires offline scope to persist access.
-   **Microsoft Graph**: Used via `microsoft-graph` SDK. Uses a similar OAuth flow.

### 4.5 Security Measures
-   **Encryption at Rest**: OAuth tokens for third-party services are encrypted using AES-256 before being stored in MongoDB.
-   **CORS Policy**: Strict Cross-Origin Resource Sharing policies prevent unauthorized domains from calling the API.
-   **Environment Variables**: Secrets are externalized (file-based or system-based), never hardcoded.

## 5. Key Libraries & Frameworks Breakdown

A detailed look at the third-party dependencies powering specific features.

### 5.1 Backend (Java / Spring Boot)

| Library | Purpose | Details |
| :--- | :--- | :--- |
| **Spring AI** | **AI Orchestration** | Provides the abstraction layer for interacting with LLMs (Ollama). Handles prompt templating and response parsing. |
| **iText 7 (Core + html2pdf)** | **PDF Generation** | Used in `PdfGenerationService`. Converts HTML/CSS specifically styled for reports into binary PDF files. It offers high-fidelity control over layout compared to simple HTML-to-PDF converters. |
| **Apache PDFBox** | **File Processing** | Used in `FileProcessingService` to extract raw text from uploaded PDF files during Quick Mode ingestion. |
| **Apache POI** | **File Processing** | Used to extract text from Microsoft Word (`.docx`) files. |
| **Java JWT (Auth0)** | **Security** | Parses and validates the signature of Supabase JWTs. |
| **Google API Client** | **Email Integration** | Handles the OAuth2 flow and REST calls to the Gmail API for sending drafts. |
| **Microsoft Graph SDK** | **Email Integration** | Wraps the complex Graph API for Outlook integration. |

### 5.2 Frontend (React / TypeScript)

| Library | Purpose | Details |
| :--- | :--- | :--- |
| **Framer Motion** | **Animation** | Powers all the specialized UI effects: page transitions (`AnimatePresence`), the "Nebula" background movement, hover effects, and staggered list appearances. |
| **@react-pdf/renderer** | **PDF Preview** | Allows reacting components to be rendered as a PDF document in the browser. Used for the real-time preview modal before downloading. |
| **react-pdf** | **PDF Viewing** | Used to display existing PDF files (distinct from *generating* them). |
| **React Quill** | **Rich Text Editing** | The core of the `ContentEditorPage`. Provides the WYSIWYG interface for editing minutes before finalization. |
| **Supabase JS** | **Auth & Data** | Official client for managing user sessions (Login/Logout) and interacting with Postgres (if needed directly). |
| **Marked** | **Formatting** | Parses Markdown returned by the AI into HTML for safe display in the UI. |
| **React Hot Toast** | **Notifications** | feedback popups (Success/Error messages) that appear at the top of the screen. |
| **Tailwind CSS** | **Styling** | Utility-first CSS framework used for layout, typography, and responsive design. |

### 5.3 Python Service (Transcription & Diarization)

| Library | Purpose | Details |
| :--- | :--- | :--- |
| **openai-whisper** | **Speech-to-Text** | The core transcription engine. Uses Transformer-based models to convert audio to text with high accuracy. |
| **pyannote.audio** | **Speaker Diarization** | Identifies "who spoke when". Uses a pre-trained segmentation model to label different speakers in the audio stream. |
| **FastAPI** | **API Framework** | Provides the high-performance async web server for the transcription service. |
| **Torch (PyTorch)** | **Deep Learning** | The underlying tensor library powering both Whisper and Pyannote. Manages CUDA/CPU offloading. |
| **Prometheus Client** | **Monitoring** | Exposes metrics (job counts, processing time) for potential observability. |
| **Python-Multipart** | **File Handling** | Handles large file uploads (audio binaries) efficiently in FastAPI. |

## 6. Summary

QuickFlow represents a robust, highly modular systems architecture. It successfully decouples the high-performance UI (React) from the logic-heavy backend (Spring Boot), connected by a secure and standard communication layer. The integration of local AI allows for powerful features without compromising data privacy or incurring high cloud usage costs.