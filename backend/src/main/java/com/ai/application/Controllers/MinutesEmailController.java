package com.ai.application.Controllers;

import com.ai.application.Services.EmailProviderService;
import com.ai.application.Services.GridFsService;
import com.ai.application.model.DTO.MinutesEmailRequest;
import com.ai.application.model.Entity.Meeting;
import com.ai.application.Repositories.MeetingRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/minutes")
public class MinutesEmailController {

    private final EmailProviderService emailProviderService;
    private final GridFsService gridFsService;
    private final MeetingRepository meetingRepository;

    @Autowired
    public MinutesEmailController(EmailProviderService emailProviderService, GridFsService gridFsService,
            MeetingRepository meetingRepository) {
        this.emailProviderService = emailProviderService;
        this.gridFsService = gridFsService;
        this.meetingRepository = meetingRepository;
    }

    @PostMapping("/send")
    public ResponseEntity<?> sendMinutesEmail(@RequestBody MinutesEmailRequest request, Principal principal) {
        // 1. Authenticate user
        if (principal == null) {
            return ResponseEntity.status(401).body(Map.of(
                    "status", "error",
                    "code", "unauthorized",
                    "message", "Not authenticated"));
        }
        String supabaseId = principal.getName();

        // 2. Validate request
        if (request.getRecipients() == null || request.getRecipients().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of(
                    "status", "error",
                    "message", "At least one recipient is required"));
        }

        if (request.getPdfFileId() == null || request.getPdfFileId().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of(
                    "status", "error",
                    "message", "PDF file ID is required"));
        }

        // 3. Retrieve PDF
        byte[] pdfBytes = gridFsService.getFile(request.getPdfFileId());
        if (pdfBytes == null) {
            return ResponseEntity.badRequest().body(Map.of(
                    "status", "error",
                    "message", "PDF file not found"));
        }

        // Generate filename
        String title = "Meeting_Minutes";
        String dateStr = "Unknown_Date";
        if (request.getMeetingMetadata() != null) {
            title = request.getMeetingMetadata().getOrDefault("title", "Meeting_Minutes");
            dateStr = request.getMeetingMetadata().getOrDefault("date", "Unknown_Date");
        }
        String filename = "Meeting_Minutes_" + dateStr + "_" + title.replaceAll("[^a-zA-Z0-9]", "_") + ".pdf";

        // 4. Build email body (use custom message or specific template)
        // For now, we wrap the custom body in simple HTML if provided, or use a default
        String htmlBody = request.getBody();
        if (htmlBody == null || htmlBody.trim().isEmpty()) {
            htmlBody = String.format(
                    "<p>Please find attached the meeting minutes for <strong>%s</strong> (%s).</p>",
                    title, dateStr);
        } else {
            // Basic newline to br conversion if it looks like plain text
            if (!htmlBody.contains("<") && !htmlBody.contains(">")) {
                htmlBody = "<p>" + htmlBody.replace("\n", "<br>") + "</p>";
            }
        }

        String subject = request.getSubject();
        if (subject == null || subject.isEmpty()) {
            subject = "Meeting Minutes: " + title;
        }

        // 5. Send email
        String recipientsStr = String.join(",", request.getRecipients());
        EmailProviderService.SendResult result = emailProviderService.sendEmailWithAttachment(
                supabaseId,
                recipientsStr,
                subject,
                htmlBody,
                pdfBytes,
                filename);

        // 6. Handle result
        if (result.isSuccess()) {
            // 7. Save meeting to database for history tracking
            try {
                Meeting meeting = new Meeting();
                meeting.setSubject(title);
                meeting.setPdfFileId(request.getPdfFileId());
                meeting.setSentAt(LocalDateTime.now());
                meeting.setStatus("sent");
                meeting.setPeople(request.getRecipients());
                meeting.setUserId(supabaseId); // NEW: Save user ID

                // Parse date if available
                if (request.getMeetingMetadata() != null && request.getMeetingMetadata().containsKey("date")) {
                    try {
                        LocalDate meetingDate = LocalDate.parse(request.getMeetingMetadata().get("date"));
                        meeting.setDate(LocalDateTime.of(meetingDate, LocalTime.MIDNIGHT));
                    } catch (Exception e) {
                        meeting.setDate(LocalDateTime.now());
                    }
                } else {
                    meeting.setDate(LocalDateTime.now());
                }

                meetingRepository.save(meeting);
            } catch (Exception e) {
                // Log but don't fail the response - email was sent successfully
                System.err.println("Failed to save meeting to history: " + e.getMessage());
            }

            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "message", "Meeting minutes sent successfully",
                    "recipientCount", request.getRecipients().size()));
        } else if (result.isSmtpNotConfigured()) {
            return ResponseEntity.ok(Map.of(
                    "status", "smtp_not_configured",
                    "message", result.getMessage()));
        } else if (result.isUnsupportedProvider()) {
            return ResponseEntity.ok(Map.of(
                    "status", "unsupported",
                    "error", "unsupported_provider",
                    "message", result.getMessage()));
        } else if (result.requiresReauth()) {
            return ResponseEntity.status(401).body(Map.of(
                    "status", "error",
                    "code", "token_expired",
                    "message", result.getMessage()));
        } else {
            return ResponseEntity.status(500).body(Map.of(
                    "status", "error",
                    "code", "send_failed",
                    "message", result.getMessage()));
        }
    }
}
