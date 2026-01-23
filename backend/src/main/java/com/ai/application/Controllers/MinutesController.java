package com.ai.application.Controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.Map;

/**
 * Placeholder controller for meeting minutes drafts.
 * Full implementation will be added in later steps.
 */
@RestController
@RequestMapping("/api/minutes")
public class MinutesController {

    /**
     * Save quick mode draft data.
     * For now, just returns success - full implementation in Step 2.
     */
    @PostMapping("/draft/quick")
    public ResponseEntity<?> saveDraftQuick(@RequestBody Map<String, Object> data, Principal principal) {
        String userId = principal != null ? principal.getName() : "anonymous";

        // Log the received data for debugging
        System.out.println("Quick mode draft received from user: " + userId);
        System.out.println("Data: " + data);

        return ResponseEntity.ok(Map.of(
                "status", "success",
                "message", "Quick mode data received successfully",
                "userId", userId));
    }

    /**
     * Save structured mode draft data.
     * For now, just returns success - full implementation in Step 2.
     */
    @PostMapping("/draft/structured")
    public ResponseEntity<?> saveDraftStructured(@RequestBody Map<String, Object> data, Principal principal) {
        String userId = principal != null ? principal.getName() : "anonymous";

        // Log the received data for debugging
        System.out.println("Structured mode draft received from user: " + userId);
        System.out.println("Data: " + data);

        return ResponseEntity.ok(Map.of(
                "status", "success",
                "message", "Structured mode data received successfully",
                "userId", userId));
    }
}
