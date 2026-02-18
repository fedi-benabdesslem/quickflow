package com.ai.application.model.Entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.index.Indexed;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Core user entity — identity-first design.
 * EMAIL is the primary identifier. One email = one account.
 * Multiple auth methods can be attached to one account.
 */
@Document(collection = "users")
public class User {

    // ── IDENTITY ──
    @Id
    private String id;

    @Indexed(unique = true)
    private String email; // PRIMARY IDENTIFIER — lowercase, trimmed, unique
    private String name;
    private String profilePhotoUrl;
    private String role; // "USER" or "ADMIN"

    // ── LOCAL AUTH ──
    private String passwordHash; // bcrypt hash, null if user never set up local auth
    private boolean localAuthEnabled; // true if they can log in with email+password

    // ── OAUTH CONNECTIONS ──
    private String primaryOAuthProvider; // "google" or "microsoft" if they originally signed up via OAuth
    private List<AuthConnection> authConnections; // all connected OAuth providers

    // ── MFA ──
    private boolean mfaEnabled;
    private String mfaSecretEncrypted; // AES-256 encrypted TOTP secret
    private List<String> recoveryCodesHashed; // bcrypt hashed one-time recovery codes

    // ── EMAIL SENDING CONFIG ──
    private String detectedHostingProvider; // "google", "microsoft", "unknown" (from MX lookup)
    private String smtpPasswordEncrypted; // AES-256 encrypted app-specific password
    private boolean smtpConfigured;
    private boolean smtpSetupSkipped;

    // ── ORGANIZATION (Business Plan) ──
    private String organizationId;
    private String organizationRole; // "admin" or "member"

    // ── SUBSCRIPTION ──
    private String plan; // "free_trial", "individual_pro", "business"
    private String stripeCustomerId;
    private String konnectCustomerId;
    private LocalDateTime trialExpiresAt;
    private LocalDateTime subscriptionExpiresAt;

    // ── PREFERENCES ──
    private String language; // "en", "fr", "ar"
    private String timezone;
    private String dateFormat;
    private Map<String, Object> preferences;

    // ── EMAIL VERIFICATION ──
    private boolean emailVerified;

    // ── METADATA ──
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime lastLoginAt;
    private String lastLoginProvider; // "local", "google", "microsoft"
    private int loginCount;
    private boolean accountDisabled;

