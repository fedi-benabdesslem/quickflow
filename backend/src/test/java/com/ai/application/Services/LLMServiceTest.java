package com.ai.application.Services;

import com.ai.application.Client.LlMClient;
import com.ai.application.model.ExtractedData;
import com.ai.application.model.StructuredModeRequest;
import com.ai.application.model.TemplateType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Unit tests for LLMService.
 * 
 * Tests cover:
 * - Tone instruction generation
 * - Length instruction generation
 * - ExtractedData parsing from JSON
 * - Structured input building
 * - Quick mode extraction
 * 
 * Not covered:
 * - Actual LLM integration (mocked)
 * - LLM prompt quality (requires human evaluation)
 */
@ExtendWith(MockitoExtension.class)
class LLMServiceTest {

    @Mock
    private LlMClient llmClient;

    private LLMService llmService;

    @BeforeEach
    void setUp() {
        llmService = new LLMService(llmClient);
    }

    @Nested
    @DisplayName("extractFromNotes() - Quick Mode")
    class ExtractFromNotesTests {

        @Test
        @DisplayName("Should parse valid JSON response")
        void parseValidJsonResponse() {
            String validJson = """
                {
                    "meetingTitle": "Sprint Planning",
                    "date": "2024-01-15",
                    "time": "10:00 AM",
                    "participants": ["John", "Jane"],
                    "discussionPoints": ["Point 1", "Point 2"],
                    "decisions": [{"statement": "Decision made", "status": "Approved"}],
                    "actionItems": [{"task": "Complete report", "owner": "John", "deadline": "2024-01-20"}],
                    "confidence": "high"
                }
                """;
            
            when(llmClient.callLLM(anyString(), anyString())).thenReturn(validJson);

            ExtractedData result = llmService.extractFromNotes("Meeting notes content", null, null);

            assertNotNull(result);
            assertEquals("Sprint Planning", result.getMeetingTitle());
            assertEquals("2024-01-15", result.getDate());
            assertEquals("10:00 AM", result.getTime());
            assertEquals(2, result.getParticipants().size());
            assertEquals("high", result.getConfidence());
        }

        @Test
        @DisplayName("Should handle JSON wrapped in markdown code blocks")
        void handleMarkdownCodeBlocks() {
            String wrappedJson = """
                ```json
                {
                    "meetingTitle": "Test Meeting",
                    "confidence": "medium"
                }
                ```
                """;
            
            when(llmClient.callLLM(anyString(), anyString())).thenReturn(wrappedJson);

            ExtractedData result = llmService.extractFromNotes("notes", null, null);

            assertNotNull(result);
            assertEquals("Test Meeting", result.getMeetingTitle());
            assertEquals("medium", result.getConfidence());
        }

        @Test
        @DisplayName("Should return fallback on invalid JSON")
        void returnFallbackOnInvalidJson() {
            when(llmClient.callLLM(anyString(), anyString()))
                .thenReturn("This is not valid JSON at all");

            ExtractedData result = llmService.extractFromNotes("notes", null, null);

            assertNotNull(result);
            assertEquals("low", result.getConfidence());
            assertEquals("Meeting Notes", result.getMeetingTitle());
            assertNotNull(result.getParticipants());
            assertTrue(result.getParticipants().isEmpty());
        }

        @Test
        @DisplayName("Should append date hint when provided")
        void appendDateHint() {
            when(llmClient.callLLM(anyString(), anyString()))
                .thenReturn("{\"meetingTitle\": \"Test\", \"confidence\": \"high\"}");

            llmService.extractFromNotes("notes", "2024-02-01", null);

            verify(llmClient).callLLM(anyString(), contains("[User provided date: 2024-02-01]"));
        }

        @Test
        @DisplayName("Should append time hint when provided")
        void appendTimeHint() {
            when(llmClient.callLLM(anyString(), anyString()))
                .thenReturn("{\"meetingTitle\": \"Test\", \"confidence\": \"high\"}");

            llmService.extractFromNotes("notes", null, "14:30");

            verify(llmClient).callLLM(anyString(), contains("[User provided time: 14:30]"));
        }

        @Test
        @DisplayName("Should append both date and time hints")
        void appendBothHints() {
            when(llmClient.callLLM(anyString(), anyString()))
                .thenReturn("{\"meetingTitle\": \"Test\", \"confidence\": \"high\"}");

            llmService.extractFromNotes("notes", "2024-02-01", "14:30");

            verify(llmClient).callLLM(anyString(), argThat(prompt ->
                prompt.contains("[User provided date: 2024-02-01]") &&
                prompt.contains("[User provided time: 14:30]")
            ));
        }
    }

