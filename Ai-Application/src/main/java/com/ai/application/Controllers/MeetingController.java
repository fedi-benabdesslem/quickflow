package com.ai.application.Controllers;

import com.ai.application.model.Entity.Meeting;
import com.ai.application.Repositories.MeetingRepository;
import com.ai.application.model.TemplateType;
import com.ai.application.model.DTO.MeetingRequest;
import com.ai.application.model.DTO.TemplateResponse;
import com.ai.application.Services.TemplateService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/meeting")
public class MeetingController {

    private final MeetingRepository meetingRepository;
    private final TemplateService templateService;

    @Autowired
    public MeetingController(MeetingRepository meetingRepository, TemplateService templateService) {
        this.meetingRepository = meetingRepository;
        this.templateService = templateService;
    }

    @PostMapping("/generate")
    public ResponseEntity<?> generateMeeting(@RequestBody MeetingRequest request) {
        if (request.getPeople() == null || request.getPeople().isEmpty() || 
            request.getLocation() == null || request.getLocation().isEmpty() ||
            request.getTimeBegin() == null || request.getTimeEnd() == null || 
            request.getDate() == null || request.getSubject() == null || request.getSubject().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of(
                    "status", "error",
                    "message", "All fields are required"
            ));
        }

        LocalDateTime timeBegin, timeEnd;
        try {
            LocalDate date = LocalDate.parse(request.getDate());
            LocalTime beginTime = LocalTime.parse(request.getTimeBegin());
            LocalTime endTime = LocalTime.parse(request.getTimeEnd());
            timeBegin = LocalDateTime.of(date, beginTime);
            timeEnd = LocalDateTime.of(date, endTime);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "status", "error",
                    "message", "Invalid date/time format. Date: YYYY-MM-DD, Time: HH:mm"
            ));
        }

        if (timeEnd.isBefore(timeBegin)) {
            return ResponseEntity.badRequest().body(Map.of(
                    "status", "error",
                    "message", "End time must be after start time"
            ));
        }

        request.setUserId("anonymous");
        request.setSenderId("anonymous");
        request.setSenderEmail("anonymous@example.com");
        request.setTemplateType(TemplateType.PV);
        if (request.getBulletPoints() == null) {
            request.setBulletPoints(List.of());
        }
        request.setTimeBegin(timeBegin.toString());
        request.setTimeEnd(timeEnd.toString());
        request.setDate(request.getDate());

        TemplateResponse response = templateService.processTemplate(request);
        String generatedContent = response.getGeneratedContent();
        if (generatedContent == null || generatedContent.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of(
                    "status", "error",
                    "message", "Failed to generate meeting summary"
            ));
        }

        Meeting draft = new Meeting(
                request.getPeople(),
                request.getLocation(),
                timeBegin,
                timeEnd,
                LocalDateTime.of(LocalDate.parse(request.getDate()), LocalTime.parse("00:00")),
                request.getSubject(),
                request.getDetails()
        );
        draft.setStatus("draft");
        Meeting saved = meetingRepository.save(draft);

        return ResponseEntity.ok(Map.of(
                "status", "success",
                "message", "Meeting generated and saved successfully",
                "meetingId", saved.getId(),
                "generatedContent", generatedContent,
                "subject", response.getSubject() != null ? response.getSubject() : request.getSubject()
        ));
    }
}