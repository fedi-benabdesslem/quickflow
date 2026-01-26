package com.ai.application.model.Entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Group entity for organizing contacts into named groups.
 */
@Document(collection = "groups")
public class Group {

    @Id
    private String id;

    @Indexed
    private String userId; // QuickFlow user who owns this group

    private String name;
    private String description;
    private List<String> memberIds = new ArrayList<>(); // Contact IDs

    // Timestamps
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Constructors
    public Group() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public Group(String userId, String name) {
        this();
        this.userId = userId;
        this.name = name;
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

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public List<String> getMemberIds() {
        return memberIds;
    }

    public void setMemberIds(List<String> memberIds) {
        this.memberIds = memberIds;
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
    public void addMember(String contactId) {
        if (!this.memberIds.contains(contactId)) {
            this.memberIds.add(contactId);
            this.updatedAt = LocalDateTime.now();
        }
    }

    public void removeMember(String contactId) {
        if (this.memberIds.remove(contactId)) {
            this.updatedAt = LocalDateTime.now();
        }
    }

    public int getMemberCount() {
        return this.memberIds.size();
    }
}