    @Nested
    @DisplayName("generateMinutes() - Structured Mode")
    class GenerateMinutesTests {

        @Test
        @DisplayName("Should call LLM with structured input")
        void callLlmWithStructuredInput() {
            StructuredModeRequest request = createMinimalStructuredRequest();
            
            when(llmClient.callLLM(anyString(), anyString())).thenReturn("Generated minutes");

            String result = llmService.generateMinutes(request);

            assertNotNull(result);
            assertEquals("Generated minutes", result);
            verify(llmClient).callLLM(anyString(), anyString());
        }

        @Test
        @DisplayName("Should include meeting info in prompt")
        void includeMeetingInfoInPrompt() {
            StructuredModeRequest request = createMinimalStructuredRequest();
            StructuredModeRequest.MeetingInfo meetingInfo = new StructuredModeRequest.MeetingInfo();
            meetingInfo.setTitle("Project Kickoff");
            meetingInfo.setDate("2024-03-01");
            meetingInfo.setStartTime("09:00");
            meetingInfo.setEndTime("10:30");
            meetingInfo.setLocation("Main Office");
            meetingInfo.setOrganizer("Project Manager");
            request.setMeetingInfo(meetingInfo);
            
            when(llmClient.callLLM(anyString(), anyString())).thenReturn("minutes");

            llmService.generateMinutes(request);

            verify(llmClient).callLLM(anyString(), argThat(userInput ->
                userInput.contains("Project Kickoff") &&
                userInput.contains("2024-03-01") &&
                userInput.contains("09:00") &&
                userInput.contains("10:30") &&
                userInput.contains("Main Office") &&
                userInput.contains("Project Manager")
            ));
        }

        @Test
        @DisplayName("Should include participants in prompt")
        void includeParticipantsInPrompt() {
            StructuredModeRequest request = createMinimalStructuredRequest();
            
            StructuredModeRequest.Participant p1 = new StructuredModeRequest.Participant();
            p1.setName("Alice");
            p1.setRole("Developer");
            
            StructuredModeRequest.Participant p2 = new StructuredModeRequest.Participant();
            p2.setName("Bob");
            
            request.setParticipants(Arrays.asList(p1, p2));
            
            when(llmClient.callLLM(anyString(), anyString())).thenReturn("minutes");

            llmService.generateMinutes(request);

            verify(llmClient).callLLM(anyString(), argThat(userInput ->
                userInput.contains("Alice") &&
                userInput.contains("Developer") &&
                userInput.contains("Bob")
            ));
        }

        @Test
        @DisplayName("Should include absent participants in prompt")
        void includeAbsentParticipantsInPrompt() {
            StructuredModeRequest request = createMinimalStructuredRequest();
            
            StructuredModeRequest.Participant absent = new StructuredModeRequest.Participant();
            absent.setName("Charlie");
            
            request.setAbsentParticipants(Arrays.asList(absent));
            
            when(llmClient.callLLM(anyString(), anyString())).thenReturn("minutes");

            llmService.generateMinutes(request);

            verify(llmClient).callLLM(anyString(), argThat(userInput ->
                userInput.contains("ATTENDEES ABSENT") &&
                userInput.contains("Charlie")
            ));
        }

        @Test
        @DisplayName("Should include agenda items in prompt")
        void includeAgendaInPrompt() {
            StructuredModeRequest request = createMinimalStructuredRequest();
            
            StructuredModeRequest.AgendaItem item = new StructuredModeRequest.AgendaItem();
            item.setTitle("Review Q1 Goals");
            item.setObjective("Information");
            item.setKeyPoints("Key metrics to discuss");
            
            request.setAgenda(Arrays.asList(item));
            
            when(llmClient.callLLM(anyString(), anyString())).thenReturn("minutes");

            llmService.generateMinutes(request);

            verify(llmClient).callLLM(anyString(), argThat(userInput ->
                userInput.contains("Review Q1 Goals") &&
                userInput.contains("Information") &&
                userInput.contains("Key metrics")
            ));
        }

        @Test
        @DisplayName("Should include decisions in prompt")
        void includeDecisionsInPrompt() {
            StructuredModeRequest request = createMinimalStructuredRequest();
            
            StructuredModeRequest.Decision decision = new StructuredModeRequest.Decision();
            decision.setStatement("Approved new budget");
            decision.setStatus("Approved");
            decision.setRationale("Within financial limits");
            
            request.setDecisions(Arrays.asList(decision));
            
            when(llmClient.callLLM(anyString(), anyString())).thenReturn("minutes");

            llmService.generateMinutes(request);

            verify(llmClient).callLLM(anyString(), argThat(userInput ->
                userInput.contains("Approved new budget") &&
                userInput.contains("[Status: Approved]") &&
                userInput.contains("Within financial limits")
            ));
        }

