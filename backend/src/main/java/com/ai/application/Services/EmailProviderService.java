package com.ai.application.Services;

import com.ai.application.Config.SmtpProviderConfig;
import com.ai.application.Repositories.UserRepository;
import com.ai.application.model.Entity.AuthConnection;
import com.ai.application.model.Entity.User;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * Facade service that routes email sending to the appropriate provider.
 * 
 * Determines if user authenticated via Google, Microsoft, or email/password,
 * and routes email sending accordingly using the new User + AuthConnection
 * model.
 */
@Service
public class EmailProviderService {

    private final GmailService gmailService;
    private final MicrosoftGraphService microsoftGraphService;
    private final SmtpEmailService smtpEmailService;
    private final TokenStorageService tokenStorageService;
    private final UserRepository userRepository;
    private final EncryptionService encryptionService;
    private final SmtpProviderConfig smtpProviderConfig;

    public EmailProviderService(GmailService gmailService,
            MicrosoftGraphService microsoftGraphService,
            SmtpEmailService smtpEmailService,
            TokenStorageService tokenStorageService,
            UserRepository userRepository,
            EncryptionService encryptionService,
            SmtpProviderConfig smtpProviderConfig) {
        this.gmailService = gmailService;
        this.microsoftGraphService = microsoftGraphService;
        this.smtpEmailService = smtpEmailService;
        this.tokenStorageService = tokenStorageService;
        this.userRepository = userRepository;
        this.encryptionService = encryptionService;
        this.smtpProviderConfig = smtpProviderConfig;
    }

    /**
     * Sends an email using the appropriate provider for the user.
     */
    public SendResult sendEmail(String userId, String to, String subject, String htmlBody) {
        return sendEmailWithAttachment(userId, to, subject, htmlBody, null, null);
    }

    /**
     * Sends an email with optional PDF attachment using the appropriate provider.
     * Now uses the new User model with AuthConnection for provider resolution.
     */
    public SendResult sendEmailWithAttachment(String userId, String to, String subject,
            String htmlBody, byte[] pdfBytes, String pdfFilename) {
        System.out.println("[EmailProviderService] sendEmail called for user: " + userId);

        // Try new User model first
        Optional<User> userOpt = userRepository.findById(userId);

        if (userOpt.isPresent()) {
            return sendUsingUserModel(userOpt.get(), to, subject, htmlBody, pdfBytes, pdfFilename);
        }

        // Fallback to legacy UserToken model (for migration period)
        return sendUsingLegacyModel(userId, to, subject, htmlBody, pdfBytes, pdfFilename);
    }

