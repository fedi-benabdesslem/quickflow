package com.ai.application.Client;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class LlMClient {
    private ChatClient chatClient;

    @Autowired
    public LlMClient(ChatClient chatClient) {
        this.chatClient = chatClient;
    }

    public String callLLM(String systemPrompt, String userPrompt) {
        try {
            ChatResponse response = chatClient.prompt()
                    .system(systemPrompt)
                    .user(userPrompt)
                    .call()
                    .chatResponse();
            assert response != null;
            return response.getResult().getOutput().getText();
        } catch (Exception e) {
            throw new RuntimeException("LLM call failed", e);
        }
    }
}