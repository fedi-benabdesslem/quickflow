package com.ai.application.Services;

import com.ai.application.Repositories.UserTokenRepository;
import com.ai.application.model.Entity.UserToken;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Service for storing and retrieving encrypted OAuth tokens.
 * 
 * Handles the storage lifecycle of user OAuth tokens in MongoDB,
 * ensuring tokens are encrypted at rest.
 */
@Service
public class TokenStorageService {

    private final UserTokenRepository userTokenRepository;
    private final EncryptionService encryptionService;

    @Autowired
    public TokenStorageService(UserTokenRepository userTokenRepository, EncryptionService encryptionService) {
        this.userTokenRepository = userTokenRepository;
        this.encryptionService = encryptionService;
    }

    /**
     * Stores OAuth tokens for a user, encrypting them before storage.
     * Updates existing record if user already has tokens stored.
     */
    public UserToken storeTokens(String supabaseId, String email, String provider,
            String accessToken, String refreshToken, long expiresInSeconds) {

        // Encrypt tokens before storage
        String encryptedAccessToken = encryptionService.encrypt(accessToken);
        String encryptedRefreshToken = encryptionService.encrypt(refreshToken);

        // Calculate expiration time
        LocalDateTime expiresAt = LocalDateTime.now().plusSeconds(expiresInSeconds);

        // Find existing or create new
        UserToken userToken = userTokenRepository.findBySupabaseId(supabaseId)
                .orElse(new UserToken(supabaseId, email, provider));

        // Update token info
        userToken.setEmail(email);
        userToken.setProvider(provider);
        userToken.setAccessToken(encryptedAccessToken);
        userToken.setRefreshToken(encryptedRefreshToken);
        userToken.setExpiresAt(expiresAt);
        userToken.setUpdatedAt(LocalDateTime.now());

        return userTokenRepository.save(userToken);
    }

    /**
     * Retrieves and decrypts OAuth tokens for a user.
     * Returns null if no tokens found.
     */
    public DecryptedTokens getDecryptedTokens(String supabaseId) {
        Optional<UserToken> tokenOpt = userTokenRepository.findBySupabaseId(supabaseId);

        if (tokenOpt.isEmpty()) {
            return null;
        }

        UserToken userToken = tokenOpt.get();

        return new DecryptedTokens(
                encryptionService.decrypt(userToken.getAccessToken()),
                encryptionService.decrypt(userToken.getRefreshToken()),
                userToken.getExpiresAt(),
                userToken.getProvider(),
                userToken.getEmail());
    }

    /**
     * Gets the UserToken entity without decrypting tokens.
     * Useful for checking provider type or expiration.
     */
    public Optional<UserToken> getUserToken(String supabaseId) {
        return userTokenRepository.findBySupabaseId(supabaseId);
    }

    /**
     * Updates tokens after a refresh operation.
     */
    public void updateTokensAfterRefresh(String supabaseId, String newAccessToken, long expiresInSeconds) {
        userTokenRepository.findBySupabaseId(supabaseId).ifPresent(userToken -> {
            userToken.setAccessToken(encryptionService.encrypt(newAccessToken));
            userToken.setExpiresAt(LocalDateTime.now().plusSeconds(expiresInSeconds));
            userToken.setUpdatedAt(LocalDateTime.now());
            userTokenRepository.save(userToken);
        });
    }

    /**
     * Checks if a user has valid OAuth tokens stored.
     */
    public boolean hasValidTokens(String supabaseId) {
        Optional<UserToken> tokenOpt = userTokenRepository.findBySupabaseId(supabaseId);
        return tokenOpt.isPresent() && tokenOpt.get().getAccessToken() != null;
    }

    /**
     * Deletes all tokens for a user (e.g., on logout or token revocation).
     */
    public void deleteTokens(String supabaseId) {
        userTokenRepository.deleteBySupabaseId(supabaseId);
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
