package com.ai.application.model.Entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.index.Indexed;

import java.time.LocalDateTime;

/**
 * Entity for storing encrypted OAuth tokens in MongoDB.
 * 
 * Used to store access and refresh tokens from Google/Microsoft OAuth
 * to enable sending emails on behalf of users.
 */
@Document(collection = "user_tokens")
public class UserToken {

    @Id
    private String id;

    @Indexed(unique = true)
    private String supabaseId;

    private String email;

    /** Provider type: "google", "azure", or "email" */
    private String provider;

    /** Encrypted access token */
    private String accessToken;

    /** Encrypted refresh token */
    private String refreshToken;

    /** When the access token expires */
    private LocalDateTime expiresAt;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    public UserToken() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public UserToken(String supabaseId, String email, String provider) {
        this();
        this.supabaseId = supabaseId;
        this.email = email;
        this.provider = provider;
    }

    // Getters and Setters

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getSupabaseId() {
        return supabaseId;
    }

    public void setSupabaseId(String supabaseId) {
        this.supabaseId = supabaseId;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
        this.updatedAt = LocalDateTime.now();
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
        this.updatedAt = LocalDateTime.now();
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(LocalDateTime expiresAt) {
        this.expiresAt = expiresAt;
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

    /**
     * Checks if the access token is expired or will expire within 5 minutes.
     */
    public boolean isTokenExpiredOrExpiringSoon() {
        if (expiresAt == null) {
            return true;
        }
        return LocalDateTime.now().plusMinutes(5).isAfter(expiresAt);
    }

    /**
     * Checks if this user can send emails (has OAuth provider, not just
     * email/password).
     */
    public boolean canSendEmail() {
        return "google".equalsIgnoreCase(provider) || "azure".equalsIgnoreCase(provider);
    }
}
