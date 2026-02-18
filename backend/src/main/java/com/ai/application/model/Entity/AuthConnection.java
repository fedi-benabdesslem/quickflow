package com.ai.application.model.Entity;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Embedded document representing an OAuth provider connection.
 * A user can have multiple connections (e.g., Google + Microsoft).
 */
public class AuthConnection {
    private String provider; // "google" or "microsoft"
    private String providerUserId; // provider's user ID
    private String providerEmail; // email from provider (might differ from primary email)
    private String accessTokenEncrypted; // AES-256 encrypted OAuth access token
    private String refreshTokenEncrypted; // AES-256 encrypted OAuth refresh token
    private LocalDateTime tokenExpiresAt;
    private LocalDateTime connectedAt;
    private LocalDateTime lastUsedAt;
    private String connectionType; // "primary" (signed up with) or "linked" (added later)
    private List<String> grantedScopes;

    public AuthConnection() {
        this.connectedAt = LocalDateTime.now();
        this.lastUsedAt = LocalDateTime.now();
    }

    public AuthConnection(String provider, String providerUserId, String providerEmail, String connectionType) {
        this();
        this.provider = provider;
        this.providerUserId = providerUserId;
        this.providerEmail = providerEmail;
        this.connectionType = connectionType;
    }

    // Getters and Setters

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public String getProviderUserId() {
        return providerUserId;
    }

    public void setProviderUserId(String providerUserId) {
        this.providerUserId = providerUserId;
    }

    public String getProviderEmail() {
        return providerEmail;
    }

    public void setProviderEmail(String providerEmail) {
        this.providerEmail = providerEmail;
    }

    public String getAccessTokenEncrypted() {
        return accessTokenEncrypted;
    }

    public void setAccessTokenEncrypted(String accessTokenEncrypted) {
        this.accessTokenEncrypted = accessTokenEncrypted;
    }

    public String getRefreshTokenEncrypted() {
        return refreshTokenEncrypted;
    }

    public void setRefreshTokenEncrypted(String refreshTokenEncrypted) {
        this.refreshTokenEncrypted = refreshTokenEncrypted;
    }

    public LocalDateTime getTokenExpiresAt() {
        return tokenExpiresAt;
    }

    public void setTokenExpiresAt(LocalDateTime tokenExpiresAt) {
        this.tokenExpiresAt = tokenExpiresAt;
    }

    public LocalDateTime getConnectedAt() {
        return connectedAt;
    }

    public void setConnectedAt(LocalDateTime connectedAt) {
        this.connectedAt = connectedAt;
    }

    public LocalDateTime getLastUsedAt() {
        return lastUsedAt;
    }

    public void setLastUsedAt(LocalDateTime lastUsedAt) {
        this.lastUsedAt = lastUsedAt;
    }

    public String getConnectionType() {
        return connectionType;
    }

    public void setConnectionType(String connectionType) {
        this.connectionType = connectionType;
    }

    public List<String> getGrantedScopes() {
        return grantedScopes;
    }

    public void setGrantedScopes(List<String> grantedScopes) {
        this.grantedScopes = grantedScopes;
    }
}