    public User() {
        this.authConnections = new ArrayList<>();
        this.recoveryCodesHashed = new ArrayList<>();
        this.preferences = new HashMap<>();
        this.role = "USER";
        this.plan = "free_trial";
        this.trialExpiresAt = LocalDateTime.now().plusDays(15);
        this.emailVerified = false;
        this.accountDisabled = false;
        this.loginCount = 0;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Find an auth connection by provider name.
     */
    public AuthConnection findConnection(String provider) {
        if (authConnections == null)
            return null;
        return authConnections.stream()
                .filter(c -> provider.equalsIgnoreCase(c.getProvider()))
                .findFirst()
                .orElse(null);
    }

    /**
     * Check if a provider is already connected.
     */
    public boolean hasProvider(String provider) {
        return findConnection(provider) != null;
    }

    /**
     * Add or update an auth connection.
     */
    public void addOrUpdateConnection(AuthConnection connection) {
        if (authConnections == null) {
            authConnections = new ArrayList<>();
        }
        authConnections.removeIf(c -> connection.getProvider().equalsIgnoreCase(c.getProvider()));
        authConnections.add(connection);
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Remove an auth connection by provider.
     */
    public void removeConnection(String provider) {
        if (authConnections != null) {
            authConnections.removeIf(c -> provider.equalsIgnoreCase(c.getProvider()));
            this.updatedAt = LocalDateTime.now();
        }
    }

    /**
     * Get list of connected provider names.
     */
    public List<String> getConnectedProviderNames() {
        if (authConnections == null)
            return new ArrayList<>();
        return authConnections.stream()
                .map(AuthConnection::getProvider)
                .toList();
    }

    /**
     * Check if user can send email via any method.
     */
    public boolean canSendEmail() {
        boolean hasOAuthEmailScope = authConnections != null && authConnections.stream().anyMatch(c -> {
            List<String> scopes = c.getGrantedScopes();
            if (scopes == null)
                return false;
            return scopes.stream().anyMatch(s -> s.contains("gmail.send") || s.contains("Mail.Send"));
        });
        return hasOAuthEmailScope || smtpConfigured;
    }

    // ── Getters and Setters ──

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getProfilePhotoUrl() {
        return profilePhotoUrl;
    }

    public void setProfilePhotoUrl(String profilePhotoUrl) {
        this.profilePhotoUrl = profilePhotoUrl;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public boolean isLocalAuthEnabled() {
        return localAuthEnabled;
    }

    public void setLocalAuthEnabled(boolean localAuthEnabled) {
        this.localAuthEnabled = localAuthEnabled;
    }

    public String getPrimaryOAuthProvider() {
        return primaryOAuthProvider;
    }

    public void setPrimaryOAuthProvider(String primaryOAuthProvider) {
        this.primaryOAuthProvider = primaryOAuthProvider;
    }

    public List<AuthConnection> getAuthConnections() {
        return authConnections;
    }

    public void setAuthConnections(List<AuthConnection> authConnections) {
        this.authConnections = authConnections;
    }

    public boolean isMfaEnabled() {
        return mfaEnabled;
    }

    public void setMfaEnabled(boolean mfaEnabled) {
        this.mfaEnabled = mfaEnabled;
    }

    public String getMfaSecretEncrypted() {
        return mfaSecretEncrypted;
    }

    public void setMfaSecretEncrypted(String mfaSecretEncrypted) {
        this.mfaSecretEncrypted = mfaSecretEncrypted;
    }

    public List<String> getRecoveryCodesHashed() {
        return recoveryCodesHashed;
    }

    public void setRecoveryCodesHashed(List<String> recoveryCodesHashed) {
        this.recoveryCodesHashed = recoveryCodesHashed;
    }

    public String getDetectedHostingProvider() {
        return detectedHostingProvider;
    }

    public void setDetectedHostingProvider(String detectedHostingProvider) {
        this.detectedHostingProvider = detectedHostingProvider;
    }

    public String getSmtpPasswordEncrypted() {
        return smtpPasswordEncrypted;
    }

    public void setSmtpPasswordEncrypted(String smtpPasswordEncrypted) {
        this.smtpPasswordEncrypted = smtpPasswordEncrypted;
    }

    public boolean isSmtpConfigured() {
        return smtpConfigured;
    }

    public void setSmtpConfigured(boolean smtpConfigured) {
        this.smtpConfigured = smtpConfigured;
    }

    public boolean isSmtpSetupSkipped() {
        return smtpSetupSkipped;
    }

    public void setSmtpSetupSkipped(boolean smtpSetupSkipped) {
        this.smtpSetupSkipped = smtpSetupSkipped;
    }

    public String getOrganizationId() {
        return organizationId;
    }

    public void setOrganizationId(String organizationId) {
        this.organizationId = organizationId;
    }

    public String getOrganizationRole() {
        return organizationRole;
    }

    public void setOrganizationRole(String organizationRole) {
        this.organizationRole = organizationRole;
    }

    public String getPlan() {
        return plan;
    }

    public void setPlan(String plan) {
        this.plan = plan;
    }

    public String getStripeCustomerId() {
        return stripeCustomerId;
    }

    public void setStripeCustomerId(String stripeCustomerId) {
        this.stripeCustomerId = stripeCustomerId;
    }

    public String getKonnectCustomerId() {
        return konnectCustomerId;
    }

    public void setKonnectCustomerId(String konnectCustomerId) {
        this.konnectCustomerId = konnectCustomerId;
    }

    public LocalDateTime getTrialExpiresAt() {
        return trialExpiresAt;
    }

    public void setTrialExpiresAt(LocalDateTime trialExpiresAt) {
        this.trialExpiresAt = trialExpiresAt;
    }

    public LocalDateTime getSubscriptionExpiresAt() {
        return subscriptionExpiresAt;
    }

    public void setSubscriptionExpiresAt(LocalDateTime subscriptionExpiresAt) {
        this.subscriptionExpiresAt = subscriptionExpiresAt;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public String getTimezone() {
        return timezone;
    }

    public void setTimezone(String timezone) {
        this.timezone = timezone;
    }

    public String getDateFormat() {
        return dateFormat;
    }

    public void setDateFormat(String dateFormat) {
        this.dateFormat = dateFormat;
    }

    public Map<String, Object> getPreferences() {
        return preferences;
    }

    public void setPreferences(Map<String, Object> preferences) {
        this.preferences = preferences;
    }

    public boolean isEmailVerified() {
        return emailVerified;
    }

    public void setEmailVerified(boolean emailVerified) {
        this.emailVerified = emailVerified;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public LocalDateTime getLastLoginAt() {
        return lastLoginAt;
    }

    public void setLastLoginAt(LocalDateTime lastLoginAt) {
        this.lastLoginAt = lastLoginAt;
    }

    public String getLastLoginProvider() {
        return lastLoginProvider;
    }

    public void setLastLoginProvider(String lastLoginProvider) {
        this.lastLoginProvider = lastLoginProvider;
    }

    public int getLoginCount() {
        return loginCount;
    }

    public void setLoginCount(int loginCount) {
        this.loginCount = loginCount;
    }

    public boolean isAccountDisabled() {
        return accountDisabled;
    }

    public void setAccountDisabled(boolean accountDisabled) {
        this.accountDisabled = accountDisabled;
    }
}
