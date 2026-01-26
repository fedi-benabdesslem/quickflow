package com.ai.application.Services;

import com.ai.application.Repositories.GeneratedOutputRepository;
import com.ai.application.model.TemplateType;
import com.ai.application.model.DTO.EmailRequest;
import com.ai.application.model.DTO.MeetingRequest;
import com.ai.application.model.DTO.TemplateRequest;
import com.ai.application.model.DTO.TemplateResponse;
import com.ai.application.model.Entity.GeneratedOutput;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Unit tests for TemplateService.
 * 
 * Tests cover:
 * - Email output parsing (subject extraction)
 * - Request hash computation (caching key)
 * - User prompt building for emails and meetings
 * - Bullet points handling
 * - Caching behavior
 * 
 * Not covered:
 * - LLM response content (mocked)
 * - MongoDB operations (mocked)
 */
@ExtendWith(MockitoExtension.class)
class TemplateServiceTest {

    @Mock
    private LLMService llmService;

    @Mock
    private GeneratedOutputRepository generatedOutputRepository;

    private TemplateService templateService;

    @BeforeEach
    void setUp() {
        templateService = new TemplateService(llmService, generatedOutputRepository);
    }

    @Nested
    @DisplayName("processTemplate() - Email handling")
    class EmailTemplateTests {

