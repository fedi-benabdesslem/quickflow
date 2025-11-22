package com.ai.application.model.DTO;

import java.util.List;

public class EmailRequest extends TemplateRequest {

    private List<String> recipients;

    public EmailRequest() {
        super();
    }

    public List<String> getRecipients() {
        return recipients;
    }

    public void setRecipients(List<String> recipients) {
        this.recipients = recipients;
    }
}