package com.ai.application.Services;

import com.ai.application.Repositories.ContactRepository;
import com.ai.application.Repositories.GroupRepository;
import com.ai.application.model.Entity.Contact;
import com.ai.application.model.Entity.Group;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for managing groups.
 */
@Service
public class GroupService {

    private static final Logger logger = LoggerFactory.getLogger(GroupService.class);

    @Autowired
    private GroupRepository groupRepository;

    @Autowired
    private ContactRepository contactRepository;

    /**
     * Get all groups for a user.
     */
    public List<Map<String, Object>> getGroups(String userId) {
        List<Group> groups = groupRepository.findByUserId(userId);

        return groups.stream().map(group -> {
            Map<String, Object> groupData = new HashMap<>();
            groupData.put("id", group.getId());
            groupData.put("name", group.getName());
            groupData.put("description", group.getDescription());
            groupData.put("memberCount", group.getMemberCount());
            groupData.put("createdAt", group.getCreatedAt());
            groupData.put("updatedAt", group.getUpdatedAt());

            // Get member details (preview)
            List<Contact> members = getGroupMembers(group);
            groupData.put("memberPreview", members.stream()
                    .limit(3)
                    .map(c -> Map.of("id", c.getId(), "name", c.getName(), "email", c.getEmail()))
                    .collect(Collectors.toList()));

            // Count QuickFlow users
            long quickflowCount = members.stream().filter(Contact::isUsesQuickFlow).count();
            groupData.put("quickflowMemberCount", quickflowCount);

            return groupData;
        }).collect(Collectors.toList());
    }

    /**
     * Get a specific group with full member details.
     */
    public Map<String, Object> getGroup(String groupId) {
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new NoSuchElementException("Group not found"));

        Map<String, Object> groupData = new HashMap<>();
        groupData.put("id", group.getId());
        groupData.put("name", group.getName());
        groupData.put("description", group.getDescription());
        groupData.put("memberCount", group.getMemberCount());
        groupData.put("createdAt", group.getCreatedAt());
        groupData.put("updatedAt", group.getUpdatedAt());

        // Get full member details
        List<Contact> members = getGroupMembers(group);
        groupData.put("members", members);

        return groupData;
    }

    /**
     * Create a new group.
     */
    public Group createGroup(String userId, String name, String description, List<String> memberIds) {
        // Check for duplicate name
        Optional<Group> existing = groupRepository.findByUserIdAndName(userId, name);
        if (existing.isPresent()) {
            throw new IllegalArgumentException("A group with this name already exists");
        }

        Group group = new Group(userId, name);
        group.setDescription(description);

        if (memberIds != null && !memberIds.isEmpty()) {
            group.setMemberIds(new ArrayList<>(memberIds));
        }

        return groupRepository.save(group);
    }

    /**
     * Update a group.
     */
    public Group updateGroup(String groupId, String name, String description, List<String> memberIds) {
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new NoSuchElementException("Group not found"));

        if (name != null && !name.equals(group.getName())) {
            // Check for duplicate name
            Optional<Group> existing = groupRepository.findByUserIdAndName(group.getUserId(), name);
            if (existing.isPresent() && !existing.get().getId().equals(groupId)) {
                throw new IllegalArgumentException("A group with this name already exists");
            }
            group.setName(name);
        }

        if (description != null) {
            group.setDescription(description);
        }

        if (memberIds != null) {
            group.setMemberIds(new ArrayList<>(memberIds));
        }

        group.setUpdatedAt(LocalDateTime.now());
        return groupRepository.save(group);
    }

    /**
     * Delete a group.
     */
    public void deleteGroup(String groupId) {
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new NoSuchElementException("Group not found"));

        groupRepository.delete(group);
    }

    /**
     * Add members to a group.
     */
    public Group addMembers(String groupId, List<String> memberIds) {
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new NoSuchElementException("Group not found"));

        for (String memberId : memberIds) {
            group.addMember(memberId);
        }

        return groupRepository.save(group);
    }

    /**
     * Remove a member from a group.
     */
    public Group removeMember(String groupId, String memberId) {
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new NoSuchElementException("Group not found"));

        group.removeMember(memberId);
        return groupRepository.save(group);
    }

    /**
     * Get groups containing a specific contact.
     */
    public List<Group> getGroupsForContact(String userId, String contactId) {
        return groupRepository.findByUserIdAndMemberIdsContaining(userId, contactId);
    }

    /**
     * Get group count for a user.
     */
    public long getGroupCount(String userId) {
        return groupRepository.countByUserId(userId);
    }

    /**
     * Search groups by name for autocomplete.
     */
    public List<Map<String, Object>> searchGroups(String userId, String query, int limit) {
        List<Group> groups = groupRepository.findByUserId(userId);

        String lowerQuery = query.toLowerCase();
        return groups.stream()
                .filter(group -> group.getName().toLowerCase().contains(lowerQuery))
                .limit(limit)
                .map(group -> {
                    Map<String, Object> groupData = new HashMap<>();
                    groupData.put("id", group.getId());
                    groupData.put("name", group.getName());
                    groupData.put("memberCount", group.getMemberCount());

                    // Get full member details for selection
                    List<Contact> members = getGroupMembers(group);
                    groupData.put("members", members);

                    return groupData;
                })
                .collect(Collectors.toList());
    }

    /**
     * Get full member details for a group.
     */
    private List<Contact> getGroupMembers(Group group) {
        if (group.getMemberIds() == null || group.getMemberIds().isEmpty()) {
            return Collections.emptyList();
        }

        List<Contact> members = new ArrayList<>();
        for (String memberId : group.getMemberIds()) {
            contactRepository.findById(memberId).ifPresent(contact -> {
                if (!contact.isDeleted()) {
                    members.add(contact);
                }
            });
        }

        return members;
    }
}
