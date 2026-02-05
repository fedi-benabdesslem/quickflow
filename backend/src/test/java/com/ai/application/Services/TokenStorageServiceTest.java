package com.ai.application.Services;

import com.ai.application.Repositories.UserTokenRepository;
import com.ai.application.model.Entity.UserToken;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for TokenStorageService.
 * 
 * Tests cover:
 * - Token storage with encryption
 * - Token retrieval with decryption
 * - Token update after refresh
 * - Token existence checking
 * - Token deletion
 * 
 * Note: EncryptionService is mocked to isolate TokenStorageService logic.
 */
@ExtendWith(MockitoExtension.class)
class TokenStorageServiceTest {

    @Mock
    private UserTokenRepository userTokenRepository;

    @Mock
    private EncryptionService encryptionService;

    private TokenStorageService tokenStorageService;

    @BeforeEach
    void setUp() {
        tokenStorageService = new TokenStorageService(userTokenRepository, encryptionService);
    }

    @Nested
    @DisplayName("storeTokens()")
    class StoreTokensTests {

        @Test
        @DisplayName("Should encrypt and store new tokens")
        void encryptAndStoreNewTokens() {
            String supabaseId = "user-123";
            String email = "test@example.com";
            String provider = "google";
            String accessToken = "access-token-value";
            String refreshToken = "refresh-token-value";
            long expiresInSeconds = 3600;

            when(userTokenRepository.findBySupabaseId(supabaseId)).thenReturn(Optional.empty());
            when(encryptionService.encrypt("access-token-value")).thenReturn("encrypted-access");
            when(encryptionService.encrypt("refresh-token-value")).thenReturn("encrypted-refresh");
            when(userTokenRepository.save(any(UserToken.class))).thenAnswer(i -> i.getArgument(0));

            UserToken result = tokenStorageService.storeTokens(
                supabaseId, email, provider, accessToken, refreshToken, expiresInSeconds);

            assertNotNull(result);
            assertEquals(email, result.getEmail());
            assertEquals(provider, result.getProvider());
            
            // Verify encryption was called
            verify(encryptionService).encrypt("access-token-value");
            verify(encryptionService).encrypt("refresh-token-value");
        }

        @Test
        @DisplayName("Should update existing tokens")
        void updateExistingTokens() {
            String supabaseId = "user-123";
            UserToken existingToken = new UserToken(supabaseId, "old@example.com", "google");
            existingToken.setAccessToken("old-encrypted-access");
            existingToken.setRefreshToken("old-encrypted-refresh");

            when(userTokenRepository.findBySupabaseId(supabaseId)).thenReturn(Optional.of(existingToken));
            when(encryptionService.encrypt("new-access")).thenReturn("new-encrypted-access");
            when(encryptionService.encrypt("new-refresh")).thenReturn("new-encrypted-refresh");
            when(userTokenRepository.save(any(UserToken.class))).thenAnswer(i -> i.getArgument(0));

            UserToken result = tokenStorageService.storeTokens(
                supabaseId, "new@example.com", "azure", "new-access", "new-refresh", 7200);

            assertEquals("new@example.com", result.getEmail());
            assertEquals("azure", result.getProvider());
            
            // Verify save was called
            verify(userTokenRepository).save(existingToken);
        }

        @Test
        @DisplayName("Should set expiration time correctly")
        void setExpirationTimeCorrectly() {
            String supabaseId = "user-123";
            long expiresInSeconds = 3600; // 1 hour

            when(userTokenRepository.findBySupabaseId(supabaseId)).thenReturn(Optional.empty());
            when(encryptionService.encrypt(anyString())).thenReturn("encrypted");
            
            ArgumentCaptor<UserToken> tokenCaptor = ArgumentCaptor.forClass(UserToken.class);
            when(userTokenRepository.save(tokenCaptor.capture())).thenAnswer(i -> i.getArgument(0));

            LocalDateTime beforeCall = LocalDateTime.now().plusSeconds(expiresInSeconds).minusSeconds(5);
            
            tokenStorageService.storeTokens(
                supabaseId, "test@example.com", "google", "access", "refresh", expiresInSeconds);

            LocalDateTime afterCall = LocalDateTime.now().plusSeconds(expiresInSeconds).plusSeconds(5);
            
            UserToken savedToken = tokenCaptor.getValue();
            assertNotNull(savedToken.getExpiresAt());
            assertTrue(savedToken.getExpiresAt().isAfter(beforeCall));
            assertTrue(savedToken.getExpiresAt().isBefore(afterCall));
        }
    }

