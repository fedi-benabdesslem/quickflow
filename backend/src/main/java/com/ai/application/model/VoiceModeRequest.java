package com.ai.application.model;

import java.util.Map;

/**
 * Request DTO for Voice Mode endpoints.
 */
public class VoiceModeRequest {
    private TranscriptResult transcriptData;
    private Map<String, String> speakerMapping; // Map SPEAKER_00 -> "John Smith"
    private String tone;
    private String length;
    private String meetingTitle;
    private String meetingDate;
    private String meetingTime;
    private String meetingLocation;

    public VoiceModeRequest() {
        this.tone = "Formal";
        this.length = "Standard";
    }

    // Getters and Setters
    public TranscriptResult getTranscriptData() {
        return transcriptData;
    }

    public void setTranscriptData(TranscriptResult transcriptData) {
        this.transcriptData = transcriptData;
    }

    public Map<String, String> getSpeakerMapping() {
        return speakerMapping;
    }

    public void setSpeakerMapping(Map<String, String> speakerMapping) {
        this.speakerMapping = speakerMapping;
    }

    public String getTone() {
        return tone;
    }

    public void setTone(String tone) {
        this.tone = tone;
    }

    public String getLength() {
        return length;
    }

    public void setLength(String length) {
        this.length = length;
    }

    public String getMeetingTitle() {
        return meetingTitle;
    }

    public void setMeetingTitle(String meetingTitle) {
        this.meetingTitle = meetingTitle;
    }

    public String getMeetingDate() {
        return meetingDate;
    }

    public void setMeetingDate(String meetingDate) {
        this.meetingDate = meetingDate;
    }

    public String getMeetingTime() {
        return meetingTime;
    }

    public void setMeetingTime(String meetingTime) {
        this.meetingTime = meetingTime;
    }

    public String getMeetingLocation() {
        return meetingLocation;
    }

    public void setMeetingLocation(String meetingLocation) {
        this.meetingLocation = meetingLocation;
    }

    /**
     * Apply speaker mapping to transcript segments.
     * Replaces "SPEAKER_00" with actual names like "John Smith".
     */
    public String getMappedTranscript() {
        if (transcriptData == null || transcriptData.getSegments() == null) {
            return "";
        }

        StringBuilder sb = new StringBuilder();
        String currentSpeaker = null;

        for (TranscriptSegment segment : transcriptData.getSegments()) {
            String speakerId = segment.getSpeaker();
            String speakerName = speakerMapping != null && speakerMapping.containsKey(speakerId)
                    ? speakerMapping.get(speakerId)
                    : speakerId;

            if (!speakerId.equals(currentSpeaker)) {
                if (currentSpeaker != null) {
                    sb.append("\n\n");
                }
                sb.append("[").append(segment.getFormattedStart()).append("] ");
                sb.append(speakerName).append(":\n");
                currentSpeaker = speakerId;
            }
            sb.append(segment.getText()).append(" ");
        }

        return sb.toString().trim();
    }
}
