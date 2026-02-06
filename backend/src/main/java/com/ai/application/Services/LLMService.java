package com.ai.application.Services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.ai.application.Client.LlMClient;
import com.ai.application.model.TemplateType;
import com.ai.application.model.StructuredModeRequest;
import com.ai.application.model.ExtractedData;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

import java.util.ArrayList;

@Service
public class LLMService {
    private static final Logger logger = LoggerFactory.getLogger(LLMService.class);
    private final LlMClient llmclient;
    private final Gson gson;

    {
        // Initialize Gson with custom deserializer for ExtractedParticipant
        // This handles both string format (from AI) and object format (from frontend)
        com.google.gson.GsonBuilder gsonBuilder = new com.google.gson.GsonBuilder();
        gsonBuilder.registerTypeAdapter(
                ExtractedData.ExtractedParticipant.class,
                new com.google.gson.JsonDeserializer<ExtractedData.ExtractedParticipant>() {
                    @Override
                    public ExtractedData.ExtractedParticipant deserialize(
                            com.google.gson.JsonElement json,
                            java.lang.reflect.Type typeOfT,
                            com.google.gson.JsonDeserializationContext context) {
                        if (json.isJsonPrimitive() && json.getAsJsonPrimitive().isString()) {
                            // Handle string format: "John Doe"
                            return new ExtractedData.ExtractedParticipant(json.getAsString());
                        } else if (json.isJsonObject()) {
                            // Handle object format: {"name": "John Doe", "email": "john@example.com"}
                            com.google.gson.JsonObject obj = json.getAsJsonObject();
                            String name = obj.has("name") ? obj.get("name").getAsString() : null;
                            String email = obj.has("email") && !obj.get("email").isJsonNull()
                                    ? obj.get("email").getAsString()
                                    : null;
                            return new ExtractedData.ExtractedParticipant(name, email);
                        }
                        return new ExtractedData.ExtractedParticipant();
                    }
                });
        gson = gsonBuilder.create();
    }

    @Autowired
    public LLMService(LlMClient llmclient) {
        this.llmclient = llmclient;
    }

    /**
     * Quick Mode: Extract structured data from unstructured meeting notes.
     */
    public ExtractedData extractFromNotes(String content, String date, String time, String location) {
        String systemPrompt = """
                You are a meeting notes analyzer. Extract structured information from unstructured meeting notes.

                TASK: Parse the provided meeting notes and extract structured data.

                IMPORTANT RULES:
                1. Return ONLY a valid JSON object, no markdown, no explanation, no code blocks
                2. Extract what you can find; use null for missing fields
                3. Infer meeting title from context if not explicit
                4. Identify all participant names mentioned
                5. Separate discussion points from decisions
                6. Identify action items with owners if mentioned
                7. Set confidence to "high" if notes are clear, "medium" if some inference, "low" if unclear

                REQUIRED JSON STRUCTURE (return exactly this format):
                {
                    "meetingTitle": "string or null",
                    "date": "YYYY-MM-DD or null",
                    "time": "HH:MM AM/PM or null",
                    "location": "string or null",
                    "participants": ["name1", "name2"],
                    "discussionPoints": ["point1", "point2"],
                    "decisions": [{"statement": "decision text", "status": "Approved|Rejected|Deferred|No Decision"}],
                    "actionItems": [{"task": "task description", "owner": "person name or null", "deadline": "date or null"}],
                    "confidence": "high|medium|low"
                }

                Return ONLY the JSON object, nothing else.
                """;

        // Add date/time/location hints if provided
        StringBuilder userInput = new StringBuilder(content);
        if (date != null && !date.isEmpty()) {
            userInput.append("\n\n[User provided date: ").append(date).append("]");
        }
        if (time != null && !time.isEmpty()) {
            userInput.append("\n[User provided time: ").append(time).append("]");
        }
        if (location != null && !location.isEmpty()) {
            userInput.append("\n[User provided location: ").append(location).append("]");
        }

        String response = llmclient.callLLM(systemPrompt, userInput.toString());
        return parseExtractedData(response);
    }

