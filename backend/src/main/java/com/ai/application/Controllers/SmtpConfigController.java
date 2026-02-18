package com.ai.application.Controllers;

import com.ai.application.Config.SmtpProviderConfig;
import com.ai.application.Repositories.UserRepository;
import com.ai.application.Services.DomainDetectionService;
import com.ai.application.Services.EncryptionService;
import com.ai.application.Services.SmtpEmailService;
import com.ai.application.Services.SmtpEmailService.SmtpSendException;
import com.ai.application.Repositories.UserTokenRepository;
import com.ai.application.model.Entity.AuthConnection;
import com.ai.application.model.Entity.User;
import com.ai.application.model.Entity.UserToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Controller for SMTP email configuration endpoints.
 * Updated to use new User model with legacy UserToken fallback.
 */
@RestController
@RequestMapping("/api/user/smtp")
public class SmtpConfigController {

    private static final Logger logger = LoggerFactory.getLogger(SmtpConfigController.class);

    private final UserRepository userRepository;
    private final UserTokenRepository userTokenRepository;
    private final EncryptionService encryptionService;
    private final SmtpEmailService smtpEmailService;
    private final SmtpProviderConfig smtpProviderConfig;
    private final DomainDetectionService domainDetectionService;

    public SmtpConfigController(UserRepository userRepository,
            UserTokenRepository userTokenRepository,
            EncryptionService encryptionService,
            SmtpEmailService smtpEmailService,
            SmtpProviderConfig smtpProviderConfig,
            DomainDetectionService domainDetectionService) {
        this.userRepository = userRepository;
        this.userTokenRepository = userTokenRepository;
        this.encryptionService = encryptionService;
        this.smtpEmailService = smtpEmailService;
        this.smtpProviderConfig = smtpProviderConfig;
        this.domainDetectionService = domainDetectionService;
    }

