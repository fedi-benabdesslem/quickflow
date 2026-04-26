package com.ai.application.Client;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class LlMClient {
    private ChatClient chatClient;

    @Autowired
    public LlMClient(ChatClient chatClient) {
        this.chatClient = chatClient;
    }

    /**
     * Call the LLM with default temperature (0.3).
     */
    public String callLLM(String systemPrompt, String userPrompt) {
        return callLLM(systemPrompt, userPrompt, 0.3f);
    }

    /**
     * Call the LLM with a specific temperature.
     * 
     * @param temperature 0.0 = deterministic, 1.0 = creative
     */
    public String callLLM(String systemPrompt, String userPrompt, float temperature) {
        try {
            ChatResponse response = chatClient.prompt()
                    .system(systemPrompt)
                    .user(userPrompt)
                    .options(OpenAiChatOptions.builder()
                            .temperature((double) temperature)
                            .build())
                    .call()
                    .chatResponse();
            return response.getResult().getOutput().getText();
        } catch (Exception e) {
            throw new RuntimeException("LLM call failed", e);
        }
    }
}