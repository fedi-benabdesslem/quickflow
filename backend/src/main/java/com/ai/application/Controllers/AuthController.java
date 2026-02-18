package com.ai.application.Controllers;

import com.ai.application.Repositories.UserRepository;
import com.ai.application.Services.AuthService;
import com.ai.application.Services.RateLimitService;
import com.ai.application.Services.TokenService;
import com.ai.application.model.Entity.User;
import com.ai.application.model.Entity.UserSession;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.*;

/**
 * Authentication controller — handles signup, login, token refresh, logout,
 * password reset, email verification, sessions, and OAuth linking.
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

    private final AuthService authService;
    private final TokenService tokenService;
    private final UserRepository userRepository;
    private final RateLimitService rateLimitService;

    @Value("${app.backend.url}")
    private String backendUrl;

    @Value("${app.frontend.url}")
    private String frontendUrl;

    public AuthController(AuthService authService,
            TokenService tokenService,
            UserRepository userRepository,
            RateLimitService rateLimitService) {
        this.authService = authService;
        this.tokenService = tokenService;
        this.userRepository = userRepository;
        this.rateLimitService = rateLimitService;
    }

    // ── Signup ──

    @PostMapping("/signup")
    public ResponseEntity<?> signup(@RequestBody Map<String, String> body,
            HttpServletRequest request, HttpServletResponse response) {
        int wait = rateLimitService.checkRateLimit("/api/auth/signup", request.getRemoteAddr());
        if (wait > 0)
            return rateLimited(wait);

        try {
            String email = body.get("email");
            String password = body.get("password");
            String name = body.get("name");

            if (email == null || password == null || name == null) {
                return error("Email, password, and name are required", 400);
            }

            AuthService.AuthResult result = authService.signup(
                    email, password, name,
                    request.getHeader("User-Agent"),
                    request.getRemoteAddr());

            setRefreshTokenCookie(response, result.getRefreshToken());

            return ResponseEntity.ok(Map.of(
                    "accessToken", result.getAccessToken(),
                    "user", authService.toPublicDTO(result.getUser())));
        } catch (AuthService.AuthException e) {
            return error(e.getMessage(), e.getStatusCode());
        }
    }

    // ── Login ──

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> body,
            HttpServletRequest request, HttpServletResponse response) {
        int wait = rateLimitService.checkRateLimit("/api/auth/login", request.getRemoteAddr());
        if (wait > 0)
            return rateLimited(wait);

        try {
            String email = body.get("email");
            String password = body.get("password");

            if (email == null || password == null) {
                return error("Email and password are required", 400);
            }

            AuthService.AuthResult result = authService.login(
                    email, password,
                    request.getHeader("User-Agent"),
                    request.getRemoteAddr());

            if (result.isRequiresMfa()) {
                return ResponseEntity.ok(Map.of(
                        "requiresMfa", true,
                        "mfaToken", result.getMfaToken()));
            }

            setRefreshTokenCookie(response, result.getRefreshToken());

            return ResponseEntity.ok(Map.of(
                    "accessToken", result.getAccessToken(),
                    "user", authService.toPublicDTO(result.getUser())));
        } catch (AuthService.AuthException e) {
            Map<String, Object> errorBody = new HashMap<>();
            errorBody.put("error", e.getMessage());
            if (e.getSuggestedProvider() != null) {
                errorBody.put("suggestedProvider", e.getSuggestedProvider());
            }
            return ResponseEntity.status(e.getStatusCode()).body(errorBody);
        }
    }

    // ── Refresh ──

    @PostMapping("/refresh")
    public ResponseEntity<?> refresh(HttpServletRequest request, HttpServletResponse response) {
        int wait = rateLimitService.checkRateLimit("/api/auth/refresh", request.getRemoteAddr());
        if (wait > 0)
            return rateLimited(wait);

        String refreshToken = extractRefreshToken(request);
        if (refreshToken == null) {
            return error("No refresh token", 401);
        }

        try {
            AuthService.AuthResult result = authService.refresh(
                    refreshToken,
                    request.getHeader("User-Agent"),
                    request.getRemoteAddr());

            setRefreshTokenCookie(response, result.getRefreshToken());

            return ResponseEntity.ok(Map.of(
                    "accessToken", result.getAccessToken(),
                    "user", authService.toPublicDTO(result.getUser())));
        } catch (Exception e) {
            clearRefreshTokenCookie(response);
            return error("Invalid refresh token", 401);
        }
    }

    // ── Logout ──

    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpServletRequest request, HttpServletResponse response) {
        String refreshToken = extractRefreshToken(request);
        if (refreshToken != null) {
            try {
                tokenService.logout(refreshToken);
            } catch (Exception e) {
                logger.debug("Logout cleanup failed: {}", e.getMessage());
            }
        }
        clearRefreshTokenCookie(response);
        return ResponseEntity.ok(Map.of("message", "Logged out"));
    }

    // ── Get Current User ──

    @GetMapping("/me")
    public ResponseEntity<?> me(Principal principal) {
        if (principal == null)
            return error("Not authenticated", 401);

        Optional<User> userOpt = userRepository.findById(principal.getName());
        if (userOpt.isEmpty())
            return error("User not found", 404);

        return ResponseEntity.ok(authService.toPublicDTO(userOpt.get()));
    }

    // ── Email Verification ──

    @PostMapping("/send-verification")
    public ResponseEntity<?> sendVerification(Principal principal) {
        if (principal == null)
            return error("Not authenticated", 401);

        Optional<User> userOpt = userRepository.findById(principal.getName());
        if (userOpt.isEmpty())
            return error("User not found", 404);

        authService.sendVerificationEmail(userOpt.get());
        return ResponseEntity.ok(Map.of("message", "Verification email sent"));
    }

    @PostMapping("/verify-email")
    public ResponseEntity<?> verifyEmail(@RequestBody Map<String, String> body) {
        String token = body.get("token");
        if (token == null)
            return error("Token is required", 400);

        try {
            authService.verifyEmail(token);
            return ResponseEntity.ok(Map.of("message", "Email verified"));
        } catch (Exception e) {
            return error("Invalid or expired token", 400);
        }
    }

    // ── Password Reset ──

    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@RequestBody Map<String, String> body,
            HttpServletRequest request) {
        int wait = rateLimitService.checkRateLimit("/api/auth/forgot-password", request.getRemoteAddr());
        if (wait > 0)
            return rateLimited(wait);

        String email = body.get("email");
        if (email == null)
            return error("Email is required", 400);

        authService.forgotPassword(email);
        // Always return success to not reveal email existence
        return ResponseEntity.ok(Map.of("message", "If an account exists, a reset email has been sent"));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@RequestBody Map<String, String> body) {
        String token = body.get("token");
        String newPassword = body.get("password");

        if (token == null || newPassword == null) {
            return error("Token and new password are required", 400);
        }

        try {
            authService.resetPassword(token, newPassword);
            return ResponseEntity.ok(Map.of("message", "Password reset successfully"));
        } catch (AuthService.AuthException e) {
            return error(e.getMessage(), e.getStatusCode());
        } catch (Exception e) {
            return error("Invalid or expired token", 400);
        }
    }

    // ── Sessions ──

    @GetMapping("/sessions")
    public ResponseEntity<?> getSessions(Principal principal) {
        if (principal == null)
            return error("Not authenticated", 401);

        List<UserSession> sessions = tokenService.getActiveSessions(principal.getName());

        List<Map<String, Object>> sessionDtos = sessions.stream().map(s -> {
            Map<String, Object> dto = new HashMap<>();
            dto.put("id", s.getId());
            dto.put("deviceInfo", s.getDeviceInfo());
            dto.put("ipAddress", s.getIpAddress());
            dto.put("loginProvider", s.getLoginProvider());
            dto.put("createdAt", s.getCreatedAt());
            dto.put("lastActiveAt", s.getLastActiveAt());
            return dto;
        }).toList();

        return ResponseEntity.ok(sessionDtos);
    }

    @DeleteMapping("/sessions/{sessionId}")
    public ResponseEntity<?> revokeSession(@PathVariable String sessionId, Principal principal) {
        if (principal == null)
            return error("Not authenticated", 401);
        tokenService.revokeSession(sessionId);
        return ResponseEntity.ok(Map.of("message", "Session revoked"));
    }

    @DeleteMapping("/sessions")
    public ResponseEntity<?> revokeAllSessions(Principal principal, HttpServletRequest request) {
        if (principal == null)
            return error("Not authenticated", 401);

        String currentRefreshToken = extractRefreshToken(request);
        if (currentRefreshToken != null) {
            tokenService.revokeAllSessionsExcept(principal.getName(), currentRefreshToken);
        } else {
            tokenService.logoutAllDevices(principal.getName());
        }
        return ResponseEntity.ok(Map.of("message", "All other sessions revoked"));
    }

    // NOTE: OAuth linking endpoints are handled by OAuthLinkingController
    // (/api/auth/link)

    // ── Providers ──

    @GetMapping("/providers")
    public ResponseEntity<?> getProviders(Principal principal) {
        if (principal == null)
            return error("Not authenticated", 401);

        Optional<User> userOpt = userRepository.findById(principal.getName());
        if (userOpt.isEmpty())
            return error("User not found", 404);

        User user = userOpt.get();
        return ResponseEntity.ok(Map.of(
                "localAuth", user.isLocalAuthEnabled(),
                "connectedProviders", user.getConnectedProviderNames(),
                "primaryOAuthProvider", user.getPrimaryOAuthProvider() != null ? user.getPrimaryOAuthProvider() : "",
                "detectedHostingProvider",
                user.getDetectedHostingProvider() != null ? user.getDetectedHostingProvider() : "unknown"));
    }

    // ── Store OAuth Tokens (for frontend callback flow) ──

    @PostMapping("/store-tokens")
    public ResponseEntity<?> storeTokens(@RequestBody Map<String, Object> body, Principal principal) {
        if (principal == null)
            return error("Not authenticated", 401);
        // This endpoint is kept for backward compatibility but tokens are now
        // stored automatically during the OAuth success handler flow.
        return ResponseEntity.ok(Map.of("status", "ok"));
    }

    // ── Helpers ──

    private String extractRefreshToken(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if ("refreshToken".equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }
        return null;
    }

    private void setRefreshTokenCookie(HttpServletResponse response, String refreshToken) {
        String cookieValue = String.format(
                "refreshToken=%s; HttpOnly; Secure; SameSite=None; Path=/api/auth; Max-Age=%d",
                refreshToken, 30 * 24 * 60 * 60);
        response.addHeader("Set-Cookie", cookieValue);
    }

    private void clearRefreshTokenCookie(HttpServletResponse response) {
        String cookieValue = "refreshToken=; HttpOnly; Secure; SameSite=None; Path=/api/auth; Max-Age=0";
        response.addHeader("Set-Cookie", cookieValue);
    }

    private ResponseEntity<Map<String, Object>> error(String message, int status) {
        return ResponseEntity.status(status).body(Map.of("error", message));
    }

    private ResponseEntity<Map<String, Object>> rateLimited(int waitSeconds) {
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(Map.of(
                "error", "Too many requests",
                "retryAfter", waitSeconds));
    }
}
