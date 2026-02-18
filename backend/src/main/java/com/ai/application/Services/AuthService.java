package com.ai.application.Services;

import com.ai.application.Repositories.UserRepository;
import com.ai.application.model.Entity.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Core authentication business logic: signup, login, password management.
 */
@Service
public class AuthService {

    private static final Logger logger = LoggerFactory.getLogger(AuthService.class);
    private final UserRepository userRepository;
    private final TokenService tokenService;
    private final DomainDetectionService domainDetectionService;
    private final SystemEmailService systemEmailService;
    private final BCryptPasswordEncoder passwordEncoder;

    @Value("${app.frontend.url}")
    private String frontendUrl;

    public AuthService(UserRepository userRepository,
            TokenService tokenService,
            DomainDetectionService domainDetectionService,
            SystemEmailService systemEmailService) {
        this.userRepository = userRepository;
        this.tokenService = tokenService;
        this.domainDetectionService = domainDetectionService;
        this.systemEmailService = systemEmailService;
        this.passwordEncoder = new BCryptPasswordEncoder(12);
    }

    // ── Signup ──

    public AuthResult signup(String email, String password, String name,
            String deviceInfo, String ipAddress) {
        email = email.trim().toLowerCase();

        if (userRepository.existsByEmail(email)) {
            throw new AuthException("Email already registered. Try signing in.", 409);
        }

        // Validate
        validatePassword(password);
        validateName(name);

        // Create user
        User user = new User();
        user.setEmail(email);
        user.setName(name.trim());
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setLocalAuthEnabled(true);
        user.setEmailVerified(false);

        // MX detection
        try {
            String domain = email.substring(email.indexOf('@') + 1);
            String hosting = domainDetectionService.detectProvider(domain);
            user.setDetectedHostingProvider(hosting);
        } catch (Exception e) {
            logger.warn("MX detection failed for {}: {}", email, e.getMessage());
            user.setDetectedHostingProvider("unknown");
        }

        user = userRepository.save(user);

        // Send verification email
        try {
            String verifyToken = tokenService.generateEmailToken(user.getId(), "verify_email");
            systemEmailService.sendVerificationEmail(user, verifyToken);
        } catch (Exception e) {
            logger.warn("Failed to send verification email: {}", e.getMessage());
        }

        // Generate tokens
        String accessToken = tokenService.generateAccessToken(user);
        String refreshToken = tokenService.generateRefreshToken(user, deviceInfo, ipAddress, "local");

        user.setLastLoginAt(LocalDateTime.now());
        user.setLastLoginProvider("local");
        user.setLoginCount(1);
        userRepository.save(user);

        return new AuthResult(accessToken, refreshToken, user);
    }

    // ── Login ──

    public AuthResult login(String email, String password, String deviceInfo, String ipAddress) {
        email = email.trim().toLowerCase();

        Optional<User> userOpt = userRepository.findByEmail(email);
        if (userOpt.isEmpty()) {
            throw new AuthException("Invalid email or password", 401);
        }

        User user = userOpt.get();

        if (!user.isLocalAuthEnabled()) {
            String provider = user.getPrimaryOAuthProvider() != null ? user.getPrimaryOAuthProvider() : "OAuth";
            throw new AuthException(
                    "This account uses " + provider + " sign-in. Please click 'Sign in with " + provider + "'.",
                    401,
                    provider);
        }

        if (user.getPasswordHash() == null || !passwordEncoder.matches(password, user.getPasswordHash())) {
            throw new AuthException("Invalid email or password", 401);
        }

        if (user.isAccountDisabled()) {
            throw new AuthException("Account disabled", 403);
        }

        // Check MFA
        if (user.isMfaEnabled()) {
            String mfaToken = tokenService.generateMfaToken(user);
            return AuthResult.mfaRequired(mfaToken);
        }

        // Generate tokens
        String accessToken = tokenService.generateAccessToken(user);
        String refreshToken = tokenService.generateRefreshToken(user, deviceInfo, ipAddress, "local");

        user.setLastLoginAt(LocalDateTime.now());
        user.setLastLoginProvider("local");
        user.setLoginCount(user.getLoginCount() + 1);
        userRepository.save(user);

        return new AuthResult(accessToken, refreshToken, user);
    }

    // ── Refresh ──

    public AuthResult refresh(String refreshToken, String deviceInfo, String ipAddress) {
        TokenService.TokenPair pair = tokenService.refreshTokens(refreshToken, deviceInfo, ipAddress);

        User user = userRepository.findById(pair.getUserId())
                .orElseThrow(() -> new AuthException("User not found", 401));

        if (user.isAccountDisabled()) {
            throw new AuthException("Account disabled", 403);
        }

        String newAccessToken = tokenService.generateAccessToken(user);
        String newRefreshToken = tokenService.generateRefreshToken(
                user, pair.getDeviceInfo(), pair.getIpAddress(), pair.getLoginProvider());

        return new AuthResult(newAccessToken, newRefreshToken, user);
    }