    /**
     * Structured Mode: Generate professional meeting minutes from form data.
     */
    public String generateMinutes(StructuredModeRequest request) {
        String toneInstruction = getToneInstruction(request.getOutputPreferences().getTone());
        String lengthInstruction = getLengthInstruction(request.getOutputPreferences().getLength());

        String systemPrompt = String.format("""
                You are a professional meeting minutes writer. Create formal, polished meeting minutes.

                STYLE REQUIREMENTS:
                %s
                %s

                FORMAT: Generate meeting minutes with clear sections and professional formatting.
                Use proper headers, bullet points, and consistent structure.

                SECTIONS TO INCLUDE:
                1. Header with meeting title, date, time, location
                2. Attendees (present and absent if provided)
                3. Agenda items (if provided)
                4. Discussion summary
                5. Decisions made (with status)
                6. Action items (with owner and deadline)
                7. Additional notes (if provided)
                8. Footer (if classification specified)

                Write in a professional, objective tone. Use complete sentences.
                Do not add information not provided. If something is missing, omit that section.
                """, toneInstruction, lengthInstruction);

        String userInput = buildStructuredInput(request);
        return llmclient.callLLM(systemPrompt, userInput);
    }

    /**
     * Generate minutes from reviewed/edited extracted data (Quick Mode final step).
     */
    public String generateMinutesFromExtracted(ExtractedData data, String tone, String length) {
        String toneInstruction = getToneInstruction(tone);
        String lengthInstruction = getLengthInstruction(length);

        String systemPrompt = String.format("""
                You are a professional meeting minutes writer. Create formal, polished meeting minutes.

                STYLE REQUIREMENTS:
                %s
                %s

                Generate professional meeting minutes from the provided structured data.
                Use clear headers, bullet points, and professional formatting.
                """, toneInstruction, lengthInstruction);

        String userInput = buildExtractedDataInput(data);
        return llmclient.callLLM(systemPrompt, userInput);
    }

    /**
     * Voice Mode: Generate meeting minutes from a transcribed audio with speaker
     * diarization.
     * This method is specifically designed for transcripts that include speaker
     * labels and timestamps.
     */
    public String generateMinutesFromTranscript(
            String transcript,
            java.util.Map<String, String> speakerMapping,
            String meetingTitle,
            String meetingDate,
            String meetingTime,
            String meetingLocation,
            String tone,
            String length) {

        String toneInstruction = getToneInstruction(tone);
        String lengthInstruction = getLengthInstruction(length);

        String systemPrompt = String.format(
                """
                        You are a professional meeting minutes writer. Create formal, polished meeting minutes from an audio transcript.

                        STYLE REQUIREMENTS:
                        %s
                        %s

                        IMPORTANT CONTEXT:
                        - This transcript was generated from an audio recording using speech-to-text with speaker diarization
                        - Each segment includes a timestamp and speaker label
                        - Your job is to transform this raw transcript into professional meeting minutes
                        - Identify key discussion topics, decisions made, and action items
                        - Attribute statements to the correct speakers where relevant
                        - Summarize lengthy discussions while preserving key points
                        - Ignore filler words, repeated phrases, or unclear segments

                        SECTIONS TO INCLUDE:
                        1. Header with meeting title, date, time, location
                        2. Attendees (based on speakers identified)
                        3. Summary of discussions by topic
                        4. Decisions made (if any)
                        5. Action items (extract tasks mentioned with owner if stated)
                        6. Key quotes or important statements (optional)

                        Write in a professional, objective tone. Use complete sentences.
                        """,
                toneInstruction, lengthInstruction);

        // Build user input with meeting metadata and transcript
        StringBuilder userInput = new StringBuilder();

        userInput.append("MEETING INFORMATION:\n");
        if (meetingTitle != null && !meetingTitle.isEmpty()) {
            userInput.append("Title: ").append(meetingTitle).append("\n");
        } else {
            userInput.append("Title: [Meeting from Audio Recording]\n");
        }
        if (meetingDate != null)
            userInput.append("Date: ").append(meetingDate).append("\n");
        if (meetingTime != null)
            userInput.append("Time: ").append(meetingTime).append("\n");
        if (meetingLocation != null)
            userInput.append("Location: ").append(meetingLocation).append("\n");

        // Add speaker mapping if provided
        if (speakerMapping != null && !speakerMapping.isEmpty()) {
            userInput.append("\nSPEAKER IDENTIFICATION:\n");
            for (var entry : speakerMapping.entrySet()) {
                userInput.append("- ").append(entry.getKey()).append(" = ").append(entry.getValue()).append("\n");
            }
        }

        userInput.append("\n--- FULL TRANSCRIPT ---\n\n");
        userInput.append(transcript);

        logger.info("Generating minutes from transcript ({} characters)", transcript.length());
        return llmclient.callLLM(systemPrompt, userInput.toString());
    }

