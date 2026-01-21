# Claude Opus Prompt: Add Email Sending Capability to Full-Stack App

## Project Overview

I have a full-stack application with the following **ALREADY COMPLETED** components:

**Tech Stack:**
- ✅ React frontend (already migrated from Angular)
- ✅ Spring Boot backend
- ✅ Supabase (cloud) for authentication
- ✅ MongoDB database
- ✅ Mistral-Nemo AI (running locally) for content generation
- ✅ Dark immersive nebula themed UI/UX (with breathing glow effects)

**Current Authentication:**
- ✅ Google OAuth (working)
- ✅ Microsoft OAuth (implemented but cannot test yet - requires Azure premium)
- ✅ Email/Password signup with confirmation (working)

**Current Features:**
The app has two AI-powered features that are partially implemented:

1. **AI Meeting Minutes Generator**
   - User fills form (meeting date, time, attendees, topics, action items)
   - Mistral-Nemo generates professional meeting minutes text
   - Currently: Text is generated but cannot be sent as email

2. **AI Professional Email Writer**
   - User inputs recipient and casual message description
   - Mistral-Nemo generates professional email with auto-generated subject
   - Currently: Email content is generated but cannot be sent

---

## What I Need You to Build

Implement a complete email sending system that allows users to **send emails from their own email accounts** (Gmail or Outlook) using OAuth tokens. This requires:

### Core Requirements:

1. **OAuth Configuration Updates**
   - Update Google OAuth to request `gmail.send` scope
   - Update Microsoft OAuth to request `Mail.Send` scope
   - Ensure refresh tokens are obtained for both providers

2. **Token Management System**
   - Extract OAuth tokens from Supabase after successful login
   - Store tokens **encrypted** in MongoDB (use AES-256)
   - Implement **automatic token refresh** when access tokens expire
   - Handle token refresh failures gracefully

3. **Email Sending Integration**
   - Implement Gmail API integration (Spring Boot)
   - Implement Microsoft Graph API integration (Spring Boot)
   - Support email attachments (for PDFs)
   - Provider-based routing (detect if user is Google/Microsoft/Email)

4. **PDF Generation & Storage**
   - Generate PDFs from rich text HTML content
   - Filename format: `Meeting_Minutes_[YYYY-MM-DD].pdf`
   - Store PDFs in MongoDB GridFS for future access
   - Support PDF preview in React frontend

5. **Rich Text Editing**
   - Implement rich text editor for both features (use React Quill or similar)
   - Features needed: Bold, italic, underline, headers, lists, basic formatting
   - Allow users to edit AI-generated content before sending

6. **Complete User Flows**
   - Full end-to-end implementation of both features
   - Proper error handling and user feedback
   - Loading states during AI generation, PDF creation, and email sending

---

## Detailed Feature Specifications

### Feature 1: AI Meeting Minutes with PDF & Email

**User Flow:**

1. **Form Input:**
   - Meeting date (date picker)
   - Meeting time
   - Attendees (comma-separated names/emails)
   - Topics discussed (textarea)
   - Action items (textarea)
   - Additional notes (optional)

2. **AI Generation:**
   - User clicks "Generate" button
   - Frontend calls Spring Boot endpoint
   - Spring Boot calls Mistral-Nemo API (already integrated)
   - Mistral generates professional meeting minutes **text**
   - Display loading state during generation

3. **Rich Text Editing:**
   - Show generated content in **rich text editor** (React Quill)
   - User can fully edit/modify the content
   - Editor toolbar: bold, italic, underline, headers (H1-H3), bullet/numbered lists

4. **PDF Preview:**
   - User clicks "Preview PDF" button
   - Spring Boot generates PDF from edited HTML content
   - PDF displayed in modal using `react-pdf` or iframe
   - User can return to editor if changes needed

5. **Email Sending:**
   - User enters recipient email addresses (comma-separated)
   - User clicks "Send Email" button
   - **Provider Detection Logic:**
     - **If Google user:** Send via Gmail API with PDF attached
     - **If Microsoft user:** Send via Microsoft Graph API with PDF attached  
     - **If Email/Password user:** Show toast notification: "Email sending not supported for your domain yet. Coming soon!"
   - Email subject: `"Meeting Minutes - [Date]"`
   - Email body: `"Please find attached the meeting minutes from [Date]."`

6. **Storage:**
   - Save PDF to MongoDB GridFS
   - Store meeting minutes metadata in MongoDB collection
   - Include references to PDF file, recipients, timestamps

---

### Feature 2: AI Professional Email Writer

**User Flow:**

1. **Form Input:**
   - Recipient email address
   - Casual/simple message description (textarea)

2. **AI Generation:**
   - User clicks "Generate" button
   - Frontend calls Spring Boot endpoint
   - Mistral-Nemo generates:
     - Professional email body
     - Auto-generated subject line
   - Display loading state