    // ── Forgot Password ──

    public void forgotPassword(String email) {
        email = email.trim().toLowerCase();
        Optional<User> userOpt = userRepository.findByEmail(email);

        // Always return success to not reveal email existence
        if (userOpt.isEmpty() || !userOpt.get().isLocalAuthEnabled()) {
            return;
        }

        User user = userOpt.get();
        String resetToken = tokenService.generateEmailToken(user.getId(), "reset_password");

        try {
            systemEmailService.sendPasswordResetEmail(user, resetToken);
        } catch (Exception e) {
            logger.error("Failed to send password reset email: {}", e.getMessage());
        }
    }

    // ── Reset Password ──

    public void resetPassword(String token, String newPassword) {
        String userId = tokenService.extractFromEmailToken(token, "reset_password");
        validatePassword(newPassword);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AuthException("User not found", 404));

        user.setPasswordHash(passwordEncoder.encode(newPassword));
        user.setLocalAuthEnabled(true);
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);

        // Revoke all sessions (force re-login)
        tokenService.logoutAllDevices(userId);
    }

    // ── Email Verification ──

    public void verifyEmail(String token) {
        String userId = tokenService.extractFromEmailToken(token, "verify_email");

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AuthException("User not found", 404));

        user.setEmailVerified(true);
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);
    }

    public void sendVerificationEmail(User user) {
        if (user.isEmailVerified())
            return;

        String verifyToken = tokenService.generateEmailToken(user.getId(), "verify_email");
        systemEmailService.sendVerificationEmail(user, verifyToken);
    }

    // ── User DTO ──

    public Map<String, Object> toPublicDTO(User user) {
        Map<String, Object> dto = new HashMap<>();
        dto.put("id", user.getId());
        dto.put("email", user.getEmail());
        dto.put("name", user.getName());
        dto.put("profilePhotoUrl", user.getProfilePhotoUrl());
        dto.put("role", user.getRole());
        dto.put("plan", user.getPlan());
        dto.put("emailVerified", user.isEmailVerified());
        dto.put("localAuthEnabled", user.isLocalAuthEnabled());
        dto.put("primaryOAuthProvider", user.getPrimaryOAuthProvider());
        dto.put("connectedProviders", user.getConnectedProviderNames());
        dto.put("detectedHostingProvider", user.getDetectedHostingProvider());
        dto.put("smtpConfigured", user.isSmtpConfigured());
        dto.put("canSendEmail", user.canSendEmail());
        dto.put("trialExpiresAt", user.getTrialExpiresAt());
        dto.put("createdAt", user.getCreatedAt());
        dto.put("mfaEnabled", user.isMfaEnabled());
        return dto;
    }

    // ── Validation ──

    private void validatePassword(String password) {
        if (password == null || password.length() < 8 || password.length() > 128) {
            throw new AuthException("Password must be between 8 and 128 characters", 400);
        }
        boolean hasLetter = password.chars().anyMatch(Character::isLetter);
        boolean hasDigit = password.chars().anyMatch(Character::isDigit);
        if (!hasLetter || !hasDigit) {
            throw new AuthException("Password must contain at least one letter and one number", 400);
        }
    }

    private void validateName(String name) {
        if (name == null || name.trim().length() < 2 || name.trim().length() > 100) {
            throw new AuthException("Name must be between 2 and 100 characters", 400);
        }
    }

    // ── Result class ──

    public static class AuthResult {
        private final String accessToken;
        private final String refreshToken;
        private final User user;
        private final boolean requiresMfa;
        private final String mfaToken;

        public AuthResult(String accessToken, String refreshToken, User user) {
            this.accessToken = accessToken;
            this.refreshToken = refreshToken;
            this.user = user;
            this.requiresMfa = false;
            this.mfaToken = null;
        }

        private AuthResult(String mfaToken) {
            this.accessToken = null;
            this.refreshToken = null;
            this.user = null;
            this.requiresMfa = true;
            this.mfaToken = mfaToken;
        }

        public static AuthResult mfaRequired(String mfaToken) {
            return new AuthResult(mfaToken);
        }

        public String getAccessToken() {
            return accessToken;
        }

        public String getRefreshToken() {
            return refreshToken;
        }

        public User getUser() {
            return user;
        }

        public boolean isRequiresMfa() {
            return requiresMfa;
        }

        public String getMfaToken() {
            return mfaToken;
        }
    }

    // ── Exception class ──

    public static class AuthException extends RuntimeException {
        private final int statusCode;
        private final String suggestedProvider;

        public AuthException(String message, int statusCode) {
            super(message);
            this.statusCode = statusCode;
            this.suggestedProvider = null;
        }

        public AuthException(String message, int statusCode, String suggestedProvider) {
            super(message);
            this.statusCode = statusCode;
            this.suggestedProvider = suggestedProvider;
        }

        public int getStatusCode() {
            return statusCode;
        }

        public String getSuggestedProvider() {
            return suggestedProvider;
        }
    }
}
