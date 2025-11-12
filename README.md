# LLM Project Backend

A Spring Boot backend application for processing emails and meeting minutes (PV) using LLM services.

## 📋 Table of Contents

- [Prerequisites](#prerequisites)
- [Getting Started](#getting-started)
- [Running the Project](#running-the-project)
- [API Endpoints](#api-endpoints)
- [Project Structure](#project-structure)

## 🔧 Prerequisites

Before running this project, make sure you have the following installed:

- **Java 17** or higher
- **Maven** 3.6+ (or use the included Maven wrapper `mvnw`)
- **Docker** and **Docker Compose**
- **PostgreSQL** (or use Docker Compose to run it)

## 🚀 Getting Started

### Step 1: Clone the Repository

```bash
git clone <repository-url>
cd ELECTRONICA-AI-PROJECT1
```

### Step 2: Start Infrastructure Services

The project uses Docker Compose to run PostgreSQL and the mock LLM service. Start them with:

```bash
docker-compose up -d
```

This will start:
- **PostgreSQL** on port `5433`
  - Database: `llm_test`
  - Username: `llm_user`
  - Password: `llm_pass`
- **Mock LLM Service** on port `8081`

### Step 3: Build the Project

Using Maven wrapper (recommended):

```bash
# On Windows
.\mvnw.cmd clean install

# On Linux/Mac
./mvnw clean install
```

Or using Maven directly:

```bash
mvn clean install
```

### Step 4: Run the Application

Using Maven wrapper:

```bash
# On Windows
.\mvnw.cmd spring-boot:run

# On Linux/Mac
./mvnw spring-boot:run
```

Or using Maven directly:

```bash
mvn spring-boot:run
```

The application will start on **http://localhost:8080** (default Spring Boot port).

### Step 5: Verify the Setup

1. Check the health endpoint:
   ```bash
   curl http://localhost:8080/api/health
   ```

2. Check the home endpoint:
   ```bash
   curl http://localhost:8080/
   ```

## 📡 API Endpoints

### 🔐 Authentication Endpoints

#### Register a new user
```http
POST /register
Content-Type: application/json

{
  "username": "john_doe",
  "password": "password123",
  "email": "john@example.com"
}
```

**Response:**
```json
{
  "id": 1,
  "username": "john_doe",
  "email": "john@example.com"
}
```

#### Login
```http
POST /login
Content-Type: application/json

{
  "username": "john_doe",
  "password": "password123"
}
```

**Response:**
```
JWT_TOKEN_STRING
```

---

### 📧 Email Endpoints (`/api/email`)

#### Process an Email
```http
POST /api/email/process
Content-Type: application/json

{
  "userId": 1,
  "subject": "Project Update",
  "bulletPoints": [
    "Completed feature X",
    "Started work on feature Y",
    "Meeting scheduled for next week"
  ],
  "recipientEmails": [
    "recipient1@example.com",
    "recipient2@example.com"
  ]
}
```

**Response:**
```json
{
  "id": 1,
  "userId": 1,
  "subject": "Project Update",
  "bulletPoints": ["..."],
  "recipientEmails": ["..."],
  "generatedContent": "...",
  "createdAt": "2024-01-15T10:30:00"
}
```

#### Get Email by User and Request ID
```http
GET /api/email/{userId}/{requestId}
```

**Example:**
```bash
curl http://localhost:8080/api/email/1/1
```

#### List All Emails
```http
GET /api/email
```

**Example:**
```bash
curl http://localhost:8080/api/email
```

---

### 📋 Meeting Minutes (PV) Endpoints (`/api/pv`)

#### Process a Meeting Minutes (PV)
```http
POST /api/pv/process
Content-Type: application/json

{
  "userId": 1,
  "date": "2024-01-15",
  "startTime": "14:00",
  "closingTime": "16:00",
  "location": "Conference Room A",
  "participants": [
    "John Doe",
    "Jane Smith",
    "Bob Johnson"
  ],
  "bulletPoints": [
    "Discussed project timeline",
    "Reviewed budget allocation",
    "Assigned tasks for next sprint"
  ]
}
```

**Response:**
```json
{
  "id": 1,
  "userId": 1,
  "date": "2024-01-15",
  "startTime": "14:00",
  "closingTime": "16:00",
  "location": "Conference Room A",
  "participants": ["..."],
  "bulletPoints": ["..."],
  "generatedContent": "...",
  "createdAt": "2024-01-15T14:00:00"
}
```

#### Get PV by User and Request ID
```http
GET /api/pv/{userId}/{requestId}
```

**Example:**
```bash
curl http://localhost:8080/api/pv/1/1
```

#### List All PVs
```http
GET /api/pv
```

**Example:**
```bash
curl http://localhost:8080/api/pv
```

---

### 🔄 Legacy Endpoints (`/api/process/*`)

> **Note:** These endpoints are maintained for backward compatibility. It's recommended to use the new modular endpoints above.

#### Process Email (Legacy)
```http
POST /api/process/email
Content-Type: application/json

{
  "userId": 1,
  "subject": "Project Update",
  "bulletPoints": ["..."],
  "recipientEmails": ["..."]
}
```

#### Process PV (Legacy)
```http
POST /api/process/pv
Content-Type: application/json

{
  "userId": 1,
  "date": "2024-01-15",
  "startTime": "14:00",
  "closingTime": "16:00",
  "location": "...",
  "participants": ["..."],
  "bulletPoints": ["..."]
}
```

#### Get Email (Legacy)
```http
GET /api/process/email/{userId}/{requestId}
```

#### Get PV (Legacy)
```http
GET /api/process/pv/{userId}/{requestId}
```

#### List All Emails (Legacy)
```http
GET /api/process/email
```

#### List All PVs (Legacy)
```http
GET /api/process/pv
```

---

### 🏠 Utility Endpoints

#### Home/Health Check
```http
GET /
```

**Response:**
```
Hello World!
```

#### API Health Check
```http
GET /api/health
```

**Response:**
```json
{
  "status": "UP",
  "service": "LLM Project Backend"
}
```

---

## 🏗️ Project Structure

```
ELECTRONICA-AI-PROJECT1/
├── src/
│   ├── main/
│   │   ├── java/com/electronica/llmprojectbackend/
│   │   │   ├── controller/          # REST Controllers
│   │   │   ├── service/             # Business Logic
│   │   │   ├── model/               # Entity Models
│   │   │   ├── repo/                # JPA Repositories
│   │   │   ├── config/              # Configuration
│   │   │   └── filter/              # JWT Filters
│   │   └── resources/
│   │       └── application.properties
│   └── test/
├── mock-llm/                        # Mock LLM Service (Python/Flask)
├── docker-compose.yml               # Docker services configuration
├── pom.xml                          # Maven dependencies
└── README.md                        # This file
```

## 🔒 Security

The application uses:
- **Spring Security** for authentication
- **JWT (JSON Web Tokens)** for authorization
- Default credentials (for development):
  - Username: `user`
  - Password: `1234`

> **⚠️ Important:** Change these credentials in production!

## 🐳 Docker Services

### PostgreSQL
- **Port:** 5433
- **Database:** llm_test
- **Username:** llm_user
- **Password:** llm_pass

### Mock LLM Service
- **Port:** 8081
- **Health Check:** http://localhost:8081/health

## 🛠️ Troubleshooting

### Port Already in Use
If port 8080 is already in use, you can change it in `application.properties`:
```properties
server.port=8080
```

### Database Connection Issues
1. Ensure Docker Compose services are running:
   ```bash
   docker-compose ps
   ```
2. Check PostgreSQL logs:
   ```bash
   docker-compose logs postgres_test
   ```

### Mock LLM Service Not Responding
1. Check if the service is running:
   ```bash
   curl http://localhost:8081/health
   ```
2. View logs:
   ```bash
   docker-compose logs mock_llm
   ```

## 📝 Notes

- The application uses Hibernate's `ddl-auto=update` mode, which automatically creates/updates database tables.
- All endpoints support CORS (Cross-Origin Resource Sharing) with `origins = "*"` (configure properly for production).
- The mock LLM service returns placeholder responses. Replace it with your actual LLM service URL in `application.properties`.

## 🤝 Contributing

1. Create a feature branch
2. Make your changes
3. Test thoroughly
4. Submit a pull request

## 📄 License

[Add your license information here]

