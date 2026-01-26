package com.ai.application.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for ExtractedData model and its nested classes.
 * 
 * Tests cover:
 * - ExtractedData POJO behavior
 * - ExtractedDecision nested class
 * - ExtractedActionItem nested class
 * - Getter/setter functionality
 * 
 * Note: These are characterization tests documenting current behavior.
 */
class ExtractedDataTest {

    @Nested
    @DisplayName("ExtractedData")
    class ExtractedDataMainTests {

        @Test
        @DisplayName("Should create instance with default constructor")
        void createWithDefaultConstructor() {
            ExtractedData data = new ExtractedData();
            
            assertNull(data.getMeetingTitle());
            assertNull(data.getDate());
            assertNull(data.getTime());
            assertNull(data.getParticipants());
            assertNull(data.getDiscussionPoints());
            assertNull(data.getDecisions());
            assertNull(data.getActionItems());
            assertNull(data.getConfidence());
        }

        @Test
        @DisplayName("Should set and get all properties")
        void setAndGetAllProperties() {
            ExtractedData data = new ExtractedData();
            
            data.setMeetingTitle("Team Meeting");
            data.setDate("2024-01-15");
            data.setTime("10:00 AM");
            data.setParticipants(Arrays.asList("Alice", "Bob"));
            data.setDiscussionPoints(Arrays.asList("Point 1", "Point 2"));
            data.setConfidence("high");
            
            assertEquals("Team Meeting", data.getMeetingTitle());
            assertEquals("2024-01-15", data.getDate());
            assertEquals("10:00 AM", data.getTime());
            assertEquals(2, data.getParticipants().size());
            assertEquals("Alice", data.getParticipants().get(0));
            assertEquals(2, data.getDiscussionPoints().size());
            assertEquals("high", data.getConfidence());
        }

        @Test
        @DisplayName("Should handle null values")
        void handleNullValues() {
            ExtractedData data = new ExtractedData();
            data.setMeetingTitle("Test");
            data.setMeetingTitle(null);
            
            assertNull(data.getMeetingTitle());
        }

        @Test
        @DisplayName("Should handle empty lists")
        void handleEmptyLists() {
            ExtractedData data = new ExtractedData();
            data.setParticipants(Arrays.asList());
            data.setDiscussionPoints(Arrays.asList());
            
            assertTrue(data.getParticipants().isEmpty());
            assertTrue(data.getDiscussionPoints().isEmpty());
        }
    }

    @Nested
    @DisplayName("ExtractedDecision")
    class ExtractedDecisionTests {

        @Test
        @DisplayName("Should create with default constructor")
        void createWithDefaultConstructor() {
            ExtractedData.ExtractedDecision decision = new ExtractedData.ExtractedDecision();
            
            assertNull(decision.getStatement());
            assertNull(decision.getStatus());
        }

        @Test
        @DisplayName("Should create with parameterized constructor")
        void createWithParameterizedConstructor() {
            ExtractedData.ExtractedDecision decision = 
                new ExtractedData.ExtractedDecision("Approve budget", "Approved");
            
            assertEquals("Approve budget", decision.getStatement());
            assertEquals("Approved", decision.getStatus());
        }

        @Test
        @DisplayName("Should set and get statement")
        void setAndGetStatement() {
            ExtractedData.ExtractedDecision decision = new ExtractedData.ExtractedDecision();
            decision.setStatement("New decision statement");
            
            assertEquals("New decision statement", decision.getStatement());
        }

        @Test
        @DisplayName("Should set and get status")
        void setAndGetStatus() {
            ExtractedData.ExtractedDecision decision = new ExtractedData.ExtractedDecision();
            decision.setStatus("Rejected");
            
            assertEquals("Rejected", decision.getStatus());
        }

        @Test
        @DisplayName("Should handle various status values")
        void handleVariousStatusValues() {
            ExtractedData.ExtractedDecision decision = new ExtractedData.ExtractedDecision();
            
            decision.setStatus("Approved");
            assertEquals("Approved", decision.getStatus());
            
            decision.setStatus("Rejected");
            assertEquals("Rejected", decision.getStatus());
            
            decision.setStatus("Deferred");
            assertEquals("Deferred", decision.getStatus());
            
            decision.setStatus("No Decision");
            assertEquals("No Decision", decision.getStatus());
        }
    }

    @Nested
    @DisplayName("ExtractedActionItem")
    class ExtractedActionItemTests {

