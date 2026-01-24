package com.ai.application.model;

import java.util.List;

/**
 * Response DTO containing extracted meeting data from Quick Mode.
 */
public class ExtractedData {
    private String meetingTitle;
    private String date;
    private String time;
    private List<String> participants;
    private List<String> discussionPoints;
    private List<ExtractedDecision> decisions;
    private List<ExtractedActionItem> actionItems;
    private String confidence; // "high", "medium", "low"

    public static class ExtractedDecision {
        private String statement;
        private String status;

        public ExtractedDecision() {
        }

        public ExtractedDecision(String statement, String status) {
            this.statement = statement;
            this.status = status;
        }

        public String getStatement() {
            return statement;
        }

        public void setStatement(String statement) {
            this.statement = statement;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }
    }

    public static class ExtractedActionItem {
        private String task;
        private String owner;
        private String deadline;

        public ExtractedActionItem() {
        }

        public ExtractedActionItem(String task, String owner, String deadline) {
            this.task = task;
            this.owner = owner;
            this.deadline = deadline;
        }

        public String getTask() {
            return task;
        }

        public void setTask(String task) {
            this.task = task;
        }

        public String getOwner() {
            return owner;
        }

        public void setOwner(String owner) {
            this.owner = owner;
        }

        public String getDeadline() {
            return deadline;
        }

        public void setDeadline(String deadline) {
            this.deadline = deadline;
        }
    }

    public ExtractedData() {
    }

    // Getters and Setters
    public String getMeetingTitle() {
        return meetingTitle;
    }

    public void setMeetingTitle(String meetingTitle) {
        this.meetingTitle = meetingTitle;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }

    public List<String> getParticipants() {
        return participants;
    }

    public void setParticipants(List<String> participants) {
        this.participants = participants;
    }

    public List<String> getDiscussionPoints() {
        return discussionPoints;
    }

    public void setDiscussionPoints(List<String> discussionPoints) {
        this.discussionPoints = discussionPoints;
    }

    public List<ExtractedDecision> getDecisions() {
        return decisions;
    }

    public void setDecisions(List<ExtractedDecision> decisions) {
        this.decisions = decisions;
    }

    public List<ExtractedActionItem> getActionItems() {
        return actionItems;
    }

    public void setActionItems(List<ExtractedActionItem> actionItems) {
        this.actionItems = actionItems;
    }

    public String getConfidence() {
        return confidence;
    }

    public void setConfidence(String confidence) {
        this.confidence = confidence;
    }
}
