package com.ai.application.Services;

import com.ai.application.Config.SmtpProviderConfig;
import com.ai.application.Repositories.UserRepository;
import com.ai.application.model.Entity.UserToken;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for EmailProviderService.
 * 
 * Tests cover:
 * - Provider routing (Google/Microsoft)
 * - Error handling for missing tokens
 * - Unsupported provider handling
 * - Email capability checking
 * 
 * Note: External email services (Gmail, Microsoft) are mocked.
 */
@ExtendWith(MockitoExtension.class)
class EmailProviderServiceTest {

    @Mock
    private GmailService gmailService;

    @Mock
    private MicrosoftGraphService microsoftGraphService;

    @Mock
    private SmtpEmailService smtpEmailService;

    @Mock
    private TokenStorageService tokenStorageService;

    @Mock
    private EncryptionService encryptionService;

    @Mock
    private SmtpProviderConfig smtpProviderConfig;

    @Mock
    private UserRepository userRepository;

    private EmailProviderService emailProviderService;

    @BeforeEach
    void setUp() {
        emailProviderService = new EmailProviderService(gmailService, microsoftGraphService,
                smtpEmailService, tokenStorageService, userRepository, encryptionService, smtpProviderConfig);
    }

    @Nested
    @DisplayName("sendEmail()")
    class SendEmailTests {

        @Test
        @DisplayName("Should return error when no tokens found")
        void returnErrorWhenNoTokensFound() {
            String supabaseId = "user-123";
            when(tokenStorageService.getUserToken(supabaseId)).thenReturn(Optional.empty());

            EmailProviderService.SendResult result = emailProviderService.sendEmail(
                    supabaseId, "to@example.com", "Subject", "Body");

            assertFalse(result.isSuccess());
            assertEquals("no_tokens", result.getCode());
            assertTrue(result.getMessage().contains("No account found"));
        }

        @Test
        @DisplayName("Should return error for unsupported provider")
        void returnErrorForUnsupportedProvider() {
            String supabaseId = "user-123";
            UserToken token = new UserToken(supabaseId, "user@example.com", "email");
            token.setAccessToken("token");

            when(tokenStorageService.getUserToken(supabaseId)).thenReturn(Optional.of(token));

            EmailProviderService.SendResult result = emailProviderService.sendEmail(
                    supabaseId, "to@example.com", "Subject", "Body");

            assertFalse(result.isSuccess());
            assertEquals("unsupported_provider", result.getCode());
            assertTrue(result.isUnsupportedProvider());
        }

        @Test
        @DisplayName("Should route to Gmail service for Google provider")
        void routeToGmailForGoogleProvider() throws Exception {
            String supabaseId = "user-123";
            UserToken token = new UserToken(supabaseId, "user@gmail.com", "google");
            token.setAccessToken("token");
            token.setExpiresAt(LocalDateTime.now().plusHours(1));

            when(tokenStorageService.getUserToken(supabaseId)).thenReturn(Optional.of(token));
            when(gmailService.sendEmail(anyString(), anyString(), anyString(), anyString())).thenReturn(true);

            EmailProviderService.SendResult result = emailProviderService.sendEmail(
                    supabaseId, "to@example.com", "Subject", "<p>Body</p>");

            assertTrue(result.isSuccess());
            verify(gmailService).sendEmail(supabaseId, "to@example.com", "Subject", "<p>Body</p>");
            verifyNoInteractions(microsoftGraphService);
        }

        @Test
        @DisplayName("Should route to Microsoft service for Azure provider")
        void routeToMicrosoftForAzureProvider() throws Exception {
            String supabaseId = "user-123";
            UserToken token = new UserToken(supabaseId, "user@outlook.com", "azure");
            token.setAccessToken("token");
            token.setExpiresAt(LocalDateTime.now().plusHours(1));

            when(tokenStorageService.getUserToken(supabaseId)).thenReturn(Optional.of(token));
            when(microsoftGraphService.sendEmail(anyString(), anyString(), anyString(), anyString())).thenReturn(true);

            EmailProviderService.SendResult result = emailProviderService.sendEmail(
                    supabaseId, "to@example.com", "Subject", "<p>Body</p>");

            assertTrue(result.isSuccess());
            verify(microsoftGraphService).sendEmail(supabaseId, "to@example.com", "Subject", "<p>Body</p>");
            verifyNoInteractions(gmailService);
        }

