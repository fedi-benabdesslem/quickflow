package com.ai.application.Services;

import com.ai.application.Repositories.SessionRepository;
import com.ai.application.model.Entity.User;
import com.ai.application.model.Entity.UserSession;
import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;
import java.util.UUID;

/**
 * Service for JWT token generation and validation, and refresh token
 * management.
 */
@Service
public class TokenService {

    private static final Logger logger = LoggerFactory.getLogger(TokenService.class);
    private static final long ACCESS_TOKEN_EXPIRY_MINUTES = 15;
    private static final long REFRESH_TOKEN_EXPIRY_DAYS = 30;
    private static final long MFA_TOKEN_EXPIRY_MINUTES = 5;
    private static final long LINKING_TOKEN_EXPIRY_MINUTES = 10;
    private static final long EMAIL_VERIFY_TOKEN_EXPIRY_HOURS = 24;
    private static final long PASSWORD_RESET_TOKEN_EXPIRY_HOURS = 1;

    private final Algorithm algorithm;
    private final JWTVerifier verifier;
    private final SessionRepository sessionRepository;
    private final BCryptPasswordEncoder passwordEncoder;

    public TokenService(@Value("${app.jwt.secret}") String jwtSecret,
            SessionRepository sessionRepository) {
        this.algorithm = Algorithm.HMAC256(jwtSecret);
        this.verifier = JWT.require(algorithm).build();
        this.sessionRepository = sessionRepository;
        this.passwordEncoder = new BCryptPasswordEncoder(12);
    }

    // ── Access Token ──

    public String generateAccessToken(User user) {
        return JWT.create()
                .withSubject(user.getId())
                .withClaim("email", user.getEmail())
                .withClaim("name", user.getName())
                .withClaim("role", user.getRole())
                .withClaim("type", "access")
                .withIssuedAt(new Date())
                .withExpiresAt(Date.from(Instant.now().plusSeconds(ACCESS_TOKEN_EXPIRY_MINUTES * 60)))
                .sign(algorithm);
    }

    // ── Refresh Token ──

    public String generateRefreshToken(User user, String deviceInfo, String ipAddress, String loginProvider) {
        String rawToken = UUID.randomUUID().toString() + "-" + UUID.randomUUID().toString();
        String tokenHash = passwordEncoder.encode(rawToken);

        LocalDateTime expiresAt = LocalDateTime.now().plusDays(REFRESH_TOKEN_EXPIRY_DAYS);

        UserSession session = new UserSession(
                user.getId(),
                tokenHash,
                deviceInfo,
                ipAddress,
                loginProvider,
                expiresAt);
        sessionRepository.save(session);

        return rawToken;
    }

    // ── Token Refresh (Rotation) ──

    public TokenPair refreshTokens(String refreshToken, String deviceInfo, String ipAddress) {
        List<UserSession> activeSessions = findSessionByRefreshToken(refreshToken);

        if (activeSessions.isEmpty()) {
            throw new InvalidTokenException("Invalid or expired refresh token");
        }

        UserSession matchedSession = activeSessions.get(0);

        // Revoke old session (rotation)
        matchedSession.setRevoked(true);
        sessionRepository.save(matchedSession);

        return new TokenPair(matchedSession.getUserId(), deviceInfo, ipAddress, matchedSession.getLoginProvider());
    }

    /**
     * Find matching session by checking bcrypt hash against active sessions.
     */
    private List<UserSession> findSessionByRefreshToken(String rawToken) {
        // Get all non-revoked sessions — then check bcrypt match
        // This is NOT efficient at scale, but fine for small-to-medium user bases
        List<UserSession> allActive = sessionRepository.findAll().stream()
                .filter(s -> !s.isRevoked() && s.getExpiresAt().isAfter(LocalDateTime.now()))
                .toList();

        return allActive.stream()
                .filter(s -> passwordEncoder.matches(rawToken, s.getRefreshTokenHash()))
                .toList();
    }

    // ── Logout ──

    public void logout(String refreshToken) {
        List<UserSession> sessions = findSessionByRefreshToken(refreshToken);
        for (UserSession session : sessions) {
            session.setRevoked(true);
            sessionRepository.save(session);
        }
    }

    public void logoutAllDevices(String userId) {
        List<UserSession> sessions = sessionRepository.findByUserIdAndRevokedFalse(userId);
        for (UserSession session : sessions) {
            session.setRevoked(true);
            sessionRepository.save(session);
        }
    }

    // ── MFA Token ──