3. **Rich Text Editing:**
   - Show generated email in rich text editor
   - User can edit both subject line and body
   - Same editor features as meeting minutes

4. **Email Sending:**
   - User clicks "Send" button
   - **Provider Detection Logic:**
     - **If Google user:** Send via Gmail API
     - **If Microsoft user:** Send via Microsoft Graph API
     - **If Email/Password user:** Show toast: "Email sending not supported for your domain yet. Coming soon!"

---

## Technical Implementation Requirements

### 1. OAuth Scope Configuration

**Update authentication to request email sending permissions:**

**Google OAuth:**
- Scope: `https://www.googleapis.com/auth/gmail.send`
- Additional params: `access_type: 'offline'` and `prompt: 'consent'` to ensure refresh token

**Microsoft OAuth:**
- Scope: `Mail.Send`
- Additional scope: `offline_access` to ensure refresh token

**Important:** Users will see OAuth consent screen requesting email sending permission.

---

### 2. Token Extraction & Storage

**After OAuth Login:**
1. Extract `provider_token` (access token) from Supabase session
2. Extract `provider_refresh_token` from Supabase session
3. Send to Spring Boot backend via API call
4. Encrypt both tokens using AES-256
5. Store in MongoDB with user information

**MongoDB User Schema:**
```
{
  _id: ObjectId,
  supabaseId: String,  // Link to Supabase user
  email: String,
  provider: String,    // "google", "azure", or "email"
  oauthTokens: {
    accessToken: String,     // Encrypted
    refreshToken: String,    // Encrypted
    expiresAt: DateTime      // When access token expires
  },
  createdAt: DateTime,
  updatedAt: DateTime
}
```

---

### 3. Automatic Token Refresh

**Logic:**

When sending an email:
1. Check if access token is expired or expiring soon (within 5 minutes)
2. If expired:
   - Use refresh token to get new access token
   - Update encrypted access token in MongoDB
   - Update expiration time
   - Retry the email send operation
3. If refresh token is invalid/expired:
   - Return error: "Please sign in again to send emails"
4. If other errors (network, API down):
   - Return error: "Service not available, try later"

**This should be transparent to the user** - they should never know tokens are being refreshed.

---

### 4. Gmail API Integration

**Requirements:**
- Use Google API Client Library for Java
- Send emails from user's Gmail account
- Support PDF attachments
- Email format: HTML body
- Use user's encrypted access token for authentication

**Email Structure:**
- From: User's Gmail address
- To: Recipient(s)
- Subject: As specified
- Body: HTML content
- Attachment: PDF (if applicable)

---

### 5. Microsoft Graph API Integration

**Requirements:**
- Use Microsoft Graph SDK for Java
- Send emails from user's Outlook/Office 365 account
- Support PDF attachments
- Email format: HTML body
- Use user's encrypted access token for authentication

**Email Structure:**
- From: User's Microsoft email address
- To: Recipient(s)
- Subject: As specified
- Body: HTML content
- Attachment: PDF (if applicable)

---

### 6. PDF Generation

**Requirements:**
- Use iText, Apache PDFBox, or similar library
- Convert rich text HTML content to professional PDF
- Clean, professional formatting
- Include meeting date in header
- Title: "Meeting Minutes"
- Proper spacing and readability

**PDF Storage:**
- Store in MongoDB GridFS (for files larger than 16MB BSON limit)
- Associate with meeting minutes record
- Enable future retrieval/download

---

### 7. Error Handling

**All error scenarios must be handled:**

| Scenario | User Message |
|----------|-------------|
| Mistral-Nemo unavailable | "Service not available, try later" |
| PDF generation fails | "Service not available, try later" |
| Gmail/Graph API down | "Service not available, try later" |
| Access token expired | → Auto-refresh → Retry (silent) |
| Refresh token invalid | "Please sign in again to send emails" |
| User provider is "email" | Toast: "Email sending not supported for your domain yet. Coming soon!" |

---

### 8. React Components Needed

**Components to build:**

1. **RichTextEditor** component
   - Wrapper around React Quill
   - Configured toolbar with essential formatting options
   - Props: value, onChange, placeholder

2. **MeetingMinutes** component
   - Meeting form
   - AI generation trigger
   - Rich text editor integration
   - PDF preview modal
   - Email sending flow
   - Recipient input
   - Loading states and error handling

3. **ProfessionalEmail** component
   - Email form (recipient + message description)
   - AI generation trigger
   - Rich text editor for subject and body
   - Email sending flow
   - Loading states and error handling

4. **PdfPreview** component (or modal)
   - Display PDF using react-pdf
   - Close/back button
   - Send email action

5. **Toast notifications** component
   - Success messages
   - Error messages
   - Unsupported provider warnings

---

### 9. Spring Boot Services & Controllers

**Required Services:**

