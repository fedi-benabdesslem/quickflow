# Security Flow Documentation

## Overview

This application implements a dual authentication system:
1. **Basic Authentication** (HTTP Basic Auth) - for initial authentication
2. **JWT Token Authentication** (Bearer Token) - for stateless session management

The security architecture uses Spring Security with JWT tokens for stateless authentication, ensuring secure access to protected API endpoints.

## Security Architecture

### Components

1. **SecurityConfig** (`securityConfig.java`)
   - Configures Spring Security filter chain
   - Sets up authentication provider
   - Configures password encoder
   - Defines public and protected endpoints

2. **JWTFilter** (`JWTFilter.java`)
   - Intercepts all incoming requests
   - Validates JWT tokens from Authorization header
   - Sets authentication in SecurityContext

3. **JWTService** (`JWTService.java`)
   - Generates JWT tokens
   - Validates JWT tokens
   - Extracts user information from tokens

4. **MyuserDetailsService** (`MyuserDetailsService.java`)
   - Loads user details from database
   - Implements Spring Security's UserDetailsService

5. **UserService** (`UserService.java`)
   - Handles user registration
   - Handles user authentication/login
   - Generates JWT tokens after successful authentication

## Authentication Flow

### 1. User Registration

```
Client Request
    ↓
POST /register
    ↓
UserController.register()
    ↓
UserService.register()
    ↓
Password hashed with BCrypt (strength 12)
    ↓
User saved to database
    ↓
User object returned (password excluded)
```

**Endpoint:** `POST /register`

**Request Body:**
```json
{
  "id": 1,
  "username": "user123",
  "password": "plaintextPassword"
}
```

**Process:**
- Password is hashed using BCryptPasswordEncoder with strength 12
- User is saved to the database
- No authentication required (public endpoint)

### 2. User Login/Authentication

```
Client Request
    ↓
POST /login (with Basic Auth credentials)
    ↓
UserController.login()
    ↓
UserService.verify()
    ↓
AuthenticationManager.authenticate()
    ↓
MyuserDetailsService.loadUserByUsername()
    ↓
User credentials validated
    ↓
JWTService.generateToken()
    ↓
JWT token returned to client
```

**Endpoint:** `POST /login`

**Authentication:** Basic Authentication required
- Username: User's username
- Password: User's plaintext password

**Request Body:**
```json
{
  "username": "user123",
  "password": "plaintextPassword"
}
```

**Response:**
- Success: JWT token string
- Failure: "fail" string

**JWT Token Details:**
- **Algorithm:** HMAC SHA-256
- **Expiration:** 30 hours (108,000 seconds)
- **Secret Key:** Generated at application startup using KeyGenerator
- **Claims:** Contains username as subject

### 3. Protected Request Flow

```
Client Request with JWT Token
    ↓
JWTFilter.doFilterInternal()
    ↓
Extract "Bearer <token>" from Authorization header
    ↓
JWTService.extractUserName(token)
    ↓
Validate token signature and expiration
    ↓
MyuserDetailsService.loadUserByUsername()
    ↓
JWTService.validateToken()
    ↓
Set Authentication in SecurityContext
    ↓
Request proceeds to controller
```

**Protected Endpoints:**
- All endpoints except `/register` and `/login` require authentication
- Examples:
  - `POST /api/email/process`
  - `GET /api/email/{userId}/{requestId}`
  - `POST /api/pv/process`
  - `GET /api/pv/{userId}/{requestId}`

**Request Header:**
```
Authorization: Bearer <jwt_token>
```

**Alternative Authentication:**
- Basic Authentication is also supported for protected endpoints
- Header: `Authorization: Basic <base64_encoded_credentials>`

## Security Configuration

### Public Endpoints

The following endpoints are publicly accessible (no authentication required):
- `POST /register` - User registration
- `POST /login` - User authentication

### Protected Endpoints

All other endpoints require authentication:
- JWT token in Authorization header (preferred)
- OR Basic Authentication

### Security Features

1. **Password Encoding**
   - Algorithm: BCrypt
   - Strength: 12 (cost factor)
   - One-way hashing (passwords cannot be retrieved)

2. **JWT Token Security**
   - Secret key generated at application startup
   - HMAC SHA-256 signing algorithm
   - Token expiration: 30 hours
   - Token validation includes:
     - Signature verification
     - Expiration check
     - Username verification

3. **Session Management**
   - Stateless (no server-side sessions)
   - Each request is authenticated independently
   - SecurityContext is set per request

4. **CSRF Protection**
   - Disabled (appropriate for REST API)
   - Stateless authentication eliminates CSRF risk

## Error Handling

### JWT Token Validation Errors

The JWTFilter handles various JWT validation errors gracefully:

1. **ExpiredJwtException**
   - Token has expired
   - Logged as warning
   - Request continues without authentication (will be rejected by Spring Security)

2. **MalformedJwtException**
   - Token format is invalid
   - Logged as warning
   - Request continues without authentication

3. **SignatureException**
   - Token signature is invalid
   - Logged as warning
   - Request continues without authentication

4. **General Exception**
   - Any other JWT-related error
   - Logged as warning
   - Request continues without authentication

**Note:** If token validation fails, the request will be rejected by Spring Security's authorization check since no authentication is set in SecurityContext.

## Security Best Practices

### Current Implementation

✅ **Good Practices:**
- Passwords are hashed with BCrypt (strong algorithm)
- JWT tokens have expiration
- Stateless authentication (scalable)
- Token validation includes multiple checks
- Secret key is cryptographically secure

⚠️ **Considerations:**
- JWT secret key is generated at startup (different per instance)
- Consider using environment variables or configuration for secret key in production
- Token expiration is fixed (consider refresh tokens for better UX)
- No role-based authorization (all authenticated users have same access)
- CSRF is disabled (acceptable for REST API, but ensure proper CORS configuration)