        @Test
        @DisplayName("Should include action items in prompt")
        void includeActionItemsInPrompt() {
            StructuredModeRequest request = createMinimalStructuredRequest();
            
            StructuredModeRequest.ActionItem action = new StructuredModeRequest.ActionItem();
            action.setTask("Complete documentation");
            action.setOwner("Alice");
            action.setDeadline("2024-03-15");
            action.setPriority("High");
            
            request.setActionItems(Arrays.asList(action));
            
            when(llmClient.callLLM(anyString(), anyString())).thenReturn("minutes");

            llmService.generateMinutes(request);

            verify(llmClient).callLLM(anyString(), argThat(userInput ->
                userInput.contains("Complete documentation") &&
                userInput.contains("Owner: Alice") &&
                userInput.contains("Deadline: 2024-03-15") &&
                userInput.contains("Priority: High")
            ));
        }

        @Test
        @DisplayName("Should include additional notes in prompt")
        void includeAdditionalNotesInPrompt() {
            StructuredModeRequest request = createMinimalStructuredRequest();
            request.setAdditionalNotes("Follow-up meeting scheduled for next week");
            
            when(llmClient.callLLM(anyString(), anyString())).thenReturn("minutes");

            llmService.generateMinutes(request);

            verify(llmClient).callLLM(anyString(), argThat(userInput ->
                userInput.contains("Follow-up meeting scheduled")
            ));
        }

        @Test
        @DisplayName("Should include footer preference when not None")
        void includeFooterPreference() {
            StructuredModeRequest request = createMinimalStructuredRequest();
            StructuredModeRequest.OutputPreferences prefs = new StructuredModeRequest.OutputPreferences();
            prefs.setTone("Formal");
            prefs.setLength("Standard");
            prefs.setPdfFooter("CONFIDENTIAL");
            request.setOutputPreferences(prefs);
            
            when(llmClient.callLLM(anyString(), anyString())).thenReturn("minutes");

            llmService.generateMinutes(request);

            verify(llmClient).callLLM(anyString(), argThat(userInput ->
                userInput.contains("CONFIDENTIAL")
            ));
        }
    }

    @Nested
    @DisplayName("Tone instruction generation")
    class ToneInstructionTests {

        @Test
        @DisplayName("Should generate Executive tone instruction")
        void generateExecutiveTone() {
            StructuredModeRequest request = createRequestWithTone("Executive");
            when(llmClient.callLLM(anyString(), anyString())).thenReturn("output");

            llmService.generateMinutes(request);

            verify(llmClient).callLLM(argThat(systemPrompt ->
                systemPrompt.contains("Executive style")
            ), anyString());
        }

        @Test
        @DisplayName("Should generate Technical tone instruction")
        void generateTechnicalTone() {
            StructuredModeRequest request = createRequestWithTone("Technical");
            when(llmClient.callLLM(anyString(), anyString())).thenReturn("output");

            llmService.generateMinutes(request);

            verify(llmClient).callLLM(argThat(systemPrompt ->
                systemPrompt.contains("Technical style")
            ), anyString());
        }

        @Test
        @DisplayName("Should default to Formal tone")
        void defaultToFormalTone() {
            StructuredModeRequest request = createRequestWithTone("Unknown");
            when(llmClient.callLLM(anyString(), anyString())).thenReturn("output");

            llmService.generateMinutes(request);

            verify(llmClient).callLLM(argThat(systemPrompt ->
                systemPrompt.contains("Formal business style")
            ), anyString());
        }

        @Test
        @DisplayName("Should default to Formal when tone is null")
        void defaultToFormalWhenNull() {
            StructuredModeRequest request = createRequestWithTone(null);
            when(llmClient.callLLM(anyString(), anyString())).thenReturn("output");

            llmService.generateMinutes(request);

            verify(llmClient).callLLM(argThat(systemPrompt ->
                systemPrompt.contains("Formal business style")
            ), anyString());
        }
    }

    @Nested
    @DisplayName("Length instruction generation")
    class LengthInstructionTests {

        @Test
        @DisplayName("Should generate Summary length instruction")
        void generateSummaryLength() {
            StructuredModeRequest request = createRequestWithLength("Summary");
            when(llmClient.callLLM(anyString(), anyString())).thenReturn("output");

            llmService.generateMinutes(request);

            verify(llmClient).callLLM(argThat(systemPrompt ->
                systemPrompt.contains("Brief summary")
            ), anyString());
        }