    @Nested
    @DisplayName("getDecryptedTokens()")
    class GetDecryptedTokensTests {

        @Test
        @DisplayName("Should return null when no tokens exist")
        void returnNullWhenNoTokensExist() {
            when(userTokenRepository.findBySupabaseId("nonexistent")).thenReturn(Optional.empty());

            TokenStorageService.DecryptedTokens result = 
                tokenStorageService.getDecryptedTokens("nonexistent");

            assertNull(result);
        }

        @Test
        @DisplayName("Should decrypt and return tokens")
        void decryptAndReturnTokens() {
            String supabaseId = "user-123";
            UserToken storedToken = new UserToken(supabaseId, "test@example.com", "google");
            storedToken.setAccessToken("encrypted-access");
            storedToken.setRefreshToken("encrypted-refresh");
            storedToken.setExpiresAt(LocalDateTime.now().plusHours(1));

            when(userTokenRepository.findBySupabaseId(supabaseId)).thenReturn(Optional.of(storedToken));
            when(encryptionService.decrypt("encrypted-access")).thenReturn("decrypted-access");
            when(encryptionService.decrypt("encrypted-refresh")).thenReturn("decrypted-refresh");

            TokenStorageService.DecryptedTokens result = 
                tokenStorageService.getDecryptedTokens(supabaseId);

            assertNotNull(result);
            assertEquals("decrypted-access", result.getAccessToken());
            assertEquals("decrypted-refresh", result.getRefreshToken());
            assertEquals("google", result.getProvider());
            assertEquals("test@example.com", result.getEmail());
        }
    }

    @Nested
    @DisplayName("DecryptedTokens.isExpiredOrExpiringSoon()")
    class DecryptedTokensExpirationTests {

        @Test
        @DisplayName("Should return true when expiresAt is null")
        void returnTrueWhenExpiresAtIsNull() {
            TokenStorageService.DecryptedTokens tokens = 
                new TokenStorageService.DecryptedTokens("access", "refresh", null, "google", "test@example.com");

            assertTrue(tokens.isExpiredOrExpiringSoon());
        }

        @Test
        @DisplayName("Should return true when token expires within 5 minutes")
        void returnTrueWhenExpiringWithin5Minutes() {
            LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(3);
            TokenStorageService.DecryptedTokens tokens = 
                new TokenStorageService.DecryptedTokens("access", "refresh", expiresAt, "google", "test@example.com");

            assertTrue(tokens.isExpiredOrExpiringSoon());
        }

        @Test
        @DisplayName("Should return false when token has more than 5 minutes remaining")
        void returnFalseWhenNotExpiring() {
            LocalDateTime expiresAt = LocalDateTime.now().plusHours(1);
            TokenStorageService.DecryptedTokens tokens = 
                new TokenStorageService.DecryptedTokens("access", "refresh", expiresAt, "google", "test@example.com");

            assertFalse(tokens.isExpiredOrExpiringSoon());
        }

        @Test
        @DisplayName("Should return true when token is already expired")
        void returnTrueWhenAlreadyExpired() {
            LocalDateTime expiresAt = LocalDateTime.now().minusMinutes(10);
            TokenStorageService.DecryptedTokens tokens = 
                new TokenStorageService.DecryptedTokens("access", "refresh", expiresAt, "google", "test@example.com");

            assertTrue(tokens.isExpiredOrExpiringSoon());
        }
    }

