package com.ai.application.Controllers;

import com.ai.application.Services.LLMService;
import com.ai.application.Services.TranscriptionService;
import com.ai.application.Services.TranscriptionService.TranscriptionException;
import com.ai.application.model.TranscriptResult;
import com.ai.application.model.VoiceModeRequest;
import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;

/**
 * Controller for Voice Mode - audio transcription and minute generation.
 */
@RestController
@RequestMapping("/api/minutes/voice")
public class VoiceModeController {

    private static final Logger logger = LoggerFactory.getLogger(VoiceModeController.class);

    private static final long MAX_FILE_SIZE = 350L * 1024 * 1024; // 350MB
    private static final String[] SUPPORTED_FORMATS = { ".mp3", ".wav", ".m4a", ".flac", ".ogg", ".wma", ".aac",
            ".webm" };

    private final TranscriptionService transcriptionService;
    private final LLMService llmService;

    @Autowired
    public VoiceModeController(TranscriptionService transcriptionService, LLMService llmService) {
        this.transcriptionService = transcriptionService;
        this.llmService = llmService;
    }

    /**
     * Check if the voice mode service is available.
     * GET /api/minutes/voice/status
     */
    @GetMapping("/status")
    public ResponseEntity<?> getServiceStatus() {
        boolean healthy = transcriptionService.isServiceHealthy();
        JsonNode details = transcriptionService.getHealthDetails();

        return ResponseEntity.ok(Map.of(
                "available", healthy,
                "details", details != null ? details : Map.of("status", "unknown")));
    }

    /**
     * Transcribe an uploaded audio file.
     * POST /api/minutes/voice/transcribe
     */
    @PostMapping("/transcribe")
    public ResponseEntity<?> transcribeAudio(
            @RequestParam("file") MultipartFile file,
            @RequestHeader(value = "X-Correlation-ID", required = false) String correlationId) {

        // Set correlation ID for logging
        if (correlationId == null) {
            correlationId = UUID.randomUUID().toString();
        }
        MDC.put("correlationId", correlationId);

        logger.info("Voice Mode: Received transcription request for file: {}", file.getOriginalFilename());

        // Validate file
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of(
                    "status", "error",
                    "message", "File is empty"));
        }

        // Check file size
        if (file.getSize() > MAX_FILE_SIZE) {
            return ResponseEntity.badRequest().body(Map.of(
                    "status", "error",
                    "message", String.format("File size (%.1f MB) exceeds limit (350 MB)",
                            file.getSize() / 1024.0 / 1024.0)));
        }

        // Check file format
        String filename = file.getOriginalFilename();
        if (filename == null || !isValidFormat(filename)) {
            return ResponseEntity.badRequest().body(Map.of(
                    "status", "error",
                    "message", "Unsupported file format. Supported: " + String.join(", ", SUPPORTED_FORMATS)));
        }

        // Check service availability
        if (!transcriptionService.isServiceHealthy()) {
            return ResponseEntity.status(503).body(Map.of(
                    "status", "error",
                    "message", "Transcription service is currently unavailable. Please try again later.",
                    "retryAfter", 30));
        }

        try {
            TranscriptResult result = transcriptionService.transcribe(file);

            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "data", result));

        } catch (TranscriptionException e) {
            logger.error("Transcription failed: {}", e.getMessage());
            return ResponseEntity.status(503).body(Map.of(
                    "status", "error",
                    "message", e.getMessage(),
                    "retryAfter", 30));

        } catch (IOException e) {
            logger.error("File processing failed: {}", e.getMessage());
            return ResponseEntity.internalServerError().body(Map.of(
                    "status", "error",
                    "message", "Failed to process uploaded file"));
        } finally {
            MDC.remove("correlationId");
        }
    }

    /**
     * Generate meeting minutes from a transcript.
     * POST /api/minutes/voice/generate
     */
    @PostMapping("/generate")
    public ResponseEntity<?> generateFromTranscript(@RequestBody VoiceModeRequest request) {
        logger.info("Voice Mode: Generating minutes from transcript");

        if (request.getTranscriptData() == null ||
                request.getTranscriptData().getSegments() == null ||
                request.getTranscriptData().getSegments().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of(
                    "status", "error",
                    "message", "Transcript data is required"));
        }

        try {
            // Build meeting context from transcript
            String transcript = request.getMappedTranscript();

            // Generate minutes using LLM
            String minutes = llmService.generateMinutesFromTranscript(
                    transcript,
                    request.getSpeakerMapping(),
                    request.getMeetingTitle(),
                    request.getMeetingDate(),
                    request.getMeetingTime(),
                    request.getMeetingLocation(),
                    request.getTone(),
                    request.getLength());

            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "content", minutes));

        } catch (Exception e) {
            logger.error("Minutes generation failed: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(Map.of(
                    "status", "error",
                    "message", "Failed to generate minutes. Please try again."));
        }
    }

    /**
     * Get the status of a transcription job.
     * GET /api/minutes/voice/job/{jobId}
     */
    @GetMapping("/job/{jobId}")
    public ResponseEntity<?> getJobStatus(@PathVariable String jobId) {
        try {
            TranscriptResult result = transcriptionService.getJobStatus(jobId);
            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "data", result));
        } catch (TranscriptionException e) {
            return ResponseEntity.status(404).body(Map.of(
                    "status", "error",
                    "message", e.getMessage()));
        }
    }

    private boolean isValidFormat(String filename) {
        String lower = filename.toLowerCase();
        for (String format : SUPPORTED_FORMATS) {
            if (lower.endsWith(format)) {
                return true;
            }
        }
        return false;
    }
}