        @Test
        @DisplayName("Should return error when Gmail service fails")
        void returnErrorWhenGmailFails() throws Exception {
            String supabaseId = "user-123";
            UserToken token = new UserToken(supabaseId, "user@gmail.com", "google");
            token.setAccessToken("token");
            token.setExpiresAt(LocalDateTime.now().plusHours(1));

            when(tokenStorageService.getUserToken(supabaseId)).thenReturn(Optional.of(token));
            when(gmailService.sendEmail(anyString(), anyString(), anyString(), anyString())).thenReturn(false);

            EmailProviderService.SendResult result = emailProviderService.sendEmail(
                    supabaseId, "to@example.com", "Subject", "Body");

            assertFalse(result.isSuccess());
            assertEquals("send_failed", result.getCode());
        }

        @Test
        @DisplayName("Should handle exception with reauth message")
        void handleExceptionWithReauthMessage() throws Exception {
            String supabaseId = "user-123";
            UserToken token = new UserToken(supabaseId, "user@gmail.com", "google");
            token.setAccessToken("token");
            token.setExpiresAt(LocalDateTime.now().plusHours(1));

            when(tokenStorageService.getUserToken(supabaseId)).thenReturn(Optional.of(token));
            when(gmailService.sendEmail(anyString(), anyString(), anyString(), anyString()))
                    .thenThrow(new RuntimeException("Please sign in again to send emails"));

            EmailProviderService.SendResult result = emailProviderService.sendEmail(
                    supabaseId, "to@example.com", "Subject", "Body");

            assertFalse(result.isSuccess());
            assertEquals("reauth_required", result.getCode());
            assertTrue(result.requiresReauth());
        }

        @Test
        @DisplayName("Should handle general exception")
        void handleGeneralException() throws Exception {
            String supabaseId = "user-123";
            UserToken token = new UserToken(supabaseId, "user@gmail.com", "google");
            token.setAccessToken("token");
            token.setExpiresAt(LocalDateTime.now().plusHours(1));

            when(tokenStorageService.getUserToken(supabaseId)).thenReturn(Optional.of(token));
            when(gmailService.sendEmail(anyString(), anyString(), anyString(), anyString()))
                    .thenThrow(new RuntimeException("Network error"));

            EmailProviderService.SendResult result = emailProviderService.sendEmail(
                    supabaseId, "to@example.com", "Subject", "Body");

            assertFalse(result.isSuccess());
            assertEquals("service_error", result.getCode());
        }
    }

    @Nested
    @DisplayName("sendEmailWithAttachment()")
    class SendEmailWithAttachmentTests {

        @Test
        @DisplayName("Should route to Gmail with attachment for Google provider")
        void routeToGmailWithAttachment() throws Exception {
            String supabaseId = "user-123";
            UserToken token = new UserToken(supabaseId, "user@gmail.com", "google");
            token.setAccessToken("token");
            token.setExpiresAt(LocalDateTime.now().plusHours(1));
            byte[] pdfBytes = new byte[] { 1, 2, 3 };

            when(tokenStorageService.getUserToken(supabaseId)).thenReturn(Optional.of(token));
            when(gmailService.sendEmailWithAttachment(anyString(), anyString(), anyString(), anyString(),
                    any(byte[].class), anyString())).thenReturn(true);

            EmailProviderService.SendResult result = emailProviderService.sendEmailWithAttachment(
                    supabaseId, "to@example.com", "Subject", "Body", pdfBytes, "report.pdf");

            assertTrue(result.isSuccess());
            verify(gmailService).sendEmailWithAttachment(
                    supabaseId, "to@example.com", "Subject", "Body", pdfBytes, "report.pdf");
        }

