package com.ai.application.model.DTO;

import com.ai.application.model.StructuredModeRequest;
import java.util.List;

public class TemplateData {
    private MeetingInfo meetingInfo;
    private List<Participant> participants;
    private List<AgendaItem> agendaStructure;
    private StructuredModeRequest.OutputPreferences outputPreferences;

    public static class MeetingInfo {
        private String title;
        private Integer defaultDuration; // minutes
        private String defaultStartTime;
        private String location;
        private String organizer;

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public Integer getDefaultDuration() {
            return defaultDuration;
        }

        public void setDefaultDuration(Integer defaultDuration) {
            this.defaultDuration = defaultDuration;
        }

        public String getDefaultStartTime() {
            return defaultStartTime;
        }

        public void setDefaultStartTime(String defaultStartTime) {
            this.defaultStartTime = defaultStartTime;
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
        private String name;
        private String email;
        private String role;

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
    }

    public static class AgendaItem {
        private String title;
        private String objective;

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
    }

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

    public List<AgendaItem> getAgendaStructure() {
        return agendaStructure;
    }

    public void setAgendaStructure(List<AgendaItem> agendaStructure) {
        this.agendaStructure = agendaStructure;
    }

    public StructuredModeRequest.OutputPreferences getOutputPreferences() {
        return outputPreferences;
    }

    public void setOutputPreferences(StructuredModeRequest.OutputPreferences outputPreferences) {
        this.outputPreferences = outputPreferences;
    }
}