        @Test
        @DisplayName("Should create with default constructor")
        void createWithDefaultConstructor() {
            ExtractedData.ExtractedActionItem actionItem = new ExtractedData.ExtractedActionItem();
            
            assertNull(actionItem.getTask());
            assertNull(actionItem.getOwner());
            assertNull(actionItem.getDeadline());
        }

        @Test
        @DisplayName("Should create with parameterized constructor")
        void createWithParameterizedConstructor() {
            ExtractedData.ExtractedActionItem actionItem = 
                new ExtractedData.ExtractedActionItem("Complete report", "John", "2024-01-20");
            
            assertEquals("Complete report", actionItem.getTask());
            assertEquals("John", actionItem.getOwner());
            assertEquals("2024-01-20", actionItem.getDeadline());
        }

        @Test
        @DisplayName("Should set and get task")
        void setAndGetTask() {
            ExtractedData.ExtractedActionItem actionItem = new ExtractedData.ExtractedActionItem();
            actionItem.setTask("Prepare presentation");
            
            assertEquals("Prepare presentation", actionItem.getTask());
        }

        @Test
        @DisplayName("Should set and get owner")
        void setAndGetOwner() {
            ExtractedData.ExtractedActionItem actionItem = new ExtractedData.ExtractedActionItem();
            actionItem.setOwner("Alice");
            
            assertEquals("Alice", actionItem.getOwner());
        }

        @Test
        @DisplayName("Should set and get deadline")
        void setAndGetDeadline() {
            ExtractedData.ExtractedActionItem actionItem = new ExtractedData.ExtractedActionItem();
            actionItem.setDeadline("2024-02-01");
            
            assertEquals("2024-02-01", actionItem.getDeadline());
        }

        @Test
        @DisplayName("Should handle null owner and deadline")
        void handleNullOwnerAndDeadline() {
            ExtractedData.ExtractedActionItem actionItem = 
                new ExtractedData.ExtractedActionItem("Task", null, null);
            
            assertEquals("Task", actionItem.getTask());
            assertNull(actionItem.getOwner());
            assertNull(actionItem.getDeadline());
        }
    }

    @Nested
    @DisplayName("Integration - Complete ExtractedData")
    class IntegrationTests {

        @Test
        @DisplayName("Should build complete ExtractedData object")
        void buildCompleteExtractedData() {
            ExtractedData data = new ExtractedData();
            
            data.setMeetingTitle("Sprint Planning");
            data.setDate("2024-01-15");
            data.setTime("10:00 AM");
            data.setParticipants(Arrays.asList("Alice", "Bob", "Charlie"));
            data.setDiscussionPoints(Arrays.asList(
                "Review sprint goals",
                "Discuss blockers",
                "Assign tasks"
            ));
            
            ExtractedData.ExtractedDecision decision1 = 
                new ExtractedData.ExtractedDecision("Extend sprint deadline", "Approved");
            ExtractedData.ExtractedDecision decision2 = 
                new ExtractedData.ExtractedDecision("Add new feature", "Deferred");
            data.setDecisions(Arrays.asList(decision1, decision2));
            
            ExtractedData.ExtractedActionItem action1 = 
                new ExtractedData.ExtractedActionItem("Update documentation", "Alice", "2024-01-20");
            ExtractedData.ExtractedActionItem action2 = 
                new ExtractedData.ExtractedActionItem("Fix critical bug", "Bob", "2024-01-18");
            data.setActionItems(Arrays.asList(action1, action2));
            
            data.setConfidence("high");
            
            // Verify complete object
            assertEquals("Sprint Planning", data.getMeetingTitle());
            assertEquals(3, data.getParticipants().size());
            assertEquals(3, data.getDiscussionPoints().size());
            assertEquals(2, data.getDecisions().size());
            assertEquals("Approved", data.getDecisions().get(0).getStatus());
            assertEquals(2, data.getActionItems().size());
            assertEquals("Alice", data.getActionItems().get(0).getOwner());
            assertEquals("high", data.getConfidence());
        }

        @Test
        @DisplayName("Should handle minimal ExtractedData")
        void handleMinimalExtractedData() {
            ExtractedData data = new ExtractedData();
            data.setMeetingTitle("Quick Sync");
            data.setConfidence("low");
            
            // All other fields remain null
            assertEquals("Quick Sync", data.getMeetingTitle());
            assertEquals("low", data.getConfidence());
            assertNull(data.getParticipants());
            assertNull(data.getDecisions());
            assertNull(data.getActionItems());
        }
    }
}
