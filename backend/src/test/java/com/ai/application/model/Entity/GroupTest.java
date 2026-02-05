package com.ai.application.model.Entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for Group entity.
 * 
 * Tests cover:
 * - Entity construction
 * - Member management (addMember, removeMember)
 * - Member count calculation
 * - Getter/setter functionality
 */
class GroupTest {

    @Nested
    @DisplayName("Constructor and Basic Properties")
    class ConstructorTests {

        @Test
        @DisplayName("Should create instance with default constructor")
        void createWithDefaultConstructor() {
            Group group = new Group();
            
            assertNull(group.getUserId());
            assertNull(group.getName());
            assertNull(group.getDescription());
            assertNotNull(group.getMemberIds());
            assertTrue(group.getMemberIds().isEmpty());
            assertNotNull(group.getCreatedAt());
            assertNotNull(group.getUpdatedAt());
        }

        @Test
        @DisplayName("Should create instance with parameterized constructor")
        void createWithParameterizedConstructor() {
            Group group = new Group("user-123", "Engineering Team");
            
            assertEquals("user-123", group.getUserId());
            assertEquals("Engineering Team", group.getName());
            assertNotNull(group.getCreatedAt());
            assertNotNull(group.getUpdatedAt());
        }
    }

    @Nested
    @DisplayName("addMember()")
    class AddMemberTests {

        @Test
        @DisplayName("Should add member to empty group")
        void addMemberToEmptyGroup() {
            Group group = new Group("user-123", "Test Group");
            
            group.addMember("contact-1");
            
            assertEquals(1, group.getMemberIds().size());
            assertTrue(group.getMemberIds().contains("contact-1"));
        }

        @Test
        @DisplayName("Should add multiple members")
        void addMultipleMembers() {
            Group group = new Group("user-123", "Test Group");
            
            group.addMember("contact-1");
            group.addMember("contact-2");
            group.addMember("contact-3");
            
            assertEquals(3, group.getMemberIds().size());
        }

        @Test
        @DisplayName("Should not duplicate existing member")
        void notDuplicateExistingMember() {
            Group group = new Group("user-123", "Test Group");
            group.addMember("contact-1");
            
            group.addMember("contact-1");
            
            assertEquals(1, group.getMemberIds().size());
        }

        @Test
        @DisplayName("Should update updatedAt when member is added")
        void updateUpdatedAtWhenMemberAdded() {
            Group group = new Group("user-123", "Test Group");
            LocalDateTime originalUpdatedAt = group.getUpdatedAt();
            
            group.addMember("contact-1");
            
            assertTrue(group.getUpdatedAt().isAfter(originalUpdatedAt.minusSeconds(1)));
        }

        @Test
        @DisplayName("Should not update updatedAt when duplicate member added")
        void notUpdateUpdatedAtWhenDuplicateAdded() throws InterruptedException {
            Group group = new Group("user-123", "Test Group");
            group.addMember("contact-1");
            LocalDateTime afterFirstAdd = group.getUpdatedAt();
            
            Thread.sleep(10);
            group.addMember("contact-1");
            
            assertEquals(afterFirstAdd, group.getUpdatedAt());
        }
    }

    @Nested
    @DisplayName("removeMember()")
    class RemoveMemberTests {

        @Test
        @DisplayName("Should remove existing member")
        void removeExistingMember() {
            Group group = new Group("user-123", "Test Group");
            group.setMemberIds(new ArrayList<>(Arrays.asList("contact-1", "contact-2", "contact-3")));
            
            group.removeMember("contact-2");
            
            assertEquals(2, group.getMemberIds().size());
            assertFalse(group.getMemberIds().contains("contact-2"));
            assertTrue(group.getMemberIds().contains("contact-1"));
            assertTrue(group.getMemberIds().contains("contact-3"));
        }

        @Test
        @DisplayName("Should do nothing when member not found")
        void doNothingWhenMemberNotFound() {
            Group group = new Group("user-123", "Test Group");
            group.setMemberIds(new ArrayList<>(Arrays.asList("contact-1")));
            LocalDateTime beforeRemove = group.getUpdatedAt();
            
            group.removeMember("nonexistent");
            
            assertEquals(1, group.getMemberIds().size());
            assertEquals(beforeRemove, group.getUpdatedAt());
        }

        @Test
        @DisplayName("Should update updatedAt when member is removed")
        void updateUpdatedAtWhenMemberRemoved() {
            Group group = new Group("user-123", "Test Group");
            group.setMemberIds(new ArrayList<>(Arrays.asList("contact-1")));
            LocalDateTime originalUpdatedAt = group.getUpdatedAt();
            
            group.removeMember("contact-1");
            
            assertTrue(group.getUpdatedAt().isAfter(originalUpdatedAt.minusSeconds(1)));
        }
    }

    @Nested
    @DisplayName("getMemberCount()")
    class GetMemberCountTests {

        @Test
        @DisplayName("Should return 0 for empty group")
        void returnZeroForEmptyGroup() {
            Group group = new Group("user-123", "Test Group");
            
            assertEquals(0, group.getMemberCount());
        }

        @Test
        @DisplayName("Should return correct count")
        void returnCorrectCount() {
            Group group = new Group("user-123", "Test Group");
            group.setMemberIds(new ArrayList<>(Arrays.asList("c1", "c2", "c3", "c4", "c5")));
            
            assertEquals(5, group.getMemberCount());
        }

        @Test
        @DisplayName("Should update count after adding member")
        void updateCountAfterAdding() {
            Group group = new Group("user-123", "Test Group");
            assertEquals(0, group.getMemberCount());
            
            group.addMember("contact-1");
            
            assertEquals(1, group.getMemberCount());
        }

        @Test
        @DisplayName("Should update count after removing member")
        void updateCountAfterRemoving() {
            Group group = new Group("user-123", "Test Group");
            group.setMemberIds(new ArrayList<>(Arrays.asList("c1", "c2")));
            assertEquals(2, group.getMemberCount());
            
            group.removeMember("c1");
            
            assertEquals(1, group.getMemberCount());
        }
    }

    @Nested
    @DisplayName("Description handling")
    class DescriptionTests {

        @Test
        @DisplayName("Should set and get description")
        void setAndGetDescription() {
            Group group = new Group("user-123", "Engineering");
            
            group.setDescription("All engineering team members");
            
            assertEquals("All engineering team members", group.getDescription());
        }

        @Test
        @DisplayName("Should handle null description")
        void handleNullDescription() {
            Group group = new Group("user-123", "Test");
            group.setDescription("Initial");
            
            group.setDescription(null);
            
            assertNull(group.getDescription());
        }
    }
}
