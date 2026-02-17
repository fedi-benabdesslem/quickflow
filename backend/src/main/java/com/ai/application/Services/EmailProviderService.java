package com.ai.application.Services;

import com.ai.application.Config.SmtpProviderConfig;
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
    private final SmtpEmailService smtpEmailService;
    private final TokenStorageService tokenStorageService;
    private final EncryptionService encryptionService;
    private final SmtpProviderConfig smtpProviderConfig;

    public EmailProviderService(GmailService gmailService,
            MicrosoftGraphService microsoftGraphService,
            SmtpEmailService smtpEmailService,
            TokenStorageService tokenStorageService,
            EncryptionService encryptionService,
            SmtpProviderConfig smtpProviderConfig) {
        this.gmailService = gmailService;
        this.microsoftGraphService = microsoftGraphService;
        this.smtpEmailService = smtpEmailService;
        this.tokenStorageService = tokenStorageService;
        this.encryptionService = encryptionService;
        this.smtpProviderConfig = smtpProviderConfig;
    }

    /**
     * Sends an email using the appropriate provider for the user.
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

        // Get user's token record
        Optional<UserToken> userTokenOpt = tokenStorageService.getUserToken(supabaseId);

        if (userTokenOpt.isEmpty()) {
            System.err.println("[EmailProviderService] ERROR: No tokens found for user: " + supabaseId);
            return new SendResult(false, "No account found. Please sign in again.", "no_tokens");
        }

        UserToken userToken = userTokenOpt.get();
        String provider = userToken.getProvider();
        System.out.println(
                "[EmailProviderService] Provider: " + provider + ", smtpConfigured: " + userToken.isSmtpConfigured());

        try {
            // Priority 1: Google OAuth
            if ("google".equalsIgnoreCase(provider)) {
                System.out.println("[EmailProviderService] Using Gmail service...");
                boolean success = pdfBytes != null
                        ? gmailService.sendEmailWithAttachment(supabaseId, to, subject, htmlBody, pdfBytes, pdfFilename)
                        : gmailService.sendEmail(supabaseId, to, subject, htmlBody);
                return success ? new SendResult(true, "Email sent successfully!", "success")
                        : new SendResult(false, "Failed to send email.", "send_failed");
            }

            // Priority 2: Microsoft OAuth
            if ("azure".equalsIgnoreCase(provider)) {
                System.out.println("[EmailProviderService] Using Microsoft Graph service...");
                boolean success = pdfBytes != null
                        ? microsoftGraphService.sendEmailWithAttachment(supabaseId, to, subject, htmlBody, pdfBytes,
                                pdfFilename)
                        : microsoftGraphService.sendEmail(supabaseId, to, subject, htmlBody);
                return success ? new SendResult(true, "Email sent successfully!", "success")
                        : new SendResult(false, "Failed to send email.", "send_failed");
            }

            // Priority 3: SMTP (configured with app password)
            if (userToken.isSmtpConfigured()) {
                System.out.println("[EmailProviderService] Using SMTP service...");
                String email = userToken.getEmail();
                String password = encryptionService.decrypt(userToken.getSmtpPasswordEncrypted());

                boolean success = pdfBytes != null
                        ? smtpEmailService.sendEmailWithAttachment(email, password, to, subject, htmlBody, pdfBytes,
                                pdfFilename)
                        : smtpEmailService.sendEmail(email, password, to, subject, htmlBody);
                return success ? new SendResult(true, "Email sent successfully!", "success")
                        : new SendResult(false, "Failed to send email.", "send_failed");
            }

            // Priority 4: SMTP domain supported but not configured
            String domain = SmtpProviderConfig.extractDomain(userToken.getEmail());
            if (smtpProviderConfig.isSupported(domain)) {
                String providerName = smtpProviderConfig.getProviderName(domain);
                return new SendResult(false,
                        "Set up email sending for " + providerName + " to send this email.",
                        "smtp_not_configured");
            }

            // Priority 5: Unsupported provider
            return new SendResult(false,
                    "Email sending is not available for your provider. You can download the PDF and send it manually.",
                    "unsupported_provider");

        } catch (SmtpEmailService.SmtpSendException e) {
            System.err.println("[EmailProviderService] SMTP error: " + e.getMessage());
            return new SendResult(false, e.getMessage(), e.getCode());
        } catch (Exception e) {
            String message = e.getMessage();
            System.err.println("[EmailProviderService] EXCEPTION: " + message);

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

        public boolean isSmtpNotConfigured() {
            return "smtp_not_configured".equals(code);
        }
    }
}
