package com.ai.application.Controllers;

import com.ai.application.Config.SmtpProviderConfig;
import com.ai.application.Services.EncryptionService;
import com.ai.application.Services.SmtpEmailService;
import com.ai.application.Services.SmtpEmailService.SmtpSendException;
import com.ai.application.Repositories.UserTokenRepository;
import com.ai.application.model.Entity.UserToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.Map;

/**
 * Controller for SMTP email configuration endpoints.
 * Allows users to configure, test, and manage SMTP app-specific passwords.
 */
@RestController
@RequestMapping("/api/user/smtp")
public class SmtpConfigController {

    private static final Logger logger = LoggerFactory.getLogger(SmtpConfigController.class);

    private final UserTokenRepository userTokenRepository;
    private final EncryptionService encryptionService;
    private final SmtpEmailService smtpEmailService;
    private final SmtpProviderConfig smtpProviderConfig;

    public SmtpConfigController(UserTokenRepository userTokenRepository,
            EncryptionService encryptionService,
            SmtpEmailService smtpEmailService,
            SmtpProviderConfig smtpProviderConfig) {
        this.userTokenRepository = userTokenRepository;
        this.encryptionService = encryptionService;
        this.smtpEmailService = smtpEmailService;
        this.smtpProviderConfig = smtpProviderConfig;
    }

    /**
     * Configure SMTP by validating and storing an app-specific password.
     */
    @PostMapping("/configure")
    public ResponseEntity<?> configureSMTP(@RequestBody Map<String, String> body, Principal principal) {
        if (principal == null) {
            return ResponseEntity.status(401).body(Map.of("success", false, "message", "Not authenticated"));
        }

        String supabaseId = principal.getName();
        String appPassword = body.get("appPassword");

        if (appPassword == null || appPassword.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "App password is required"));
        }

        // Get or create user token record
        UserToken userToken = userTokenRepository.findBySupabaseId(supabaseId).orElse(null);
        if (userToken == null) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "User record not found. Please sign in again."));
        }

        String email = userToken.getEmail();
        String domain = SmtpProviderConfig.extractDomain(email);

        if (!smtpProviderConfig.isSupported(domain)) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Your email provider is not supported for SMTP sending."));
        }

        // Validate the password by attempting SMTP connection
        try {
            smtpEmailService.validateConnection(email, appPassword.trim());
        } catch (SmtpSendException e) {
            logger.warn("SMTP validation failed for user {}: {}", supabaseId, e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage(),
                    "code", e.getCode()));
        }

        // Encrypt and store
        String encrypted = encryptionService.encrypt(appPassword.trim());
        userToken.setSmtpPasswordEncrypted(encrypted);
        userToken.setSmtpConfigured(true);
        userToken.setSmtpSetupSkipped(false);
        userTokenRepository.save(userToken);

        logger.info("SMTP configured successfully for user {}", supabaseId);
        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Email sending configured successfully!"));
    }

    /**
     * Send a test email to the user's own address.
     */
    @PostMapping("/test")
    public ResponseEntity<?> testSMTP(Principal principal) {
        if (principal == null) {
            return ResponseEntity.status(401).body(Map.of("success", false, "message", "Not authenticated"));
        }

        String supabaseId = principal.getName();
        UserToken userToken = userTokenRepository.findBySupabaseId(supabaseId).orElse(null);

        if (userToken == null || !userToken.isSmtpConfigured()) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "SMTP is not configured. Please set up your app password first."));
        }

        String email = userToken.getEmail();
        String password = encryptionService.decrypt(userToken.getSmtpPasswordEncrypted());

        try {
            smtpEmailService.sendEmail(email, password, email,
                    "QuickFlow — Test Email",
                    "<h2>✅ Your email is set up!</h2>" +
                            "<p>This is a test email from QuickFlow. If you're reading this, " +
                            "your email sending is configured correctly.</p>" +
                            "<p style='color: #666; font-size: 12px;'>Sent via SMTP</p>");

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Test email sent! Check your inbox."));
        } catch (SmtpSendException e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage(),
                    "code", e.getCode()));
        }
    }

    /**
     * Remove SMTP configuration (disconnect).
     */
    @DeleteMapping("/configure")
    public ResponseEntity<?> removeSMTP(Principal principal) {
        if (principal == null) {
            return ResponseEntity.status(401).body(Map.of("success", false, "message", "Not authenticated"));
        }

        String supabaseId = principal.getName();
        UserToken userToken = userTokenRepository.findBySupabaseId(supabaseId).orElse(null);

        if (userToken == null) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "User record not found"));
        }

        userToken.setSmtpPasswordEncrypted(null);
        userToken.setSmtpConfigured(false);
        userTokenRepository.save(userToken);

        logger.info("SMTP configuration removed for user {}", supabaseId);
        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Email configuration removed."));
    }

    /**
     * Get SMTP configuration status.
     */
    @GetMapping("/status")
    public ResponseEntity<?> getStatus(Principal principal) {
        if (principal == null) {
            return ResponseEntity.status(401).body(Map.of("success", false, "message", "Not authenticated"));
        }

        String supabaseId = principal.getName();
        UserToken userToken = userTokenRepository.findBySupabaseId(supabaseId).orElse(null);

        if (userToken == null) {
            return ResponseEntity.ok(Map.of(
                    "smtpConfigured", false,
                    "smtpSetupSkipped", false,
                    "providerSupported", false,
                    "needsSetup", false));
        }

        String email = userToken.getEmail();
        String domain = SmtpProviderConfig.extractDomain(email);
        boolean supported = smtpProviderConfig.isSupported(domain);
        boolean blocked = smtpProviderConfig.isBlocked(domain);
        String providerName = smtpProviderConfig.getProviderName(domain);
        boolean isOAuth = "google".equalsIgnoreCase(userToken.getProvider())
                || "azure".equalsIgnoreCase(userToken.getProvider());

        return ResponseEntity.ok(Map.of(
                "smtpConfigured", userToken.isSmtpConfigured(),
                "smtpSetupSkipped", userToken.isSmtpSetupSkipped(),
                "providerSupported", supported,
                "providerBlocked", blocked,
                "providerName", providerName != null ? providerName : "Unknown",
                "needsSetup", supported && !userToken.isSmtpConfigured() && !userToken.isSmtpSetupSkipped() && !isOAuth,
                "isOAuth", isOAuth));
    }

    /**
     * Skip SMTP setup.
     */
    @PostMapping("/skip-setup")
    public ResponseEntity<?> skipSetup(Principal principal) {
        if (principal == null) {
            return ResponseEntity.status(401).body(Map.of("success", false, "message", "Not authenticated"));
        }

        String supabaseId = principal.getName();
        UserToken userToken = userTokenRepository.findBySupabaseId(supabaseId).orElse(null);

        if (userToken == null) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "User record not found"));
        }

        userToken.setSmtpSetupSkipped(true);
        userTokenRepository.save(userToken);

        return ResponseEntity.ok(Map.of("success", true));
    }
}