        @Test
        @DisplayName("Should process email template and extract subject")
        void processEmailTemplateWithSubject() {
            EmailRequest emailRequest = new EmailRequest();
            emailRequest.setTemplateType(TemplateType.email);
            emailRequest.setRecipients(Arrays.asList("test@example.com"));
            emailRequest.setInput("Please send a reminder about the meeting");
            emailRequest.setBulletPoints(Collections.emptyList());

            String llmResponse = "Subject: Meeting Reminder\n\nDear Team,\n\nThis is a reminder...";
            
            when(generatedOutputRepository.findByRequestHash(anyString()))
                .thenReturn(Optional.empty());
            when(llmService.generateContent(anyString(), eq(TemplateType.email)))
                .thenReturn(llmResponse);
            when(generatedOutputRepository.save(any(GeneratedOutput.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

            TemplateResponse response = templateService.processTemplate(emailRequest);

            assertNotNull(response);
            assertEquals("Meeting Reminder", response.getSubject());
            assertTrue(response.getGeneratedContent().contains("Dear Team"));
            assertFalse(response.getGeneratedContent().contains("Subject:"));
        }

        @Test
        @DisplayName("Should handle email without subject line")
        void processEmailWithoutSubject() {
            EmailRequest emailRequest = new EmailRequest();
            emailRequest.setTemplateType(TemplateType.email);
            emailRequest.setRecipients(Arrays.asList("test@example.com"));
            emailRequest.setInput("Send greeting");
            emailRequest.setBulletPoints(Collections.emptyList());

            String llmResponse = "Dear Team,\n\nHello everyone.\n\nBest regards";
            
            when(generatedOutputRepository.findByRequestHash(anyString()))
                .thenReturn(Optional.empty());
            when(llmService.generateContent(anyString(), eq(TemplateType.email)))
                .thenReturn(llmResponse);
            when(generatedOutputRepository.save(any(GeneratedOutput.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

            TemplateResponse response = templateService.processTemplate(emailRequest);

            assertNotNull(response);
            assertNull(response.getSubject());
            assertEquals(llmResponse, response.getGeneratedContent());
        }

        @Test
        @DisplayName("Should handle subject line with different casing")
        void processEmailWithCasedSubject() {
            EmailRequest emailRequest = new EmailRequest();
            emailRequest.setTemplateType(TemplateType.email);
            emailRequest.setRecipients(Arrays.asList("test@example.com"));
            emailRequest.setInput("reminder");
            emailRequest.setBulletPoints(Collections.emptyList());

            String llmResponse = "SUBJECT: Important Update\n\nPlease read...";
            
            when(generatedOutputRepository.findByRequestHash(anyString()))
                .thenReturn(Optional.empty());
            when(llmService.generateContent(anyString(), eq(TemplateType.email)))
                .thenReturn(llmResponse);
            when(generatedOutputRepository.save(any(GeneratedOutput.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

            TemplateResponse response = templateService.processTemplate(emailRequest);

            assertEquals("Important Update", response.getSubject());
        }

        @Test
        @DisplayName("Should skip blank lines after subject")
        void skipBlankLinesAfterSubject() {
            EmailRequest emailRequest = new EmailRequest();
            emailRequest.setTemplateType(TemplateType.email);
            emailRequest.setRecipients(Arrays.asList("test@example.com"));
            emailRequest.setInput("test");
            emailRequest.setBulletPoints(Collections.emptyList());

            String llmResponse = "Subject: Test\n\n\n\nActual content starts here.";
            
            when(generatedOutputRepository.findByRequestHash(anyString()))
                .thenReturn(Optional.empty());
            when(llmService.generateContent(anyString(), eq(TemplateType.email)))
                .thenReturn(llmResponse);
            when(generatedOutputRepository.save(any(GeneratedOutput.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

            TemplateResponse response = templateService.processTemplate(emailRequest);

            assertEquals("Test", response.getSubject());
            assertTrue(response.getGeneratedContent().startsWith("Actual content"));
        }
    }

    @Nested
    @DisplayName("processTemplate() - PV/Meeting handling")
    class PvTemplateTests {

        @Test
        @DisplayName("Should process PV template request")
        void processPvTemplate() {
            MeetingRequest meetingRequest = new MeetingRequest();
            meetingRequest.setTemplateType(TemplateType.PV);
            meetingRequest.setPeople(Arrays.asList("John", "Jane"));
            meetingRequest.setLocation("Room A");
            meetingRequest.setDate("2024-01-15");
            meetingRequest.setTimeBegin("10:00");
            meetingRequest.setTimeEnd("11:00");
            meetingRequest.setSubject("Sprint Planning");
            meetingRequest.setDetails("Discuss Q1 goals");
            meetingRequest.setBulletPoints(Arrays.asList("Review backlog", "Assign tasks"));

            String llmResponse = "MEETING MINUTES\n================\nDate: 2024-01-15...";
            
            when(generatedOutputRepository.findByRequestHash(anyString()))
                .thenReturn(Optional.empty());
            when(llmService.generateContent(anyString(), eq(TemplateType.PV)))
                .thenReturn(llmResponse);
            when(generatedOutputRepository.save(any(GeneratedOutput.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

            TemplateResponse response = templateService.processTemplate(meetingRequest);

            assertNotNull(response);
            assertEquals(llmResponse, response.getGeneratedContent());
            assertNull(response.getSubject()); // PV doesn't extract subject
        }

        @Test
        @DisplayName("Should handle meeting with minimal data")
        void processMeetingWithMinimalData() {
            MeetingRequest meetingRequest = new MeetingRequest();
            meetingRequest.setTemplateType(TemplateType.PV);
            meetingRequest.setBulletPoints(Collections.emptyList());

            String llmResponse = "Meeting summary with TBD values";
            
            when(generatedOutputRepository.findByRequestHash(anyString()))
                .thenReturn(Optional.empty());
            when(llmService.generateContent(anyString(), eq(TemplateType.PV)))
                .thenReturn(llmResponse);
            when(generatedOutputRepository.save(any(GeneratedOutput.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

            TemplateResponse response = templateService.processTemplate(meetingRequest);

            assertNotNull(response);
            // Verify user prompt was built with TBD values
            verify(llmService).generateContent(contains("TBD"), eq(TemplateType.PV));
        }
    }

    @Nested
    @DisplayName("processTemplate() - Caching behavior")
    class CachingTests {

        @Test
        @DisplayName("Should return cached response if available")
        void returnCachedResponse() {
            EmailRequest emailRequest = new EmailRequest();
            emailRequest.setTemplateType(TemplateType.email);
            emailRequest.setRecipients(Arrays.asList("test@example.com"));
            emailRequest.setInput("test input");
            emailRequest.setBulletPoints(Collections.emptyList());

            GeneratedOutput cachedOutput = new GeneratedOutput();
            cachedOutput.setContent("Cached email content");
            
            when(generatedOutputRepository.findByRequestHash(anyString()))
                .thenReturn(Optional.of(cachedOutput));

            TemplateResponse response = templateService.processTemplate(emailRequest);

            assertEquals("Cached email content", response.getGeneratedContent());
            verify(llmService, never()).generateContent(anyString(), any());
        }

        @Test
        @DisplayName("Should save new response to cache")
        void saveNewResponseToCache() {
            EmailRequest emailRequest = new EmailRequest();
            emailRequest.setTemplateType(TemplateType.email);
            emailRequest.setRecipients(Arrays.asList("new@example.com"));
            emailRequest.setInput("new request");
            emailRequest.setBulletPoints(Collections.emptyList());

            when(generatedOutputRepository.findByRequestHash(anyString()))
                .thenReturn(Optional.empty());
            when(llmService.generateContent(anyString(), any()))
                .thenReturn("New content");
            when(generatedOutputRepository.save(any(GeneratedOutput.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

            templateService.processTemplate(emailRequest);

            ArgumentCaptor<GeneratedOutput> captor = ArgumentCaptor.forClass(GeneratedOutput.class);
            verify(generatedOutputRepository).save(captor.capture());
            
            GeneratedOutput saved = captor.getValue();
            assertNotNull(saved.getRequestHash());
            assertEquals("New content", saved.getContent());
            assertNotNull(saved.getCreatedAt());
            assertEquals("llama3.2", saved.getModelUsed());
        }

        @Test
        @DisplayName("Should generate different hashes for different requests")
        void generateDifferentHashesForDifferentRequests() {
            EmailRequest request1 = new EmailRequest();
            request1.setTemplateType(TemplateType.email);
            request1.setInput("first request");
            request1.setRecipients(Arrays.asList("a@test.com"));
            request1.setBulletPoints(Collections.emptyList());

            EmailRequest request2 = new EmailRequest();
            request2.setTemplateType(TemplateType.email);
            request2.setInput("second request");
            request2.setRecipients(Arrays.asList("b@test.com"));
            request2.setBulletPoints(Collections.emptyList());

            when(generatedOutputRepository.findByRequestHash(anyString()))
                .thenReturn(Optional.empty());
            when(llmService.generateContent(anyString(), any()))
                .thenReturn("response");
            when(generatedOutputRepository.save(any(GeneratedOutput.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

            templateService.processTemplate(request1);
            templateService.processTemplate(request2);

            ArgumentCaptor<GeneratedOutput> captor = ArgumentCaptor.forClass(GeneratedOutput.class);
            verify(generatedOutputRepository, times(2)).save(captor.capture());
            
            List<GeneratedOutput> savedOutputs = captor.getAllValues();
            assertNotEquals(
                savedOutputs.get(0).getRequestHash(),
                savedOutputs.get(1).getRequestHash()
            );
        }
    }

    @Nested
    @DisplayName("User prompt building")
    class UserPromptBuildingTests {

        @Test
        @DisplayName("Should include recipients in email prompt")
        void includeRecipientsInEmailPrompt() {
            EmailRequest emailRequest = new EmailRequest();
            emailRequest.setTemplateType(TemplateType.email);
            emailRequest.setRecipients(Arrays.asList("john@test.com", "jane@test.com"));
            emailRequest.setInput("hello");
            emailRequest.setBulletPoints(Collections.emptyList());
            emailRequest.setSenderEmail("sender@test.com");

            when(generatedOutputRepository.findByRequestHash(anyString()))
                .thenReturn(Optional.empty());
            when(llmService.generateContent(anyString(), any()))
                .thenReturn("response");
            when(generatedOutputRepository.save(any(GeneratedOutput.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

            templateService.processTemplate(emailRequest);

            verify(llmService).generateContent(
                argThat(prompt -> 
                    prompt.contains("john@test.com") && 
                    prompt.contains("jane@test.com")
                ),
                eq(TemplateType.email)
            );
        }

        @Test
        @DisplayName("Should include bullet points in prompt")
        void includeBulletPointsInPrompt() {
            EmailRequest emailRequest = new EmailRequest();
            emailRequest.setTemplateType(TemplateType.email);
            emailRequest.setRecipients(Arrays.asList("test@test.com"));
            emailRequest.setBulletPoints(Arrays.asList("Point 1", "Point 2", "Point 3"));

            when(generatedOutputRepository.findByRequestHash(anyString()))
                .thenReturn(Optional.empty());
            when(llmService.generateContent(anyString(), any()))
                .thenReturn("response");
            when(generatedOutputRepository.save(any(GeneratedOutput.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

            templateService.processTemplate(emailRequest);

            verify(llmService).generateContent(
                argThat(prompt -> 
                    prompt.contains("- Point 1") && 
                    prompt.contains("- Point 2") &&
                    prompt.contains("- Point 3")
                ),
                eq(TemplateType.email)
            );
        }

        @Test
        @DisplayName("Should use input when no bullet points provided")
        void useInputWhenNoBulletPoints() {
            EmailRequest emailRequest = new EmailRequest();
            emailRequest.setTemplateType(TemplateType.email);
            emailRequest.setRecipients(Arrays.asList("test@test.com"));
            emailRequest.setInput("Please send the report");
            emailRequest.setBulletPoints(null);

            when(generatedOutputRepository.findByRequestHash(anyString()))
                .thenReturn(Optional.empty());
            when(llmService.generateContent(anyString(), any()))
                .thenReturn("response");
            when(generatedOutputRepository.save(any(GeneratedOutput.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

            templateService.processTemplate(emailRequest);

            verify(llmService).generateContent(
                argThat(prompt -> prompt.contains("- Please send the report")),
                eq(TemplateType.email)
            );
        }

        @Test
        @DisplayName("Should include meeting details in PV prompt")
        void includeMeetingDetailsInPvPrompt() {
            MeetingRequest meetingRequest = new MeetingRequest();
            meetingRequest.setTemplateType(TemplateType.PV);
            meetingRequest.setPeople(Arrays.asList("Alice", "Bob"));
            meetingRequest.setLocation("Conference Room B");
            meetingRequest.setDate("2024-03-20");
            meetingRequest.setTimeBegin("14:00");
            meetingRequest.setTimeEnd("15:30");
            meetingRequest.setSubject("Quarterly Review");
            meetingRequest.setDetails("Review Q4 performance metrics");
            meetingRequest.setBulletPoints(Collections.emptyList());

            when(generatedOutputRepository.findByRequestHash(anyString()))
                .thenReturn(Optional.empty());
            when(llmService.generateContent(anyString(), any()))
                .thenReturn("response");
            when(generatedOutputRepository.save(any(GeneratedOutput.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

            templateService.processTemplate(meetingRequest);

            verify(llmService).generateContent(
                argThat(prompt -> 
                    prompt.contains("Alice") &&
                    prompt.contains("Bob") &&
                    prompt.contains("Conference Room B") &&
                    prompt.contains("2024-03-20") &&
                    prompt.contains("Quarterly Review") &&
                    prompt.contains("Q4 performance")
                ),
                eq(TemplateType.PV)
            );
        }
    }

    @Nested
    @DisplayName("Error handling")
    class ErrorHandlingTests {

        @Test
        @DisplayName("Should throw exception for unsupported template type combination")
        void throwExceptionForUnsupportedCombination() {
            // Create a TemplateRequest that's not EmailRequest or MeetingRequest
            // When type is email but request is TemplateRequest (not EmailRequest)
            TemplateRequest genericRequest = new TemplateRequest(
                TemplateType.email,
                Arrays.asList("point"),
                "user",
                "input",
                "sender",
                Arrays.asList("recipient"),
                "sender@test.com"
            );

            when(generatedOutputRepository.findByRequestHash(anyString()))
                .thenReturn(Optional.empty());

            // The service throws IllegalArgumentException for mismatched type/instance
            IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> templateService.processTemplate(genericRequest)
            );
            
            assertTrue(exception.getMessage().contains("Unsupported template type"));
        }
    }
}
