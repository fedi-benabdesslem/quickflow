package com.ai.application.model.DTO;

public class UpdateMeetingTemplateRequest {
    private String name;
    private String description;
    private MeetingTemplateData templateData;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public MeetingTemplateData getTemplateData() {
        return templateData;
    }

    public void setTemplateData(MeetingTemplateData templateData) {
        this.templateData = templateData;
    }
}
