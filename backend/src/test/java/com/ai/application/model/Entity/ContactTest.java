package com.ai.application.model.Entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for Contact entity.
 * 
 * Tests cover:
 * - Entity construction
 * - Utility methods (incrementUsage, markSynced)
 * - Getter/setter functionality
 */
class ContactTest {

    @Nested
    @DisplayName("Constructor and Basic Properties")
    class ConstructorTests {

        @Test
        @DisplayName("Should create instance with default constructor")
        void createWithDefaultConstructor() {
            Contact contact = new Contact();
            
            assertNull(contact.getUserId());
            assertNull(contact.getName());
            assertNull(contact.getEmail());
            assertNotNull(contact.getCreatedAt());
            assertNotNull(contact.getUpdatedAt());
            assertFalse(contact.isDeleted());
            assertFalse(contact.isIgnored());
            assertFalse(contact.isFavorite());
            assertFalse(contact.isUsesQuickFlow());
            assertEquals(0, contact.getUsageCount());
        }

        @Test
        @DisplayName("Should create instance with parameterized constructor")
        void createWithParameterizedConstructor() {
            Contact contact = new Contact("user-123", "John Doe", "john@example.com", "google");
            
            assertEquals("user-123", contact.getUserId());
            assertEquals("John Doe", contact.getName());
            assertEquals("john@example.com", contact.getEmail());
            assertEquals("google", contact.getSource());
        }
    }

    @Nested
    @DisplayName("incrementUsage()")
    class IncrementUsageTests {

        @Test
        @DisplayName("Should increment usage count from zero")
        void incrementFromZero() {
            Contact contact = new Contact("user", "Name", "email@test.com", "manual");
            assertEquals(0, contact.getUsageCount());
            
            contact.incrementUsage();
            
            assertEquals(1, contact.getUsageCount());
        }

        @Test
        @DisplayName("Should increment usage count multiple times")
        void incrementMultipleTimes() {
            Contact contact = new Contact("user", "Name", "email@test.com", "manual");
            
            contact.incrementUsage();
            contact.incrementUsage();
            contact.incrementUsage();
            
            assertEquals(3, contact.getUsageCount());
        }

        @Test
        @DisplayName("Should update lastUsed timestamp")
        void updateLastUsedTimestamp() {
            Contact contact = new Contact("user", "Name", "email@test.com", "manual");
            assertNull(contact.getLastUsed());
            
            LocalDateTime before = LocalDateTime.now();
            contact.incrementUsage();
            LocalDateTime after = LocalDateTime.now();
            
            assertNotNull(contact.getLastUsed());
            assertTrue(contact.getLastUsed().isAfter(before.minusSeconds(1)));
            assertTrue(contact.getLastUsed().isBefore(after.plusSeconds(1)));
        }

        @Test
        @DisplayName("Should update updatedAt timestamp")
        void updateUpdatedAtTimestamp() {
            Contact contact = new Contact("user", "Name", "email@test.com", "manual");
            LocalDateTime originalUpdatedAt = contact.getUpdatedAt();
            
            contact.incrementUsage();
            
            assertTrue(contact.getUpdatedAt().isAfter(originalUpdatedAt.minusSeconds(1)));
        }
    }

    @Nested
    @DisplayName("markSynced()")
    class MarkSyncedTests {

        @Test
        @DisplayName("Should set lastSynced timestamp")
        void setLastSyncedTimestamp() {
            Contact contact = new Contact("user", "Name", "email@test.com", "google");
            assertNull(contact.getLastSynced());
            
            LocalDateTime before = LocalDateTime.now();
            contact.markSynced();
            LocalDateTime after = LocalDateTime.now();
            
            assertNotNull(contact.getLastSynced());
            assertTrue(contact.getLastSynced().isAfter(before.minusSeconds(1)));
            assertTrue(contact.getLastSynced().isBefore(after.plusSeconds(1)));
        }

        @Test
        @DisplayName("Should update updatedAt timestamp")
        void updateUpdatedAtTimestamp() {
            Contact contact = new Contact("user", "Name", "email@test.com", "google");
            LocalDateTime originalUpdatedAt = contact.getUpdatedAt();
            
            contact.markSynced();
            
            assertTrue(contact.getUpdatedAt().isAfter(originalUpdatedAt.minusSeconds(1)));
        }
    }

    @Nested
    @DisplayName("Groups management")
    class GroupsTests {

        @Test
        @DisplayName("Should initialize with empty groups list")
        void initializeWithEmptyGroups() {
            Contact contact = new Contact();
            
            assertNotNull(contact.getGroups());
            assertTrue(contact.getGroups().isEmpty());
        }

        @Test
        @DisplayName("Should set and get groups")
        void setAndGetGroups() {
            Contact contact = new Contact("user", "Name", "email@test.com", "manual");
            
            contact.setGroups(Arrays.asList("Engineering", "Management"));
            
            assertEquals(2, contact.getGroups().size());
            assertTrue(contact.getGroups().contains("Engineering"));
            assertTrue(contact.getGroups().contains("Management"));
        }
    }

    @Nested
    @DisplayName("QuickFlow detection")
    class QuickFlowTests {

        @Test
        @DisplayName("Should initially not be a QuickFlow user")
        void initiallyNotQuickFlowUser() {
            Contact contact = new Contact("user", "Name", "email@test.com", "google");
            
            assertFalse(contact.isUsesQuickFlow());
            assertNull(contact.getQuickflowUserId());
        }

        @Test
        @DisplayName("Should set QuickFlow status")
        void setQuickFlowStatus() {
            Contact contact = new Contact("user", "Name", "email@test.com", "google");
            
            contact.setUsesQuickFlow(true);
            contact.setQuickflowUserId("qf-user-123");
            
            assertTrue(contact.isUsesQuickFlow());
            assertEquals("qf-user-123", contact.getQuickflowUserId());
        }
    }

    @Nested
    @DisplayName("Source tracking")
    class SourceTrackingTests {

        @Test
        @DisplayName("Should track Google source")
        void trackGoogleSource() {
            Contact contact = new Contact("user", "Name", "email@test.com", "google");
            contact.setSourceId("google-contact-123");
            
            assertEquals("google", contact.getSource());
            assertEquals("google-contact-123", contact.getSourceId());
        }

        @Test
        @DisplayName("Should track Microsoft source")
        void trackMicrosoftSource() {
            Contact contact = new Contact("user", "Name", "email@test.com", "microsoft");
            contact.setSourceId("ms-contact-456");
            
            assertEquals("microsoft", contact.getSource());
            assertEquals("ms-contact-456", contact.getSourceId());
        }

        @Test
        @DisplayName("Should track manual source")
        void trackManualSource() {
            Contact contact = new Contact("user", "Name", "email@test.com", "manual");
            
            assertEquals("manual", contact.getSource());
            assertNull(contact.getSourceId());
        }
    }
}
