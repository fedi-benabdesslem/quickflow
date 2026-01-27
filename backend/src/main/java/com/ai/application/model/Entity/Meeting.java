package com.ai.application.model.Entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.index.Indexed;
import java.time.LocalDateTime;
import java.util.List;

@Document(collection = "meetings")
public class Meeting {
    @Id
    private String id;

    private List<String> people; // Changed to List for multiple attendees
    private String location;
    private LocalDateTime timeBegin; // Improved: Use LocalDateTime
    private LocalDateTime timeEnd;
    @Indexed // For date-based queries
    private LocalDateTime date; // Changed to LocalDateTime
    private String subject;
    private String details;
    private String status = "draft"; // "draft" or "sent"
    private String generatedContent; // Generated meeting summary content

    @Indexed
    private LocalDateTime sentAt; // Timestamp when minutes were sent/shared
    private boolean deleted = false; // Soft delete flag
    private LocalDateTime deletedAt; // Timestamp when minutes were deleted

    // Constructors
    public Meeting() {
    }

    public Meeting(List<String> people, String location, LocalDateTime timeBegin, LocalDateTime timeEnd,
            LocalDateTime date, String subject, String details) {
        this.people = people;
        this.location = location;
        this.timeBegin = timeBegin;
        this.timeEnd = timeEnd;
        this.date = date;
        this.subject = subject;
        this.details = details;
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public List<String> getPeople() {
        return people;
    }

    public void setPeople(List<String> people) {
        this.people = people;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public LocalDateTime getTimeBegin() {
        return timeBegin;
    }

    public void setTimeBegin(LocalDateTime timeBegin) {
        this.timeBegin = timeBegin;
    }

    public LocalDateTime getTimeEnd() {
        return timeEnd;
    }

    public void setTimeEnd(LocalDateTime timeEnd) {
        this.timeEnd = timeEnd;
    }

    public LocalDateTime getDate() {
        return date;
    }

    public void setDate(LocalDateTime date) {
        this.date = date;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getDetails() {
        return details;
    }

    public void setDetails(String details) {
        this.details = details;
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

    public LocalDateTime getSentAt() {
        return sentAt;
    }

    public void setSentAt(LocalDateTime sentAt) {
        this.sentAt = sentAt;
    }

    private String pdfFileId; // GridFS file ID for attached PDF

    public String getPdfFileId() {
        return pdfFileId;
    }

    public void setPdfFileId(String pdfFileId) {
        this.pdfFileId = pdfFileId;
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