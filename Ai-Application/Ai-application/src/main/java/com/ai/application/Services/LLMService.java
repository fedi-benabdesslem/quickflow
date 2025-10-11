package com.ai.application.Services;

import org.springframework.ai.chat.client.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.ai.application.Client.LlMClient;
import com.ai.application.model.TemplateType;

@Service
public class LLMService {
    private final LlMClient llmclient;

    @Autowired
    public LLMService(LlMClient llmclient) {
        this.llmclient = llmclient;
    }

    public String generateContent(String input, TemplateType tempType) {
        String systemPrompt = switch (tempType) {
            case email -> "You are an assistant that writes professional emails.";
            case PV -> "You are an assistant that writes formal meeting summaries (PV).";
        };
        return llmclient.callLLM(systemPrompt, input);
    }
}