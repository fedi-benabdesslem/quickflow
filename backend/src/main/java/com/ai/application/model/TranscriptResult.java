package com.ai.application.model;

import java.util.List;

/**
 * Complete transcription result from the Python transcription service.
 */
public class TranscriptResult {
    private String jobId;
    private String status;
    private List<TranscriptSegment> segments;
    private List<String> speakers;
    private Double duration;
    private String language;
    private String fullText;
    private Double processingTimeSeconds;
    private String error;

    public TranscriptResult() {
    }

    // Getters and Setters
    public String getJobId() {
        return jobId;
    }

    public void setJobId(String jobId) {
        this.jobId = jobId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public List<TranscriptSegment> getSegments() {
        return segments;
    }

    public void setSegments(List<TranscriptSegment> segments) {
        this.segments = segments;
    }

    public List<String> getSpeakers() {
        return speakers;
    }

    public void setSpeakers(List<String> speakers) {
        this.speakers = speakers;
    }

    public Double getDuration() {
        return duration;
    }

    public void setDuration(Double duration) {
        this.duration = duration;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public String getFullText() {
        return fullText;
    }

    public void setFullText(String fullText) {
        this.fullText = fullText;
    }

    public Double getProcessingTimeSeconds() {
        return processingTimeSeconds;
    }

    public void setProcessingTimeSeconds(Double processingTimeSeconds) {
        this.processingTimeSeconds = processingTimeSeconds;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }

    public boolean isCompleted() {
        return "completed".equals(status);
    }

    public boolean isFailed() {
        return "failed".equals(status);
    }

    public boolean isQueued() {
        return "queued".equals(status);
    }

    public boolean isProcessing() {
        return "processing".equals(status);
    }

    /**
     * Get a formatted transcript with speaker labels.
     */
    public String getFormattedTranscript() {
        if (segments == null || segments.isEmpty()) {
            return fullText != null ? fullText : "";
        }

        StringBuilder sb = new StringBuilder();
        String currentSpeaker = null;

        for (TranscriptSegment segment : segments) {
            if (!segment.getSpeaker().equals(currentSpeaker)) {
                if (currentSpeaker != null) {
                    sb.append("\n\n");
                }
                sb.append("[").append(segment.getFormattedStart()).append("] ");
                sb.append(segment.getSpeaker()).append(":\n");
                currentSpeaker = segment.getSpeaker();
            }
            sb.append(segment.getText()).append(" ");
        }

        return sb.toString().trim();
    }

    /**
     * Get duration formatted as HH:MM:SS
     */
    public String getFormattedDuration() {
        if (duration == null)
            return "00:00:00";
        int hours = (int) (duration / 3600);
        int minutes = (int) ((duration % 3600) / 60);
        int seconds = (int) (duration % 60);
        return String.format("%02d:%02d:%02d", hours, minutes, seconds);
    }
}
