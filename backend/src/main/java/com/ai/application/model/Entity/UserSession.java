package com.ai.application.model.Entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.index.Indexed;

import java.time.LocalDateTime;

/**
 * Session entity for refresh token management.
 * Stored in "sessions" collection with TTL auto-deletion.
 */
@Document(collection = "sessions")
public class UserSession {

    @Id
    private String id;

    @Indexed
    private String userId;

    private String refreshTokenHash; // bcrypt hash of the refresh token
    private String deviceInfo; // User-Agent header
    private String ipAddress;
    private String loginProvider; // "local", "google", "microsoft"

    private LocalDateTime createdAt;

    @Indexed(expireAfterSeconds = 0) // TTL index — MongoDB auto-deletes expired sessions
    private LocalDateTime expiresAt;

    private LocalDateTime lastActiveAt;
    private boolean revoked;

    public UserSession() {
        this.createdAt = LocalDateTime.now();
        this.lastActiveAt = LocalDateTime.now();
        this.revoked = false;
    }

    public UserSession(String userId, String refreshTokenHash, String deviceInfo,
            String ipAddress, String loginProvider, LocalDateTime expiresAt) {
        this();
        this.userId = userId;
        this.refreshTokenHash = refreshTokenHash;
        this.deviceInfo = deviceInfo;
        this.ipAddress = ipAddress;
        this.loginProvider = loginProvider;
        this.expiresAt = expiresAt;
    }

    // Getters and Setters

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getRefreshTokenHash() {
        return refreshTokenHash;
    }

    public void setRefreshTokenHash(String refreshTokenHash) {
        this.refreshTokenHash = refreshTokenHash;
    }

    public String getDeviceInfo() {
        return deviceInfo;
    }

    public void setDeviceInfo(String deviceInfo) {
        this.deviceInfo = deviceInfo;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public String getLoginProvider() {
        return loginProvider;
    }

    public void setLoginProvider(String loginProvider) {
        this.loginProvider = loginProvider;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(LocalDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }

    public LocalDateTime getLastActiveAt() {
        return lastActiveAt;
    }

    public void setLastActiveAt(LocalDateTime lastActiveAt) {
        this.lastActiveAt = lastActiveAt;
    }

    public boolean isRevoked() {
        return revoked;
    }

    public void setRevoked(boolean revoked) {
        this.revoked = revoked;
    }
}
