package com.ai.application.model.DTO;

import java.util.List;
import java.util.Map;

public class MinutesEmailRequest {

    private String pdfFileId;
    private List<String> recipients;
    private String subject;
    private String body;
    private Map<String, String> meetingMetadata;

    public MinutesEmailRequest() {
    }

    public String getPdfFileId() {
        return pdfFileId;
    }

    public void setPdfFileId(String pdfFileId) {
        this.pdfFileId = pdfFileId;
    }

    public List<String> getRecipients() {
        return recipients;
    }

    public void setRecipients(List<String> recipients) {
        this.recipients = recipients;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public Map<String, String> getMeetingMetadata() {
        return meetingMetadata;
    }

    public void setMeetingMetadata(Map<String, String> meetingMetadata) {
        this.meetingMetadata = meetingMetadata;
    }
}
