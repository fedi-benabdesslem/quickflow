# Test Documentation

## 1. Overview

### Purpose of the Test Suite

This test suite provides automated verification for the core business logic and service layer of the QuickFlow backend application. The tests ensure reliability and correctness of critical functionality including:

- File processing and text extraction
- Encryption and decryption of sensitive tokens
- PDF document generation
- Meeting template management
- LLM service integration and prompt handling
- Template processing and caching
- Data model validation

### Testing Strategy

The test suite employs a **unit testing strategy** with the following characteristics:

- **Unit Tests**: All tests are isolated unit tests focusing on individual service classes
- **Mocking**: External dependencies (repositories, LLM clients) are mocked using Mockito
- **No Integration Tests**: Tests do not require running databases, LLM servers, or external services
- **Characterization Tests**: Some model tests document existing POJO behavior

The testing framework stack:
- **JUnit 5** (Jupiter) - Test execution framework
- **Mockito** - Mocking framework for dependency isolation
- **Spring Boot Test** - MockMultipartFile and other Spring test utilities

---

## 2. Test Inventory

### Service Tests

| Test File | Type | Component | Behaviors Validated |
|-----------|------|-----------|---------------------|
| `FileProcessingServiceTest.java` | Unit | FileProcessingService | Text extraction from .txt/.md files, file validation (null, empty, size limits, unsupported types), file extension handling |
| `EncryptionServiceTest.java` | Unit | EncryptionService | AES-256 encryption/decryption round-trips, null/empty handling, Base64 encoding, key length handling |
| `PdfGenerationServiceTest.java` | Unit | PdfGenerationService | PDF generation from HTML/markdown, metadata handling, footer preferences, XSS escaping, Unicode support |
| `MeetingTemplateServiceTest.java` | Unit | MeetingTemplateService | CRUD operations for templates, user authorization checks, duplicate name validation, usage tracking |
| `LLMServiceTest.java` | Unit | LLMService | JSON parsing from LLM responses, tone/length instructions, structured input building, quick mode extraction |
| `TemplateServiceTest.java` | Unit | TemplateService | Email subject extraction, request hash computation, caching behavior, user prompt building |

### Model Tests

| Test File | Type | Component | Behaviors Validated |
|-----------|------|-----------|---------------------|
| `TemplateTypeTest.java` | Unit | TemplateType enum | Case-insensitive parsing, invalid value handling, null handling, enum value existence |
| `ExtractedDataTest.java` | Unit | ExtractedData model | POJO getter/setter behavior, nested classes (ExtractedDecision, ExtractedActionItem), null handling |

### File Paths

```
backend/src/test/java/com/ai/application/
├── Services/
│   ├── FileProcessingServiceTest.java
│   ├── EncryptionServiceTest.java
│   ├── PdfGenerationServiceTest.java
│   ├── MeetingTemplateServiceTest.java
│   ├── LLMServiceTest.java
│   └── TemplateServiceTest.java
└── model/
    ├── TemplateTypeTest.java
    └── ExtractedDataTest.java
```

---

## 3. Coverage Explanation

### Critical Paths Covered

1. **File Processing Pipeline**
   - Text extraction from plain text and markdown files
   - File size validation (max 10MB)
   - File type validation (allowed: .txt, .md, .pdf, .docx)
   - Filename edge cases (null, empty, no extension, multiple dots)

2. **Security-Critical Encryption**
   - AES-256 encryption with random IV
   - Encrypt/decrypt round-trip integrity
   - OAuth token format handling
   - Key length normalization (short keys padded, long keys truncated)

3. **PDF Generation**
   - HTML to PDF conversion
   - Markdown syntax handling (bold, italic, headers, lists)
   - XSS prevention via HTML escaping in metadata
   - Unicode character support

4. **Template Management**
   - User-scoped CRUD operations
   - Authorization enforcement (users can only access their own templates)
   - Duplicate template name prevention
   - Usage statistics tracking

5. **LLM Integration**
   - JSON response parsing with code block unwrapping
   - Fallback behavior on invalid JSON
   - Prompt building for different tones and lengths
   - Date/time hint injection

6. **Email/Meeting Processing**
   - Subject line extraction from generated emails
   - Request hashing for response caching
   - Bullet point formatting
   - Meeting metadata inclusion in prompts

### Intentionally NOT Covered

