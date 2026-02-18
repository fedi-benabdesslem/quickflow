package com.ai.application.Controllers;

import com.ai.application.model.Entity.Email;
import com.ai.application.Repositories.EmailRepository;
import com.ai.application.model.TemplateType;
import com.ai.application.model.DTO.EmailRequest;
import com.ai.application.model.DTO.TemplateResponse;
import com.ai.application.Services.TemplateService;
import com.ai.application.Services.EmailProviderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/email")
public class EmailController {

    @Autowired
    private EmailRepository emailRepository;

    @Autowired
    private TemplateService templateService;

    @Autowired
    private EmailProviderService emailProviderService;

    @PostMapping("/send")
    public ResponseEntity<?> sendEmail(@RequestBody EmailRequest templateReq, Principal principal) {
        if (templateReq.getRecipients() == null || templateReq.getRecipients().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of(
                    "status", "error",
                    "message", "Recipients cannot be empty"));
        }

        String input = templateReq.getInput();

        // Get user info from JWT token (Principal)
        String userId = "anonymous";
        String senderEmail = "anonymous@quickflow.com";

        if (principal != null) {
            userId = principal.getName(); // This is the user's ID from Keycloak
            senderEmail = userId + "@quickflow.com";
        }

        // Set request fields
        templateReq.setSenderEmail(senderEmail);
        templateReq.setSenderId(userId);
        templateReq.setTemplateType(TemplateType.email);
        if (templateReq.getBulletPoints() == null) {
            templateReq.setBulletPoints(List.of());
        }

        // Generate content
        TemplateResponse response = templateService.processTemplate(templateReq);
        String generatedContent = response.getGeneratedContent();
        String subject = response.getSubject() != null ? (String) response.getSubject() : "Draft Email";

        if (generatedContent == null || generatedContent.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of(
                    "status", "error",
                    "message", "Failed to generate email content"));
        }

        // Save DRAFT: request fields only (no generated yet)
        Email draftEmail = new Email(templateReq.getRecipients(), input, userId, userId, senderEmail);
        draftEmail.setStatus("draft");
        Email savedEmail = emailRepository.save(draftEmail);

        // Return generated for review (JS expects these keys)
        return ResponseEntity.ok(Map.of(
                "status", "success",
                "message", "Email generated and saved successfully",
                "emailId", savedEmail.getId(),
                "generatedContent", generatedContent,
                "subject", subject));
    }

    @PostMapping("/send-final")
    public ResponseEntity<?> sendFinalEmail(@RequestBody Map<String, Object> body, Principal principal) {
        String id = (String) body.get("id");
        String finalSubject = (String) body.get("subject");
        String finalContent = (String) body.get("content");
        @SuppressWarnings("unchecked")
        List<String> finalRecipients = (List<String>) body.get("recipients");

        if (principal == null) {
            return ResponseEntity.status(401).body(Map.of(
                    "status", "error",
                    "message", "Not authenticated"));
        }

        Optional<Email> optionalEmail = emailRepository.findById(id);
        if (optionalEmail.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("status", "error", "message", "Draft not found"));
        }

        Email email = optionalEmail.get();
        email.setRecipients(finalRecipients != null ? finalRecipients : email.getRecipients());
        email.setSubject(finalSubject);
        email.setGeneratedContent(finalContent);

        // Try sending via OAuth provider first
        String userId = principal.getName();
        String recipients = String.join(", ", email.getRecipients());

        // Convert plain text to HTML if it's not already
        String htmlContent = finalContent;
        if (!finalContent.trim().startsWith("<")) {
            htmlContent = "<p>" + finalContent.replace("\n", "</p><p>") + "</p>";
        }

        EmailProviderService.SendResult result = emailProviderService.sendEmail(
                userId, recipients, email.getSubject(), htmlContent);

        if (result.isSuccess()) {
            email.setStatus("sent");
            email.setSentAt(LocalDateTime.now());
            emailRepository.save(email);
            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "message", "Email sent successfully"));
        } else if (result.isSmtpNotConfigured()) {
            email.setStatus("draft");
            emailRepository.save(email);
            return ResponseEntity.ok(Map.of(
                    "status", "smtp_not_configured",
                    "message", result.getMessage()));
        } else if (result.isUnsupportedProvider()) {
            // Email/password user - show toast message
            email.setStatus("draft");
            emailRepository.save(email);
            return ResponseEntity.ok(Map.of(
                    "status", "unsupported",
                    "message", result.getMessage()));
        } else if (result.requiresReauth()) {
            return ResponseEntity.status(401).body(Map.of(
                    "status", "reauth_required",
                    "message", result.getMessage()));
        } else {
            return ResponseEntity.status(500).body(Map.of(
                    "status", "error",
                    "message", result.getMessage()));
        }
    }
}