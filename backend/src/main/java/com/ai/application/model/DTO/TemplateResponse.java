package com.ai.application.model.DTO;

public class TemplateResponse {

    private String generatedContent;
    private String generatedSubject;

    public TemplateResponse(String generatedContent, String generatedSubject) {
        this.generatedContent = generatedContent;
        this.generatedSubject = generatedSubject;
    }
    public String getGeneratedContent() {
        return generatedContent;
    }
    public void setGeneratedContent(String generatedContent) {
        this.generatedContent = generatedContent;
    }

	public String getSubject() {
        return generatedSubject;
	}
}
