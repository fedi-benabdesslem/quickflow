package com.ai.application.Controllers;

import com.ai.application.Services.EmailProviderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.Map;

/**
 * Controller for handling tech support email reports.
 * Sends user problem reports to QuickFlow support team.
 */
@RestController
@RequestMapping("/api/support")
public class SupportController {

    @Autowired
    private EmailProviderService emailProviderService;

    /**
     * Send a support report email from the user to the support team.
     * 
     * @param body      Contains the "message" field with the user's problem
     *                  description
     * @param principal Authenticated user
     * @return Response indicating success or failure
     */
    @PostMapping("/report")
    public ResponseEntity<?> sendSupportReport(@RequestBody Map<String, Object> body, Principal principal) {
        if (principal == null) {
            return ResponseEntity.status(401).body(Map.of(
                    "status", "error",
                    "message", "Not authenticated"));
        }

        String message = (String) body.get("message");
        if (message == null || message.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of(
                    "status", "error",
                    "message", "Message cannot be empty"));
        }

        String supabaseId = principal.getName();

        // Fixed support email addresses
        String supportRecipients = "fadib.abdesslem2004@gmail.com, oussemabenameur9@gmail.com";
        String subject = "user report on quickflow";

        // Convert message to HTML format
        String htmlContent = "<p>" + message.replace("\n", "</p><p>") + "</p>";

        EmailProviderService.SendResult result = emailProviderService.sendEmail(
                supabaseId, supportRecipients, subject, htmlContent);

        if (result.isSuccess()) {
            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "message", "Support report sent successfully"));
        } else if (result.isUnsupportedProvider()) {
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