| Area | Reason |
|------|--------|
| PDF/DOCX binary file extraction | Requires actual binary test files; belongs in integration tests |
| Visual PDF rendering verification | Cannot be automated; requires manual inspection |
| Actual LLM response quality | Mocked in tests; would require human evaluation |
| MongoDB persistence | Mocked via repository mocks; integration test territory |
| Concurrent access patterns | Requires multi-threaded integration tests |
| Controller/REST endpoints | Not part of current test scope |
| Frontend components | No frontend tests in this branch |
| External OAuth flows | Requires live OAuth providers |

### Known Limitations and Assumptions

1. **MockMultipartFile Behavior**: The `MockMultipartFile` class returns empty string (not null) when `originalFilename` is null, which affects some edge case tests
2. **Time-Sensitive Tests**: Tests involving timestamps assume near-instant execution
3. **Default Keys**: EncryptionService tests rely on a known test key; production uses environment variables
4. **LLM Client Mock**: Tests assume the LLM client returns well-formed responses or specific error patterns

---

## 4. How to Run the Tests

### Prerequisites

- **Java 17** or higher
- **Maven 3.8+** (or use the included Maven wrapper)

### Running All Tests

```bash
cd backend

# Using Maven wrapper (recommended)
./mvnw test

# Or using system Maven
mvn test
```

### Running Specific Test Classes

```bash
# Run a specific test class
./mvnw test -Dtest=FileProcessingServiceTest

# Run multiple test classes
./mvnw test -Dtest=FileProcessingServiceTest,EncryptionServiceTest

# Run tests matching a pattern
./mvnw test -Dtest=*ServiceTest
```

### Running Specific Test Methods

```bash
# Run a specific test method
./mvnw test -Dtest=EncryptionServiceTest#roundTripSimpleString

# Run tests in a nested class
./mvnw test -Dtest=FileProcessingServiceTest$ValidationTests
```

### Test Configuration

Tests use the configuration in `backend/src/test/resources/application-test.properties`:

```properties
# MongoDB auto-configuration excluded for unit tests
spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration,org.springframework.boot.autoconfigure.data.mongo.MongoDataAutoConfiguration

# Test encryption key (32 bytes for AES-256)
token.encryption.key=TestKey32BytesExactlyForAES256!!

# OAuth clients set to test placeholder values (not actual credentials)
google.oauth.client-id=test-client-id
microsoft.oauth.client-id=test-ms-client-id
```

### Required Setup

- **None** - All tests use mocks and do not require:
  - Running MongoDB instance
  - Running Ollama/LLM server
  - OAuth credentials
  - Network access

---

## 5. Test Design Notes

### Mocking Strategy

All tests use **Mockito** for dependency isolation:

```java
@ExtendWith(MockitoExtension.class)
class MeetingTemplateServiceTest {
    @Mock
    private MeetingTemplateRepository meetingTemplateRepository;

    @InjectMocks
    private MeetingTemplateService meetingTemplateService;
}
```

**Key mocking patterns:**
- **Repository mocks**: Return `Optional.of()` or `Optional.empty()` to simulate find operations
- **LLM client mock**: Returns predefined JSON strings to test parsing logic
- **ArgumentCaptor**: Used to verify saved entities have correct values

### Test Organization

Tests are organized using **JUnit 5 `@Nested` classes** for logical grouping:

```java
class FileProcessingServiceTest {
    @Nested
    @DisplayName("extractText() - Validation")
    class ValidationTests { ... }

    @Nested
    @DisplayName("extractText() - Text files (.txt)")
    class TextFileTests { ... }
}
```

### Fixtures and Test Data

- **No external test files**: All test data is created inline using `MockMultipartFile` or string constants
- **JSON constants**: Complex JSON structures are defined as static constants for readability
- **Helper methods**: `createMinimalStructuredRequest()` and similar helpers reduce test boilerplate

### Use of Spring Test Utilities

- **MockMultipartFile**: Used for file upload testing without actual files
- **StandardCharsets.UTF_8**: Ensures consistent encoding in text tests

### Assertion Patterns

- **assertThrows**: Used for expected exception testing
- **ArgumentMatchers**: `argThat()` for flexible prompt content verification
- **verify()**: Confirms mock interactions occurred as expected

### Rationale for Key Decisions

1. **No @SpringBootTest**: Tests instantiate services directly to avoid slow context loading
2. **No test database**: MongoDB is fully mocked to ensure fast, isolated tests
3. **Extensive null/empty tests**: These edge cases are common in real-world usage
4. **Security-focused PDF tests**: XSS prevention in metadata is a critical security control
5. **Characterization tests for models**: Document existing POJO behavior without prescribing changes
