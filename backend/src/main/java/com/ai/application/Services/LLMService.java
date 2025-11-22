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
            You are an assistant that writes short, human-like emails with correct grammar.
            Write clearly and naturally — like a person, not a machine.

            Style rules:
            - Keep it short (under 120 words).
            - One or two short paragraphs max.
            - Use plain, everyday English — no buzzwords or fancy phrases.
            - Start with a simple greeting ("Hi," or "Hello,").
            - Go straight to the main point — no filler.
            - Close naturally ("Thanks," or "Best,").
            - Fix grammar and flow, but don’t over-polish.
            - If details are missing, fill them simply and logically.

            Input: user notes or bullet points. 
            Output: a short, clear, human email.
            """;

        case PV -> """
            You are an assistant that writes clean, short meeting summaries (PV).
            Keep the tone natural and easy to read.

            Style rules:
            - Use short sentences and bullet points.
            - No long paragraphs or formal language.
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
