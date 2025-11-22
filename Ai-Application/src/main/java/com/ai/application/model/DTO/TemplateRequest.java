package com.ai.application.model.DTO;

import com.ai.application.model.TemplateType;

import java.util.List;

public class TemplateRequest {

    private TemplateType templateType;
    private List<String> bulletPoints;
    private String userId; 
    private String input;   
    private String senderId;
    private List<String> recipients;  
	private String senderEmail;

    public TemplateRequest() {}

    public TemplateRequest(TemplateType templateType, List<String> bulletPoints, String userId, String input, String senderId,
    		List<String> recipients,String senderEmail) {
        this.templateType = templateType;
        this.bulletPoints = bulletPoints;
        this.userId = userId;
        this.input = input;
        this.senderId = senderId;
        this.senderEmail =  senderEmail;
        this.recipients = recipients;

    }

    // Getters and Setters
    public TemplateType getTemplateType() {
        return templateType;
    }

    public void setTemplateType(TemplateType templateType) {
        this.templateType = templateType;
    }

    public List<String> getBulletPoints() {
        return bulletPoints;
    }

    public void setBulletPoints(List<String> bulletPoints) {
        this.bulletPoints = bulletPoints;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getInput() {
        return input;
    }

    public void setInput(String input) {
        this.input = input;
    }

    public String getSenderId() {
        return senderId;
    }

    public void setSenderId(String senderId) {
        this.senderId = senderId;
    }

    public List<String> getRecipients() {
        return recipients;
    }

    public void setRecipients(List<String> recipients) {
        this.recipients = recipients;
    }
    public void setSenderEmail(String senderEmail) {
    	this.senderEmail = senderEmail;
    }
    public String getSenderEmail() {
    	return this.senderEmail;
    }
}