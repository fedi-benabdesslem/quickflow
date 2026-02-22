package com.ai.application.Services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.ai.application.Client.LlMClient;
import com.ai.application.model.TemplateType;
import com.ai.application.model.StructuredModeRequest;
import com.ai.application.model.ExtractedData;
import com.ai.application.model.EmailContent;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class LLMService {
    private static final Logger logger = LoggerFactory.getLogger(LLMService.class);
    private final LlMClient llmclient;
    private final Gson gson;

    // Max characters before we need to chunk a transcript
    private static final int MAX_TRANSCRIPT_LENGTH = 12000;
    // Max characters per chunk when splitting
    private static final int CHUNK_SIZE = 3000;

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
                            return new ExtractedData.ExtractedParticipant(json.getAsString());
                        } else if (json.isJsonObject()) {
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

    // ══════════════════════════════════════════════════════════════
    // EXTRACTION — Quick Mode (Returns parsed Java object)
    // ══════════════════════════════════════════════════════════════

    /**
     * Quick Mode: Extract structured data from unstructured meeting notes.
     * Returns parsed ExtractedData object from LLM JSON output.
     */
    public ExtractedData extractFromNotes(String content, String date, String time, String location) {
        String systemPrompt = """
                You are a meeting notes analyzer. Extract structured information from unstructured meeting notes.

                CONTEXT (use as fallback if not found in notes):
                - Date: %s
                - Time: %s
                - Location: %s

                RULES:
                1. Return ONLY a valid JSON object. No markdown, no backticks, no explanation, no extra text.
                2. Extract what you find. Use null for missing fields.
                3. Infer meeting title from context if not explicitly stated.
                4. Extract ALL participant names mentioned anywhere (e.g., "Ahmed suggested..." means Ahmed is a participant).
                5. Keep discussion points concise — one sentence each, maximum 10 points.
                6. Separate decisions from general discussion points.
                7. Extract action items with owner and deadline when mentioned in the notes.
                8. Output must be in the SAME language as the input notes.
                9. Confidence: "high" if notes are detailed and clear, "medium" if some inference was needed, "low" if notes are vague or very brief.
                10. Do NOT invent or assume any information not present in the notes.

                JSON FORMAT (follow exactly):
                {"meetingTitle":"string or null","date":"YYYY-MM-DD or null","time":"HH:MM AM/PM or null","location":"string or null","participants":[{"name":"person name","email":null}],"discussionPoints":["point1","point2"],"decisions":[{"statement":"text","status":"Approved|Rejected|Deferred|No Decision"}],"actionItems":[{"task":"description","owner":"name or null","deadline":"date or null"}],"confidence":"high|medium|low"}

                EXAMPLE INPUT:
                "Réunion d'équipe. Ahmed a présenté les résultats Q4, hausse de 15%%. Sarah s'inquiète du recrutement. Décision: reporter les embauches à janvier. Ahmed prépare les prévisions pour vendredi."

                EXAMPLE OUTPUT:
                {"meetingTitle":"Réunion d'équipe - Résultats Q4","date":null,"time":null,"location":null,"participants":[{"name":"Ahmed","email":null},{"name":"Sarah","email":null}],"discussionPoints":["Présentation des résultats Q4 montrant une hausse de 15%%","Préoccupations concernant le calendrier de recrutement"],"decisions":[{"statement":"Reporter les nouvelles embauches à janvier","status":"Approved"}],"actionItems":[{"task":"Préparer les prévisions mises à jour","owner":"Ahmed","deadline":"vendredi"}],"confidence":"high"}

                EXAMPLE INPUT:
                "Met with the team today. John showed the new designs, everyone liked them. We agreed to launch next Monday. Sarah will handle social media posts. Mike needs to finish the landing page by Friday."

                EXAMPLE OUTPUT:
                {"meetingTitle":"Team Meeting - New Designs Review","date":null,"time":null,"location":null,"participants":[{"name":"John","email":null},{"name":"Sarah","email":null},{"name":"Mike","email":null}],"discussionPoints":["John presented new designs","Team response was positive"],"decisions":[{"statement":"Launch scheduled for next Monday","status":"Approved"}],"actionItems":[{"task":"Handle social media posts for launch","owner":"Sarah","deadline":null},{"task":"Finish the landing page","owner":"Mike","deadline":"Friday"}],"confidence":"high"}

                Return ONLY the JSON object for the following notes.
                """
                .formatted(
                        date != null && !date.isEmpty() ? date : "Not provided",
                        time != null && !time.isEmpty() ? time : "Not provided",
                        location != null && !location.isEmpty() ? location : "Not provided");

        String response = callLLMWithRetry(systemPrompt, content, 0.1f, 1);
        return parseExtractedData(response);
    }

    // ══════════════════════════════════════════════════════════════
    // MINUTES GENERATION — All Modes (Returns markdown string)
    // ══════════════════════════════════════════════════════════════

    /**
     * Structured Mode: Generate professional meeting minutes from form data.
     * Returns markdown-formatted string.
     */
    public String generateMinutes(StructuredModeRequest request) {
        String toneInstruction = getToneInstruction(request.getOutputPreferences().getTone());
        String lengthInstruction = getLengthInstruction(request.getOutputPreferences().getLength());

        String systemPrompt = String.format(
                """
                        You are a professional meeting minutes writer.

                        STYLE:
                        %s
                        %s

                        RULES:
                        1. Write professional, objective meeting minutes.
                        2. Use ONLY the information provided. Do NOT invent, assume, or add any details not given.
                        3. If a section has no data provided, OMIT that section entirely. Do not write "None", "N/A", or "No items".
                        4. Use complete sentences. No sentence fragments.
                        5. Write in the same language as the input data. If input is in French, write entirely in French. If English, write in English.
                        6. Action items must include the responsible person and deadline exactly as provided.
                        7. Do not add commentary, opinions, recommendations, or suggestions.
                        8. Be factual and objective throughout.

                        OUTPUT FORMAT — Use EXACTLY this markdown structure:

                        # [Meeting Title]

                        **Date:** [date]
                        **Time:** [start time] - [end time]
                        **Location:** [location]
                        **Organizer:** [organizer name]

                        ## Attendees

                        **Present:**
                        - [Name] — [Role]

                        **Absent:**
                        - [Name]

                        ## Agenda

                        1. [Agenda item 1]
                        2. [Agenda item 2]

                        ## Discussion

                        ### [Agenda Item or Topic Title]
                        [Summary of discussion on this topic. Use bullet points for multiple sub-points.]

                        ## Decisions

                        | # | Decision | Status |
                        |---|----------|--------|
                        | 1 | [Decision text] | [Approved/Rejected/Deferred] |

                        ## Action Items

                        | # | Task | Responsible | Deadline | Priority |
                        |---|------|-------------|----------|----------|
                        | 1 | [Task description] | [Person] | [Date] | [High/Medium/Low] |

                        ## Additional Notes

                        [Any additional information that was provided]

                        ---
                        [Footer text if specified]

                        IMPORTANT: Use this EXACT markdown format with # headers, ## sub-headers, **bold**, tables with |, and - bullet points. Do not deviate from this structure.
                        """,
                toneInstruction, lengthInstruction);

        String userInput = buildStructuredInput(request);
        return callLLMWithRetry(systemPrompt, userInput, 0.3f, 1);
    }

    /**
     * Quick Mode Final Step: Generate minutes from reviewed/edited extracted data.
     * Returns markdown-formatted string using the same template as generateMinutes.
     */
    public String generateMinutesFromExtracted(ExtractedData data, String tone, String length) {
        String toneInstruction = getToneInstruction(tone);
        String lengthInstruction = getLengthInstruction(length);

        String systemPrompt = String.format(
                """
                        You are a professional meeting minutes writer.

                        STYLE:
                        %s
                        %s

                        RULES:
                        1. Write professional, objective meeting minutes from the structured data provided below.
                        2. Use ONLY the information provided. Do NOT invent, assume, or add any details not given.
                        3. If a section has no data, OMIT that section entirely.
                        4. Use complete sentences. Expand brief bullet points into proper prose where appropriate.
                        5. Write in the same language as the input data.
                        6. Do not add commentary, opinions, or suggestions.

                        OUTPUT FORMAT — Use EXACTLY this markdown structure:

                        # [Meeting Title]

                        **Date:** [date]
                        **Time:** [time]
                        **Location:** [location]

                        ## Attendees

                        - [Name1]
                        - [Name2]

                        ## Discussion

                        [Expand each discussion point into a clear paragraph or structured bullet list. Group related points together under sub-headings if there are many.]

                        ## Decisions

                        | # | Decision | Status |
                        |---|----------|--------|
                        | 1 | [Decision text] | [Status] |

                        ## Action Items

                        | # | Task | Responsible | Deadline |
                        |---|------|-------------|----------|
                        | 1 | [Task description] | [Person] | [Date] |

                        IMPORTANT: Use this EXACT markdown format with # headers, ## sub-headers, **bold**, tables with |, and - bullet points. Omit any section that has no data.
                        """,
                toneInstruction, lengthInstruction);

        String userInput = buildExtractedDataInput(data);
        return callLLMWithRetry(systemPrompt, userInput, 0.3f, 1);
    }

    /**
     * Voice Mode: Generate meeting minutes from a transcribed audio with speaker
     * diarization.
     * Handles long transcripts by chunking and summarizing.
     * Returns markdown-formatted string.
     */
    public String generateMinutesFromTranscript(
            String transcript,
            Map<String, String> speakerMapping,
            String meetingTitle,
            String meetingDate,
            String meetingTime,
            String meetingEndTime,
            String meetingLocation,
            String tone,
            String length) {

        String toneInstruction = getToneInstruction(tone);
        String lengthInstruction = getLengthInstruction(length);

        // Handle long transcripts — chunk and summarize first
        String processedTranscript = transcript;
        if (transcript != null && transcript.length() > MAX_TRANSCRIPT_LENGTH) {
            logger.info("Transcript too long ({} chars), chunking and summarizing...", transcript.length());
            processedTranscript = summarizeLongTranscript(transcript, speakerMapping);
            logger.info("Summarized transcript to {} chars", processedTranscript.length());
        }

        String systemPrompt = String.format(
                """
                        You are a professional meeting minutes writer. Transform an audio transcript into polished meeting minutes.

                        STYLE:
                        %s
                        %s

                        CONTEXT:
                        - This transcript was generated from speech-to-text with speaker diarization.
                        - Speaker labels (SPEAKER_00, SPEAKER_01, etc.) have been mapped to real names where provided.
                        - The transcript may contain filler words, repetitions, stutters, and unclear segments. Clean these up.

                        RULES:
                        1. SUMMARIZE discussions by topic. Do NOT reproduce the transcript word-for-word.
                        2. Group related conversation segments into coherent topics, even if they appear scattered in the transcript.
                        3. Attribute key statements, proposals, and decisions to the correct speaker.
                        4. Extract action items from conversation (e.g., "I'll send that report by Friday" becomes an action item for that speaker).
                        5. Ignore small talk, greetings, off-topic chatter, and filler words.
                        6. Write in the same language as the transcript.
                        7. Use ONLY information from the transcript. Do NOT invent topics, decisions, or action items.
                        8. If something is unclear or inaudible in the transcript, omit it rather than guess.
                        9. Identify the main topics discussed and organize minutes around those topics, not chronologically.

                        OUTPUT FORMAT — Use EXACTLY this markdown structure:

                        # [Meeting Title]

                        **Date:** [date]
                        **Time:** [start time] - [end time]
                        **Location:** [location]

                        ## Attendees

                        - [Speaker Name 1]
                        - [Speaker Name 2]

                        ## Discussion Summary

                        ### [Topic 1 — inferred from the conversation]
                        [Summary of what was discussed on this topic.]
                        - **[Speaker Name]:** [Key point or position they expressed]
                        - **[Speaker Name]:** [Their response or input]

                        ### [Topic 2 — inferred from the conversation]
                        [Summary of this topic.]

                        ## Decisions

                        | # | Decision | Proposed By | Status |
                        |---|----------|-------------|--------|
                        | 1 | [Decision extracted from conversation] | [Speaker] | [Agreed/Pending/Rejected] |

                        ## Action Items

                        | # | Task | Responsible | Deadline |
                        |---|------|-------------|----------|
                        | 1 | [Task extracted from conversation] | [Speaker who committed] | [Mentioned deadline or TBD] |

                        IMPORTANT: Use this EXACT markdown format with # headers, ## sub-headers, ### topic headers, **bold** for speaker attribution, tables with |, and - bullet points.
                        """,
                toneInstruction, lengthInstruction);

        // Build user input
        StringBuilder userInput = new StringBuilder();

        userInput.append("MEETING INFORMATION:\n");
        userInput.append("Title: ").append(
                meetingTitle != null && !meetingTitle.isEmpty() ? meetingTitle : "Meeting").append("\n");
        if (meetingDate != null && !meetingDate.isEmpty()) {
            userInput.append("Date: ").append(meetingDate).append("\n");
        }
        if (meetingTime != null && !meetingTime.isEmpty()) {
            userInput.append("Time: ").append(meetingTime);
            if (meetingEndTime != null && !meetingEndTime.isEmpty()) {
                userInput.append(" - ").append(meetingEndTime);
            }
            userInput.append("\n");
        }
        if (meetingLocation != null && !meetingLocation.isEmpty()) {
            userInput.append("Location: ").append(meetingLocation).append("\n");
        }

        // Speaker mapping
        if (speakerMapping != null && !speakerMapping.isEmpty()) {
            userInput.append("\nSPEAKER MAPPING:\n");
            for (var entry : speakerMapping.entrySet()) {
                userInput.append(entry.getKey()).append(" → ").append(entry.getValue()).append("\n");
            }
        }

        userInput.append("\n--- TRANSCRIPT ---\n\n");
        userInput.append(processedTranscript);

        logger.info("Generating minutes from transcript ({} chars processed)", processedTranscript.length());
        return callLLMWithRetry(systemPrompt, userInput.toString(), 0.3f, 1);
    }

    // ══════════════════════════════════════════════════════════════
    // EMAIL GENERATION (Returns parsed EmailContent object)
    // ══════════════════════════════════════════════════════════════

    /**
     * Generate a professional email from casual user notes.
     * Returns parsed EmailContent with separate subject and body.
     *
     * @param userNotes     The user's casual input/notes for the email
     * @param recipientName Name of the recipient (can be null)
     * @param senderName    Name of the sender (can be null)
     * @param language      Language code: "en", "fr", "ar" (can be null, defaults
     *                      to "auto")
     */
    public EmailContent generateEmail(String userNotes, String recipientName,
            String senderName, String language) {

        String langInstruction = getLanguageInstruction(language);

        String systemPrompt = """
                You are a professional email writing assistant. Transform casual notes into polished business emails.

                CONTEXT:
                - Sender: %s
                - Recipient: %s
                %s

                RULES:
                1. Return ONLY a valid JSON object. No markdown, no backticks, no explanation, no extra text before or after.
                2. Professional but warm tone. Natural, human-like language.
                3. Keep body concise — under 150 words, 2-3 short paragraphs maximum.
                4. Fix grammar and spelling from the user's notes.
                5. Do NOT invent information not present in the notes.
                6. Start with an appropriate greeting using the recipient's name if provided.
                7. End with a professional closing using the sender's name if provided.
                8. Subject line should be clear, specific, and under 10 words.
                9. Use \\n for line breaks within the body text.

                JSON FORMAT (return exactly this):
                {"subject":"Clear specific subject line","body":"Full email body with greeting, content, and closing"}

                EXAMPLE INPUT:
                "tell john about the meeting tomorrow at 3pm, need him to bring the Q4 report"

                EXAMPLE OUTPUT:
                {"subject":"Meeting Tomorrow at 3:00 PM","body":"Hi John,\\n\\nI wanted to confirm our meeting scheduled for tomorrow at 3:00 PM.\\n\\nCould you please bring the Q4 report along? It would be very helpful for our discussion.\\n\\nBest regards,\\nAhmed"}

                EXAMPLE INPUT:
                "remercier sarah pour son aide sur le projet, lui dire que la deadline est repoussée à vendredi"

                EXAMPLE OUTPUT:
                {"subject":"Merci et mise à jour du projet","body":"Bonjour Sarah,\\n\\nJe tenais à vous remercier pour votre aide précieuse sur le projet.\\n\\nPar ailleurs, je vous informe que la deadline a été repoussée à vendredi. N'hésitez pas à me contacter si vous avez des questions.\\n\\nCordialement,\\nAhmed"}

                Return ONLY the JSON object.
                """
                .formatted(
                        senderName != null && !senderName.isEmpty() ? senderName : "Not specified",
                        recipientName != null && !recipientName.isEmpty() ? recipientName : "Not specified",
                        langInstruction);

        String response = callLLMWithRetry(systemPrompt, userNotes, 0.4f, 1);
        return parseEmailContent(response);
    }

    /**
     * Generate a professional email specifically for sending meeting minutes.
     * This creates a cover email to accompany the attached PDF minutes.
     *
     * @param meetingTitle  Title of the meeting
     * @param meetingDate   Date of the meeting
     * @param attendeeNames List of attendee names
     * @param senderName    Name of the person sending
     * @param language      Language code: "en", "fr", "ar"
     */
    public EmailContent generateMinutesEmail(String meetingTitle, String meetingDate,
            List<String> attendeeNames, String senderName,
            String language) {

        String langInstruction = getLanguageInstruction(language);

        String systemPrompt = """
                You are a professional email writing assistant. Write a brief cover email to accompany meeting minutes being sent as a PDF attachment.

                %s

                RULES:
                1. Return ONLY a valid JSON object. No markdown, no backticks, no explanation.
                2. Keep it very brief — 2-3 sentences maximum for the body.
                3. Mention that the meeting minutes are attached as a PDF.
                4. Reference the meeting title and date.
                5. Invite recipients to reach out with corrections or additions.
                6. Professional but friendly tone.
                7. Use \\n for line breaks.

                JSON FORMAT:
                {"subject":"Clear subject referencing the meeting","body":"Brief cover email body"}

                EXAMPLE OUTPUT:
                {"subject":"Meeting Minutes — Q4 Review (Jan 15, 2025)","body":"Hi everyone,\\n\\nPlease find attached the minutes from our Q4 Review meeting held on January 15, 2025.\\n\\nKindly review and let me know if you have any corrections or additions.\\n\\nBest regards,\\nAhmed"}

                Return ONLY the JSON object.
                """
                .formatted(langInstruction);

        String userInput = String.format(
                "Meeting: %s\nDate: %s\nAttendees: %s\nSender: %s",
                meetingTitle != null ? meetingTitle : "Team Meeting",
                meetingDate != null ? meetingDate : "Recent",
                attendeeNames != null ? String.join(", ", attendeeNames) : "Team members",
                senderName != null ? senderName : "The organizer");

        String response = callLLMWithRetry(systemPrompt, userInput, 0.4f, 1);
        return parseEmailContent(response);
    }

    // ══════════════════════════════════════════════════════════════
    // BACKWARD COMPATIBILITY — Wrapper for old generateContent calls
    // Used by TemplateService.processTemplate() — do not remove
    // ══════════════════════════════════════════════════════════════

    /**
     * Original email/PV generation method (kept for backward compatibility).
     * Used by TemplateService — do not remove until TemplateService is migrated.
     */
    public String generateContent(String input, TemplateType tempType) {
        return switch (tempType) {
            case email -> {
                EmailContent email = generateEmail(input, null, null, null);
                yield "Subject: " + email.getSubject() + "\n\n" + email.getBody();
            }
            case PV -> {
                // Create a minimal ExtractedData from the raw input
                ExtractedData data = extractFromNotes(input, null, null, null);
                yield generateMinutesFromExtracted(data, "Formal", "Standard");
            }
            default -> throw new IllegalArgumentException("Unexpected template type: " + tempType);
        };
    }

    // ══════════════════════════════════════════════════════════════
    // LLM CALL WITH RETRY
    // ══════════════════════════════════════════════════════════════

    /**
     * Call the LLM with retry logic for handling invalid outputs.
     *
     * @param systemPrompt The system prompt
     * @param userInput    The user input
     * @param temperature  0.0 = deterministic, 1.0 = creative
     * @param maxRetries   Number of retries on invalid output (0 = no retry)
     */
    private String callLLMWithRetry(String systemPrompt, String userInput,
            float temperature, int maxRetries) {
        Exception lastException = null;

        for (int attempt = 0; attempt <= maxRetries; attempt++) {
            try {
                String response = llmclient.callLLM(systemPrompt, userInput, temperature);

                // Basic validation — response should not be empty or just whitespace
                if (response == null || response.trim().isEmpty()) {
                    logger.warn("LLM returned empty response (attempt {}/{})",
                            attempt + 1, maxRetries + 1);
                    continue;
                }

                return response.trim();

            } catch (Exception e) {
                lastException = e;
                logger.warn("LLM call failed (attempt {}/{}): {}",
                        attempt + 1, maxRetries + 1, e.getMessage());

                if (attempt < maxRetries) {
                    try {
                        // Brief pause before retry
                        Thread.sleep(1000);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException("LLM retry interrupted", ie);
                    }
                }
            }
        }

        // All attempts failed
        String errorMsg = "LLM call failed after " + (maxRetries + 1) + " attempts";
        logger.error(errorMsg, lastException);
        throw new RuntimeException(errorMsg, lastException);
    }

    // ══════════════════════════════════════════════════════════════
    // LONG TRANSCRIPT HANDLING
    // ══════════════════════════════════════════════════════════════

    /**
     * Summarize a long transcript by chunking it and summarizing each chunk.
     * Used when transcript exceeds MAX_TRANSCRIPT_LENGTH.
     */
    private String summarizeLongTranscript(String transcript, Map<String, String> speakerMapping) {
        List<String> chunks = splitIntoChunks(transcript, CHUNK_SIZE);
        logger.info("Split transcript into {} chunks for summarization", chunks.size());

        // Build speaker context for the summarizer
        String speakerContext = "";
        if (speakerMapping != null && !speakerMapping.isEmpty()) {
            StringBuilder sb = new StringBuilder("Speaker names: ");
            for (var entry : speakerMapping.entrySet()) {
                sb.append(entry.getKey()).append("=").append(entry.getValue()).append(", ");
            }
            speakerContext = sb.toString();
        }

        String chunkSummaryPrompt = """
                You are a meeting transcript summarizer. Summarize this segment of a meeting transcript.

                %s

                RULES:
                1. Keep ALL speaker names and who said what.
                2. Keep ALL decisions made and action items mentioned.
                3. Keep key discussion points and important statements.
                4. Remove filler words, repetitions, small talk, and greetings.
                5. Condense to approximately 30%% of the original length.
                6. Maintain the same language as the transcript.
                7. Preserve chronological order within this segment.
                8. Do NOT add any information not in the transcript.

                Summarize this transcript segment:
                """.formatted(speakerContext);

        StringBuilder summarized = new StringBuilder();
        for (int i = 0; i < chunks.size(); i++) {
            logger.info("Summarizing chunk {}/{}", i + 1, chunks.size());

            String chunkInput = "--- Segment " + (i + 1) + " of " + chunks.size() + " ---\n\n" + chunks.get(i);

            try {
                String summary = callLLMWithRetry(chunkSummaryPrompt, chunkInput, 0.2f, 1);
                summarized.append("--- Part ").append(i + 1).append(" ---\n");
                summarized.append(summary).append("\n\n");
            } catch (Exception e) {
                logger.warn("Failed to summarize chunk {}, using original text", i + 1);
                // Fallback: use a truncated version of the original chunk
                summarized.append("--- Part ").append(i + 1).append(" ---\n");
                summarized.append(chunks.get(i), 0, Math.min(chunks.get(i).length(), 1000));
                summarized.append("\n\n");
            }
        }

        return summarized.toString();
    }

    /**
     * Split text into chunks of approximately the given size,
     * breaking at sentence or paragraph boundaries when possible.
     */
    private List<String> splitIntoChunks(String text, int chunkSize) {
        List<String> chunks = new ArrayList<>();

        if (text.length() <= chunkSize) {
            chunks.add(text);
            return chunks;
        }

        int start = 0;
        while (start < text.length()) {
            int end = Math.min(start + chunkSize, text.length());

            // If we're not at the end, try to break at a good boundary
            if (end < text.length()) {
                // Try to find a paragraph break (double newline)
                int paragraphBreak = text.lastIndexOf("\n\n", end);
                if (paragraphBreak > start && paragraphBreak > end - 500) {
                    end = paragraphBreak + 2;
                } else {
                    // Try to find a sentence break (period followed by space or newline)
                    int sentenceBreak = -1;
                    for (int i = end; i > Math.max(start, end - 500); i--) {
                        if (i < text.length() - 1 && text.charAt(i) == '.'
                                && (text.charAt(i + 1) == ' ' || text.charAt(i + 1) == '\n')) {
                            sentenceBreak = i + 1;
                            break;
                        }
                    }
                    if (sentenceBreak > start) {
                        end = sentenceBreak;
                    } else {
                        // Try a newline
                        int lineBreak = text.lastIndexOf("\n", end);
                        if (lineBreak > start && lineBreak > end - 300) {
                            end = lineBreak + 1;
                        }
                        // Otherwise just cut at chunkSize
                    }
                }
            }

            chunks.add(text.substring(start, end).trim());
            start = end;
        }

        return chunks;
    }

    // ══════════════════════════════════════════════════════════════
    // PARSING HELPERS
    // ══════════════════════════════════════════════════════════════

    /**
     * Parse LLM response as ExtractedData JSON.
     * Handles common LLM output issues (markdown code blocks, extra text).
     */
    private ExtractedData parseExtractedData(String response) {
        String cleaned = cleanJsonResponse(response);

        try {
            ExtractedData data = gson.fromJson(cleaned, ExtractedData.class);

            // Validate required fields have defaults
            if (data.getParticipants() == null)
                data.setParticipants(new ArrayList<>());
            if (data.getDiscussionPoints() == null)
                data.setDiscussionPoints(new ArrayList<>());
            if (data.getDecisions() == null)
                data.setDecisions(new ArrayList<>());
            if (data.getActionItems() == null)
                data.setActionItems(new ArrayList<>());
            if (data.getConfidence() == null)
                data.setConfidence("medium");

            return data;
        } catch (JsonSyntaxException e) {
            logger.warn("Failed to parse LLM response as ExtractedData JSON: {}", e.getMessage());
            logger.debug("Raw response was: {}", response);

            // Return low-confidence empty result
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

    /**
     * Parse LLM response as EmailContent JSON.
     * Falls back to text parsing if JSON parsing fails.
     */
    private EmailContent parseEmailContent(String response) {
        String cleaned = cleanJsonResponse(response);

        try {
            EmailContent email = gson.fromJson(cleaned, EmailContent.class);

            // Validate
            if (email.getSubject() == null || email.getSubject().isEmpty()) {
                email.setSubject("Follow-up");
            }
            if (email.getBody() == null || email.getBody().isEmpty()) {
                email.setBody(cleaned);
            }

            // Unescape \\n to actual newlines in body
            email.setBody(email.getBody().replace("\\n", "\n"));

            return email;
        } catch (JsonSyntaxException e) {
            logger.warn("Failed to parse email JSON, falling back to text parsing: {}", e.getMessage());

            // Fallback: try to extract subject from "Subject: ..." format
            EmailContent fallback = new EmailContent();

            if (cleaned.toLowerCase().startsWith("subject:")) {
                int firstNewline = cleaned.indexOf("\n");
                if (firstNewline > 0) {
                    fallback.setSubject(cleaned.substring(8, firstNewline).trim());
                    fallback.setBody(cleaned.substring(firstNewline).trim());
                } else {
                    fallback.setSubject("Follow-up");
                    fallback.setBody(cleaned);
                }
            } else {
                fallback.setSubject("Follow-up");
                fallback.setBody(cleaned);
            }

            return fallback;
        }
    }

    /**
     * Clean LLM response — remove markdown code blocks, extra whitespace, etc.
     */
    private String cleanJsonResponse(String response) {
        if (response == null)
            return "{}";

        String cleaned = response.trim();

        // Remove markdown code blocks
        cleaned = cleaned.replaceAll("```json\\s*", "");
        cleaned = cleaned.replaceAll("```\\s*", "");

        // Remove any text before the first { or [
        int jsonStart = -1;
        for (int i = 0; i < cleaned.length(); i++) {
            if (cleaned.charAt(i) == '{' || cleaned.charAt(i) == '[') {
                jsonStart = i;
                break;
            }
        }
        if (jsonStart > 0) {
            cleaned = cleaned.substring(jsonStart);
        }

        // Remove any text after the last } or ]
        int jsonEnd = -1;
        for (int i = cleaned.length() - 1; i >= 0; i--) {
            if (cleaned.charAt(i) == '}' || cleaned.charAt(i) == ']') {
                jsonEnd = i;
                break;
            }
        }
        if (jsonEnd > 0 && jsonEnd < cleaned.length() - 1) {
            cleaned = cleaned.substring(0, jsonEnd + 1);
        }

        return cleaned.trim();
    }

    // ══════════════════════════════════════════════════════════════
    // CONFIGURATION HELPERS
    // ══════════════════════════════════════════════════════════════

    private String getToneInstruction(String tone) {
        if (tone == null)
            tone = "Formal";
        return switch (tone) {
            case "Executive" ->
                "TONE: Executive style — concise, high-level, focused on outcomes, decisions, and impact. Minimal detail on discussions.";
            case "Technical" ->
                "TONE: Technical style — detailed, specific, includes methodology, technical context, and precise terminology.";
            case "Casual" ->
                "TONE: Casual professional style — friendly, conversational, but still organized and clear.";
            default ->
                "TONE: Formal business style — professional language, no contractions, objective, traditional corporate structure.";
        };
    }

    private String getLengthInstruction(String length) {
        if (length == null)
            length = "Standard";
        return switch (length) {
            case "Summary" ->
                "LENGTH: Brief summary — 1 page maximum. Only key decisions, action items, and critical discussion points.";
            case "Detailed" ->
                "LENGTH: Comprehensive documentation — full details, all context included, complete discussion coverage, thorough action items.";
            default ->
                "LENGTH: Standard format — 2-3 pages, balanced between detail and conciseness. Cover main points without excessive detail.";
        };
    }

    private String getLanguageInstruction(String language) {
        if (language == null)
            language = "auto";
        return switch (language) {
            case "fr" -> "LANGUAGE: Write the email entirely in FRENCH.";
            case "ar" -> "LANGUAGE: Write the email entirely in ARABIC.";
            case "en" -> "LANGUAGE: Write the email entirely in ENGLISH.";
            default ->
                "LANGUAGE: Write the email in the SAME language as the user's notes. If notes are in French, write in French. If in English, write in English. If in Arabic, write in Arabic.";
        };
    }

    // ══════════════════════════════════════════════════════════════
    // INPUT BUILDERS
    // ══════════════════════════════════════════════════════════════

    /**
     * Build structured text input from StructuredModeRequest for the LLM.
     */
    private String buildStructuredInput(StructuredModeRequest req) {
        StringBuilder sb = new StringBuilder();

        // Meeting Info
        if (req.getMeetingInfo() != null) {
            sb.append("MEETING INFORMATION:\n");
            if (req.getMeetingInfo().getTitle() != null) {
                sb.append("Title: ").append(req.getMeetingInfo().getTitle()).append("\n");
            }
            if (req.getMeetingInfo().getDate() != null) {
                sb.append("Date: ").append(req.getMeetingInfo().getDate()).append("\n");
            }
            if (req.getMeetingInfo().getStartTime() != null) {
                sb.append("Time: ").append(req.getMeetingInfo().getStartTime());
                if (req.getMeetingInfo().getEndTime() != null && !req.getMeetingInfo().getEndTime().isEmpty()) {
                    sb.append(" - ").append(req.getMeetingInfo().getEndTime());
                }
                sb.append("\n");
            }
            if (req.getMeetingInfo().getLocation() != null && !req.getMeetingInfo().getLocation().isEmpty()) {
                sb.append("Location: ").append(req.getMeetingInfo().getLocation()).append("\n");
            }
            if (req.getMeetingInfo().getOrganizer() != null && !req.getMeetingInfo().getOrganizer().isEmpty()) {
                sb.append("Organizer: ").append(req.getMeetingInfo().getOrganizer()).append("\n");
            }
        }

        // Present Participants
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

        // Absent Participants
        if (req.getAbsentParticipants() != null && !req.getAbsentParticipants().isEmpty()) {
            sb.append("\nATTENDEES ABSENT:\n");
            for (var p : req.getAbsentParticipants()) {
                sb.append("- ").append(p.getName()).append("\n");
            }
        }

        // Agenda
        if (req.getAgenda() != null && !req.getAgenda().isEmpty()) {
            sb.append("\nAGENDA ITEMS:\n");
            int i = 1;
            for (var item : req.getAgenda()) {
                sb.append(i++).append(". ").append(item.getTitle());
                if (item.getObjective() != null && !item.getObjective().isEmpty()) {
                    sb.append(" — Objective: ").append(item.getObjective());
                }
                sb.append("\n");
                if (item.getKeyPoints() != null && !item.getKeyPoints().isEmpty()) {
                    sb.append("   Discussion: ").append(item.getKeyPoints()).append("\n");
                }
            }
        }

        // Decisions
        if (req.getDecisions() != null && !req.getDecisions().isEmpty()) {
            sb.append("\nDECISIONS:\n");
            for (var d : req.getDecisions()) {
                sb.append("- ").append(d.getStatement());
                if (d.getStatus() != null) {
                    sb.append(" [Status: ").append(d.getStatus()).append("]");
                }
                if (d.getRationale() != null && !d.getRationale().isEmpty()) {
                    sb.append(" — Rationale: ").append(d.getRationale());
                }
                sb.append("\n");
            }
        }

        // Action Items
        if (req.getActionItems() != null && !req.getActionItems().isEmpty()) {
            sb.append("\nACTION ITEMS:\n");
            for (var a : req.getActionItems()) {
                sb.append("- ").append(a.getTask());
                if (a.getOwner() != null && !a.getOwner().isEmpty()) {
                    sb.append(" | Responsible: ").append(a.getOwner());
                }
                if (a.getDeadline() != null && !a.getDeadline().isEmpty()) {
                    sb.append(" | Deadline: ").append(a.getDeadline());
                }
                if (a.getPriority() != null && !a.getPriority().isEmpty()) {
                    sb.append(" | Priority: ").append(a.getPriority());
                }
                sb.append("\n");
            }
        }

        // Additional Notes
        if (req.getAdditionalNotes() != null && !req.getAdditionalNotes().isEmpty()) {
            sb.append("\nADDITIONAL NOTES:\n").append(req.getAdditionalNotes()).append("\n");
        }

        // Footer preference
        if (req.getOutputPreferences() != null
                && req.getOutputPreferences().getPdfFooter() != null
                && !req.getOutputPreferences().getPdfFooter().isEmpty()
                && !"None".equals(req.getOutputPreferences().getPdfFooter())) {
            sb.append("\nFOOTER TEXT: ").append(req.getOutputPreferences().getPdfFooter()).append("\n");
        }

        return sb.toString();
    }

    /**
     * Build text input from ExtractedData for minutes generation.
     */
    private String buildExtractedDataInput(ExtractedData data) {
        StringBuilder sb = new StringBuilder();

        sb.append("MEETING INFORMATION:\n");
        if (data.getMeetingTitle() != null && !data.getMeetingTitle().isEmpty()) {
            sb.append("Title: ").append(data.getMeetingTitle()).append("\n");
        }
        if (data.getDate() != null && !data.getDate().isEmpty()) {
            sb.append("Date: ").append(data.getDate()).append("\n");
        }
        if (data.getTime() != null && !data.getTime().isEmpty()) {
            sb.append("Time: ").append(data.getTime()).append("\n");
        }
        if (data.getLocation() != null && !data.getLocation().isEmpty()) {
            sb.append("Location: ").append(data.getLocation()).append("\n");
        }

        if (data.getParticipants() != null && !data.getParticipants().isEmpty()) {
            sb.append("\nATTENDEES:\n");
            for (var p : data.getParticipants()) {
                if (p.getName() != null && !p.getName().isEmpty()) {
                    sb.append("- ").append(p.getName());
                    if (p.getEmail() != null && !p.getEmail().isEmpty()) {
                        sb.append(" (").append(p.getEmail()).append(")");
                    }
                    sb.append("\n");
                }
            }
        }

        if (data.getDiscussionPoints() != null && !data.getDiscussionPoints().isEmpty()) {
            sb.append("\nDISCUSSION POINTS:\n");
            for (String point : data.getDiscussionPoints()) {
                sb.append("- ").append(point).append("\n");
            }
        }

        if (data.getDecisions() != null && !data.getDecisions().isEmpty()) {
            sb.append("\nDECISIONS:\n");
            for (var d : data.getDecisions()) {
                sb.append("- ").append(d.getStatement());
                if (d.getStatus() != null) {
                    sb.append(" [").append(d.getStatus()).append("]");
                }
                sb.append("\n");
            }
        }

        if (data.getActionItems() != null && !data.getActionItems().isEmpty()) {
            sb.append("\nACTION ITEMS:\n");
            for (var a : data.getActionItems()) {
                sb.append("- ").append(a.getTask());
                if (a.getOwner() != null && !a.getOwner().isEmpty()) {
                    sb.append(" | Responsible: ").append(a.getOwner());
                }
                if (a.getDeadline() != null && !a.getDeadline().isEmpty()) {
                    sb.append(" | Deadline: ").append(a.getDeadline());
                }
                sb.append("\n");
            }
        }

        return sb.toString();
    }
}
