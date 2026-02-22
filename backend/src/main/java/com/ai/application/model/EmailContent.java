package com.ai.application.model;

/**
 * Represents a parsed email with separate subject and body fields.
 * Used by LLMService to return structured email output from the LLM.
 */
public class EmailContent {
    private String subject;
    private String body;

    public EmailContent() {
    }

    public EmailContent(String subject, String body) {
        this.subject = subject;
        this.body = body;
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
}
