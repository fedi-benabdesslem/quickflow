package com.ai.application.Services;

import com.ai.application.Repositories.UserRepository;
import com.ai.application.Repositories.UserTokenRepository;
import com.ai.application.model.Entity.AuthConnection;
import com.ai.application.model.Entity.User;
import com.ai.application.model.Entity.UserToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * One-time migration service that transforms existing UserToken documents
 * into the new User model, preserving OAuth tokens and email config.
 *
 * Run this once after deploying the new auth system to migrate existing users.
 * After migration, the legacy UserToken documents are kept as backup but
 * are no longer the primary data source.
 */
@Service
public class DataMigrationService {

    private static final Logger logger = LoggerFactory.getLogger(DataMigrationService.class);

    private final UserRepository userRepository;
    private final UserTokenRepository userTokenRepository;

    public DataMigrationService(UserRepository userRepository,
            UserTokenRepository userTokenRepository) {
        this.userRepository = userRepository;
        this.userTokenRepository = userTokenRepository;
    }

    /**
     * Migrates all UserToken documents to the new User model.
     * Skips users that already exist in the new collection.
     *
     * @return MigrationResult with counts
     */
    public MigrationResult migrateAllUsers() {
        List<UserToken> allTokens = userTokenRepository.findAll();
        int migrated = 0;
        int skipped = 0;
        int failed = 0;
        List<String> errors = new ArrayList<>();

        logger.info("[DataMigration] Starting migration of {} UserToken documents", allTokens.size());

        for (UserToken token : allTokens) {
            try {
                if (token.getEmail() == null || token.getEmail().isBlank()) {
                    logger.warn("[DataMigration] Skipping UserToken with no email: {}", token.getSupabaseId());
                    skipped++;
                    continue;
                }

                // Check if user already exists
                if (userRepository.findByEmail(token.getEmail()).isPresent()) {
                    logger.info("[DataMigration] User already exists for email: {}", token.getEmail());
                    skipped++;
                    continue;
                }

                User user = convertTokenToUser(token);
                userRepository.save(user);
                migrated++;
                logger.info("[DataMigration] Migrated user: {} ({})", token.getEmail(), user.getId());

            } catch (Exception e) {
                failed++;
                String error = "Failed to migrate " + token.getEmail() + ": " + e.getMessage();
                errors.add(error);
                logger.error("[DataMigration] {}", error, e);
            }
        }

        logger.info("[DataMigration] Migration complete: {} migrated, {} skipped, {} failed",
                migrated, skipped, failed);

        return new MigrationResult(allTokens.size(), migrated, skipped, failed, errors);
    }

    /**
     * Converts a legacy UserToken to the new User model.
     */
    private User convertTokenToUser(UserToken token) {
        User user = new User();
        user.setEmail(token.getEmail());
        user.setName(token.getEmail().split("@")[0]); // default name from email
        user.setLocalAuthEnabled(false); // OAuth-only users from Supabase
        user.setEmailVerified(true); // Supabase users were verified
        user.setRole("USER");
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());

        // Migrate primary OAuth provider connection
        List<AuthConnection> connections = new ArrayList<>();

        if (token.getProvider() != null && token.getAccessToken() != null) {
            AuthConnection primaryConn = new AuthConnection();
            primaryConn.setProvider(token.getProvider().toLowerCase());
            primaryConn.setConnectionType("primary");
            primaryConn.setProviderEmail(token.getEmail());
            primaryConn.setAccessTokenEncrypted(token.getAccessToken());

            if (token.getRefreshToken() != null) {
                primaryConn.setRefreshTokenEncrypted(token.getRefreshToken());
            }

            primaryConn.setConnectedAt(LocalDateTime.now());
            connections.add(primaryConn);
        }

        // Migrate linked provider if present
        if (token.getLinkedProvider() != null && token.getLinkedProviderTokenEncrypted() != null) {
            AuthConnection linkedConn = new AuthConnection();
            linkedConn.setProvider(token.getLinkedProvider().toLowerCase());
            linkedConn.setConnectionType("linked");
            linkedConn.setProviderEmail(token.getLinkedProviderEmail());
            linkedConn.setAccessTokenEncrypted(token.getLinkedProviderTokenEncrypted());

            if (token.getLinkedProviderRefreshToken() != null) {
                linkedConn.setRefreshTokenEncrypted(token.getLinkedProviderRefreshToken());
            }

            linkedConn.setConnectedAt(LocalDateTime.now());
            connections.add(linkedConn);
        }

        user.setAuthConnections(connections);

        // Migrate SMTP config if present
        if (token.getSmtpPasswordEncrypted() != null) {
            user.setSmtpPasswordEncrypted(token.getSmtpPasswordEncrypted());
            user.setSmtpConfigured(true);
        }

        return user;
    }

    /**
     * Result of a migration run.
     */
    public record MigrationResult(
            int total,
            int migrated,
            int skipped,
            int failed,
            List<String> errors) {
    }
}
