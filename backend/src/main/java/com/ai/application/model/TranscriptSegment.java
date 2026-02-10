package com.ai.application.model;

/**
 * Represents a segment of transcribed text with speaker and timing information.
 */
public class TranscriptSegment {
    private String speaker;
    private double start;
    private double end;
    private String text;

    public TranscriptSegment() {
    }

    public TranscriptSegment(String speaker, double start, double end, String text) {
        this.speaker = speaker;
        this.start = start;
        this.end = end;
        this.text = text;
    }

    // Getters and Setters
    public String getSpeaker() {
        return speaker;
    }

    public void setSpeaker(String speaker) {
        this.speaker = speaker;
    }

    public double getStart() {
        return start;
    }

    public void setStart(double start) {
        this.start = start;
    }

    public double getEnd() {
        return end;
    }

    public void setEnd(double end) {
        this.end = end;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    /**
     * Format the timestamp as MM:SS
     */
    public String getFormattedStart() {
        int minutes = (int) (start / 60);
        int seconds = (int) (start % 60);
        return String.format("%02d:%02d", minutes, seconds);
    }

    public String getFormattedEnd() {
        int minutes = (int) (end / 60);
        int seconds = (int) (end % 60);
        return String.format("%02d:%02d", minutes, seconds);
    }

    @Override
    public String toString() {
        return String.format("[%s] %s: %s", getFormattedStart(), speaker, text);
    }
}
