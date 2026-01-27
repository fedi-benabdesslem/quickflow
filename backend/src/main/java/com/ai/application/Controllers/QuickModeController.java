package com.ai.application.Controllers;

import com.ai.application.Services.FileProcessingService;
import com.ai.application.Services.LLMService;
import com.ai.application.model.ExtractedData;
import com.ai.application.model.QuickModeRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

/**
 * Controller for Quick Mode AI extraction endpoints.
 */
@RestController
@RequestMapping("/api/minutes/quick")
public class QuickModeController {

    private static final Logger logger = LoggerFactory.getLogger(QuickModeController.class);
    private static final int MIN_CONTENT_LENGTH = 50;

    private final LLMService llmService;
    private final FileProcessingService fileProcessingService;

    @Autowired
    public QuickModeController(LLMService llmService, FileProcessingService fileProcessingService) {
        this.llmService = llmService;
        this.fileProcessingService = fileProcessingService;
    }

    /**
     * Extract structured data from pasted notes.
     * POST /api/minutes/quick/extract
     */
    @PostMapping("/extract")
    public ResponseEntity<?> extractFromNotes(@RequestBody QuickModeRequest request) {
        logger.info("Received Quick Mode extraction request");

        // Validate content
        if (request.getContent() == null || request.getContent().trim().length() < MIN_CONTENT_LENGTH) {
            return ResponseEntity.badRequest().body(Map.of(
                    "status", "error",
                    "message", "Content must be at least " + MIN_CONTENT_LENGTH + " characters"));
        }

        try {
            ExtractedData extracted = llmService.extractFromNotes(
                    request.getContent(),
                    request.getDate(),
                    request.getTime(),
                    request.getLocation());

            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "data", extracted));
        } catch (Exception e) {
            logger.error("Extraction failed: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(Map.of(
                    "status", "error",
                    "message", "AI service temporarily unavailable. Please try again."));
        }
    }

    /**
     * Extract structured data from an uploaded file.
     * POST /api/minutes/quick/extract-file
     */
    @PostMapping("/extract-file")
    public ResponseEntity<?> extractFromFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "date", required = false) String date,
            @RequestParam(value = "time", required = false) String time,
            @RequestParam(value = "location", required = false) String location) {

        logger.info("Received file extraction request: {}", file.getOriginalFilename());

        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of(
                    "status", "error",
                    "message", "File is empty"));
        }

        try {
            // Extract text from file
            String content = fileProcessingService.extractText(file);

            if (content.trim().length() < MIN_CONTENT_LENGTH) {
                return ResponseEntity.badRequest().body(Map.of(
                        "status", "error",
                        "message", "Extracted content must be at least " + MIN_CONTENT_LENGTH + " characters"));
            }

            // Extract structured data using LLM
            ExtractedData extracted = llmService.extractFromNotes(content, date, time, location);

            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "data", extracted));
        } catch (IllegalArgumentException e) {
            logger.warn("Invalid file: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                    "status", "error",
                    "message", e.getMessage()));
        } catch (Exception e) {
            logger.error("File extraction failed: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(Map.of(
                    "status", "error",
                    "message", "Failed to process file. Please try again."));
        }
    }

    /**
     * Generate minutes from reviewed extracted data.
     * POST /api/minutes/quick/generate
     */
    @PostMapping("/generate")
    public ResponseEntity<?> generateFromExtracted(@RequestBody Map<String, Object> request) {
        logger.info("Received Quick Mode generation request");

        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> dataMap = (Map<String, Object>) request.get("data");
            String tone = (String) request.getOrDefault("tone", "Formal");
            String length = (String) request.getOrDefault("length", "Standard");

            // Convert map to ExtractedData using custom Gson with participant deserializer
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
            com.google.gson.Gson gson = gsonBuilder.create();
            String json = gson.toJson(dataMap);
            ExtractedData data = gson.fromJson(json, ExtractedData.class);

            String content = llmService.generateMinutesFromExtracted(data, tone, length);

            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "content", content));
        } catch (Exception e) {
            logger.error("Generation failed: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(Map.of(
                    "status", "error",
                    "message", "AI service temporarily unavailable. Please try again."));
        }
    }
}
