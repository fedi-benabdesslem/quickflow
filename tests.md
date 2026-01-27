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
- **OAuth token storage and management**
- **Contact management and sync operations**
- **Contact group management**
- **Email provider routing**
- **QuickFlow user detection**

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
| `TokenStorageServiceTest.java` | Unit | TokenStorageService | Token encryption/storage, token retrieval/decryption, expiration checking, token refresh updates |
| `ContactServiceTest.java` | Unit | ContactService | Contact CRUD, import/sync operations, search/filtering, favorite toggling, usage tracking |
| `GroupServiceTest.java` | Unit | GroupService | Group CRUD, member management, duplicate name validation, search functionality |
| `EmailProviderServiceTest.java` | Unit | EmailProviderService | Provider routing (Google/Azure), error handling, capability checking |
| `QuickFlowDetectionServiceTest.java` | Unit | QuickFlowDetectionService | QuickFlow user detection, case-insensitive matching, status updates |

### Model Tests

| Test File | Type | Component | Behaviors Validated |
|-----------|------|-----------|---------------------|
| `TemplateTypeTest.java` | Unit | TemplateType enum | Case-insensitive parsing, invalid value handling, null handling, enum value existence |
| `ExtractedDataTest.java` | Unit | ExtractedData model | POJO getter/setter behavior, nested classes (ExtractedDecision, ExtractedActionItem, ExtractedParticipant), null handling |
| `UserTokenTest.java` | Unit | UserToken entity | Token expiration checks, email capability checks, constructor behavior |
| `ContactTest.java` | Unit | Contact entity | Usage tracking (incrementUsage), sync marking (markSynced), QuickFlow detection fields |
| `GroupTest.java` | Unit | Group entity | Member management (add/remove), member count calculation |

### File Paths

```
backend/src/test/java/com/ai/application/
├── Services/
│   ├── FileProcessingServiceTest.java
│   ├── EncryptionServiceTest.java
│   ├── PdfGenerationServiceTest.java
│   ├── MeetingTemplateServiceTest.java
│   ├── LLMServiceTest.java
│   ├── TemplateServiceTest.java
│   ├── TokenStorageServiceTest.java          # NEW
│   ├── ContactServiceTest.java               # NEW
│   ├── GroupServiceTest.java                 # NEW
│   ├── EmailProviderServiceTest.java         # NEW
│   └── QuickFlowDetectionServiceTest.java    # NEW
└── model/
    ├── TemplateTypeTest.java
    ├── ExtractedDataTest.java
    └── Entity/
        ├── UserTokenTest.java                # NEW
        ├── ContactTest.java                  # NEW
        └── GroupTest.java                    # NEW
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

7. **OAuth Token Management** (NEW)
   - Secure token storage with encryption
   - Token retrieval with decryption
   - Token expiration detection (5-minute buffer)
   - Token refresh updates
   - Provider type validation

8. **Contact Management** (NEW)
   - Contact CRUD operations
   - Import/sync from OAuth providers (Google/Microsoft)
   - Favorite toggling and usage tracking
   - Search and filtering by source, favorites, QuickFlow status
   - Soft delete with ignore flag for OAuth contacts

9. **Group Management** (NEW)
   - Group CRUD with duplicate name validation
   - Member add/remove operations
   - Search functionality with case-insensitive matching
   - Member count tracking

10. **Email Provider Routing** (NEW)
    - Provider detection (Google, Azure, email)
    - Routing to appropriate service (Gmail/Microsoft Graph)
    - Error handling for missing tokens
    - Attachment support routing

11. **QuickFlow User Detection** (NEW)
    - Detection of QuickFlow users among contacts
    - Case-insensitive email matching
    - Status update when users join/leave QuickFlow
    - User-specific detection for manual triggers

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
| Actual Gmail/Microsoft API calls | Requires credentials; mocked in tests |
| Scheduled job execution | Tested via direct method calls, not scheduler |

### Known Limitations and Assumptions

1. **MockMultipartFile Behavior**: The `MockMultipartFile` class returns empty string (not null) when `originalFilename` is null, which affects some edge case tests
2. **Time-Sensitive Tests**: Tests involving timestamps assume near-instant execution
3. **Default Keys**: EncryptionService tests rely on a known test key; production uses environment variables
4. **LLM Client Mock**: Tests assume the LLM client returns well-formed responses or specific error patterns
5. **Reflection for DI**: Some service tests use reflection to inject mocks due to `@Autowired` field injection

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

# Run new service tests only
./mvnw test -Dtest=TokenStorageServiceTest,ContactServiceTest,GroupServiceTest,EmailProviderServiceTest,QuickFlowDetectionServiceTest
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

---

## 6. New Tests Added (This Branch)

### TokenStorageServiceTest.java

**Feature Covered**: OAuth token storage and retrieval with encryption

**Behaviors Validated**:
- Token encryption before storage
- Token decryption on retrieval
- Expiration time calculation
- Token existence checking
- Token updates after refresh
- Token deletion

**How to Execute**:
```bash
./mvnw test -Dtest=TokenStorageServiceTest
```

### ContactServiceTest.java

**Feature Covered**: Contact management including import/sync from OAuth providers

**Behaviors Validated**:
- CRUD operations for contacts
- Import logic with duplicate detection
- Update restrictions for OAuth vs manual contacts
- Soft delete with ignore flag for OAuth contacts
- Favorite toggling
- Search with sorting (favorites first, then by usage)
- Filtering by source, favorites, QuickFlow status

**How to Execute**:
```bash
./mvnw test -Dtest=ContactServiceTest
```

### GroupServiceTest.java

**Feature Covered**: Contact group management

**Behaviors Validated**:
- Group CRUD operations
- Duplicate name prevention per user
- Member add/remove operations
- Group search with case-insensitive matching
- Member preview with contact details
- Exclusion of deleted contacts from member lists

**How to Execute**:
```bash
./mvnw test -Dtest=GroupServiceTest
```

### EmailProviderServiceTest.java

**Feature Covered**: Email sending via OAuth providers

**Behaviors Validated**:
- Provider routing (Google → Gmail, Azure → Microsoft Graph)
- Error handling for missing tokens
- Unsupported provider detection
- Re-authentication requirement detection
- Attachment routing
- Email capability checking

**How to Execute**:
```bash
./mvnw test -Dtest=EmailProviderServiceTest
```

### QuickFlowDetectionServiceTest.java

**Feature Covered**: Detection of QuickFlow users among contacts

**Behaviors Validated**:
- Detection of registered QuickFlow users
- Case-insensitive email matching
- Handling of null/empty emails
- Status clearing when user is no longer registered
- No-op when status is unchanged
- Filtering of invalid QuickFlow user emails

**How to Execute**:
```bash
./mvnw test -Dtest=QuickFlowDetectionServiceTest
```

### Entity Tests (UserTokenTest, ContactTest, GroupTest)

**Features Covered**: Entity model behavior

**Behaviors Validated**:
- **UserToken**: Token expiration checks, email sending capability by provider type
- **Contact**: Usage tracking (incrementUsage), sync marking (markSynced), QuickFlow fields
- **Group**: Member add/remove, member count, duplicate prevention in addMember

**How to Execute**:
```bash
./mvnw test -Dtest=UserTokenTest,ContactTest,GroupTest
```