    @Nested
    @DisplayName("getUserToken()")
    class GetUserTokenTests {

        @Test
        @DisplayName("Should return optional with token when exists")
        void returnOptionalWithToken() {
            String supabaseId = "user-123";
            UserToken token = new UserToken(supabaseId, "test@example.com", "azure");
            when(userTokenRepository.findBySupabaseId(supabaseId)).thenReturn(Optional.of(token));

            Optional<UserToken> result = tokenStorageService.getUserToken(supabaseId);

            assertTrue(result.isPresent());
            assertEquals("azure", result.get().getProvider());
        }

        @Test
        @DisplayName("Should return empty optional when not exists")
        void returnEmptyOptional() {
            when(userTokenRepository.findBySupabaseId("nonexistent")).thenReturn(Optional.empty());

            Optional<UserToken> result = tokenStorageService.getUserToken("nonexistent");

            assertFalse(result.isPresent());
        }
    }

    @Nested
    @DisplayName("updateTokensAfterRefresh()")
    class UpdateTokensAfterRefreshTests {

        @Test
        @DisplayName("Should update access token and expiration")
        void updateAccessTokenAndExpiration() {
            String supabaseId = "user-123";
            UserToken existingToken = new UserToken(supabaseId, "test@example.com", "google");
            existingToken.setAccessToken("old-encrypted");
            
            when(userTokenRepository.findBySupabaseId(supabaseId)).thenReturn(Optional.of(existingToken));
            when(encryptionService.encrypt("new-access-token")).thenReturn("new-encrypted-access");

            LocalDateTime beforeUpdate = LocalDateTime.now();
            
            tokenStorageService.updateTokensAfterRefresh(supabaseId, "new-access-token", 3600);

            verify(encryptionService).encrypt("new-access-token");
            verify(userTokenRepository).save(existingToken);
            assertTrue(existingToken.getUpdatedAt().isAfter(beforeUpdate.minusSeconds(1)));
        }

        @Test
        @DisplayName("Should do nothing when user not found")
        void doNothingWhenUserNotFound() {
            when(userTokenRepository.findBySupabaseId("nonexistent")).thenReturn(Optional.empty());

            tokenStorageService.updateTokensAfterRefresh("nonexistent", "new-token", 3600);

            verify(encryptionService, never()).encrypt(anyString());
            verify(userTokenRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("hasValidTokens()")
    class HasValidTokensTests {

        @Test
        @DisplayName("Should return true when token exists with access token")
        void returnTrueWhenTokenExists() {
            String supabaseId = "user-123";
            UserToken token = new UserToken(supabaseId, "test@example.com", "google");
            token.setAccessToken("encrypted-token");
            
            when(userTokenRepository.findBySupabaseId(supabaseId)).thenReturn(Optional.of(token));

            assertTrue(tokenStorageService.hasValidTokens(supabaseId));
        }

        @Test
        @DisplayName("Should return false when token does not exist")
        void returnFalseWhenTokenNotExists() {
            when(userTokenRepository.findBySupabaseId("nonexistent")).thenReturn(Optional.empty());

            assertFalse(tokenStorageService.hasValidTokens("nonexistent"));
        }

        @Test
        @DisplayName("Should return false when access token is null")
        void returnFalseWhenAccessTokenIsNull() {
            String supabaseId = "user-123";
            UserToken token = new UserToken(supabaseId, "test@example.com", "google");
            token.setAccessToken(null);
            
            when(userTokenRepository.findBySupabaseId(supabaseId)).thenReturn(Optional.of(token));

            assertFalse(tokenStorageService.hasValidTokens(supabaseId));
        }
    }

    @Nested
    @DisplayName("deleteTokens()")
    class DeleteTokensTests {

        @Test
        @DisplayName("Should delete tokens by supabaseId")
        void deleteTokensBySupabaseId() {
            String supabaseId = "user-123";

            tokenStorageService.deleteTokens(supabaseId);

            verify(userTokenRepository).deleteBySupabaseId(supabaseId);
        }
    }
}
