package com.ai.application.Controllers;

import com.ai.application.Services.ContactService;
import com.ai.application.Services.GoogleContactsService;
import com.ai.application.Services.MicrosoftContactsService;
import com.ai.application.Services.TokenRefreshService;
import com.ai.application.Services.TokenStorageService;
import com.ai.application.model.Entity.Contact;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * REST Controller for contact management operations.
 */
@RestController
@RequestMapping("/api/contacts")
@CrossOrigin(origins = "http://localhost:5173", allowCredentials = "true")
public class ContactController {

    private static final Logger logger = LoggerFactory.getLogger(ContactController.class);

    @Autowired
    private ContactService contactService;

    @Autowired
    private GoogleContactsService googleContactsService;

    @Autowired
    private MicrosoftContactsService microsoftContactsService;

    @Autowired
    private TokenRefreshService tokenRefreshService;

    @Autowired
    private TokenStorageService tokenStorageService;

    @Autowired
    private com.ai.application.Services.QuickFlowDetectionService quickFlowDetectionService;

    /**
     * Get all contacts for the current user with optional filtering.
     */
    @GetMapping
    public ResponseEntity<?> getContacts(
            Authentication authentication,
            @RequestParam(required = false, defaultValue = "all") String filter,
            @RequestParam(required = false) String source,
            @RequestParam(required = false, defaultValue = "name") String sortBy) {
        try {
            String userId = authentication.getName();
            List<Contact> contacts = contactService.getContacts(userId, filter, source, sortBy);

            Map<String, Object> response = new HashMap<>();
            response.put("contacts", contacts);
            response.put("total", contacts.size());
            response.put("lastSync", contactService.getLastSyncTime(userId, "google"));

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error getting contacts", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to get contacts"));
        }
    }

    /**
     * Search contacts by name or email (for autocomplete).
     */
    @GetMapping("/search")
    public ResponseEntity<?> searchContacts(
            Authentication authentication,
            @RequestParam String q,
            @RequestParam(required = false, defaultValue = "10") int limit) {
        try {
            String userId = authentication.getName();
            List<Contact> results = contactService.searchContacts(userId, q, limit);
            return ResponseEntity.ok(results);
        } catch (Exception e) {
            logger.error("Error searching contacts", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to search contacts"));
        }
    }

    /**
     * Get recently used contacts (for autocomplete suggestions).
     */
    @GetMapping("/recent")
    public ResponseEntity<?> getRecentContacts(
            Authentication authentication,
            @RequestParam(required = false, defaultValue = "5") int limit) {
        try {
            String userId = authentication.getName();
            List<Contact> contacts = contactService.getRecentContacts(userId, limit);
            return ResponseEntity.ok(contacts);
        } catch (Exception e) {
            logger.error("Error getting recent contacts", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to get recent contacts"));
        }
    }

    /**
     * Import contacts from Google or Microsoft.
     */
    @PostMapping("/import")
    public ResponseEntity<?> importContacts(
            Authentication authentication,
            @RequestParam(required = false) String provider) {
        try {
            String userId = authentication.getName();
            logger.info("Import contacts requested for user {} from provider {}", userId, provider);

            // Get tokens
            TokenStorageService.DecryptedTokens tokens = tokenStorageService.getDecryptedTokens(userId);
            if (tokens == null) {
                return ResponseEntity.status(401)
                        .body(Map.of("error",
                                "No stored tokens found. Please sign out and sign in again to grant contacts permission."));
            }

            String detectedProvider = provider != null ? provider : tokens.getProvider();

            // Get fresh access token
            String accessToken = tokenRefreshService.refreshTokenIfNeeded(userId);
            if (accessToken == null) {
                return ResponseEntity.status(401)
                        .body(Map.of("error", "Failed to get valid access token. Please sign out and sign in again."));
            }

            List<Contact> contacts;

            try {
                if ("google".equalsIgnoreCase(detectedProvider)) {
                    contacts = googleContactsService.fetchContacts(accessToken);
                } else if ("microsoft".equalsIgnoreCase(detectedProvider) ||
                        "azure".equalsIgnoreCase(detectedProvider)) {
                    contacts = microsoftContactsService.fetchContacts(accessToken);
                } else {
                    return ResponseEntity.badRequest()
                            .body(Map.of("error",
                                    "Contact import is only supported for Google and Microsoft accounts"));
                }
            } catch (Exception e) {
                // Check if this is a scope/permission error
                String errorMsg = e.getMessage();
                if (errorMsg != null && (errorMsg.contains("403") || errorMsg.contains("forbidden") ||
                        errorMsg.contains("insufficient") || errorMsg.contains("scope"))) {
                    return ResponseEntity.status(403).body(Map.of(
                            "error",
                            "Permission denied. Please sign out and sign in again to grant contacts access permission."));
                }
                throw e;
            }

            // Import to database
            Map<String, Integer> result = contactService.importContacts(userId, contacts,
                    detectedProvider.toLowerCase());
            result.put("success", 1);

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            logger.error("Error importing contacts", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to import contacts"));
        }
    }

    /**
     * Sync contacts (re-import from provider).
     */
    @PostMapping("/sync")
    public ResponseEntity<?> syncContacts(Authentication authentication) {
        // Same as import
        return importContacts(authentication, null);
    }

    /**
     * Create a manual contact.
     */
    @PostMapping
    public ResponseEntity<?> createContact(
            Authentication authentication,
            @RequestBody CreateContactRequest request) {
        try {
            String userId = authentication.getName();

            Contact contact = contactService.createContact(
                    userId,
                    request.getName(),
                    request.getEmail(),
                    request.getPhone(),
                    request.getPhoto());

            return ResponseEntity.ok(contact);

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            logger.error("Error creating contact", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to create contact"));
        }
    }

    /**
     * Update a contact.
     */
    @PutMapping("/{id}")
    public ResponseEntity<?> updateContact(
            Authentication authentication,
            @PathVariable String id,
            @RequestBody UpdateContactRequest request) {
        try {
            String userId = authentication.getName();
            // Verify contact belongs to user
            var existing = contactService.getContactById(id);
            if (existing == null || !userId.equals(existing.getUserId())) {
                return ResponseEntity.status(403)
                        .body(Map.of("error", "Access denied"));
            }

            Contact contact = contactService.updateContact(
                    id,
                    request.getName(),
                    request.getEmail(),
                    request.getPhone(),
                    request.getPhoto(),
                    request.getGroups(),
                    request.getIsFavorite());

            return ResponseEntity.ok(contact);

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            logger.error("Error updating contact", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to update contact"));
        }
    }

    /**
     * Delete a contact.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteContact(Authentication authentication, @PathVariable String id) {
        try {
            String userId = authentication.getName();
            var existing = contactService.getContactById(id);
            if (existing == null || !userId.equals(existing.getUserId())) {
                return ResponseEntity.status(403)
                        .body(Map.of("error", "Access denied"));
            }
            contactService.deleteContact(id);
            return ResponseEntity.ok(Map.of("success", true, "message", "Contact deleted"));
        } catch (Exception e) {
            logger.error("Error deleting contact", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to delete contact"));
        }
    }

    /**
     * Toggle favorite status.
     */
    @PostMapping("/{id}/favorite")
    public ResponseEntity<?> toggleFavorite(Authentication authentication, @PathVariable String id) {
        try {
            String userId = authentication.getName();
            var existing = contactService.getContactById(id);
            if (existing == null || !userId.equals(existing.getUserId())) {
                return ResponseEntity.status(403)
                        .body(Map.of("error", "Access denied"));
            }
            Contact contact = contactService.toggleFavorite(id);
            return ResponseEntity.ok(contact);
        } catch (Exception e) {
            logger.error("Error toggling favorite", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to toggle favorite"));
        }
    }

    /**
     * Increment usage count (for tracking frequently used contacts).
     */
    @PostMapping("/{id}/use")
    public ResponseEntity<?> incrementUsage(@PathVariable String id) {
        try {
            contactService.incrementUsage(id);
            return ResponseEntity.ok(Map.of("success", true));
        } catch (Exception e) {
            logger.error("Error incrementing usage", e);
            return ResponseEntity.ok(Map.of("success", false));
        }
    }

    /**
     * Get contact count for sidebar badge.
     */
    @GetMapping("/count")
    public ResponseEntity<?> getContactCount(Authentication authentication) {
        try {
            String userId = authentication.getName();
            long count = contactService.getContactCount(userId);
            return ResponseEntity.ok(Map.of("count", count));
        } catch (Exception e) {
            return ResponseEntity.ok(Map.of("count", 0));
        }
    }

    /**
     * Trigger QuickFlow user detection for the user's contacts.
     */
    @PostMapping("/detect-quickflow")
    public ResponseEntity<?> detectQuickFlowUsers(Authentication authentication) {
        try {
            String userId = authentication.getName();
            int updated = quickFlowDetectionService.detectForUser(userId);
            return ResponseEntity.ok(Map.of("success", true, "updated", updated));
        } catch (Exception e) {
            logger.error("Error detecting QuickFlow users", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to detect QuickFlow users"));
        }
    }

    // Request DTOs
    public static class CreateContactRequest {
        private String name;
        private String email;
        private String phone;
        private String photo;

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

        public String getPhone() {
            return phone;
        }

        public void setPhone(String phone) {
            this.phone = phone;
        }

        public String getPhoto() {
            return photo;
        }

        public void setPhoto(String photo) {
            this.photo = photo;
        }
    }

    public static class UpdateContactRequest {
        private String name;
        private String email;
        private String phone;
        private String photo;
        private List<String> groups;
        private Boolean isFavorite;

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

        public String getPhone() {
            return phone;
        }

        public void setPhone(String phone) {
            this.phone = phone;
        }

        public String getPhoto() {
            return photo;
        }

        public void setPhoto(String photo) {
            this.photo = photo;
        }

        public List<String> getGroups() {
            return groups;
        }

        public void setGroups(List<String> groups) {
            this.groups = groups;
        }

        public Boolean getIsFavorite() {
            return isFavorite;
        }

        public void setIsFavorite(Boolean isFavorite) {
            this.isFavorite = isFavorite;
        }
    }
}
