package com.ai.application.Services;

import com.ai.application.model.Entity.UserToken;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * Facade service that routes email sending to the appropriate provider.
 * 
 * Determines if user authenticated via Google, Microsoft, or email/password,
 * and routes email sending accordingly.
 */
@Service
public class EmailProviderService {

    private final GmailService gmailService;
    private final MicrosoftGraphService microsoftGraphService;
    private final TokenStorageService tokenStorageService;

    public EmailProviderService(GmailService gmailService,
            MicrosoftGraphService microsoftGraphService,
            TokenStorageService tokenStorageService) {
        this.gmailService = gmailService;
        this.microsoftGraphService = microsoftGraphService;
        this.tokenStorageService = tokenStorageService;
    }

    /**
     * Sends an email using the appropriate provider for the user.
     * 
     * @param supabaseId User's Supabase ID
     * @param to         Recipient email addresses (comma-separated)
     * @param subject    Email subject
     * @param htmlBody   Email body in HTML format
     * @return SendResult with status and message
     */
    public SendResult sendEmail(String supabaseId, String to, String subject, String htmlBody) {
        return sendEmailWithAttachment(supabaseId, to, subject, htmlBody, null, null);
    }

    /**
     * Sends an email with optional PDF attachment using the appropriate provider.
     */
    public SendResult sendEmailWithAttachment(String supabaseId, String to, String subject,
            String htmlBody, byte[] pdfBytes, String pdfFilename) {
        System.out.println("[EmailProviderService] sendEmail called for user: " + supabaseId);
        System.out.println("[EmailProviderService] Recipients: " + to + ", Subject: " + subject);

        // Get user's provider
        Optional<UserToken> userTokenOpt = tokenStorageService.getUserToken(supabaseId);

        if (userTokenOpt.isEmpty()) {
            System.err.println("[EmailProviderService] ERROR: No OAuth tokens found for user: " + supabaseId);
            return new SendResult(false, "No OAuth tokens found. Please sign in again.", "no_tokens");
        }

        UserToken userToken = userTokenOpt.get();
        String provider = userToken.getProvider();
        System.out.println("[EmailProviderService] Found token for user, provider: " + provider);

        // Check if user can send emails
        if (!userToken.canSendEmail()) {
            System.out.println("[EmailProviderService] User cannot send email - unsupported provider: " + provider);
            return new SendResult(false,
                    "Email sending not supported for your domain yet. Coming soon!",
                    "unsupported_provider");
        }

        try {
            System.out.println("[EmailProviderService] Attempting to send via provider: " + provider);
            boolean success = switch (provider.toLowerCase()) {
                case "google" -> {
                    System.out.println("[EmailProviderService] Using Gmail service...");
                    if (pdfBytes != null) {
                        yield gmailService.sendEmailWithAttachment(supabaseId, to, subject, htmlBody, pdfBytes,
                                pdfFilename);
                    } else {
                        yield gmailService.sendEmail(supabaseId, to, subject, htmlBody);
                    }
                }
                case "azure" -> {
                    System.out.println("[EmailProviderService] Using Microsoft Graph service...");
                    if (pdfBytes != null) {
                        yield microsoftGraphService.sendEmailWithAttachment(supabaseId, to, subject, htmlBody, pdfBytes,
                                pdfFilename);
                    } else {
                        yield microsoftGraphService.sendEmail(supabaseId, to, subject, htmlBody);
                    }
                }
                default -> throw new Exception("Unsupported provider: " + provider);
            };

            if (success) {
                System.out.println("[EmailProviderService] SUCCESS: Email sent successfully!");
                return new SendResult(true, "Email sent successfully!", "success");
            } else {
                System.err.println("[EmailProviderService] FAILED: Email sending returned false");
                return new SendResult(false, "Failed to send email.", "send_failed");
            }
        } catch (Exception e) {
            String message = e.getMessage();
            System.err.println("[EmailProviderService] EXCEPTION: " + message);
            e.printStackTrace();

            // Handle specific error cases
            if (message != null && message.contains("sign in again")) {
                return new SendResult(false, "Please sign in again to send emails.", "reauth_required");
            }

            return new SendResult(false, "Service not available, try later.", "service_error");
        }
    }

    /**
     * Checks if a user can send emails via OAuth.
     */
    public boolean canUserSendEmail(String supabaseId) {
        return tokenStorageService.getUserToken(supabaseId)
                .map(UserToken::canSendEmail)
                .orElse(false);
    }

    /**
     * Gets the provider type for a user.
     */
    public String getUserProvider(String supabaseId) {
        return tokenStorageService.getUserToken(supabaseId)
                .map(UserToken::getProvider)
                .orElse("none");
    }

    /**
     * Result class for email sending operations.
     */
    public static class SendResult {
        private final boolean success;
        private final String message;
        private final String code;

        public SendResult(boolean success, String message, String code) {
            this.success = success;
            this.message = message;
            this.code = code;
        }

        public boolean isSuccess() {
            return success;
        }

        public String getMessage() {
            return message;
        }

        public String getCode() {
            return code;
        }

        public boolean isUnsupportedProvider() {
            return "unsupported_provider".equals(code);
        }

        public boolean requiresReauth() {
            return "reauth_required".equals(code);
        }
    }
}
