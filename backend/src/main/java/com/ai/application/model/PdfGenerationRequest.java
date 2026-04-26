package com.ai.application.model;

import java.util.Map;

public class PdfGenerationRequest {
    private String htmlContent;
    private String markdownContent;
    private Map<String, String> meetingMetadata;
    private Map<String, Object> outputPreferences;

    // Getters and Setters
    public String getHtmlContent() {
        return htmlContent;
    }

    public void setHtmlContent(String htmlContent) {
        this.htmlContent = htmlContent;
    }

    public String getMarkdownContent() {
        return markdownContent;
    }

    public void setMarkdownContent(String markdownContent) {
        this.markdownContent = markdownContent;
    }

    public Map<String, String> getMeetingMetadata() {
        return meetingMetadata;
    }

    public void setMeetingMetadata(Map<String, String> meetingMetadata) {
        this.meetingMetadata = meetingMetadata;
    }

    public Map<String, Object> getOutputPreferences() {
        return outputPreferences;
    }

    public void setOutputPreferences(Map<String, Object> outputPreferences) {
        this.outputPreferences = outputPreferences;
    }
}