    /**
     * Original email/PV generation method (kept for backwards compatibility)
     */
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

    // ============ Helper Methods ============

    private ExtractedData parseExtractedData(String response) {
        // Clean response - remove markdown code blocks if present
        String cleaned = response
                .replaceAll("```json\\s*", "")
                .replaceAll("```\\s*", "")
                .trim();

        try {
            return gson.fromJson(cleaned, ExtractedData.class);
        } catch (JsonSyntaxException e) {
            logger.warn("Failed to parse LLM response as JSON: {}", e.getMessage());
            // Return a low-confidence empty result
            ExtractedData fallback = new ExtractedData();
            fallback.setConfidence("low");
            fallback.setMeetingTitle("Meeting Notes");
            fallback.setParticipants(new ArrayList<>());
            fallback.setDiscussionPoints(new ArrayList<>());
            fallback.setDecisions(new ArrayList<>());
            fallback.setActionItems(new ArrayList<>());
            return fallback;
        }
    }

    private String getToneInstruction(String tone) {
        if (tone == null)
            tone = "Formal";
        return switch (tone) {
            case "Executive" -> "TONE: Executive style - concise, high-level, focused on outcomes and decisions.";
            case "Technical" ->
                "TONE: Technical style - detailed, specific, includes methodology and technical context.";
            default -> "TONE: Formal business style - professional language, no contractions, traditional structure.";
        };
    }

    private String getLengthInstruction(String length) {
        if (length == null)
            length = "Standard";
        return switch (length) {
            case "Summary" -> "LENGTH: Brief summary - 1 page maximum, key points only.";
            case "Detailed" -> "LENGTH: Comprehensive documentation - full details, all context included.";
            default -> "LENGTH: Standard format - 2-3 pages, balanced detail and conciseness.";
        };
    }