    /**
     * Send email using the new User + AuthConnection model.
     */
    private SendResult sendUsingUserModel(User user, String to, String subject,
            String htmlBody, byte[] pdfBytes, String pdfFilename) {
        try {
            // Priority 1: Google OAuth connection
            AuthConnection googleConn = user.findConnection("google");
            if (googleConn != null && googleConn.getAccessTokenEncrypted() != null) {
                System.out.println("[EmailProviderService] Using Gmail service via AuthConnection...");
                String accessToken = encryptionService.decrypt(googleConn.getAccessTokenEncrypted());
                String senderEmail = googleConn.getProviderEmail() != null
                        ? googleConn.getProviderEmail()
                        : user.getEmail();

                boolean success = pdfBytes != null
                        ? gmailService.sendEmailWithAccessToken(accessToken, senderEmail,
                                to, subject, htmlBody, pdfBytes, pdfFilename)
                        : gmailService.sendEmailWithAccessToken(accessToken, senderEmail,
                                to, subject, htmlBody);
                return success ? new SendResult(true, "Email sent successfully!", "success")
                        : new SendResult(false, "Failed to send email.", "send_failed");
            }

            // Priority 2: Microsoft OAuth connection
            AuthConnection msConn = user.findConnection("microsoft");
            if (msConn != null && msConn.getAccessTokenEncrypted() != null) {
                System.out.println("[EmailProviderService] Using Microsoft Graph service via AuthConnection...");
                String accessToken = encryptionService.decrypt(msConn.getAccessTokenEncrypted());

                boolean success = pdfBytes != null
                        ? microsoftGraphService.sendEmailWithAccessToken(accessToken,
                                to, subject, htmlBody, pdfBytes, pdfFilename)
                        : microsoftGraphService.sendEmailWithAccessToken(accessToken,
                                to, subject, htmlBody);
                return success ? new SendResult(true, "Email sent successfully!", "success")
                        : new SendResult(false, "Failed to send email.", "send_failed");
            }

            // Priority 3: SMTP (configured with app password)
            if (user.isSmtpConfigured() && user.getSmtpPasswordEncrypted() != null) {
                System.out.println("[EmailProviderService] Using SMTP service...");
                String email = user.getEmail();
                String password = encryptionService.decrypt(user.getSmtpPasswordEncrypted());

                boolean success = pdfBytes != null
                        ? smtpEmailService.sendEmailWithAttachment(email, password, to, subject, htmlBody, pdfBytes,
                                pdfFilename)
                        : smtpEmailService.sendEmail(email, password, to, subject, htmlBody);
                return success ? new SendResult(true, "Email sent successfully!", "success")
                        : new SendResult(false, "Failed to send email.", "send_failed");
            }

            // Priority 4: SMTP domain supported but not configured
            String domain = SmtpProviderConfig.extractDomain(user.getEmail());
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
     * Fallback: Send email using legacy UserToken model.
     * Kept for backward compatibility during migration.
     */
    private SendResult sendUsingLegacyModel(String userId, String to, String subject,
            String htmlBody, byte[] pdfBytes, String pdfFilename) {
        // Delegate to old TokenStorageService-based flow
        var userTokenOpt = tokenStorageService.getUserToken(userId);
        if (userTokenOpt.isEmpty()) {
            System.err.println("[EmailProviderService] ERROR: No tokens found for user: " + userId);
            return new SendResult(false, "No account found. Please sign in again.", "no_tokens");
        }

        var userToken = userTokenOpt.get();
        String provider = userToken.getProvider();

        try {
            if ("google".equalsIgnoreCase(provider)) {
                boolean success = pdfBytes != null
                        ? gmailService.sendEmailWithAttachment(userId, to, subject, htmlBody, pdfBytes, pdfFilename)
                        : gmailService.sendEmail(userId, to, subject, htmlBody);
                return success ? new SendResult(true, "Email sent successfully!", "success")
                        : new SendResult(false, "Failed to send email.", "send_failed");
            }

            if ("azure".equalsIgnoreCase(provider)) {
                boolean success = pdfBytes != null
                        ? microsoftGraphService.sendEmailWithAttachment(userId, to, subject, htmlBody, pdfBytes,
                                pdfFilename)
                        : microsoftGraphService.sendEmail(userId, to, subject, htmlBody);
                return success ? new SendResult(true, "Email sent successfully!", "success")
                        : new SendResult(false, "Failed to send email.", "send_failed");
            }

            if (userToken.isSmtpConfigured()) {
                String email = userToken.getEmail();
                String password = encryptionService.decrypt(userToken.getSmtpPasswordEncrypted());
                boolean success = pdfBytes != null
                        ? smtpEmailService.sendEmailWithAttachment(email, password, to, subject, htmlBody, pdfBytes,
                                pdfFilename)
                        : smtpEmailService.sendEmail(email, password, to, subject, htmlBody);
                return success ? new SendResult(true, "Email sent successfully!", "success")
                        : new SendResult(false, "Failed to send email.", "send_failed");
            }

            return new SendResult(false, "Email sending is not available.", "unsupported_provider");
        } catch (Exception e) {
            return new SendResult(false, "Service not available, try later.", "service_error");
        }
    }

    /**
     * Checks if a user can send emails.
     */
    public boolean canUserSendEmail(String userId) {
        // Check new model first
        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isPresent()) {
            return userOpt.get().canSendEmail();
        }
        // Legacy fallback
        return tokenStorageService.getUserToken(userId)
                .map(ut -> ut.canSendEmail())
                .orElse(false);
    }

    /**
     * Gets the provider type for a user.
     */
    public String getUserProvider(String userId) {
        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            if (user.hasProvider("google"))
                return "google";
            if (user.hasProvider("microsoft"))
                return "azure";
            if (user.isSmtpConfigured())
                return "smtp";
            return "email";
        }
        return tokenStorageService.getUserToken(userId)
                .map(ut -> ut.getProvider())
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