        @Test
        @DisplayName("Should route to Microsoft with attachment for Azure provider")
        void routeToMicrosoftWithAttachment() throws Exception {
            String supabaseId = "user-123";
            UserToken token = new UserToken(supabaseId, "user@outlook.com", "azure");
            token.setAccessToken("token");
            token.setExpiresAt(LocalDateTime.now().plusHours(1));
            byte[] pdfBytes = new byte[] { 1, 2, 3 };

            when(tokenStorageService.getUserToken(supabaseId)).thenReturn(Optional.of(token));
            when(microsoftGraphService.sendEmailWithAttachment(anyString(), anyString(), anyString(), anyString(),
                    any(byte[].class), anyString())).thenReturn(true);

            EmailProviderService.SendResult result = emailProviderService.sendEmailWithAttachment(
                    supabaseId, "to@example.com", "Subject", "Body", pdfBytes, "report.pdf");

            assertTrue(result.isSuccess());
            verify(microsoftGraphService).sendEmailWithAttachment(
                    supabaseId, "to@example.com", "Subject", "Body", pdfBytes, "report.pdf");
        }
    }

    @Nested
    @DisplayName("canUserSendEmail()")
    class CanUserSendEmailTests {

        @Test
        @DisplayName("Should return true for Google provider")
        void returnTrueForGoogleProvider() {
            String supabaseId = "user-123";
            UserToken token = new UserToken(supabaseId, "user@gmail.com", "google");

            when(tokenStorageService.getUserToken(supabaseId)).thenReturn(Optional.of(token));

            assertTrue(emailProviderService.canUserSendEmail(supabaseId));
        }

        @Test
        @DisplayName("Should return true for Azure provider")
        void returnTrueForAzureProvider() {
            String supabaseId = "user-123";
            UserToken token = new UserToken(supabaseId, "user@outlook.com", "azure");

            when(tokenStorageService.getUserToken(supabaseId)).thenReturn(Optional.of(token));

            assertTrue(emailProviderService.canUserSendEmail(supabaseId));
        }

        @Test
        @DisplayName("Should return false for email provider")
        void returnFalseForEmailProvider() {
            String supabaseId = "user-123";
            UserToken token = new UserToken(supabaseId, "user@example.com", "email");

            when(tokenStorageService.getUserToken(supabaseId)).thenReturn(Optional.of(token));

            assertFalse(emailProviderService.canUserSendEmail(supabaseId));
        }

        @Test
        @DisplayName("Should return false when no tokens")
        void returnFalseWhenNoTokens() {
            when(tokenStorageService.getUserToken("nonexistent")).thenReturn(Optional.empty());

            assertFalse(emailProviderService.canUserSendEmail("nonexistent"));
        }
    }

    @Nested
    @DisplayName("getUserProvider()")
    class GetUserProviderTests {

        @Test
        @DisplayName("Should return provider type")
        void returnProviderType() {
            String supabaseId = "user-123";
            UserToken token = new UserToken(supabaseId, "user@gmail.com", "google");

            when(tokenStorageService.getUserToken(supabaseId)).thenReturn(Optional.of(token));

            assertEquals("google", emailProviderService.getUserProvider(supabaseId));
        }

        @Test
        @DisplayName("Should return 'none' when no tokens")
        void returnNoneWhenNoTokens() {
            when(tokenStorageService.getUserToken("nonexistent")).thenReturn(Optional.empty());

            assertEquals("none", emailProviderService.getUserProvider("nonexistent"));
        }
    }

    @Nested
    @DisplayName("SendResult")
    class SendResultTests {

        @Test
        @DisplayName("Should create success result")
        void createSuccessResult() {
            EmailProviderService.SendResult result = new EmailProviderService.SendResult(true, "Email sent!",
                    "success");

            assertTrue(result.isSuccess());
            assertEquals("Email sent!", result.getMessage());
            assertEquals("success", result.getCode());
        }

        @Test
        @DisplayName("Should detect unsupported provider")
        void detectUnsupportedProvider() {
            EmailProviderService.SendResult result = new EmailProviderService.SendResult(false, "Not supported",
                    "unsupported_provider");

            assertTrue(result.isUnsupportedProvider());
            assertFalse(result.requiresReauth());
        }

        @Test
        @DisplayName("Should detect reauth required")
        void detectReauthRequired() {
            EmailProviderService.SendResult result = new EmailProviderService.SendResult(false, "Please sign in",
                    "reauth_required");

            assertTrue(result.requiresReauth());
            assertFalse(result.isUnsupportedProvider());
        }
    }
}
