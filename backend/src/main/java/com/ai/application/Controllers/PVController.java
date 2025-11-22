package com.ai.application.Controllers;

import com.ai.application.model.Entity.Meeting;
import com.ai.application.Repositories.MeetingRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/pv")
public class PVController {

    private final MeetingRepository meetingRepository;

    @Autowired
    public PVController(MeetingRepository meetingRepository) {
        this.meetingRepository = meetingRepository;
    }

    @PostMapping("/send-final")
    public ResponseEntity<?> sendFinalPV(@RequestBody Map<String, Object> body) {
        String id = (String) body.get("id");
        String finalSubject = (String) body.get("subject");
        String finalContent = (String) body.get("content");
        @SuppressWarnings("unchecked")
        List<String> finalPeople = (List<String>) body.get("people");
        String finalLocation = (String) body.get("location");
        String finalDate = (String) body.get("date");

        Optional<Meeting> optionalMeeting = meetingRepository.findById(id);
        if (optionalMeeting.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("status", "error", "message", "Draft not found"));
        }

        Meeting meeting = optionalMeeting.get();
        if (finalPeople != null) {
            meeting.setPeople(finalPeople);
        }
        if (finalLocation != null) {
            meeting.setLocation(finalLocation);
        }
        if (finalSubject != null) {
            meeting.setSubject(finalSubject);
        }
        if (finalContent != null) {
            meeting.setGeneratedContent(finalContent);
        }
        if (finalDate != null) {
            try {
                LocalDate date = LocalDate.parse(finalDate);
                meeting.setDate(LocalDateTime.of(date, LocalTime.parse("00:00")));
            } catch (Exception e) {
                // Keep existing date if parsing fails
            }
        }
        meeting.setStatus("sent");
        meetingRepository.save(meeting);

        // TODO: Actually send meeting summary (e.g., via email or notification)

        return ResponseEntity.ok(Map.of(
                "status", "success",
                "message", "Meeting summary sent successfully"
        ));
    }
}

