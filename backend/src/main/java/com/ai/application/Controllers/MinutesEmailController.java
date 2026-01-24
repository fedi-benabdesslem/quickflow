package com.ai.application.Controllers;

import com.ai.application.Services.EmailProviderService;
import com.ai.application.Services.GridFsService;
import com.ai.application.model.DTO.MinutesEmailRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.Map;

@RestController
@RequestMapping("/api/minutes")
public class MinutesEmailController {

    private final EmailProviderService emailProviderService;
    private final GridFsService gridFsService;

    @Autowired
    public MinutesEmailController(EmailProviderService emailProviderService, GridFsService gridFsService) {
        this.emailProviderService = emailProviderService;
        this.gridFsService = gridFsService;
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
        String date = "Unknown_Date";
        if (request.getMeetingMetadata() != null) {
            title = request.getMeetingMetadata().getOrDefault("title", "Meeting_Minutes");
            date = request.getMeetingMetadata().getOrDefault("date", "Unknown_Date");
        }
        String filename = "Meeting_Minutes_" + date + "_" + title.replaceAll("[^a-zA-Z0-9]", "_") + ".pdf";

        // 4. Build email body (use custom message or specific template)
        // For now, we wrap the custom body in simple HTML if provided, or use a default
        String htmlBody = request.getBody();
        if (htmlBody == null || htmlBody.trim().isEmpty()) {
            htmlBody = String.format(
                    "<p>Please find attached the meeting minutes for <strong>%s</strong> (%s).</p>",
                    title, date);
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
            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "message", "Meeting minutes sent successfully",
                    "recipientCount", request.getRecipients().size()));
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
