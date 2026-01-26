package com.ai.application.Services;

import com.ai.application.Repositories.MeetingTemplateRepository;
import com.ai.application.model.DTO.MeetingTemplateData;
import com.ai.application.model.Entity.MeetingTemplate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for MeetingTemplateService.
 * 
 * Tests cover:
 * - CRUD operations for meeting templates
 * - User authorization checks
 * - Duplicate name validation
 * - Usage tracking
 * 
 * Not covered:
 * - MongoDB-specific behavior (integration test territory)
 * - Concurrent access patterns (requires integration tests)
 */
@ExtendWith(MockitoExtension.class)
class MeetingTemplateServiceTest {

    @Mock
    private MeetingTemplateRepository meetingTemplateRepository;

    @InjectMocks
    private MeetingTemplateService meetingTemplateService;

    private MeetingTemplate testTemplate;
    private MeetingTemplateData testTemplateData;
    private static final String TEST_USER_ID = "user-123";
    private static final String TEST_TEMPLATE_ID = "template-456";

    @BeforeEach
    void setUp() {
        testTemplateData = new MeetingTemplateData();
        
        testTemplate = new MeetingTemplate();
        testTemplate.setId(TEST_TEMPLATE_ID);
        testTemplate.setUserId(TEST_USER_ID);
        testTemplate.setName("Weekly Standup");
        testTemplate.setDescription("Template for weekly standup meetings");
        testTemplate.setTemplateData(testTemplateData);
        testTemplate.setUsageCount(5);
        testTemplate.setCreatedAt(LocalDateTime.now());
        testTemplate.setUpdatedAt(LocalDateTime.now());
    }

    @Nested
    @DisplayName("saveTemplate()")
    class SaveTemplateTests {

        @Test
        @DisplayName("Should save new template successfully")
        void saveNewTemplateSuccessfully() {
            when(meetingTemplateRepository.existsByUserIdAndName(TEST_USER_ID, "New Template"))
                .thenReturn(false);
            when(meetingTemplateRepository.save(any(MeetingTemplate.class)))
                .thenAnswer(invocation -> {
                    MeetingTemplate saved = invocation.getArgument(0);
                    saved.setId("new-id");
                    return saved;
                });

            MeetingTemplate result = meetingTemplateService.saveTemplate(
                TEST_USER_ID, "New Template", "Description", testTemplateData
            );

            assertNotNull(result);
            assertEquals(TEST_USER_ID, result.getUserId());
            assertEquals("New Template", result.getName());
            assertEquals("Description", result.getDescription());
            assertEquals(0, result.getUsageCount());
            assertNotNull(result.getCreatedAt());
            
            verify(meetingTemplateRepository).save(any(MeetingTemplate.class));
        }

        @Test
        @DisplayName("Should throw exception when template name already exists")
        void throwExceptionWhenNameExists() {
            when(meetingTemplateRepository.existsByUserIdAndName(TEST_USER_ID, "Existing Template"))
                .thenReturn(true);

            IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> meetingTemplateService.saveTemplate(
                    TEST_USER_ID, "Existing Template", "Desc", testTemplateData
                )
            );