1. **TokenStorageService**
   - Store encrypted tokens in MongoDB
   - Retrieve and decrypt tokens
   - Update tokens after refresh

2. **TokenRefreshService**
   - Check token expiration
   - Refresh Google tokens via Google API
   - Refresh Microsoft tokens via Graph API
   - Update database with new tokens

3. **EncryptionService**
   - AES-256 encryption for tokens
   - Secure key management (from environment variables)

4. **GmailService**
   - Send emails via Gmail API
   - Support attachments
   - Handle authentication with access tokens

5. **GraphService**
   - Send emails via Microsoft Graph API
   - Support attachments
   - Handle authentication with access tokens

6. **PdfService**
   - Generate PDFs from HTML content
   - Professional formatting
   - Return byte array

7. **GridFsService**
   - Store PDFs in GridFS
   - Retrieve PDFs by ID

**Required Controllers:**

1. **AuthController**
   - Endpoint: `POST /api/auth/store-tokens`
   - Store OAuth tokens after login

2. **EmailController**
   - Endpoint: `POST /api/email/send-meeting-minutes`
   - Endpoint: `POST /api/email/send-professional-email`

3. **PdfController** (optional)
   - Endpoint: `POST /api/pdf/preview` (for preview before sending)

---

### 10. MongoDB Collections

**Collections to create/update:**

1. **users** collection
   - Store user info and encrypted OAuth tokens

2. **meeting_minutes** collection
   - Store meeting minutes metadata
   - Reference to GridFS PDF file
   - Timestamps, recipients, etc.

3. **GridFS** (files and chunks)
   - Automatic with GridFS setup
   - Stores PDF binary data

---

## Important Implementation Notes

1. **Testing:** Currently, only test with Google OAuth. Microsoft OAuth is fully implemented but cannot be tested until Azure premium account is available. Leave Microsoft implementation ready but note it's untested.

2. **Security:**
   - Never log or expose unencrypted tokens
   - Use environment variables for encryption keys
   - Validate all user inputs
   - Sanitize HTML content before PDF generation

3. **Dependencies:** Add necessary Maven dependencies:
   - Google API Client
   - Gmail API
   - Microsoft Graph SDK
   - iText (or Apache PDFBox)
   - Spring Data MongoDB
   - React Quill (npm)
   - react-pdf (npm)
   - react-hot-toast (npm)

4. **UI/UX:** Maintain the existing dark immersive nebula theme throughout all new components. Ensure smooth transitions, loading states, and elegant error handling.

5. **Provider Detection:** The provider type is stored during authentication. Use this to route email sending to the correct API.

6. **Future Considerations:** Design the system to be extensible for future features like:
   - Email history/dashboard
   - PDF templates customization
   - Resend functionality
   - User preferences

---

## Deliverables

Provide a complete, production-ready implementation including:

1. ✅ Updated OAuth configuration with proper scopes
2. ✅ Token extraction from Supabase and storage in MongoDB
3. ✅ Encryption service for token security
4. ✅ Automatic token refresh logic
5. ✅ Gmail API integration with attachment support
6. ✅ Microsoft Graph API integration with attachment support
7. ✅ PDF generation from rich text HTML
8. ✅ GridFS setup and storage
9. ✅ Rich text editor components (React)
10. ✅ Complete Meeting Minutes feature (frontend + backend)
11. ✅ Complete Professional Email feature (frontend + backend)
12. ✅ Provider detection and routing
13. ✅ Comprehensive error handling
14. ✅ Toast notifications for user feedback
15. ✅ All necessary Spring Boot services and controllers
16. ✅ MongoDB schema definitions
17. ✅ Updated dependencies (pom.xml and package.json)

---

## Testing Instructions

After implementation:

1. **Google OAuth Flow:**
   - Sign in with Google
   - Verify email sending permission is requested
   - Generate meeting minutes
   - Edit content
   - Preview PDF
   - Send to test recipient
   - Verify email arrives from user's Gmail

2. **Professional Email:**
   - Generate professional email
   - Edit content
   - Send to test recipient
   - Verify email arrives from user's Gmail

3. **Email/Password Users:**
   - Sign up with email
   - Try to send email
   - Verify toast appears: "Email sending not supported for your domain yet. Coming soon!"

4. **Token Refresh:**
   - Wait for token to expire (or manually set expired time in DB)
   - Try sending email
   - Verify automatic refresh works
   - Email sends successfully

---

## Additional Context

- The Mistral-Nemo integration is already working - you just need to call existing endpoints
- The UI theme uses dark backgrounds with deep blue and magenta glows - match this aesthetic
- Users should have a seamless experience - they shouldn't know about OAuth complexity
- Error messages should be friendly and actionable
- The app is in development phase - no rate limiting needed yet

---

Please implement this comprehensively and ensure all components work together seamlessly. Focus on clean, maintainable code with proper error handling and user feedback throughout.