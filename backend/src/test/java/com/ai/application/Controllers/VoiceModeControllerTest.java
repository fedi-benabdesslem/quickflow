package com.ai.application.Controllers;

import com.ai.application.Services.LLMService;
import com.ai.application.Services.TranscriptionService;
import com.ai.application.model.TranscriptResult;
import com.ai.application.model.TranscriptSegment;
import com.ai.application.model.VoiceModeRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit tests for VoiceModeController.
 * Tests all voice mode endpoints with mocked dependencies.
 */
@ExtendWith(MockitoExtension.class)
class VoiceModeControllerTest {

    private MockMvc mockMvc;

    @Mock
    private TranscriptionService transcriptionService;

    @Mock
    private LLMService llmService;

    @InjectMocks
    private VoiceModeController voiceModeController;

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(voiceModeController).build();
        objectMapper = new ObjectMapper();
    }

    // ==================== GET /status Tests ====================

    @Test
    void getServiceStatus_WhenHealthy_ReturnsAvailableTrue() throws Exception {
        when(transcriptionService.isServiceHealthy()).thenReturn(true);
        when(transcriptionService.getHealthDetails()).thenReturn(null);

        mockMvc.perform(get("/api/minutes/voice/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.available").value(true));
    }

    @Test
    void getServiceStatus_WhenUnhealthy_ReturnsAvailableFalse() throws Exception {
        when(transcriptionService.isServiceHealthy()).thenReturn(false);
        when(transcriptionService.getHealthDetails()).thenReturn(null);

        mockMvc.perform(get("/api/minutes/voice/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.available").value(false));
    }

    // ==================== POST /transcribe Tests ====================

    @Test
    void transcribeAudio_WithEmptyFile_ReturnsBadRequest() throws Exception {
        MockMultipartFile emptyFile = new MockMultipartFile(
                "file", "test.mp3", "audio/mpeg", new byte[0]);

        mockMvc.perform(multipart("/api/minutes/voice/transcribe")
                .file(emptyFile))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("error"))
                .andExpect(jsonPath("$.message").value("File is empty"));
    }

    @Test
    void transcribeAudio_WithInvalidFormat_ReturnsBadRequest() throws Exception {
        MockMultipartFile invalidFile = new MockMultipartFile(
                "file", "test.txt", "text/plain", "not audio".getBytes());

        mockMvc.perform(multipart("/api/minutes/voice/transcribe")
                .file(invalidFile))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("error"))
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void transcribeAudio_WhenServiceUnavailable_ReturnsServiceUnavailable() throws Exception {
        MockMultipartFile audioFile = new MockMultipartFile(
                "file", "test.mp3", "audio/mpeg", "fake audio content".getBytes());

        when(transcriptionService.isServiceHealthy()).thenReturn(false);

        mockMvc.perform(multipart("/api/minutes/voice/transcribe")
                .file(audioFile))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.status").value("error"))
                .andExpect(jsonPath("$.retryAfter").value(30));
    }

    @Test
    void transcribeAudio_WhenSuccessful_ReturnsTranscriptResult() throws Exception {
        MockMultipartFile audioFile = new MockMultipartFile(
                "file", "test.mp3", "audio/mpeg", "fake audio content".getBytes());

        TranscriptResult mockResult = createMockTranscriptResult();

        when(transcriptionService.isServiceHealthy()).thenReturn(true);
        when(transcriptionService.transcribe(any())).thenReturn(mockResult);

        mockMvc.perform(multipart("/api/minutes/voice/transcribe")
                .file(audioFile))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.data").exists());
    }

    // ==================== POST /generate Tests ====================

    @Test
    void generateFromTranscript_WithNullTranscript_ReturnsBadRequest() throws Exception {
        VoiceModeRequest request = new VoiceModeRequest();
        // transcriptData is null

        mockMvc.perform(post("/api/minutes/voice/generate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("error"))
                .andExpect(jsonPath("$.message").value("Transcript data is required"));
    }

    @Test
    void generateFromTranscript_WhenSuccessful_ReturnsMinutes() throws Exception {
        VoiceModeRequest request = createValidVoiceModeRequest();
        String expectedMinutes = "# Meeting Minutes\n\nThis is the generated content.";

        when(llmService.generateMinutesFromTranscript(
                any(), any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(expectedMinutes);

        mockMvc.perform(post("/api/minutes/voice/generate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.content").value(expectedMinutes));
    }

    @Test
    void generateFromTranscript_WhenLLMFails_ReturnsInternalError() throws Exception {
        VoiceModeRequest request = createValidVoiceModeRequest();

        when(llmService.generateMinutesFromTranscript(
                any(), any(), any(), any(), any(), any(), any(), any(), any()))
                .thenThrow(new RuntimeException("LLM service error"));

        mockMvc.perform(post("/api/minutes/voice/generate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value("error"));
    }

    // ==================== GET /job/{jobId} Tests ====================

    @Test
    void getJobStatus_WithValidJobId_ReturnsJob() throws Exception {
        String jobId = "test-job-123";
        TranscriptResult mockResult = createMockTranscriptResult();

        when(transcriptionService.getJobStatus(jobId)).thenReturn(mockResult);

        mockMvc.perform(get("/api/minutes/voice/job/{jobId}", jobId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.data").exists());
    }

    @Test
    void getJobStatus_WithInvalidJobId_ReturnsNotFound() throws Exception {
        String jobId = "nonexistent-job";

        when(transcriptionService.getJobStatus(jobId))
                .thenThrow(new TranscriptionService.TranscriptionException("Job not found"));

        mockMvc.perform(get("/api/minutes/voice/job/{jobId}", jobId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value("error"));
    }

    // ==================== Helper Methods ====================

    private TranscriptResult createMockTranscriptResult() {
        TranscriptResult result = new TranscriptResult();
        result.setJobId("test-job-123");
        result.setStatus("completed");
        result.setLanguage("en");
        result.setDuration(60.0);
        result.setFullText("Hello world. This is a test.");
        result.setProcessingTimeSeconds(5.0);

        TranscriptSegment segment = new TranscriptSegment();
        segment.setSpeaker("SPEAKER_00");
        segment.setStart(0.0);
        segment.setEnd(2.0);
        segment.setText("Hello world.");
        result.setSegments(List.of(segment));
        result.setSpeakers(List.of("SPEAKER_00"));

        return result;
    }

    private VoiceModeRequest createValidVoiceModeRequest() {
        VoiceModeRequest request = new VoiceModeRequest();

        TranscriptResult transcriptData = createMockTranscriptResult();
        request.setTranscriptData(transcriptData);

        Map<String, String> speakerMapping = new HashMap<>();
        speakerMapping.put("SPEAKER_00", "John");
        request.setSpeakerMapping(speakerMapping);

        request.setMeetingTitle("Test Meeting");
        request.setMeetingDate("2024-01-15");
        request.setMeetingTime("10:00");
        request.setMeetingEndTime("11:00");
        request.setMeetingLocation("Conference Room A");
        request.setTone("Formal");
        request.setLength("Standard");

        return request;
    }
}