    /**
     * Configure SMTP by validating and storing an app-specific password.
     */
    @PostMapping("/configure")
    public ResponseEntity<?> configureSMTP(@RequestBody Map<String, String> body, Principal principal) {
        if (principal == null) {
            return ResponseEntity.status(401).body(Map.of("success", false, "message", "Not authenticated"));
        }

        String userId = principal.getName();
        String appPassword = body.get("appPassword");

        if (appPassword == null || appPassword.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "App password is required"));
        }

        // Try new User model first
        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            String email = user.getEmail();
            String domain = SmtpProviderConfig.extractDomain(email);

            if (!smtpProviderConfig.isSupported(domain)) {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "message", "Your email provider is not supported for SMTP sending."));
            }

            try {
                smtpEmailService.validateConnection(email, appPassword.trim());
            } catch (SmtpSendException e) {
                logger.warn("SMTP validation failed for user {}: {}", userId, e.getMessage());
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false, "message", e.getMessage(), "code", e.getCode()));
            }

            String encrypted = encryptionService.encrypt(appPassword.trim());
            user.setSmtpPasswordEncrypted(encrypted);
            user.setSmtpConfigured(true);
            user.setSmtpSetupSkipped(false);
            userRepository.save(user);

            logger.info("SMTP configured successfully for user {}", userId);
            return ResponseEntity.ok(Map.of("success", true, "message", "Email sending configured successfully!"));
        }

        // Legacy fallback
        UserToken userToken = userTokenRepository.findByEmail(userId).orElse(null);
        if (userToken == null) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "User record not found."));
        }

        String email = userToken.getEmail();
        String domain = SmtpProviderConfig.extractDomain(email);

        if (!smtpProviderConfig.isSupported(domain)) {
            return ResponseEntity.badRequest().body(Map.of("success", false,
                    "message", "Your email provider is not supported for SMTP sending."));
        }

        try {
            smtpEmailService.validateConnection(email, appPassword.trim());
        } catch (SmtpSendException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("success", false, "message", e.getMessage(), "code", e.getCode()));
        }

        String encrypted = encryptionService.encrypt(appPassword.trim());
        userToken.setSmtpPasswordEncrypted(encrypted);
        userToken.setSmtpConfigured(true);
        userToken.setSmtpSetupSkipped(false);
        userTokenRepository.save(userToken);

        return ResponseEntity.ok(Map.of("success", true, "message", "Email sending configured successfully!"));
    }

    /**
     * Send a test email to the user's own address.
     */
    @PostMapping("/test")
    public ResponseEntity<?> testSMTP(Principal principal) {
        if (principal == null) {
            return ResponseEntity.status(401).body(Map.of("success", false, "message", "Not authenticated"));
        }

        String userId = principal.getName();

        // Try new User model
        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            if (!user.isSmtpConfigured()) {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false, "message", "SMTP is not configured. Please set up your app password first."));
            }
            String email = user.getEmail();
            String password = encryptionService.decrypt(user.getSmtpPasswordEncrypted());
            return sendTestEmail(email, password);
        }

        // Legacy fallback
        UserToken userToken = userTokenRepository.findByEmail(userId).orElse(null);
        if (userToken == null || !userToken.isSmtpConfigured()) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false, "message", "SMTP is not configured. Please set up your app password first."));
        }
        String email = userToken.getEmail();
        String password = encryptionService.decrypt(userToken.getSmtpPasswordEncrypted());
        return sendTestEmail(email, password);
    }

    private ResponseEntity<?> sendTestEmail(String email, String password) {
        try {
            smtpEmailService.sendEmail(email, password, email,
                    "QuickFlow — Test Email",
                    "<h2>✅ Your email is set up!</h2>" +
                            "<p>This is a test email from QuickFlow. If you're reading this, " +
                            "your email sending is configured correctly.</p>" +
                            "<p style='color: #666; font-size: 12px;'>Sent via SMTP</p>");
            return ResponseEntity.ok(Map.of("success", true, "message", "Test email sent! Check your inbox."));
        } catch (SmtpSendException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("success", false, "message", e.getMessage(), "code", e.getCode()));
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

        String userId = principal.getName();

        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            user.setSmtpPasswordEncrypted(null);
            user.setSmtpConfigured(false);
            userRepository.save(user);
            logger.info("SMTP configuration removed for user {}", userId);
            return ResponseEntity.ok(Map.of("success", true, "message", "Email configuration removed."));
        }

        // Legacy fallback
        UserToken userToken = userTokenRepository.findByEmail(userId).orElse(null);
        if (userToken == null) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "User record not found"));
        }
        userToken.setSmtpPasswordEncrypted(null);
        userToken.setSmtpConfigured(false);
        userTokenRepository.save(userToken);
        return ResponseEntity.ok(Map.of("success", true, "message", "Email configuration removed."));
    }

    /**
     * Get SMTP configuration status.
     */
    @GetMapping("/status")
    public ResponseEntity<?> getStatus(Principal principal) {
        if (principal == null) {
            return ResponseEntity.status(401).body(Map.of("success", false, "message", "Not authenticated"));
        }

        String userId = principal.getName();

        // Try new User model first
        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isPresent()) {
            return buildStatusFromUser(userOpt.get());
        }

        // Legacy fallback
        UserToken userToken = userTokenRepository.findByEmail(userId).orElse(null);
        if (userToken == null) {
            return ResponseEntity.ok(Map.of(
                    "smtpConfigured", false, "smtpSetupSkipped", false,
                    "providerSupported", false, "needsSetup", false));
        }
        return buildStatusFromUserToken(userToken);
    }

    private ResponseEntity<?> buildStatusFromUser(User user) {
        String email = user.getEmail();
        String domain = SmtpProviderConfig.extractDomain(email);
        boolean supported = smtpProviderConfig.isSupported(domain);
        boolean blocked = smtpProviderConfig.isBlocked(domain);
        String providerName = smtpProviderConfig.getProviderName(domain);
        boolean isOAuth = user.hasProvider("google") || user.hasProvider("microsoft");

        String hostingProvider = user.getDetectedHostingProvider();
        if (hostingProvider == null) {
            hostingProvider = domainDetectionService.detectProvider(email);
            user.setDetectedHostingProvider(hostingProvider);
            userRepository.save(user);
        }
        String hostingProviderName = domainDetectionService.getProviderName(hostingProvider);

        boolean hasLinked = user.getAuthConnections() != null && user.getAuthConnections().stream()
                .anyMatch(c -> "linked".equals(c.getConnectionType()));

        String action = computeActionForUser(isOAuth, user, hostingProvider, supported);

        Map<String, Object> response = new HashMap<>();
        response.put("smtpConfigured", user.isSmtpConfigured());
        response.put("smtpSetupSkipped", user.isSmtpSetupSkipped());
        response.put("providerSupported", supported);
        response.put("providerBlocked", blocked);
        response.put("providerName", providerName != null ? providerName : "Unknown");
        response.put("needsSetup", supported && !user.isSmtpConfigured()
                && !user.isSmtpSetupSkipped() && !isOAuth && !hasLinked);
        response.put("isOAuth", isOAuth);
        response.put("hostingProvider", hostingProvider);
        response.put("hostingProviderName", hostingProviderName != null ? hostingProviderName : "");

        // Linked provider info from AuthConnections
        AuthConnection linkedConn = user.getAuthConnections() != null ? user.getAuthConnections().stream()
                .filter(c -> "linked".equals(c.getConnectionType()))
                .findFirst().orElse(null) : null;
        response.put("linkedProvider", linkedConn != null ? linkedConn.getProvider() : "");
        response.put("linkedProviderName", linkedConn != null
                ? domainDetectionService.getProviderName(linkedConn.getProvider())
                : "");
        response.put("linkedProviderEmail", linkedConn != null && linkedConn.getProviderEmail() != null
                ? linkedConn.getProviderEmail()
                : "");
        response.put("action", action);

        return ResponseEntity.ok(response);
    }

    private String computeActionForUser(boolean isOAuth, User user, String hostingProvider, boolean smtpSupported) {
        if (isOAuth)
            return "ready";
        if (user.getAuthConnections() != null && user.getAuthConnections().stream()
                .anyMatch(c -> "linked".equals(c.getConnectionType()))) {
            return "ready";
        }
        if (user.isSmtpConfigured())
            return "ready";
        if ("google".equals(hostingProvider) || "microsoft".equals(hostingProvider))
            return "link_oauth";
        if (smtpSupported)
            return "setup_smtp";
        return "unsupported";
    }

    /**
     * Legacy: Build status from UserToken.
     */
    private ResponseEntity<?> buildStatusFromUserToken(UserToken userToken) {
        String email = userToken.getEmail();
        String domain = SmtpProviderConfig.extractDomain(email);
        boolean supported = smtpProviderConfig.isSupported(domain);
        boolean blocked = smtpProviderConfig.isBlocked(domain);
        String providerName = smtpProviderConfig.getProviderName(domain);
        boolean isOAuth = "google".equalsIgnoreCase(userToken.getProvider())
                || "azure".equalsIgnoreCase(userToken.getProvider());

        String hostingProvider = domainDetectionService.detectProvider(email);
        String hostingProviderName = domainDetectionService.getProviderName(hostingProvider);

        Map<String, Object> response = new HashMap<>();
        response.put("smtpConfigured", userToken.isSmtpConfigured());
        response.put("smtpSetupSkipped", userToken.isSmtpSetupSkipped());
        response.put("providerSupported", supported);
        response.put("providerBlocked", blocked);
        response.put("providerName", providerName != null ? providerName : "Unknown");
        response.put("needsSetup", supported && !userToken.isSmtpConfigured()
                && !userToken.isSmtpSetupSkipped() && !isOAuth && !userToken.hasLinkedProvider());
        response.put("isOAuth", isOAuth);
        response.put("hostingProvider", hostingProvider);
        response.put("hostingProviderName", hostingProviderName != null ? hostingProviderName : "");
        response.put("linkedProvider", userToken.getLinkedProvider() != null ? userToken.getLinkedProvider() : "");
        response.put("linkedProviderName",
                userToken.getLinkedProvider() != null
                        ? domainDetectionService.getProviderName(userToken.getLinkedProvider())
                        : "");
        response.put("linkedProviderEmail",
                userToken.getLinkedProviderEmail() != null ? userToken.getLinkedProviderEmail() : "");
        response.put("action", "ready");

        return ResponseEntity.ok(response);
    }

    /**
     * Skip SMTP setup.
     */
    @PostMapping("/skip-setup")
    public ResponseEntity<?> skipSetup(Principal principal) {
        if (principal == null) {
            return ResponseEntity.status(401).body(Map.of("success", false, "message", "Not authenticated"));
        }

        String userId = principal.getName();

        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            user.setSmtpSetupSkipped(true);
            userRepository.save(user);
            return ResponseEntity.ok(Map.of("success", true));
        }

        // Legacy fallback
        UserToken userToken = userTokenRepository.findByEmail(userId).orElse(null);
        if (userToken == null) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "User record not found"));
        }
        userToken.setSmtpSetupSkipped(true);
        userTokenRepository.save(userToken);
        return ResponseEntity.ok(Map.of("success", true));
    }
}
