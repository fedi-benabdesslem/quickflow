package com.ai.application.model.Entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for UserToken entity.
 * 
 * Tests cover:
 * - Entity construction
 * - Token expiration checks
 * - Email sending capability checks
 * - Getter/setter functionality
 */
class UserTokenTest {

    @Nested
    @DisplayName("Constructor and Basic Properties")
    class ConstructorTests {

        @Test
        @DisplayName("Should create instance with default constructor")
        void createWithDefaultConstructor() {
            UserToken token = new UserToken();
            
            assertNull(token.getSupabaseId());
            assertNull(token.getEmail());
            assertNull(token.getProvider());
            assertNotNull(token.getCreatedAt());
            assertNotNull(token.getUpdatedAt());
        }

        @Test
        @DisplayName("Should create instance with parameterized constructor")
        void createWithParameterizedConstructor() {
            UserToken token = new UserToken("supabase-123", "test@example.com", "google");
            
            assertEquals("supabase-123", token.getSupabaseId());
            assertEquals("test@example.com", token.getEmail());
            assertEquals("google", token.getProvider());
            assertNotNull(token.getCreatedAt());
            assertNotNull(token.getUpdatedAt());
        }
    }

    @Nested
    @DisplayName("isTokenExpiredOrExpiringSoon()")
    class TokenExpirationTests {

        @Test
        @DisplayName("Should return true when expiresAt is null")
        void returnTrueWhenExpiresAtIsNull() {
            UserToken token = new UserToken("id", "email@test.com", "google");
            token.setExpiresAt(null);
            
            assertTrue(token.isTokenExpiredOrExpiringSoon());
        }

        @Test
        @DisplayName("Should return true when token is already expired")
        void returnTrueWhenAlreadyExpired() {
            UserToken token = new UserToken("id", "email@test.com", "google");
            token.setExpiresAt(LocalDateTime.now().minusMinutes(10));
            
            assertTrue(token.isTokenExpiredOrExpiringSoon());
        }

        @Test
        @DisplayName("Should return true when token expires within 5 minutes")
        void returnTrueWhenExpiringWithin5Minutes() {
            UserToken token = new UserToken("id", "email@test.com", "google");
            token.setExpiresAt(LocalDateTime.now().plusMinutes(3));
            
            assertTrue(token.isTokenExpiredOrExpiringSoon());
        }

        @Test
        @DisplayName("Should return false when token has more than 5 minutes remaining")
        void returnFalseWhenNotExpiring() {
            UserToken token = new UserToken("id", "email@test.com", "google");
            token.setExpiresAt(LocalDateTime.now().plusHours(1));
            
            assertFalse(token.isTokenExpiredOrExpiringSoon());
        }

        @Test
        @DisplayName("Should return true when token expires in less than 5 minutes")
        void returnTrueAtBoundary() {
            UserToken token = new UserToken("id", "email@test.com", "google");
            // Set to just under 5 minutes from now - this should be treated as "expiring soon"
            token.setExpiresAt(LocalDateTime.now().plusMinutes(4).plusSeconds(59));
            
            assertTrue(token.isTokenExpiredOrExpiringSoon());
        }
    }

    @Nested
    @DisplayName("canSendEmail()")
    class CanSendEmailTests {

        @Test
        @DisplayName("Should return true for Google provider")
        void returnTrueForGoogleProvider() {
            UserToken token = new UserToken("id", "email@test.com", "google");
            
            assertTrue(token.canSendEmail());
        }

        @Test
        @DisplayName("Should return true for Azure provider")
        void returnTrueForAzureProvider() {
            UserToken token = new UserToken("id", "email@test.com", "azure");
            
            assertTrue(token.canSendEmail());
        }

        @Test
        @DisplayName("Should return true for Google provider (case-insensitive)")
        void returnTrueForGoogleCaseInsensitive() {
            UserToken token = new UserToken("id", "email@test.com", "GOOGLE");
            
            assertTrue(token.canSendEmail());
        }

        @Test
        @DisplayName("Should return true for Azure provider (case-insensitive)")
        void returnTrueForAzureCaseInsensitive() {
            UserToken token = new UserToken("id", "email@test.com", "AZURE");
            
            assertTrue(token.canSendEmail());
        }

        @Test
        @DisplayName("Should return false for email provider")
        void returnFalseForEmailProvider() {
            UserToken token = new UserToken("id", "email@test.com", "email");
            
            assertFalse(token.canSendEmail());
        }

        @Test
        @DisplayName("Should return false for null provider")
        void returnFalseForNullProvider() {
            UserToken token = new UserToken("id", "email@test.com", null);
            
            assertFalse(token.canSendEmail());
        }

        @Test
        @DisplayName("Should return false for unknown provider")
        void returnFalseForUnknownProvider() {
            UserToken token = new UserToken("id", "email@test.com", "unknown");
            
            assertFalse(token.canSendEmail());
        }
    }

    @Nested
    @DisplayName("Setter behavior")
    class SetterTests {

        @Test
        @DisplayName("Should update updatedAt when access token is set")
        void updateUpdatedAtOnAccessTokenChange() throws InterruptedException {
            UserToken token = new UserToken("id", "email@test.com", "google");
            LocalDateTime beforeSet = LocalDateTime.now();
            
            // Small delay to ensure time difference
            Thread.sleep(15);
            
            token.setAccessToken("new-token");
            
            // The updatedAt should be set to now() which is after beforeSet
            assertTrue(token.getUpdatedAt().isAfter(beforeSet) || 
                       token.getUpdatedAt().isEqual(beforeSet),
                       "updatedAt should be at or after the time before setAccessToken was called");
        }

        @Test
        @DisplayName("Should update updatedAt when refresh token is set")
        void updateUpdatedAtOnRefreshTokenChange() throws InterruptedException {
            UserToken token = new UserToken("id", "email@test.com", "google");
            LocalDateTime beforeSet = LocalDateTime.now();
            
            Thread.sleep(15);
            
            token.setRefreshToken("new-refresh-token");
            
            // The updatedAt should be set to now() which is after beforeSet
            assertTrue(token.getUpdatedAt().isAfter(beforeSet) || 
                       token.getUpdatedAt().isEqual(beforeSet),
                       "updatedAt should be at or after the time before setRefreshToken was called");
        }
    }
}
