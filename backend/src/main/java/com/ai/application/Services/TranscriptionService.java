package com.ai.application.Services;

import com.ai.application.model.TranscriptResult;
import com.ai.application.model.TranscriptSegment;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Service for communicating with the Python transcription microservice.
 * Handles audio file upload, job polling, and result retrieval.
 */
@Service
public class TranscriptionService {

    private static final Logger logger = LoggerFactory.getLogger(TranscriptionService.class);

    @Value("${transcription.service.url:http://localhost:8001}")
    private String transcriptionServiceUrl;

    @Value("${transcription.service.timeout:600000}")
    private int timeoutMs;

    @Value("${transcription.service.poll-interval:5000}")
    private int pollIntervalMs;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final AtomicBoolean serviceHealthy = new AtomicBoolean(false);

    public TranscriptionService() {
        this.restTemplate = new RestTemplate();
        this.objectMapper = new ObjectMapper();
        this.objectMapper.setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);
    }

    /**
     * Transcribe an audio file using the Python service.
     * This method handles both immediate and queued responses.
     *
     * @param audioFile The audio file to transcribe
     * @return TranscriptResult with segments and speaker information
     * @throws IOException            If file processing fails
     * @throws TranscriptionException If transcription fails
     */
    public TranscriptResult transcribe(MultipartFile audioFile) throws IOException, TranscriptionException {
        String correlationId = MDC.get("correlationId");
        if (correlationId == null) {
            correlationId = UUID.randomUUID().toString();
            MDC.put("correlationId", correlationId);
        }

        logger.info("Starting transcription for file: {} (size: {} bytes)",
                audioFile.getOriginalFilename(), audioFile.getSize());

        // Check service health first
        if (!serviceHealthy.get()) {
            checkHealth();
            if (!serviceHealthy.get()) {
                throw new TranscriptionException("Transcription service is not available");
            }
        }

        // Prepare multipart request
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        headers.set("X-Correlation-ID", correlationId);

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", new ByteArrayResource(audioFile.getBytes()) {
            @Override
            public String getFilename() {
                return audioFile.getOriginalFilename();
            }
        });

        HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

        try {
            // POST to transcription service
            ResponseEntity<String> response = restTemplate.exchange(
                    transcriptionServiceUrl + "/transcribe",
                    HttpMethod.POST,
                    requestEntity,
                    String.class);

            if (!response.getStatusCode().is2xxSuccessful()) {
                throw new TranscriptionException("Transcription service returned: " + response.getStatusCode());
            }

            TranscriptResult result = parseResponse(response.getBody());

            // If queued, poll for completion
            if (result.isQueued() || result.isProcessing()) {
                result = pollForCompletion(result.getJobId(), correlationId);
            }

            if (result.isFailed()) {
                throw new TranscriptionException("Transcription failed: " + result.getError());
            }

            logger.info("Transcription completed: {} speakers, {} segments, duration: {}",
                    result.getSpeakers() != null ? result.getSpeakers().size() : 0,
                    result.getSegments() != null ? result.getSegments().size() : 0,
                    result.getFormattedDuration());

            return result;

        } catch (RestClientException e) {
            logger.error("Failed to connect to transcription service: {}", e.getMessage());
            serviceHealthy.set(false);
            throw new TranscriptionException("Transcription service unavailable: " + e.getMessage());
        }
    }

    /**
     * Poll for job completion with exponential backoff.
     */
    private TranscriptResult pollForCompletion(String jobId, String correlationId)
            throws TranscriptionException {

        logger.info("Job {} queued, polling for completion...", jobId);

        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Correlation-ID", correlationId);

        long startTime = System.currentTimeMillis();
        int attempts = 0;
        int currentInterval = pollIntervalMs;

        while (System.currentTimeMillis() - startTime < timeoutMs) {
            try {
                Thread.sleep(currentInterval);
                attempts++;

                ResponseEntity<String> response = restTemplate.exchange(
                        transcriptionServiceUrl + "/result/" + jobId,
                        HttpMethod.GET,
                        new HttpEntity<>(headers),
                        String.class);

                TranscriptResult result = parseResponse(response.getBody());

                if (result.isCompleted() || result.isFailed()) {
                    logger.info("Job {} finished after {} attempts", jobId, attempts);
                    return result;
                }

                logger.debug("Job {} still {}, attempt {}", jobId, result.getStatus(), attempts);

                // Exponential backoff, max 30 seconds
                currentInterval = Math.min(currentInterval * 2, 30000);

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new TranscriptionException("Polling interrupted");
            } catch (RestClientException e) {
                logger.warn("Poll attempt {} failed: {}", attempts, e.getMessage());
            }
        }

        throw new TranscriptionException("Transcription timed out after " + (timeoutMs / 1000) + " seconds");
    }

    /**
     * Parse JSON response into TranscriptResult.
     */
    private TranscriptResult parseResponse(String json) throws TranscriptionException {
        try {
            JsonNode root = objectMapper.readTree(json);

            TranscriptResult result = new TranscriptResult();
            result.setJobId(getTextOrNull(root, "job_id"));
            result.setStatus(getTextOrNull(root, "status"));
            result.setLanguage(getTextOrNull(root, "language"));
            result.setFullText(getTextOrNull(root, "full_text"));
            result.setError(getTextOrNull(root, "error"));

            if (root.has("duration") && !root.get("duration").isNull()) {
                result.setDuration(root.get("duration").asDouble());
            }
            if (root.has("processing_time_seconds") && !root.get("processing_time_seconds").isNull()) {
                result.setProcessingTimeSeconds(root.get("processing_time_seconds").asDouble());
            }

            // Parse speakers list
            if (root.has("speakers") && root.get("speakers").isArray()) {
                List<String> speakers = new ArrayList<>();
                for (JsonNode speaker : root.get("speakers")) {
                    speakers.add(speaker.asText());
                }
                result.setSpeakers(speakers);
            }

            // Parse segments
            if (root.has("segments") && root.get("segments").isArray()) {
                List<TranscriptSegment> segments = new ArrayList<>();
                for (JsonNode segNode : root.get("segments")) {
                    TranscriptSegment segment = new TranscriptSegment(
                            getTextOrNull(segNode, "speaker"),
                            segNode.has("start") ? segNode.get("start").asDouble() : 0,
                            segNode.has("end") ? segNode.get("end").asDouble() : 0,
                            getTextOrNull(segNode, "text"));
                    segments.add(segment);
                }
                result.setSegments(segments);
            }

            return result;

        } catch (Exception e) {
            logger.error("Failed to parse transcription response: {}", e.getMessage());
            throw new TranscriptionException("Failed to parse response: " + e.getMessage());
        }
    }

    private String getTextOrNull(JsonNode node, String field) {
        if (node.has(field) && !node.get(field).isNull()) {
            return node.get(field).asText();
        }
        return null;
    }

    /**
     * Get the status of a specific job.
     */
    public TranscriptResult getJobStatus(String jobId) throws TranscriptionException {
        try {
            ResponseEntity<String> response = restTemplate.getForEntity(
                    transcriptionServiceUrl + "/result/" + jobId,
                    String.class);
            return parseResponse(response.getBody());
        } catch (RestClientException e) {
            throw new TranscriptionException("Failed to get job status: " + e.getMessage());
        }
    }

    /**
     * Submit an audio file for async transcription.
     * Returns job info including device/duration for time estimation.
     *
     * @param audioFile The audio file to transcribe
     * @return Map with jobId, audioDuration, transcriptionDevice, diarizationDevice
     * @throws IOException            If file processing fails
     * @throws TranscriptionException If submission fails
     */
    public Map<String, Object> transcribeAsync(MultipartFile audioFile) throws IOException, TranscriptionException {
        String correlationId = MDC.get("correlationId");
        if (correlationId == null) {
            correlationId = UUID.randomUUID().toString();
            MDC.put("correlationId", correlationId);
        }

        logger.info("Starting async transcription for file: {} (size: {} bytes)",
                audioFile.getOriginalFilename(), audioFile.getSize());

        // Check service health first
        if (!serviceHealthy.get()) {
            checkHealth();
            if (!serviceHealthy.get()) {
                throw new TranscriptionException("Transcription service is not available");
            }
        }

        // Prepare multipart request
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        headers.set("X-Correlation-ID", correlationId);

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", new ByteArrayResource(audioFile.getBytes()) {
            @Override
            public String getFilename() {
                return audioFile.getOriginalFilename();
            }
        });

        HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    transcriptionServiceUrl + "/transcribe",
                    HttpMethod.POST,
                    requestEntity,
                    String.class);

            if (!response.getStatusCode().is2xxSuccessful()) {
                throw new TranscriptionException("Transcription service returned: " + response.getStatusCode());
            }

            // Parse the job_id from response
            JsonNode root = objectMapper.readTree(response.getBody());
            String jobId = root.has("job_id") ? root.get("job_id").asText() : null;

            if (jobId == null || jobId.isEmpty()) {
                throw new TranscriptionException("No job ID returned from transcription service");
            }

            Map<String, Object> result = new HashMap<>();
            result.put("jobId", jobId);
            if (root.has("audio_duration") && !root.get("audio_duration").isNull()) {
                result.put("audioDuration", root.get("audio_duration").asDouble());
            }
            if (root.has("transcription_device") && !root.get("transcription_device").isNull()) {
                result.put("transcriptionDevice", root.get("transcription_device").asText());
            }
            if (root.has("diarization_device") && !root.get("diarization_device").isNull()) {
                result.put("diarizationDevice", root.get("diarization_device").asText());
            }

            logger.info("Async transcription submitted, job ID: {}", jobId);
            return result;

        } catch (RestClientException e) {
            logger.error("Failed to connect to transcription service: {}", e.getMessage());
            serviceHealthy.set(false);
            throw new TranscriptionException("Transcription service unavailable: " + e.getMessage());
        } catch (Exception e) {
            if (e instanceof TranscriptionException)
                throw (TranscriptionException) e;
            logger.error("Failed to parse transcription response: {}", e.getMessage());
            throw new TranscriptionException("Failed to process transcription response: " + e.getMessage());
        }
    }

    /**
     * Get progress of a transcription job from the Python service.
     *
     * @param jobId The job ID to check
     * @return Map with job_id, status, progress (0-100), and stage
     * @throws TranscriptionException If the request fails
     */
    public Map<String, Object> getJobProgress(String jobId) throws TranscriptionException {
        try {
            ResponseEntity<String> response = restTemplate.getForEntity(
                    transcriptionServiceUrl + "/progress/" + jobId,
                    String.class);

            JsonNode root = objectMapper.readTree(response.getBody());

            Map<String, Object> progress = new HashMap<>();
            progress.put("jobId", root.has("job_id") ? root.get("job_id").asText() : jobId);
            progress.put("status", root.has("status") ? root.get("status").asText() : "unknown");
            progress.put("progress", root.has("progress") ? root.get("progress").asInt() : 0);
            progress.put("stage", root.has("stage") ? root.get("stage").asText() : "unknown");
            if (root.has("audio_duration") && !root.get("audio_duration").isNull()) {
                progress.put("audioDuration", root.get("audio_duration").asDouble());
            }
            if (root.has("transcription_device") && !root.get("transcription_device").isNull()) {
                progress.put("transcriptionDevice", root.get("transcription_device").asText());
            }
            if (root.has("diarization_device") && !root.get("diarization_device").isNull()) {
                progress.put("diarizationDevice", root.get("diarization_device").asText());
            }

            return progress;

        } catch (RestClientException e) {
            throw new TranscriptionException("Failed to get job progress: " + e.getMessage());
        } catch (Exception e) {
            throw new TranscriptionException("Failed to parse progress response: " + e.getMessage());
        }
    }

    /**
     * Cancel a running transcription job.
     */
    public void cancelJob(String jobId) throws TranscriptionException {
        try {
            restTemplate.postForEntity(
                    transcriptionServiceUrl + "/cancel/" + jobId,
                    null,
                    String.class);
            logger.info("Cancelled job {}", jobId);
        } catch (RestClientException e) {
            logger.warn("Failed to cancel job {}: {}", jobId, e.getMessage());
            // Don't throw - cancellation is best-effort
        }
    }

    /**
     * Check if the transcription service is healthy.
     */
    public boolean isServiceHealthy() {
        return serviceHealthy.get();
    }

    /**
     * Get detailed health information from the transcription service.
     */
    public JsonNode getHealthDetails() {
        try {
            ResponseEntity<String> response = restTemplate.getForEntity(
                    transcriptionServiceUrl + "/health",
                    String.class);
            return objectMapper.readTree(response.getBody());
        } catch (Exception e) {
            logger.warn("Failed to get health details: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Periodic health check (every 30 seconds).
     */
    @Scheduled(fixedRate = 30000)
    public void checkHealth() {
        try {
            ResponseEntity<String> response = restTemplate.getForEntity(
                    transcriptionServiceUrl + "/health",
                    String.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                JsonNode health = objectMapper.readTree(response.getBody());
                String status = health.has("status") ? health.get("status").asText() : "unknown";
                boolean wasHealthy = serviceHealthy.get();
                serviceHealthy.set("healthy".equals(status) || "degraded".equals(status));

                if (!wasHealthy && serviceHealthy.get()) {
                    logger.info("Transcription service is now available");
                } else if (wasHealthy && !serviceHealthy.get()) {
                    logger.warn("Transcription service is now unavailable (status: {})", status);
                }
            } else {
                serviceHealthy.set(false);
            }
        } catch (Exception e) {
            if (serviceHealthy.get()) {
                logger.warn("Transcription service health check failed: {}", e.getMessage());
            }
            serviceHealthy.set(false);
        }
    }

    /**
     * Custom exception for transcription errors.
     */
    public static class TranscriptionException extends Exception {
        public TranscriptionException(String message) {
            super(message);
        }
    }
}
