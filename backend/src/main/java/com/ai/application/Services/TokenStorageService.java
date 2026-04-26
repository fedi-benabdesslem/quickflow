package com.ai.application.Services;

import com.ai.application.Repositories.UserRepository;
import com.ai.application.model.Entity.AuthConnection;
import com.ai.application.model.Entity.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Service for storing and retrieving encrypted OAuth tokens.
 * 
 * Uses the User.authConnections model to find and manage OAuth tokens.
 */
@Service
public class TokenStorageService {

    private static final Logger logger = LoggerFactory.getLogger(TokenStorageService.class);

    private final UserRepository userRepository;
    private final EncryptionService encryptionService;

    @Autowired
    public TokenStorageService(UserRepository userRepository,
            EncryptionService encryptionService) {
        this.userRepository = userRepository;
        this.encryptionService = encryptionService;
    }

    /**
     * Retrieves and decrypts OAuth tokens for a user.
     * Looks up User by email (primary) or by ID (fallback).
     * Returns null if no tokens found.
     */
    public DecryptedTokens getDecryptedTokens(String userId) {
        Optional<User> userOpt = userRepository.findByEmail(userId);
        if (userOpt.isEmpty()) {
            userOpt = userRepository.findById(userId);
        }

        if (userOpt.isEmpty()) {
            logger.debug("No User found for '{}'", userId);
            return null;
        }

        User user = userOpt.get();
        List<AuthConnection> connections = user.getAuthConnections();

        if (connections == null || connections.isEmpty()) {
            logger.debug("User '{}' has no auth connections", userId);
            return null;
        }

        // Find the best connection with tokens (prefer primary OAuth provider)
        AuthConnection tokenConnection = null;

        String primaryProvider = user.getPrimaryOAuthProvider();
        if (primaryProvider != null) {
            tokenConnection = connections.stream()
                    .filter(c -> primaryProvider.equalsIgnoreCase(c.getProvider()))
                    .filter(c -> c.getAccessTokenEncrypted() != null)
                    .findFirst()
                    .orElse(null);
        }

        // If no primary, take any connection with tokens
        if (tokenConnection == null) {
            tokenConnection = connections.stream()
                    .filter(c -> c.getAccessTokenEncrypted() != null)
                    .findFirst()
                    .orElse(null);
        }

        if (tokenConnection == null) {
            logger.debug("User '{}' has connections but none with stored tokens", userId);
            return null;
        }

        try {
            String accessToken = encryptionService.decrypt(tokenConnection.getAccessTokenEncrypted());
            String refreshToken = tokenConnection.getRefreshTokenEncrypted() != null
                    ? encryptionService.decrypt(tokenConnection.getRefreshTokenEncrypted())
                    : null;

            logger.info("Found OAuth tokens for user '{}' (provider: {})",
                    userId, tokenConnection.getProvider());

            return new DecryptedTokens(
                    accessToken,
                    refreshToken,
                    tokenConnection.getTokenExpiresAt(),
                    tokenConnection.getProvider(),
                    tokenConnection.getProviderEmail() != null
                            ? tokenConnection.getProviderEmail()
                            : user.getEmail());
        } catch (Exception e) {
            logger.error("Failed to decrypt tokens for user '{}'", userId, e);
            return null;
        }
    }

    /**
     * Updates tokens after a refresh operation.
     */
    public void updateTokensAfterRefresh(String userId, String newAccessToken, long expiresInSeconds) {
        Optional<User> userOpt = userRepository.findByEmail(userId);
        if (userOpt.isEmpty()) {
            userOpt = userRepository.findById(userId);
        }

        if (userOpt.isPresent()) {
            User user = userOpt.get();
            List<AuthConnection> connections = user.getAuthConnections();
            if (connections != null) {
                String primaryProvider = user.getPrimaryOAuthProvider();
                AuthConnection conn = connections.stream()
                        .filter(c -> primaryProvider != null && primaryProvider.equalsIgnoreCase(c.getProvider()))
                        .findFirst()
                        .orElse(connections.stream()
                                .filter(c -> c.getAccessTokenEncrypted() != null)
                                .findFirst()
                                .orElse(null));

                if (conn != null) {
                    conn.setAccessTokenEncrypted(encryptionService.encrypt(newAccessToken));
                    conn.setTokenExpiresAt(LocalDateTime.now().plusSeconds(expiresInSeconds));
                    conn.setLastUsedAt(LocalDateTime.now());
                    userRepository.save(user);
                    logger.info("Updated access token for user '{}'", userId);
                }
            }
        }
    }

    /**
     * Checks if a user has valid OAuth tokens stored.
     */
    public boolean hasValidTokens(String userId) {
        DecryptedTokens tokens = getDecryptedTokens(userId);
        return tokens != null && tokens.getAccessToken() != null;
    }

    /**
     * Record class for holding decrypted tokens.
     */
    public static class DecryptedTokens {
        private final String accessToken;
        private final String refreshToken;
        private final LocalDateTime expiresAt;
        private final String provider;
        private final String email;

        public DecryptedTokens(String accessToken, String refreshToken,
                LocalDateTime expiresAt, String provider, String email) {
            this.accessToken = accessToken;
            this.refreshToken = refreshToken;
            this.expiresAt = expiresAt;
            this.provider = provider;
            this.email = email;
        }

        public String getAccessToken() {
            return accessToken;
        }

        public String getRefreshToken() {
            return refreshToken;
        }

        public LocalDateTime getExpiresAt() {
            return expiresAt;
        }

        public String getProvider() {
            return provider;
        }

        public String getEmail() {
            return email;
        }

        public boolean isExpiredOrExpiringSoon() {
            return expiresAt == null || LocalDateTime.now().plusMinutes(5).isAfter(expiresAt);
        }
    }
}
