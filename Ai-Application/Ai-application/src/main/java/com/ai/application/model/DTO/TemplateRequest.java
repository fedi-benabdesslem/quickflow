package com.ai.application.model.DTO;

import com.ai.application.model.TemplateType;

import com.ai.application.model.Entity.userData;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import java.util.List;
import com.ai.application.model.*;
public class TemplateRequest {

    private TemplateType templateType;
    private List<String> bulletPoints;
    private Long userId;
    private String input;  // <-- New: For single-string input fallback
    @JsonDeserialize(using = UserDataListDeserializer.class)
    private List<userData> userData;

    public TemplateRequest() {}

    public TemplateRequest(TemplateType templateType, List<String> bulletPoints, Long userId, List<userData> userData) {
        this(templateType, bulletPoints, userId, null, userData);  // Overload for backward compat
    }

    public TemplateRequest(TemplateType templateType, List<String> bulletPoints, Long userId, String input, List<userData> userData) {
        this.templateType = templateType;
        this.bulletPoints = bulletPoints;
        this.userId = userId;
        this.input = input;
        this.userData = userData;
    }

    // Existing getters/setters...
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

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getInput() {  // <-- New getter
        return input;
    }

    public void setInput(String input) {  // <-- New setter
        this.input = input;
    }

    public List<userData> getUserData() {
        return userData;
    }

    public void setUserData(List<userData> userData) {
        this.userData = userData;
    }
}