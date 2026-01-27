package com.ai.application.model.Entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.LocalDateTime;

@Document(collection = "bookmarks")
@CompoundIndexes({
        @CompoundIndex(name = "user_item_idx", def = "{'userId': 1, 'itemId': 1}", unique = true),
        @CompoundIndex(name = "user_category_idx", def = "{'userId': 1, 'categoryName': 1}")
})
public class Bookmark {
    @Id
    private String id;

    @Indexed
    private String userId;

    private String itemId;
    private String itemType; // "email" or "minute"
    private String categoryName;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Bookmark() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public Bookmark(String userId, String itemId, String itemType, String categoryName) {
        this.userId = userId;
        this.itemId = itemId;
        this.itemType = itemType;
        this.categoryName = categoryName;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
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

    public String getItemId() {
        return itemId;
    }

    public void setItemId(String itemId) {
        this.itemId = itemId;
    }

    public String getItemType() {
        return itemType;
    }

    public void setItemType(String itemType) {
        this.itemType = itemType;
    }

    public String getCategoryName() {
        return categoryName;
    }

    public void setCategoryName(String categoryName) {
        this.categoryName = categoryName;
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
}
