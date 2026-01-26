package com.ai.application.model.Entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Contact entity for storing user contacts imported from Google/Microsoft or
 * added manually.
 */
@Document(collection = "contacts")
public class Contact {

    @Id
    private String id;

    @Indexed
    private String userId; // QuickFlow user who owns this contact

    // Contact info
    private String name;

    @Indexed
    private String email;

    private String phone;
    private String photo; // URL or base64

    // Source tracking
    private String source; // "google", "microsoft", "manual"
    private String sourceId; // Original ID from Google/Microsoft

    // QuickFlow detection
    private boolean usesQuickFlow;
    private String quickflowUserId; // If uses QuickFlow, their user ID

    // Organization
    private List<String> groups = new ArrayList<>(); // Group names user assigned
    private boolean isFavorite;

    // Usage tracking
    private int usageCount = 0;
    private LocalDateTime lastUsed;

    // Sync tracking
    private LocalDateTime lastSynced;
    private boolean isDeleted = false; // Soft delete flag
    private boolean isIgnored = false; // Don't re-import this contact

    // Timestamps
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Constructors
    public Contact() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public Contact(String userId, String name, String email, String source) {
        this();
        this.userId = userId;
        this.name = name;
        this.email = email;
        this.source = source;
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

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getPhoto() {
        return photo;
    }

    public void setPhoto(String photo) {
        this.photo = photo;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getSourceId() {
        return sourceId;
    }

    public void setSourceId(String sourceId) {
        this.sourceId = sourceId;
    }

    public boolean isUsesQuickFlow() {
        return usesQuickFlow;
    }

    public void setUsesQuickFlow(boolean usesQuickFlow) {
        this.usesQuickFlow = usesQuickFlow;
    }

    public String getQuickflowUserId() {
        return quickflowUserId;
    }

    public void setQuickflowUserId(String quickflowUserId) {
        this.quickflowUserId = quickflowUserId;
    }

    public List<String> getGroups() {
        return groups;
    }

    public void setGroups(List<String> groups) {
        this.groups = groups;
    }

    public boolean isFavorite() {
        return isFavorite;
    }

    public void setFavorite(boolean favorite) {
        isFavorite = favorite;
    }

    public int getUsageCount() {
        return usageCount;
    }

    public void setUsageCount(int usageCount) {
        this.usageCount = usageCount;
    }

    public LocalDateTime getLastUsed() {
        return lastUsed;
    }

    public void setLastUsed(LocalDateTime lastUsed) {
        this.lastUsed = lastUsed;
    }

    public LocalDateTime getLastSynced() {
        return lastSynced;
    }

    public void setLastSynced(LocalDateTime lastSynced) {
        this.lastSynced = lastSynced;
    }

    public boolean isDeleted() {
        return isDeleted;
    }

    public void setDeleted(boolean deleted) {
        isDeleted = deleted;
    }

    public boolean isIgnored() {
        return isIgnored;
    }

    public void setIgnored(boolean ignored) {
        isIgnored = ignored;
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

    // Utility methods
    public void incrementUsage() {
        this.usageCount++;
        this.lastUsed = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public void markSynced() {
        this.lastSynced = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }
}
