package com.ai.application.model.DTO;

public class TemplateResponse {

    private String generatedContent;

    private String subject;

    public TemplateResponse() {
    }

    public TemplateResponse(String generatedContent) {
        this.generatedContent = generatedContent;
    }

    public TemplateResponse(String generatedContent, String subject) {
        this.generatedContent = generatedContent;
        this.subject = subject;
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
}