    public String generateMfaToken(User user) {
        return JWT.create()
                .withSubject(user.getId())
                .withClaim("type", "mfa_pending")
                .withIssuedAt(new Date())
                .withExpiresAt(Date.from(Instant.now().plusSeconds(MFA_TOKEN_EXPIRY_MINUTES * 60)))
                .sign(algorithm);
    }

    // ── Linking Token ──

    public String generateLinkingToken(String userId, String provider) {
        return JWT.create()
                .withSubject(userId)
                .withClaim("provider", provider)
                .withClaim("type", "oauth_link")
                .withIssuedAt(new Date())
                .withExpiresAt(Date.from(Instant.now().plusSeconds(LINKING_TOKEN_EXPIRY_MINUTES * 60)))
                .sign(algorithm);
    }

    public LinkingInfo extractFromLinkingToken(String token) {
        DecodedJWT jwt = verifyToken(token);
        if (jwt == null || !"oauth_link".equals(jwt.getClaim("type").asString())) {
            throw new InvalidTokenException("Invalid linking token");
        }
        return new LinkingInfo(jwt.getSubject(), jwt.getClaim("provider").asString());
    }

    // ── Email Verification / Password Reset Token ──

    public String generateEmailToken(String userId, String purpose) {
        long expiryHours = "verify_email".equals(purpose)
                ? EMAIL_VERIFY_TOKEN_EXPIRY_HOURS
                : PASSWORD_RESET_TOKEN_EXPIRY_HOURS;

        return JWT.create()
                .withSubject(userId)
                .withClaim("purpose", purpose)
                .withClaim("type", "email_action")
                .withIssuedAt(new Date())
                .withExpiresAt(Date.from(Instant.now().plusSeconds(expiryHours * 3600)))
                .sign(algorithm);
    }

    public String extractFromEmailToken(String token, String expectedPurpose) {
        DecodedJWT jwt = verifyToken(token);
        if (jwt == null || !"email_action".equals(jwt.getClaim("type").asString())) {
            throw new InvalidTokenException("Invalid email token");
        }
        String purpose = jwt.getClaim("purpose").asString();
        if (!expectedPurpose.equals(purpose)) {
            throw new InvalidTokenException("Token purpose mismatch");
        }
        return jwt.getSubject();
    }

    // ── Validation ──

    public String extractUserId(String token) {
        DecodedJWT jwt = verifyToken(token);
        return jwt != null ? jwt.getSubject() : null;
    }

    public boolean validateToken(String token) {
        return verifyToken(token) != null;
    }

    private DecodedJWT verifyToken(String token) {
        try {
            return verifier.verify(token);
        } catch (JWTVerificationException e) {
            logger.debug("Token verification failed: {}", e.getMessage());
            return null;
        }
    }

    // ── Helper: get active sessions for user ──

    public List<UserSession> getActiveSessions(String userId) {
        return sessionRepository.findByUserIdAndRevokedFalse(userId);
    }

    public void revokeSession(String sessionId) {
        sessionRepository.findById(sessionId).ifPresent(session -> {
            session.setRevoked(true);
            sessionRepository.save(session);
        });
    }

    public void revokeAllSessionsExcept(String userId, String currentRefreshToken) {
        List<UserSession> sessions = sessionRepository.findByUserIdAndRevokedFalse(userId);
        for (UserSession session : sessions) {
            if (!passwordEncoder.matches(currentRefreshToken, session.getRefreshTokenHash())) {
                session.setRevoked(true);
                sessionRepository.save(session);
            }
        }
    }

    // ── Inner classes ──

    public static class TokenPair {
        private final String userId;
        private final String deviceInfo;
        private final String ipAddress;
        private final String loginProvider;

        public TokenPair(String userId, String deviceInfo, String ipAddress, String loginProvider) {
            this.userId = userId;
            this.deviceInfo = deviceInfo;
            this.ipAddress = ipAddress;
            this.loginProvider = loginProvider;
        }

        public String getUserId() {
            return userId;
        }

        public String getDeviceInfo() {
            return deviceInfo;
        }

        public String getIpAddress() {
            return ipAddress;
        }

        public String getLoginProvider() {
            return loginProvider;
        }
    }

    public static class LinkingInfo {
        private final String userId;
        private final String provider;

        public LinkingInfo(String userId, String provider) {
            this.userId = userId;
            this.provider = provider;
        }

        public String getUserId() {
            return userId;
        }

        public String getProvider() {
            return provider;
        }
    }

    public static class InvalidTokenException extends RuntimeException {
        public InvalidTokenException(String message) {
            super(message);
        }
    }
}
