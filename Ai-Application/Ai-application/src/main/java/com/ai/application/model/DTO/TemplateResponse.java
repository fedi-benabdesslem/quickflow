package com.ai.application.model.DTO;

public class TemplateResponse {

    private String generatedContent;

    public TemplateResponse() {}

    public TemplateResponse(String generatedContent) {
        this.generatedContent = generatedContent;
    }

    public String getGeneratedContent() {
        return generatedContent;
    }

    public void setGeneratedContent(String generatedContent) {
        this.generatedContent = generatedContent;
    }
}