    private String buildStructuredInput(StructuredModeRequest req) {
        StringBuilder sb = new StringBuilder();

        // Meeting Info
        if (req.getMeetingInfo() != null) {
            sb.append("MEETING INFORMATION:\n");
            sb.append("Title: ").append(req.getMeetingInfo().getTitle()).append("\n");
            sb.append("Date: ").append(req.getMeetingInfo().getDate()).append("\n");
            sb.append("Time: ").append(req.getMeetingInfo().getStartTime());
            if (req.getMeetingInfo().getEndTime() != null) {
                sb.append(" - ").append(req.getMeetingInfo().getEndTime());
            }
            sb.append("\n");
            if (req.getMeetingInfo().getLocation() != null) {
                sb.append("Location: ").append(req.getMeetingInfo().getLocation()).append("\n");
            }
            if (req.getMeetingInfo().getOrganizer() != null) {
                sb.append("Organizer: ").append(req.getMeetingInfo().getOrganizer()).append("\n");
            }
        }

        // Participants
        if (req.getParticipants() != null && !req.getParticipants().isEmpty()) {
            sb.append("\nATTENDEES PRESENT:\n");
            for (var p : req.getParticipants()) {
                sb.append("- ").append(p.getName());
                if (p.getRole() != null && !p.getRole().isEmpty()) {
                    sb.append(" (").append(p.getRole()).append(")");
                }
                sb.append("\n");
            }
        }

        if (req.getAbsentParticipants() != null && !req.getAbsentParticipants().isEmpty()) {
            sb.append("\nATTENDEES ABSENT:\n");
            for (var p : req.getAbsentParticipants()) {
                sb.append("- ").append(p.getName()).append("\n");
            }
        }

        // Agenda
        if (req.getAgenda() != null && !req.getAgenda().isEmpty()) {
            sb.append("\nAGENDA ITEMS:\n");
            for (var item : req.getAgenda()) {
                sb.append("- ").append(item.getTitle());
                if (item.getObjective() != null) {
                    sb.append(" [").append(item.getObjective()).append("]");
                }
                sb.append("\n");
                if (item.getKeyPoints() != null && !item.getKeyPoints().isEmpty()) {
                    sb.append("  Key Points: ").append(item.getKeyPoints()).append("\n");
                }
            }
        }

        // Decisions
        if (req.getDecisions() != null && !req.getDecisions().isEmpty()) {
            sb.append("\nDECISIONS:\n");
            for (var d : req.getDecisions()) {
                sb.append("- ").append(d.getStatement());
                sb.append(" [Status: ").append(d.getStatus()).append("]");
                if (d.getRationale() != null && !d.getRationale().isEmpty()) {
                    sb.append(" Rationale: ").append(d.getRationale());
                }
                sb.append("\n");
            }
        }

        // Action Items
        if (req.getActionItems() != null && !req.getActionItems().isEmpty()) {
            sb.append("\nACTION ITEMS:\n");
            for (var a : req.getActionItems()) {
                sb.append("- ").append(a.getTask());
                if (a.getOwner() != null)
                    sb.append(" | Owner: ").append(a.getOwner());
                if (a.getDeadline() != null)
                    sb.append(" | Deadline: ").append(a.getDeadline());
                if (a.getPriority() != null)
                    sb.append(" | Priority: ").append(a.getPriority());
                sb.append("\n");
            }
        }

        // Additional Notes
        if (req.getAdditionalNotes() != null && !req.getAdditionalNotes().isEmpty()) {
            sb.append("\nADDITIONAL NOTES:\n").append(req.getAdditionalNotes()).append("\n");
        }

        // Footer preference
        if (req.getOutputPreferences() != null && req.getOutputPreferences().getPdfFooter() != null
                && !"None".equals(req.getOutputPreferences().getPdfFooter())) {
            sb.append("\nFOOTER: Include \"").append(req.getOutputPreferences().getPdfFooter())
                    .append("\" at the bottom.\n");
        }

        return sb.toString();
    }

    private String buildExtractedDataInput(ExtractedData data) {
        StringBuilder sb = new StringBuilder();

        if (data.getMeetingTitle() != null) {
            sb.append("Meeting Title: ").append(data.getMeetingTitle()).append("\n");
        }
        if (data.getDate() != null) {
            sb.append("Date: ").append(data.getDate()).append("\n");
        }
        if (data.getTime() != null) {
            sb.append("Time: ").append(data.getTime()).append("\n");
        }
        if (data.getLocation() != null) {
            sb.append("Location: ").append(data.getLocation()).append("\n");
        }

        if (data.getParticipants() != null && !data.getParticipants().isEmpty()) {
            String participantNames = data.getParticipants().stream()
                    .map(p -> p.getName())
                    .filter(name -> name != null && !name.isEmpty())
                    .collect(java.util.stream.Collectors.joining(", "));
            sb.append("\nParticipants: ").append(participantNames).append("\n");
        }

        if (data.getDiscussionPoints() != null && !data.getDiscussionPoints().isEmpty()) {
            sb.append("\nDiscussion Points:\n");
            for (String point : data.getDiscussionPoints()) {
                sb.append("- ").append(point).append("\n");
            }
        }

        if (data.getDecisions() != null && !data.getDecisions().isEmpty()) {
            sb.append("\nDecisions:\n");
            for (var d : data.getDecisions()) {
                sb.append("- ").append(d.getStatement()).append(" [").append(d.getStatus()).append("]\n");
            }
        }

        if (data.getActionItems() != null && !data.getActionItems().isEmpty()) {
            sb.append("\nAction Items:\n");
            for (var a : data.getActionItems()) {
                sb.append("- ").append(a.getTask());
                if (a.getOwner() != null)
                    sb.append(" (Owner: ").append(a.getOwner()).append(")");
                if (a.getDeadline() != null)
                    sb.append(" Due: ").append(a.getDeadline());
                sb.append("\n");
            }
        }

        return sb.toString();
    }
}
