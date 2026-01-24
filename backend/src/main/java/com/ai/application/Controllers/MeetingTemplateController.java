package com.ai.application.Controllers;

import com.ai.application.Services.MeetingTemplateService;
import com.ai.application.model.DTO.CreateMeetingTemplateRequest;
import com.ai.application.model.DTO.UpdateMeetingTemplateRequest;
import com.ai.application.model.Entity.MeetingTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/meeting-templates")
@CrossOrigin(origins = "http://localhost:5173", allowCredentials = "true")
public class MeetingTemplateController {

    @Autowired
    private MeetingTemplateService meetingTemplateService;

    private String getUserId(Principal principal) {
        if (principal == null) {
            throw new RuntimeException("User not authenticated");
        }
        return principal.getName(); // Supabase user ID
    }

    @PostMapping
    public ResponseEntity<?> createTemplate(@RequestBody CreateMeetingTemplateRequest request, Principal principal) {
        try {
            String userId = getUserId(principal);
            MeetingTemplate template = meetingTemplateService.saveTemplate(
                    userId,
                    request.getName(),
                    request.getDescription(),
                    request.getTemplateData());
            return ResponseEntity.ok(Map.of("success", true, "template", template));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("success", false, "message", "Failed to create template: " + e.getMessage()));
        }
    }

    @GetMapping
    public ResponseEntity<?> getUserTemplates(Principal principal) {
        try {
            String userId = getUserId(principal);
            List<MeetingTemplate> templates = meetingTemplateService.getUserTemplates(userId);
            return ResponseEntity.ok(Map.of("success", true, "templates", templates));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("success", false, "message", "Failed to fetch templates: " + e.getMessage()));
        }
    }

    @GetMapping("/{templateId}")
    public ResponseEntity<?> getTemplate(@PathVariable String templateId, Principal principal) {
        try {
            String userId = getUserId(principal);
            MeetingTemplate template = meetingTemplateService.getTemplate(userId, templateId);
            return ResponseEntity.ok(Map.of("success", true, "template", template));
        } catch (RuntimeException e) {
            // covers not found or unauthorized
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("success", false, "message", "Error getting template"));
        }
    }

    @PutMapping("/{templateId}")
    public ResponseEntity<?> updateTemplate(
            @PathVariable String templateId,
            @RequestBody UpdateMeetingTemplateRequest request,
            Principal principal) {
        try {
            String userId = getUserId(principal);
            MeetingTemplate template = meetingTemplateService.updateTemplate(
                    userId,
                    templateId,
                    request.getName(),
                    request.getDescription(),
                    request.getTemplateData());
            return ResponseEntity.ok(Map.of("success", true, "template", template));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("success", false, "message", "Failed to update template"));
        }
    }

    @DeleteMapping("/{templateId}")
    public ResponseEntity<?> deleteTemplate(@PathVariable String templateId, Principal principal) {
        try {
            String userId = getUserId(principal);
            meetingTemplateService.deleteTemplate(userId, templateId);
            return ResponseEntity.ok(Map.of("success", true, "message", "Template deleted"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("success", false, "message", "Failed to delete template"));
        }
    }

    @PostMapping("/{templateId}/track-usage")
    public ResponseEntity<?> trackUsage(@PathVariable String templateId, Principal principal) {
        try {
            String userId = getUserId(principal);
            meetingTemplateService.trackUsage(userId, templateId);
            return ResponseEntity.ok(Map.of("success", true));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("success", false, "message", "Failed to track usage"));
        }
    }
}
