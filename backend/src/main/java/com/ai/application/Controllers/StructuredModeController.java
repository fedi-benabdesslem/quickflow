package com.ai.application.Controllers;

import com.ai.application.Services.LLMService;
import com.ai.application.model.GeneratedMinutes;
import com.ai.application.model.StructuredModeRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Controller for Structured Mode AI generation endpoints.
 */
@RestController
@RequestMapping("/api/minutes/structured")
public class StructuredModeController {

    private static final Logger logger = LoggerFactory.getLogger(StructuredModeController.class);

    private final LLMService llmService;

    @Autowired
    public StructuredModeController(LLMService llmService) {
        this.llmService = llmService;
    }

    /**
     * Generate professional meeting minutes from structured form data.
     * POST /api/minutes/structured/generate
     */
    @PostMapping("/generate")
    public ResponseEntity<GeneratedMinutes> generateMinutes(@RequestBody StructuredModeRequest request) {
        logger.info("Received Structured Mode generation request");

        // Validate required fields
        if (request.getMeetingInfo() == null) {
            return ResponseEntity.badRequest().body(
                    GeneratedMinutes.error("Meeting information is required"));
        }

        if (request.getMeetingInfo().getTitle() == null || request.getMeetingInfo().getTitle().trim().isEmpty()) {
            return ResponseEntity.badRequest().body(
                    GeneratedMinutes.error("Meeting title is required"));
        }

        if (request.getMeetingInfo().getDate() == null || request.getMeetingInfo().getDate().trim().isEmpty()) {
            return ResponseEntity.badRequest().body(
                    GeneratedMinutes.error("Meeting date is required"));
        }

        if (request.getMeetingInfo().getStartTime() == null
                || request.getMeetingInfo().getStartTime().trim().isEmpty()) {
            return ResponseEntity.badRequest().body(
                    GeneratedMinutes.error("Meeting start time is required"));
        }

        try {
            String content = llmService.generateMinutes(request);
            logger.info("Successfully generated meeting minutes");
            return ResponseEntity.ok(GeneratedMinutes.success(content));
        } catch (Exception e) {
            logger.error("Generation failed: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(
                    GeneratedMinutes.error("AI service temporarily unavailable. Please try again."));
        }
    }

    /**
     * Regenerate meeting minutes (same endpoint, allows re-calling).
     * POST /api/minutes/structured/regenerate
     */
    @PostMapping("/regenerate")
    public ResponseEntity<GeneratedMinutes> regenerateMinutes(@RequestBody StructuredModeRequest request) {
        logger.info("Received regeneration request");
        return generateMinutes(request);
    }
}
