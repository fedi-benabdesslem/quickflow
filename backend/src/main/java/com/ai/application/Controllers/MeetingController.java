package com.ai.application.Controllers;

import com.ai.application.model.Entity.Meeting;
import com.ai.application.Repositories.MeetingRepository;
import com.ai.application.model.TemplateType;
import com.ai.application.model.DTO.MeetingRequest;
import com.ai.application.model.DTO.TemplateResponse;
import com.ai.application.Services.TemplateService;
import com.ai.application.Services.PdfService;
import com.ai.application.Services.GridFsService;
import com.ai.application.Services.EmailProviderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/meeting")
public class MeetingController {

    private final MeetingRepository meetingRepository;
    private final TemplateService templateService;
    private final PdfService pdfService;
    private final GridFsService gridFsService;
    private final EmailProviderService emailProviderService;

    @Autowired
    public MeetingController(MeetingRepository meetingRepository, TemplateService templateService,
            PdfService pdfService, GridFsService gridFsService,
            EmailProviderService emailProviderService) {
        this.meetingRepository = meetingRepository;
        this.templateService = templateService;
        this.pdfService = pdfService;
        this.gridFsService = gridFsService;
        this.emailProviderService = emailProviderService;
    }

    @PostMapping("/generate")
    public ResponseEntity<?> generateMeeting(@RequestBody MeetingRequest request, java.security.Principal principal) {
        if (request.getPeople() == null || request.getPeople().isEmpty() ||
                request.getLocation() == null || request.getLocation().isEmpty() ||
                request.getTimeBegin() == null || request.getTimeEnd() == null ||
                request.getDate() == null || request.getSubject() == null || request.getSubject().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of(
                    "status", "error",
                    "message", "All fields are required"));
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
                    "message", "Invalid date/time format. Date: YYYY-MM-DD, Time: HH:mm"));
        }

        if (timeEnd.isBefore(timeBegin)) {
            return ResponseEntity.badRequest().body(Map.of(
                    "status", "error",
                    "message", "End time must be after start time"));
        }

        String userId = principal != null ? principal.getName() : "anonymous";
        request.setUserId(userId);
        request.setSenderId(userId);
        request.setSenderEmail(userId + "@placeholder.com");
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
                    "message", "Failed to generate meeting summary"));
        }

        Meeting draft = new Meeting(
                request.getPeople(),
                request.getLocation(),
                timeBegin,
                timeEnd,
                LocalDateTime.of(LocalDate.parse(request.getDate()), LocalTime.parse("00:00")),
                request.getSubject(),
                request.getDetails());
        draft.setStatus("draft");
        draft.setUserId(userId); // NEW: Save user ID
        Meeting saved = meetingRepository.save(draft);

        return ResponseEntity.ok(Map.of(
                "status", "success",
                "message", "Meeting generated and saved successfully",
                "meetingId", saved.getId(),
                "generatedContent", generatedContent,
                "subject", response.getSubject() != null ? response.getSubject() : request.getSubject()));
    }

    @PostMapping("/send-final")
    public ResponseEntity<?> sendFinalMeeting(@RequestBody Map<String, Object> body, Principal principal) {
        String id = (String) body.get("id");
        String finalSubject = (String) body.get("subject");
        String finalContent = (String) body.get("content");
        @SuppressWarnings("unchecked")
        List<String> finalPeople = (List<String>) body.get("people");
        String finalLocation = (String) body.get("location");
        String finalDate = (String) body.get("date");
        @SuppressWarnings("unchecked")
        List<String> recipients = (List<String>) body.get("recipients");

        if (principal == null) {
            return ResponseEntity.status(401).body(Map.of(
                    "status", "error",
                    "message", "Not authenticated"));
        }

        Optional<Meeting> optionalMeeting = meetingRepository.findById(id);
        if (optionalMeeting.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("status", "error", "message", "Draft not found"));
        }

        Meeting meeting = optionalMeeting.get();

        // Security check: Ensure user owns this meeting
        String currentUserId = principal.getName();
        if (meeting.getUserId() != null && !meeting.getUserId().equals(currentUserId)) {
            return ResponseEntity.status(403).body(Map.of(
                    "status", "error",
                    "message", "You do not have permission to modify this meeting"));
        }

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

        LocalDate meetingDate = LocalDate.now();
        if (finalDate != null) {
            try {
                meetingDate = LocalDate.parse(finalDate);
                meeting.setDate(LocalDateTime.of(meetingDate, LocalTime.parse("00:00")));
            } catch (Exception e) {
                // Keep existing date if parsing fails
            }
        }

        // Generate PDF if recipients are provided (for email sending)
        if (recipients != null && !recipients.isEmpty()) {
            String supabaseId = principal.getName();

            // Check if user can send emails
            if (!emailProviderService.canUserSendEmail(supabaseId)) {
                meeting.setStatus("draft");
                meetingRepository.save(meeting);
                return ResponseEntity.ok(Map.of(
                        "status", "unsupported",
                        "message", "Email sending not supported for your domain yet. Coming soon!"));
            }

            try {
                // Generate PDF
                String htmlContent = finalContent != null ? finalContent : meeting.getGeneratedContent();
                if (!htmlContent.trim().startsWith("<")) {
                    htmlContent = "<p>" + htmlContent.replace("\n", "</p><p>") + "</p>";
                }

                byte[] pdfBytes = pdfService.generateMeetingMinutesPdf(
                        meeting.getSubject(), meetingDate, htmlContent);
                String pdfFilename = pdfService.getMeetingMinutesFilename(meetingDate);

                // Store PDF in GridFS
                String pdfFileId = gridFsService.storePdf(pdfBytes, pdfFilename);
                meeting.setPdfFileId(pdfFileId);

                // Send email with PDF attachment
                String recipientString = String.join(", ", recipients);
                String emailSubject = "Meeting Minutes - " + meetingDate;
                String emailBody = "<p>Please find attached the meeting minutes from " + meetingDate + ".</p>";

                EmailProviderService.SendResult result = emailProviderService.sendEmailWithAttachment(
                        supabaseId, recipientString, emailSubject, emailBody, pdfBytes, pdfFilename);

                if (result.isSuccess()) {
                    meeting.setStatus("sent");
                    meeting.setSentAt(LocalDateTime.now());
                    meetingRepository.save(meeting);
                    return ResponseEntity.ok(Map.of(
                            "status", "success",
                            "message", "Meeting minutes sent successfully",
                            "pdfFileId", pdfFileId));
                } else {
                    meeting.setStatus("draft");
                    meetingRepository.save(meeting);
                    return ResponseEntity.ok(Map.of(
                            "status", result.isUnsupportedProvider() ? "unsupported" : "error",
                            "message", result.getMessage()));
                }
            } catch (Exception e) {
                return ResponseEntity.status(500).body(Map.of(
                        "status", "error",
                        "message", "Failed to process: " + e.getMessage()));
            }
        }

        // No recipients - just save the meeting
        meeting.setStatus("sent");
        meeting.setSentAt(LocalDateTime.now());
        meetingRepository.save(meeting);

        return ResponseEntity.ok(Map.of(
                "status", "success",
                "message", "Meeting summary saved successfully"));
    }
}