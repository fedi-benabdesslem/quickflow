package com.ai.application.Services;

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

        case email -> """
            You are an assistant that writes extremely professional, short, human-like emails with correct grammar.
            Write clearly and naturally — like a person, not a machine.

            Style rules:
            -ALWAYS ALWAYS write the subject in the first line of your output
            - Keep it short (no more than 500 words).
            - If details are missing, fill them simply and logically.
            - Keep it professional

            Input: user notes or bullet points.
            Output: a professional human email.
            """;

        case PV -> """
            You are an assistant that writes clean, short meeting summaries (PV).
            Keep the tone natural and easy to read.

            Style rules:
            - Write Coherent Paragraphs
            - Elaborate on ideas
            - No Emojis
            - Cover only what matters: topics, key points, decisions, and next steps.
            - Use today’s date if none is given.
            - Write like a real person summarizing a meeting, not like a report.
            - Correct grammar and flow, but keep it minimal and simple.

            Input: notes or bullet points from the meeting.
            Output: a short, clear, human-style meeting summary.
            """;
		default -> throw new IllegalArgumentException("Unexpected value: " + tempType);
        };

        return llmclient.callLLM(systemPrompt, input);
    }
}
