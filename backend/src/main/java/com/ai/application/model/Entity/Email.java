package com.ai.application.model.Entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.util.List;
import java.time.LocalDateTime;

@Document(collection = "emails")
public class Email {

    @Id
    private String id;
    private List<String> recipients;
    private String input; // Original user input/prompt
    private String userId;
    private String senderId;
    private String senderEmail;
    private String status = "draft"; // "draft" or "sent"
    private String generatedContent; // Populated on final send
    private String subject; // From service or final edit
    private LocalDateTime sentAt; // Timestamp when email was sent
    private boolean deleted = false; // Soft delete flag
    private LocalDateTime deletedAt; // Timestamp when email was deleted

    // Constructors
    public Email() {
    }

    public Email(List<String> recipients, String input, String userId, String senderId, String senderEmail) {
        this.recipients = recipients;
        this.input = input;
        this.userId = userId;
        this.senderId = senderId;
        this.senderEmail = senderEmail;
    }

    // Getters/Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public List<String> getRecipients() {
        return recipients;
    }

    public void setRecipients(List<String> recipients) {
        this.recipients = recipients;
    }

    public String getInput() {
        return input;
    }

    public void setInput(String input) {
        this.input = input;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getSenderId() {
        return senderId;
    }

    public void setSenderId(String senderId) {
        this.senderId = senderId;
    }

    public String getSenderEmail() {
        return senderEmail;
    }

    public void setSenderEmail(String senderEmail) {
        this.senderEmail = senderEmail;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getGeneratedContent() {
        return generatedContent;
    }

    public void setGeneratedContent(String generatedContent) {
        this.generatedContent = generatedContent;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public LocalDateTime getSentAt() {
        return sentAt;
    }

    public void setSentAt(LocalDateTime sentAt) {
        this.sentAt = sentAt;
    }

    public boolean isDeleted() {
        return deleted;
    }

    public void setDeleted(boolean deleted) {
        this.deleted = deleted;
    }

    public LocalDateTime getDeletedAt() {
        return deletedAt;
    }

    public void setDeletedAt(LocalDateTime deletedAt) {
        this.deletedAt = deletedAt;
    }
}