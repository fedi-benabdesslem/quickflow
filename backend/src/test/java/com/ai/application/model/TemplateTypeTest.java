package com.ai.application.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for TemplateType enum.
 * 
 * Tests cover:
 * - Valid enum values parsing
 * - Case-insensitive parsing
 * - Invalid value handling
 * - Null value handling
 * 
 * Not covered:
 * - JSON serialization (covered by controller integration tests)
 */
class TemplateTypeTest {

    @Nested
    @DisplayName("fromValue() - Valid inputs")
    class ValidInputTests {

        @Test
        @DisplayName("Should parse 'email' value")
        void parseEmailLowercase() {
            assertEquals(TemplateType.email, TemplateType.fromValue("email"));
        }

        @Test
        @DisplayName("Should parse 'EMAIL' value (uppercase)")
        void parseEmailUppercase() {
            assertEquals(TemplateType.email, TemplateType.fromValue("EMAIL"));
        }

        @Test
        @DisplayName("Should parse 'Email' value (mixed case)")
        void parseEmailMixedCase() {
            assertEquals(TemplateType.email, TemplateType.fromValue("Email"));
        }

        @Test
        @DisplayName("Should parse 'PV' value")
        void parsePvUppercase() {
            assertEquals(TemplateType.PV, TemplateType.fromValue("PV"));
        }

        @Test
        @DisplayName("Should parse 'pv' value (lowercase)")
        void parsePvLowercase() {
            assertEquals(TemplateType.PV, TemplateType.fromValue("pv"));
        }

        @Test
        @DisplayName("Should parse value with leading/trailing whitespace")
        void parseWithWhitespace() {
            assertEquals(TemplateType.email, TemplateType.fromValue("  email  "));
            assertEquals(TemplateType.PV, TemplateType.fromValue(" PV "));
        }
    }

    @Nested
    @DisplayName("fromValue() - Invalid inputs")
    class InvalidInputTests {

        @Test
        @DisplayName("Should throw exception for null value")
        void throwExceptionForNull() {
            IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> TemplateType.fromValue(null)
            );
            assertEquals("TemplateType cannot be null", exception.getMessage());
        }

        @Test
        @DisplayName("Should throw exception for unknown value")
        void throwExceptionForUnknownValue() {
            IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> TemplateType.fromValue("unknown")
            );
            assertTrue(exception.getMessage().contains("Unknown TemplateType value: UNKNOWN"));
        }

        @Test
        @DisplayName("Should throw exception for empty string")
        void throwExceptionForEmptyString() {
            assertThrows(
                IllegalArgumentException.class,
                () -> TemplateType.fromValue("")
            );
        }

        @Test
        @DisplayName("Should throw exception for whitespace-only string")
        void throwExceptionForWhitespaceOnly() {
            assertThrows(
                IllegalArgumentException.class,
                () -> TemplateType.fromValue("   ")
            );
        }

        @Test
        @DisplayName("Should throw exception for 'meeting' value (not supported in fromValue)")
        void throwExceptionForMeeting() {
            IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> TemplateType.fromValue("meeting")
            );
            assertTrue(exception.getMessage().contains("Unknown TemplateType value: MEETING"));
            assertTrue(exception.getMessage().contains("Accepted: email, PV"));
        }
    }

    @Nested
    @DisplayName("Enum value existence")
    class EnumValueTests {

        @Test
        @DisplayName("Should have email enum value")
        void hasEmailValue() {
            assertNotNull(TemplateType.email);
            assertEquals("email", TemplateType.email.name());
        }

        @Test
        @DisplayName("Should have PV enum value")
        void hasPvValue() {
            assertNotNull(TemplateType.PV);
            assertEquals("PV", TemplateType.PV.name());
        }

        @Test
        @DisplayName("Should have meeting enum value")
        void hasMeetingValue() {
            assertNotNull(TemplateType.meeting);
            assertEquals("meeting", TemplateType.meeting.name());
        }

        @Test
        @DisplayName("Should have exactly 3 enum values")
        void hasExactlyThreeValues() {
            assertEquals(3, TemplateType.values().length);
        }
    }

    @Nested
    @DisplayName("Enum behavior")
    class EnumBehaviorTests {

        @Test
        @DisplayName("Enum valueOf should work for email")
        void valueOfEmail() {
            assertEquals(TemplateType.email, TemplateType.valueOf("email"));
        }

        @Test
        @DisplayName("Enum valueOf should work for PV")
        void valueOfPV() {
            assertEquals(TemplateType.PV, TemplateType.valueOf("PV"));
        }

        @Test
        @DisplayName("Enum valueOf should work for meeting")
        void valueOfMeeting() {
            assertEquals(TemplateType.meeting, TemplateType.valueOf("meeting"));
        }

        @Test
        @DisplayName("Standard valueOf should throw for unknown")
        void valueOfUnknownThrows() {
            assertThrows(IllegalArgumentException.class, 
                () -> TemplateType.valueOf("unknown"));
        }
    }
}
