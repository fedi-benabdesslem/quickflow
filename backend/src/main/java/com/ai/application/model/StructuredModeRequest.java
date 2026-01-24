package com.ai.application.model;

import java.util.List;

/**
 * Request DTO for Structured Mode generation.
 * Maps to the frontend StructuredModeData type.
 */
public class StructuredModeRequest {

    private MeetingInfo meetingInfo;
    private List<Participant> participants;
    private List<Participant> absentParticipants;
    private List<AgendaItem> agenda;
    private List<Decision> decisions;
    private List<ActionItem> actionItems;
    private String additionalNotes;
    private OutputPreferences outputPreferences;

    public static class MeetingInfo {
        private String title;
        private String date;
        private String startTime;
        private String endTime;
        private String location;
        private String organizer;

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public String getDate() {
            return date;
        }

        public void setDate(String date) {
            this.date = date;
        }

        public String getStartTime() {
            return startTime;
        }

        public void setStartTime(String startTime) {
            this.startTime = startTime;
        }

        public String getEndTime() {
            return endTime;
        }

        public void setEndTime(String endTime) {
            this.endTime = endTime;
        }

        public String getLocation() {
            return location;
        }

        public void setLocation(String location) {
            this.location = location;
        }

        public String getOrganizer() {
            return organizer;
        }

        public void setOrganizer(String organizer) {
            this.organizer = organizer;
        }
    }

    public static class Participant {
        private String id;
        private String name;
        private String email;
        private String role;
        private boolean present;

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }

        public String getRole() {
            return role;
        }

        public void setRole(String role) {
            this.role = role;
        }

        public boolean isPresent() {
            return present;
        }

        public void setPresent(boolean present) {
            this.present = present;
        }
    }

    public static class AgendaItem {
        private String id;
        private String title;
        private String objective;
        private String keyPoints;

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public String getObjective() {
            return objective;
        }

        public void setObjective(String objective) {
            this.objective = objective;
        }

        public String getKeyPoints() {
            return keyPoints;
        }

        public void setKeyPoints(String keyPoints) {
            this.keyPoints = keyPoints;
        }
    }

    public static class Decision {
        private String id;
        private String statement;
        private String status;
        private String relatedAgendaId;
        private String rationale;

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
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

        public String getRelatedAgendaId() {
            return relatedAgendaId;
        }

        public void setRelatedAgendaId(String relatedAgendaId) {
            this.relatedAgendaId = relatedAgendaId;
        }

        public String getRationale() {
            return rationale;
        }

        public void setRationale(String rationale) {
            this.rationale = rationale;
        }
    }

    public static class ActionItem {
        private String id;
        private String task;
        private String owner;
        private String deadline;
        private String priority;

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
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

        public String getPriority() {
            return priority;
        }

        public void setPriority(String priority) {
            this.priority = priority;
        }
    }

    public static class OutputPreferences {
        private String tone; // Formal, Executive, Technical
        private String length; // Standard, Detailed, Summary
        private IncludeSections includeSections;
        private String pdfFooter;

        public static class IncludeSections {
            private boolean attendees;
            private boolean agenda;
            private boolean decisions;
            private boolean actionItems;
            private boolean additionalNotes;

            public boolean isAttendees() {
                return attendees;
            }

            public void setAttendees(boolean attendees) {
                this.attendees = attendees;
            }

            public boolean isAgenda() {
                return agenda;
            }

            public void setAgenda(boolean agenda) {
                this.agenda = agenda;
            }

            public boolean isDecisions() {
                return decisions;
            }

            public void setDecisions(boolean decisions) {
                this.decisions = decisions;
            }

            public boolean isActionItems() {
                return actionItems;
            }

            public void setActionItems(boolean actionItems) {
                this.actionItems = actionItems;
            }

            public boolean isAdditionalNotes() {
                return additionalNotes;
            }

            public void setAdditionalNotes(boolean additionalNotes) {
                this.additionalNotes = additionalNotes;
            }
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

        public IncludeSections getIncludeSections() {
            return includeSections;
        }

        public void setIncludeSections(IncludeSections includeSections) {
            this.includeSections = includeSections;
        }

        public String getPdfFooter() {
            return pdfFooter;
        }

        public void setPdfFooter(String pdfFooter) {
            this.pdfFooter = pdfFooter;
        }
    }

    // Root getters and setters
    public MeetingInfo getMeetingInfo() {
        return meetingInfo;
    }

    public void setMeetingInfo(MeetingInfo meetingInfo) {
        this.meetingInfo = meetingInfo;
    }

    public List<Participant> getParticipants() {
        return participants;
    }

    public void setParticipants(List<Participant> participants) {
        this.participants = participants;
    }

    public List<Participant> getAbsentParticipants() {
        return absentParticipants;
    }

    public void setAbsentParticipants(List<Participant> absentParticipants) {
        this.absentParticipants = absentParticipants;
    }

    public List<AgendaItem> getAgenda() {
        return agenda;
    }

    public void setAgenda(List<AgendaItem> agenda) {
        this.agenda = agenda;
    }

    public List<Decision> getDecisions() {
        return decisions;
    }

    public void setDecisions(List<Decision> decisions) {
        this.decisions = decisions;
    }

    public List<ActionItem> getActionItems() {
        return actionItems;
    }

    public void setActionItems(List<ActionItem> actionItems) {
        this.actionItems = actionItems;
    }

    public String getAdditionalNotes() {
        return additionalNotes;
    }

    public void setAdditionalNotes(String additionalNotes) {
        this.additionalNotes = additionalNotes;
    }

    public OutputPreferences getOutputPreferences() {
        return outputPreferences;
    }

    public void setOutputPreferences(OutputPreferences outputPreferences) {
        this.outputPreferences = outputPreferences;
    }
}
