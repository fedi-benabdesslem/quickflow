package com.ai.application.Services;

import com.ai.application.model.Entity.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Service for sending system/transactional emails (verification, password
 * reset).
 * Uses Resend API when configured; logs to console as fallback.
 * 
 * IMPORTANT: This is for app system emails, NOT for user email sending
 * (Gmail API / Graph API / SMTP).
 */
@Service
public class SystemEmailService {

    private static final Logger logger = LoggerFactory.getLogger(SystemEmailService.class);

    @Value("${app.resend.api-key:}")
    private String resendApiKey;

    @Value("${app.frontend.url}")
    private String frontendUrl;

    @Value("${app.email.from:QuickFlow <noreply@quickflow.app>}")
    private String fromAddress;

    /**
     * Sends a verification email with a link to verify the user's email address.
     */
    public void sendVerificationEmail(User user, String token) {
        String verifyLink = frontendUrl + "/verify-email?token=" + token;
        String subject = "Verify your email address";
        String html = buildVerificationEmailHtml(user.getName(), verifyLink);

        sendEmail(user.getEmail(), subject, html);
    }

    /**
     * Sends a password reset email with a link to reset the password.
     */
    public void sendPasswordResetEmail(User user, String token) {
        String resetLink = frontendUrl + "/reset-password?token=" + token;
        String subject = "Reset your password";
        String html = buildPasswordResetEmailHtml(user.getName(), resetLink);

        sendEmail(user.getEmail(), subject, html);
    }

    private void sendEmail(String to, String subject, String html) {
        if (resendApiKey == null || resendApiKey.isEmpty()) {
            // Fallback: log to console
            logger.info("╔══════════════════════════════════════════╗");
            logger.info("║  SYSTEM EMAIL (Resend not configured)    ║");
            logger.info("╠══════════════════════════════════════════╣");
            logger.info("║ To: {}", to);
            logger.info("║ Subject: {}", subject);
            logger.info("║ HTML: {} chars", html.length());
            logger.info("╚══════════════════════════════════════════╝");

            // Also log the link for easy testing
            if (html.contains("href=\"")) {
                int start = html.indexOf("href=\"") + 6;
                int end = html.indexOf("\"", start);
                if (end > start) {
                    logger.info("🔗 ACTION LINK: {}", html.substring(start, end));
                }
            }
            return;
        }

        try {
            // Use Resend API via HTTP
            var httpClient = java.net.http.HttpClient.newHttpClient();
            String json = String.format(
                    "{\"from\":\"%s\",\"to\":[\"%s\"],\"subject\":\"%s\",\"html\":\"%s\"}",
                    fromAddress,
                    to,
                    subject.replace("\"", "\\\""),
                    html.replace("\"", "\\\"").replace("\n", "\\n"));

            var request = java.net.http.HttpRequest.newBuilder()
                    .uri(java.net.URI.create("https://api.resend.com/emails"))
                    .header("Authorization", "Bearer " + resendApiKey)
                    .header("Content-Type", "application/json")
                    .POST(java.net.http.HttpRequest.BodyPublishers.ofString(json))
                    .build();

            var response = httpClient.send(request, java.net.http.HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                logger.info("System email sent to {} via Resend", to);
            } else {
                logger.error("Resend API error {}: {}", response.statusCode(), response.body());
            }
        } catch (Exception e) {
            logger.error("Failed to send email via Resend: {}", e.getMessage());
        }
    }

    private String buildVerificationEmailHtml(String name, String link) {
        return """
                <div style="font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto; padding: 20px;">
                    <h2 style="color: #6366f1;">Verify your email address</h2>
                    <p>Hi %s,</p>
                    <p>Please verify your email address by clicking the button below:</p>
                    <a href="%s" style="display: inline-block; background-color: #6366f1; color: white;
                       padding: 12px 24px; text-decoration: none; border-radius: 6px; margin: 20px 0;">
                        Verify Email
                    </a>
                    <p style="color: #666; font-size: 14px;">
                        This link expires in 24 hours. If you didn't create an account, you can safely ignore this email.
                    </p>
                    <p style="color: #999; font-size: 12px;">— QuickFlow</p>
                </div>
                """
                .formatted(name != null ? name : "there", link);
    }

    private String buildPasswordResetEmailHtml(String name, String link) {
        return """
                <div style="font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto; padding: 20px;">
                    <h2 style="color: #6366f1;">Reset your password</h2>
                    <p>Hi %s,</p>
                    <p>You requested a password reset. Click the button below to set a new password:</p>
                    <a href="%s" style="display: inline-block; background-color: #6366f1; color: white;
                       padding: 12px 24px; text-decoration: none; border-radius: 6px; margin: 20px 0;">
                        Reset Password
                    </a>
                    <p style="color: #666; font-size: 14px;">
                        This link expires in 1 hour. If you didn't request this, ignore this email.
                    </p>
                    <p style="color: #999; font-size: 12px;">— QuickFlow</p>
                </div>
                """.formatted(name != null ? name : "there", link);
    }
}
