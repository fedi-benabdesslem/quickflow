package com.ai.application.model;

/**
 * Request DTO for Quick Mode extraction.
 */
public class QuickModeRequest {
    private String content;
    private String date;
    private String time;

    public QuickModeRequest() {
    }

    public QuickModeRequest(String content, String date, String time) {
        this.content = content;
        this.date = date;
        this.time = time;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }
}