            assertEquals("Template with this name already exists", exception.getMessage());
            verify(meetingTemplateRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should allow same template name for different users")
        void allowSameNameForDifferentUsers() {
            String otherUserId = "user-different";
            when(meetingTemplateRepository.existsByUserIdAndName(otherUserId, "Weekly Standup"))
                .thenReturn(false);
            when(meetingTemplateRepository.save(any(MeetingTemplate.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

            MeetingTemplate result = meetingTemplateService.saveTemplate(
                otherUserId, "Weekly Standup", "Another user's standup", testTemplateData
            );

            assertNotNull(result);
            assertEquals(otherUserId, result.getUserId());
        }
    }

    @Nested
    @DisplayName("getUserTemplates()")
    class GetUserTemplatesTests {

        @Test
        @DisplayName("Should return user's templates ordered by last used")
        void returnUserTemplatesOrderedByLastUsed() {
            MeetingTemplate template1 = new MeetingTemplate();
            template1.setName("Template 1");
            MeetingTemplate template2 = new MeetingTemplate();
            template2.setName("Template 2");
            
            when(meetingTemplateRepository.findByUserIdOrderByLastUsedDesc(TEST_USER_ID))
                .thenReturn(Arrays.asList(template1, template2));

            List<MeetingTemplate> result = meetingTemplateService.getUserTemplates(TEST_USER_ID);

            assertEquals(2, result.size());
            assertEquals("Template 1", result.get(0).getName());
            verify(meetingTemplateRepository).findByUserIdOrderByLastUsedDesc(TEST_USER_ID);
        }

        @Test
        @DisplayName("Should return empty list for user with no templates")
        void returnEmptyListForNewUser() {
            when(meetingTemplateRepository.findByUserIdOrderByLastUsedDesc("new-user"))
                .thenReturn(Collections.emptyList());

            List<MeetingTemplate> result = meetingTemplateService.getUserTemplates("new-user");

            assertTrue(result.isEmpty());
        }
    }

    @Nested
    @DisplayName("getTemplate()")
    class GetTemplateTests {

        @Test
        @DisplayName("Should return template for authorized user")
        void returnTemplateForAuthorizedUser() {
            when(meetingTemplateRepository.findById(TEST_TEMPLATE_ID))
                .thenReturn(Optional.of(testTemplate));

            MeetingTemplate result = meetingTemplateService.getTemplate(TEST_USER_ID, TEST_TEMPLATE_ID);

            assertNotNull(result);
            assertEquals(TEST_TEMPLATE_ID, result.getId());
            assertEquals(TEST_USER_ID, result.getUserId());
        }

        @Test
        @DisplayName("Should throw exception when template not found")
        void throwExceptionWhenNotFound() {
            when(meetingTemplateRepository.findById("nonexistent"))
                .thenReturn(Optional.empty());

            RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> meetingTemplateService.getTemplate(TEST_USER_ID, "nonexistent")
            );

            assertEquals("Template not found", exception.getMessage());
        }

        @Test
        @DisplayName("Should throw exception for unauthorized access")
        void throwExceptionForUnauthorizedAccess() {
            when(meetingTemplateRepository.findById(TEST_TEMPLATE_ID))
                .thenReturn(Optional.of(testTemplate));

            RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> meetingTemplateService.getTemplate("other-user", TEST_TEMPLATE_ID)
            );

            assertEquals("Unauthorized access to template", exception.getMessage());
        }
    }

    @Nested
    @DisplayName("updateTemplate()")
    class UpdateTemplateTests {

        @Test
        @DisplayName("Should update template name")
        void updateTemplateName() {
            when(meetingTemplateRepository.findById(TEST_TEMPLATE_ID))
                .thenReturn(Optional.of(testTemplate));
            when(meetingTemplateRepository.existsByUserIdAndName(TEST_USER_ID, "New Name"))
                .thenReturn(false);
            when(meetingTemplateRepository.save(any(MeetingTemplate.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

            MeetingTemplate result = meetingTemplateService.updateTemplate(
                TEST_USER_ID, TEST_TEMPLATE_ID, "New Name", null, null
            );

            assertEquals("New Name", result.getName());
            verify(meetingTemplateRepository).save(any());
        }

        @Test
        @DisplayName("Should update template description")
        void updateTemplateDescription() {
            when(meetingTemplateRepository.findById(TEST_TEMPLATE_ID))
                .thenReturn(Optional.of(testTemplate));
            when(meetingTemplateRepository.save(any(MeetingTemplate.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

            MeetingTemplate result = meetingTemplateService.updateTemplate(
                TEST_USER_ID, TEST_TEMPLATE_ID, null, "New Description", null
            );

            assertEquals("New Description", result.getDescription());
        }

        @Test
        @DisplayName("Should update template data")
        void updateTemplateData() {
            MeetingTemplateData newData = new MeetingTemplateData();
            when(meetingTemplateRepository.findById(TEST_TEMPLATE_ID))
                .thenReturn(Optional.of(testTemplate));
            when(meetingTemplateRepository.save(any(MeetingTemplate.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

            MeetingTemplate result = meetingTemplateService.updateTemplate(
                TEST_USER_ID, TEST_TEMPLATE_ID, null, null, newData
            );

            assertEquals(newData, result.getTemplateData());
        }

        @Test
        @DisplayName("Should throw exception when renaming to existing name")
        void throwExceptionWhenRenamingToExistingName() {
            when(meetingTemplateRepository.findById(TEST_TEMPLATE_ID))
                .thenReturn(Optional.of(testTemplate));
            when(meetingTemplateRepository.existsByUserIdAndName(TEST_USER_ID, "Existing Name"))
                .thenReturn(true);

            IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> meetingTemplateService.updateTemplate(
                    TEST_USER_ID, TEST_TEMPLATE_ID, "Existing Name", null, null
                )
            );

            assertEquals("Template with this name already exists", exception.getMessage());
            verify(meetingTemplateRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should not check for duplicate when name unchanged")
        void notCheckDuplicateWhenNameUnchanged() {
            when(meetingTemplateRepository.findById(TEST_TEMPLATE_ID))
                .thenReturn(Optional.of(testTemplate));
            when(meetingTemplateRepository.save(any(MeetingTemplate.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

            meetingTemplateService.updateTemplate(
                TEST_USER_ID, TEST_TEMPLATE_ID, "Weekly Standup", "New desc", null
            );

            verify(meetingTemplateRepository, never())
                .existsByUserIdAndName(anyString(), anyString());
        }

        @Test
        @DisplayName("Should update updatedAt timestamp")
        void updateTimestamp() {
            LocalDateTime originalUpdatedAt = testTemplate.getUpdatedAt();
            when(meetingTemplateRepository.findById(TEST_TEMPLATE_ID))
                .thenReturn(Optional.of(testTemplate));
            when(meetingTemplateRepository.save(any(MeetingTemplate.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

            MeetingTemplate result = meetingTemplateService.updateTemplate(
                TEST_USER_ID, TEST_TEMPLATE_ID, null, "Updated", null
            );

            // Verify updatedAt was set (use ArgumentCaptor to check the saved value)
            ArgumentCaptor<MeetingTemplate> captor = ArgumentCaptor.forClass(MeetingTemplate.class);
            verify(meetingTemplateRepository).save(captor.capture());
            
            MeetingTemplate saved = captor.getValue();
            assertNotNull(saved.getUpdatedAt());
            // The updatedAt should be set to a value at or after the original
            assertTrue(saved.getUpdatedAt().isAfter(originalUpdatedAt.minusSeconds(1)));
        }
    }

    @Nested
    @DisplayName("deleteTemplate()")
    class DeleteTemplateTests {

        @Test
        @DisplayName("Should delete template for authorized user")
        void deleteTemplateForAuthorizedUser() {
            when(meetingTemplateRepository.findById(TEST_TEMPLATE_ID))
                .thenReturn(Optional.of(testTemplate));

            meetingTemplateService.deleteTemplate(TEST_USER_ID, TEST_TEMPLATE_ID);

            verify(meetingTemplateRepository).delete(testTemplate);
        }

        @Test
        @DisplayName("Should throw exception for unauthorized deletion")
        void throwExceptionForUnauthorizedDeletion() {
            when(meetingTemplateRepository.findById(TEST_TEMPLATE_ID))
                .thenReturn(Optional.of(testTemplate));

            RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> meetingTemplateService.deleteTemplate("other-user", TEST_TEMPLATE_ID)
            );

            assertEquals("Unauthorized access to template", exception.getMessage());
            verify(meetingTemplateRepository, never()).delete(any());
        }

        @Test
        @DisplayName("Should throw exception when deleting nonexistent template")
        void throwExceptionWhenDeletingNonexistent() {
            when(meetingTemplateRepository.findById("nonexistent"))
                .thenReturn(Optional.empty());

            assertThrows(RuntimeException.class,
                () -> meetingTemplateService.deleteTemplate(TEST_USER_ID, "nonexistent")
            );

            verify(meetingTemplateRepository, never()).delete(any());
        }
    }

    @Nested
    @DisplayName("trackUsage()")
    class TrackUsageTests {

        @Test
        @DisplayName("Should increment usage count")
        void incrementUsageCount() {
            int originalCount = testTemplate.getUsageCount();
            when(meetingTemplateRepository.findById(TEST_TEMPLATE_ID))
                .thenReturn(Optional.of(testTemplate));
            when(meetingTemplateRepository.save(any(MeetingTemplate.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

            meetingTemplateService.trackUsage(TEST_USER_ID, TEST_TEMPLATE_ID);

            ArgumentCaptor<MeetingTemplate> captor = ArgumentCaptor.forClass(MeetingTemplate.class);
            verify(meetingTemplateRepository).save(captor.capture());
            
            MeetingTemplate saved = captor.getValue();
            assertEquals(originalCount + 1, saved.getUsageCount());
        }

        @Test
        @DisplayName("Should update last used timestamp")
        void updateLastUsedTimestamp() {
            testTemplate.setLastUsed(null);
            when(meetingTemplateRepository.findById(TEST_TEMPLATE_ID))
                .thenReturn(Optional.of(testTemplate));
            when(meetingTemplateRepository.save(any(MeetingTemplate.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

            meetingTemplateService.trackUsage(TEST_USER_ID, TEST_TEMPLATE_ID);

            ArgumentCaptor<MeetingTemplate> captor = ArgumentCaptor.forClass(MeetingTemplate.class);
            verify(meetingTemplateRepository).save(captor.capture());
            
            MeetingTemplate saved = captor.getValue();
            assertNotNull(saved.getLastUsed());
        }

        @Test
        @DisplayName("Should throw exception for unauthorized usage tracking")
        void throwExceptionForUnauthorizedUsageTracking() {
            when(meetingTemplateRepository.findById(TEST_TEMPLATE_ID))
                .thenReturn(Optional.of(testTemplate));

            assertThrows(RuntimeException.class,
                () -> meetingTemplateService.trackUsage("other-user", TEST_TEMPLATE_ID)
            );

            verify(meetingTemplateRepository, never()).save(any());
        }
    }
}