        @Test
        @DisplayName("Should generate Detailed length instruction")
        void generateDetailedLength() {
            StructuredModeRequest request = createRequestWithLength("Detailed");
            when(llmClient.callLLM(anyString(), anyString())).thenReturn("output");

            llmService.generateMinutes(request);

            verify(llmClient).callLLM(argThat(systemPrompt ->
                systemPrompt.contains("Comprehensive documentation")
            ), anyString());
        }

        @Test
        @DisplayName("Should default to Standard length")
        void defaultToStandardLength() {
            StructuredModeRequest request = createRequestWithLength("Unknown");
            when(llmClient.callLLM(anyString(), anyString())).thenReturn("output");

            llmService.generateMinutes(request);

            verify(llmClient).callLLM(argThat(systemPrompt ->
                systemPrompt.contains("Standard format")
            ), anyString());
        }
    }

    @Nested
    @DisplayName("generateMinutesFromExtracted()")
    class GenerateMinutesFromExtractedTests {

        @Test
        @DisplayName("Should generate minutes from extracted data")
        void generateFromExtractedData() {
            ExtractedData data = new ExtractedData();
            data.setMeetingTitle("Team Sync");
            data.setDate("2024-01-20");
            data.setTime("2:00 PM");
            data.setParticipants(Arrays.asList("Alice", "Bob"));
            data.setDiscussionPoints(Arrays.asList("Topic A", "Topic B"));
            data.setDecisions(Collections.emptyList());
            data.setActionItems(Collections.emptyList());
            
            when(llmClient.callLLM(anyString(), anyString())).thenReturn("Generated minutes");

            String result = llmService.generateMinutesFromExtracted(data, "Formal", "Standard");

            assertNotNull(result);
            verify(llmClient).callLLM(anyString(), argThat(userInput ->
                userInput.contains("Team Sync") &&
                userInput.contains("2024-01-20") &&
                userInput.contains("Alice") &&
                userInput.contains("Topic A")
            ));
        }

        @Test
        @DisplayName("Should handle null fields in extracted data")
        void handleNullFieldsInExtractedData() {
            ExtractedData data = new ExtractedData();
            data.setMeetingTitle(null);
            data.setParticipants(null);
            data.setDiscussionPoints(null);
            data.setDecisions(null);
            data.setActionItems(null);
            
            when(llmClient.callLLM(anyString(), anyString())).thenReturn("Generated minutes");

            String result = llmService.generateMinutesFromExtracted(data, null, null);

            assertNotNull(result);
            // Should not throw NPE
        }
    }

    @Nested
    @DisplayName("generateContent() - Legacy method")
    class GenerateContentTests {

        @Test
        @DisplayName("Should generate email content")
        void generateEmailContent() {
            when(llmClient.callLLM(anyString(), anyString())).thenReturn("Generated email");

            String result = llmService.generateContent("Write an email about...", TemplateType.email);

            assertNotNull(result);
            assertEquals("Generated email", result);
            verify(llmClient).callLLM(
                argThat(systemPrompt -> systemPrompt.contains("professional email")),
                anyString()
            );
        }

        @Test
        @DisplayName("Should generate PV content")
        void generatePvContent() {
            when(llmClient.callLLM(anyString(), anyString())).thenReturn("Generated PV");

            String result = llmService.generateContent("Meeting notes...", TemplateType.PV);

            assertNotNull(result);
            assertEquals("Generated PV", result);
            verify(llmClient).callLLM(
                argThat(systemPrompt -> systemPrompt.contains("meeting minutes")),
                anyString()
            );
        }

        @Test
        @DisplayName("Should throw exception for meeting type")
        void throwExceptionForMeetingType() {
            assertThrows(IllegalArgumentException.class,
                () -> llmService.generateContent("input", TemplateType.meeting)
            );
        }
    }

    // Helper methods

    private StructuredModeRequest createMinimalStructuredRequest() {
        StructuredModeRequest request = new StructuredModeRequest();
        StructuredModeRequest.OutputPreferences prefs = new StructuredModeRequest.OutputPreferences();
        prefs.setTone("Formal");
        prefs.setLength("Standard");
        request.setOutputPreferences(prefs);
        return request;
    }

    private StructuredModeRequest createRequestWithTone(String tone) {
        StructuredModeRequest request = new StructuredModeRequest();
        StructuredModeRequest.OutputPreferences prefs = new StructuredModeRequest.OutputPreferences();
        prefs.setTone(tone);
        prefs.setLength("Standard");
        request.setOutputPreferences(prefs);
        return request;
    }

    private StructuredModeRequest createRequestWithLength(String length) {
        StructuredModeRequest request = new StructuredModeRequest();
        StructuredModeRequest.OutputPreferences prefs = new StructuredModeRequest.OutputPreferences();
        prefs.setTone("Formal");
        prefs.setLength(length);
        request.setOutputPreferences(prefs);
        return request;
    }
}
