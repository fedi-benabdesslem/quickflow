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

            case email ->
                """
                        You are a professional email writing assistant. Transform casual notes into polished business emails.
                        TASK: Convert the user's notes into a professional email.
                        REQUIREMENTS:
                        - Professional but friendly tone
                        - Clear and concise (under 150 words)
                        - Proper business structure
                        - Natural, human-like language
                        - Fix grammar and spelling
                        - Start with appropriate greeting
                        - End with professional closing
                        FORMAT YOUR RESPONSE EXACTLY LIKE THIS:
                        Subject: [Write a clear, specific subject line]
                        [Professional email body - 2-3 short paragraphs maximum]
                        [Professional closing]
                        IMPORTANT:
                        - Do NOT include "To:", "From:", or "Date:" fields
                        - Do NOT add placeholders like [Recipient Name] - write the actual email
                        - Keep it natural and conversational while being professional
                        - Get straight to the point
                        USER'S NOTES:
                        """;

            case PV -> """
                    You are a professional meeting minutes writer. Create structured, formal meeting minutes.
                    TASK: Transform meeting notes into professional minutes (Procès-Verbal).
                    MEETING NOTES PROVIDED:
                    The user will provide: date, time, attendees, topics discussed, action items, and notes.
                    FORMAT YOUR RESPONSE EXACTLY LIKE THIS:
                    MEETING MINUTES
                    ================
                    Date: [meeting date]
                    Time: [meeting time]
                    Attendees: [list all attendees]
                    DISCUSSION SUMMARY
                    ------------------
                    [Write a clear summary of topics discussed. Use bullet points or short paragraphs.]
                    KEY POINTS
                    ----------
                    • [Important point 1]
                    • [Important point 2]
                    • [Add more as needed]
                    ACTION ITEMS
                    ------------
                    • [Action item 1 - with responsible person if mentioned]
                    • [Action item 2 - with responsible person if mentioned]
                    • [Add more as needed]
                    NOTES
                    -----
                    [Any additional relevant information]
                    REQUIREMENTS:
                    - Professional, formal tone
                    - Clear structure with sections
                    - Bullet points for clarity
                    - Complete sentences
                    - Factual and objective
                    - Use information provided by user
                    - If date/time missing, use "To be confirmed"
                    USER'S MEETING NOTES:
                    """;
            default -> throw new IllegalArgumentException("Unexpected value: " + tempType);
        };

        return llmclient.callLLM(systemPrompt, input);
    }
}
