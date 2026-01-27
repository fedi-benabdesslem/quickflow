package com.ai.application.Controllers;

import com.ai.application.Services.GroupService;
import com.ai.application.model.Entity.Group;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REST Controller for group management operations.
 */
@RestController
@RequestMapping("/api/groups")
@CrossOrigin(origins = "http://localhost:5173", allowCredentials = "true")
public class GroupController {

    private static final Logger logger = LoggerFactory.getLogger(GroupController.class);

    @Autowired
    private GroupService groupService;

    /**
     * Get all groups for the current user.
     */
    @GetMapping
    public ResponseEntity<?> getGroups(Authentication authentication) {
        try {
            String userId = authentication.getName();
            List<Map<String, Object>> groups = groupService.getGroups(userId);
            return ResponseEntity.ok(Map.of("groups", groups, "total", groups.size()));
        } catch (Exception e) {
            logger.error("Error getting groups", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to get groups"));
        }
    }

    /**
     * Get a specific group with member details.
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> getGroup(@PathVariable String id) {
        try {
            Map<String, Object> group = groupService.getGroup(id);
            return ResponseEntity.ok(group);
        } catch (Exception e) {
            logger.error("Error getting group", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to get group"));
        }
    }

    /**
     * Create a new group.
     */
    @PostMapping
    public ResponseEntity<?> createGroup(
            Authentication authentication,
            @RequestBody CreateGroupRequest request) {
        try {
            String userId = authentication.getName();

            Group group = groupService.createGroup(
                    userId,
                    request.getName(),
                    request.getDescription(),
                    request.getMemberIds());

            return ResponseEntity.ok(group);

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            logger.error("Error creating group", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to create group"));
        }
    }

    /**
     * Update a group.
     */
    @PutMapping("/{id}")
    public ResponseEntity<?> updateGroup(
            @PathVariable String id,
            @RequestBody UpdateGroupRequest request) {
        try {
            Group group = groupService.updateGroup(
                    id,
                    request.getName(),
                    request.getDescription(),
                    request.getMemberIds());

            return ResponseEntity.ok(group);

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            logger.error("Error updating group", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to update group"));
        }
    }

    /**
     * Delete a group.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteGroup(@PathVariable String id) {
        try {
            groupService.deleteGroup(id);
            return ResponseEntity.ok(Map.of("success", true, "message", "Group deleted"));
        } catch (Exception e) {
            logger.error("Error deleting group", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to delete group"));
        }
    }

    /**
     * Add members to a group.
     */
    @PostMapping("/{id}/members")
    public ResponseEntity<?> addMembers(
            @PathVariable String id,
            @RequestBody AddMembersRequest request) {
        try {
            Group group = groupService.addMembers(id, request.getMemberIds());
            return ResponseEntity.ok(group);
        } catch (Exception e) {
            logger.error("Error adding members", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to add members"));
        }
    }

    /**
     * Remove a member from a group.
     */
    @DeleteMapping("/{id}/members/{memberId}")
    public ResponseEntity<?> removeMember(
            @PathVariable String id,
            @PathVariable String memberId) {
        try {
            Group group = groupService.removeMember(id, memberId);
            return ResponseEntity.ok(group);
        } catch (Exception e) {
            logger.error("Error removing member", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to remove member"));
        }
    }

    /**
     * Search groups by name for autocomplete.
     */
    @GetMapping("/search")
    public ResponseEntity<?> searchGroups(
            Authentication authentication,
            @RequestParam String q,
            @RequestParam(defaultValue = "5") int limit) {
        try {
            String userId = authentication.getName();
            List<Map<String, Object>> groups = groupService.searchGroups(userId, q, limit);
            return ResponseEntity.ok(groups);
        } catch (Exception e) {
            logger.error("Error searching groups", e);
            return ResponseEntity.ok(List.of());
        }
    }

    /**
     * Get group count for sidebar badge.
     */
    @GetMapping("/count")
    public ResponseEntity<?> getGroupCount(Authentication authentication) {
        try {
            String userId = authentication.getName();
            long count = groupService.getGroupCount(userId);
            return ResponseEntity.ok(Map.of("count", count));
        } catch (Exception e) {
            return ResponseEntity.ok(Map.of("count", 0));
        }
    }

    // Request DTOs
    public static class CreateGroupRequest {
        private String name;
        private String description;
        private List<String> memberIds;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public List<String> getMemberIds() {
            return memberIds;
        }

        public void setMemberIds(List<String> memberIds) {
            this.memberIds = memberIds;
        }
    }

    public static class UpdateGroupRequest {
        private String name;
        private String description;
        private List<String> memberIds;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public List<String> getMemberIds() {
            return memberIds;
        }

        public void setMemberIds(List<String> memberIds) {
            this.memberIds = memberIds;
        }
    }

    public static class AddMembersRequest {
        private List<String> memberIds;

        public List<String> getMemberIds() {
            return memberIds;
        }

        public void setMemberIds(List<String> memberIds) {
            this.memberIds = memberIds;
        }
    }
}