### Recommendations for Production

1. **Secret Key Management**
   - Store JWT secret key in environment variables or secure configuration
   - Use the same secret key across all instances for consistency

2. **Token Refresh**
   - Implement refresh token mechanism
   - Shorter access token expiration (e.g., 15 minutes)
   - Longer refresh token expiration (e.g., 7 days)

3. **Role-Based Access Control**
   - Add user roles to User entity
   - Implement role-based authorization for different endpoints

4. **CORS Configuration**
   - Configure CORS properly for production
   - Restrict allowed origins, methods, and headers

5. **Rate Limiting**
   - Implement rate limiting for login and registration endpoints
   - Prevent brute force attacks

6. **Password Policy**
   - Enforce strong password requirements
   - Minimum length, complexity requirements

7. **Audit Logging**
   - Log authentication attempts (success and failure)
   - Log access to sensitive endpoints

## Example Usage

### 1. Register a New User

```bash
POST http://localhost:8080/register
Content-Type: application/json

{
  "id": 1,
  "username": "testuser",
  "password": "securePassword123"
}
```

### 2. Login and Get JWT Token

```bash
POST http://localhost:8080/login
Authorization: Basic dGVzdHVzZXI6c2VjdXJlUGFzc3dvcmQxMjM=
Content-Type: application/json

{
  "username": "testuser",
  "password": "securePassword123"
}
```

**Response:**
```
eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

### 3. Access Protected Endpoint with JWT

```bash
POST http://localhost:8080/api/email/process
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
Content-Type: application/json

{
  "userId": 1,
  "subject": "Test Subject",
  "bulletPoints": ["Point 1", "Point 2"],
  "recipientEmails": ["test@example.com"]
}
```

### 4. Access Protected Endpoint with Basic Auth

```bash
GET http://localhost:8080/api/email/1/1
Authorization: Basic dGVzdHVzZXI6c2VjdXJlUGFzc3dvcmQxMjM=
```

## Security Flow Diagram

```
┌─────────────┐
│   Client    │
└──────┬──────┘
       │
       │ 1. POST /register (public)
       ├──────────────────────────────────┐
       │                                  │
       │ 2. POST /login (Basic Auth)      │
       ├──────────────────────────────────┤
       │                                  │
       │ 3. Receive JWT Token             │
       │                                  │
       │ 4. Request with JWT Token        │
       ├──────────────────────────────────┤
       │                                  ▼
       │                          ┌───────────────┐
       │                          │   JWTFilter   │
       │                          │  (validates)  │
       │                          └───────┬───────┘
       │                                  │
       │                          ┌───────▼───────┐
       │                          │ SecurityConfig│
       │                          │  (authorizes) │
       │                          └───────┬───────┘
       │                                  │
       │                          ┌───────▼───────┐
       │                          │   Controller  │
       │                          │   (processes) │
       │                          └───────────────┘
       │
       │ 5. Response
       └──────────────────────────────────────────┘
```

## Code References

### Security Configuration
```30:39:src/main/java/com/electronica/llmprojectbackend/config/securityConfig.java
public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    return http
            .csrf(c->c.disable())
            .authorizeHttpRequests(request->request
                    .requestMatchers("/register","/login").permitAll()
                    .anyRequest().authenticated())
            .httpBasic(Customizer.withDefaults())
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)
            .build();
}
```

### JWT Filter
```30:64:src/main/java/com/electronica/llmprojectbackend/filter/JWTFilter.java
protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
    String authHeader = request.getHeader("Authorization");
    String token = null;
    String username = null;

    if (authHeader != null && authHeader.startsWith("Bearer ")) {
        token = authHeader.substring(7);
        try {
            username = jwtService.extractUserName(token);
        } catch (ExpiredJwtException e) {
            // Token expiré - continuer sans authentification
            logger.warn("JWT token has expired: " + e.getMessage());
        } catch (MalformedJwtException e) {
            // Token malformé - continuer sans authentification
            logger.warn("JWT token is malformed: " + e.getMessage());
        } catch (SignatureException e) {
            // Signature invalide - continuer sans authentification
            logger.warn("JWT signature validation failed: " + e.getMessage());
        } catch (Exception e) {
            // Autre erreur JWT - continuer sans authentification
            logger.warn("JWT token validation failed: " + e.getMessage());
        }
    }

    if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
        UserDetails userDetails = context.getBean(MyuserDetailsService.class).loadUserByUsername(username);
        if (jwtService.validateToken(token, userDetails)) {
            UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
            authToken.setDetails(new WebAuthenticationDetailsSource()
                    .buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(authToken);
        }
    }

    filterChain.doFilter(request, response);
}
```

### JWT Token Generation
```33:44:src/main/java/com/electronica/llmprojectbackend/service/JWTService.java
public String generateToken(String username) {
    Map<String, Object> claims = new HashMap<>();
    return Jwts.builder()
            .claims()
            .add(claims)
            .subject(username)
            .issuedAt(new Date(System.currentTimeMillis()))
            .expiration(new Date(System.currentTimeMillis() + 60 * 60 * 30))
            .and()
            .signWith(getKey())
            .compact();

}
```

### Password Encoding
```19:22:src/main/java/com/electronica/llmprojectbackend/service/UserService.java
public Users register(Users user) {
    user.setPassword(encoder.encode(user.getPassword()));
    repo.save(user);
    return user;
}
```

## Conclusion

This application implements a secure authentication system using JWT tokens with Spring Security. The system is stateless, scalable, and provides secure access to protected endpoints. The dual authentication support (JWT and Basic Auth) provides flexibility for different client types while maintaining security standards.

