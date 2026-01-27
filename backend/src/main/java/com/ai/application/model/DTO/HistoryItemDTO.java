package com.ai.application.model.DTO;

import java.time.LocalDateTime;
import java.util.List;

public class HistoryItemDTO {
    private String id;
    private String type; // "email" or "minute"
    private String title;
    private List<String> recipients;
    private int recipientCount;
    private LocalDateTime sentAt;
    private boolean isBookmarked;
    private String pdfFileId; // Only for minutes

    // Constructors, Getters, Setters
    public HistoryItemDTO() {
    }

    public HistoryItemDTO(String id, String type, String title, List<String> recipients,
            int recipientCount, LocalDateTime sentAt, boolean isBookmarked, String pdfFileId) {
        this.id = id;
        this.type = type;
        this.title = title;
        this.recipients = recipients;
        this.recipientCount = recipientCount;
        this.sentAt = sentAt;
        this.isBookmarked = isBookmarked;
        this.pdfFileId = pdfFileId;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public List<String> getRecipients() {
        return recipients;
    }

    public void setRecipients(List<String> recipients) {
        this.recipients = recipients;
    }

    public int getRecipientCount() {
        return recipientCount;
    }

    public void setRecipientCount(int recipientCount) {
        this.recipientCount = recipientCount;
    }

    public LocalDateTime getSentAt() {
        return sentAt;
    }

    public void setSentAt(LocalDateTime sentAt) {
        this.sentAt = sentAt;
    }

    public boolean getIsBookmarked() {
        return isBookmarked;
    }

    public void setIsBookmarked(boolean isBookmarked) {
        this.isBookmarked = isBookmarked;
    }

    public String getPdfFileId() {
        return pdfFileId;
    }

    public void setPdfFileId(String pdfFileId) {
        this.pdfFileId = pdfFileId;
    }
}
