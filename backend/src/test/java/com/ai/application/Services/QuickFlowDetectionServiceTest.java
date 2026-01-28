package com.ai.application.Services;

import com.ai.application.Repositories.ContactRepository;
import com.ai.application.Repositories.UserTokenRepository;
import com.ai.application.model.Entity.Contact;
import com.ai.application.model.Entity.UserToken;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for QuickFlowDetectionService.
 * 
 * Tests cover:
 * - Detection of QuickFlow users among contacts
 * - User-specific detection
 * - Handling of null emails
 * - Case-insensitive email matching
 * 
 * Note: Repositories are mocked to isolate service logic.
 */
@ExtendWith(MockitoExtension.class)
class QuickFlowDetectionServiceTest {

    @Mock
    private ContactRepository contactRepository;

    @Mock
    private UserTokenRepository userTokenRepository;

    private QuickFlowDetectionService quickFlowDetectionService;

    @BeforeEach
    void setUp() {
        quickFlowDetectionService = new QuickFlowDetectionService();
        try {
            java.lang.reflect.Field contactRepoField = QuickFlowDetectionService.class.getDeclaredField("contactRepository");
            contactRepoField.setAccessible(true);
            contactRepoField.set(quickFlowDetectionService, contactRepository);
            
            java.lang.reflect.Field userTokenRepoField = QuickFlowDetectionService.class.getDeclaredField("userTokenRepository");
            userTokenRepoField.setAccessible(true);
            userTokenRepoField.set(quickFlowDetectionService, userTokenRepository);
        } catch (Exception e) {
            throw new RuntimeException("Failed to inject mocks", e);
        }
    }

    @Nested
    @DisplayName("detectForUser()")
    class DetectForUserTests {

        @Test
        @DisplayName("Should detect QuickFlow user among contacts")
        void detectQuickFlowUserAmongContacts() {
            String userId = "user-123";
            
            // Setup QuickFlow users
            UserToken qfUser = new UserToken("qf-user-1", "quickflow@example.com", "google");
            when(userTokenRepository.findAll()).thenReturn(Arrays.asList(qfUser));
            
            // Setup contacts - one matches QuickFlow user
            Contact contact1 = new Contact(userId, "QF User", "quickflow@example.com", "google");
            contact1.setId("contact-1");
            contact1.setUsesQuickFlow(false);
            
            Contact contact2 = new Contact(userId, "Regular User", "regular@example.com", "google");
            contact2.setId("contact-2");
            contact2.setUsesQuickFlow(false);
            
            when(contactRepository.findByUserIdAndIsDeletedFalse(userId))
                .thenReturn(Arrays.asList(contact1, contact2));
            when(contactRepository.save(any(Contact.class))).thenAnswer(i -> i.getArgument(0));

            int updated = quickFlowDetectionService.detectForUser(userId);

            assertEquals(1, updated);
            assertTrue(contact1.isUsesQuickFlow());
            assertEquals("qf-user-1", contact1.getQuickflowUserId());
            assertFalse(contact2.isUsesQuickFlow());
        }

        @Test
        @DisplayName("Should handle case-insensitive email matching")
        void handleCaseInsensitiveEmailMatching() {
            String userId = "user-123";
            
            UserToken qfUser = new UserToken("qf-user-1", "QUICKFLOW@EXAMPLE.COM", "google");
            when(userTokenRepository.findAll()).thenReturn(Arrays.asList(qfUser));
            
            Contact contact = new Contact(userId, "QF User", "quickflow@example.com", "google");
            contact.setId("contact-1");
            contact.setUsesQuickFlow(false);
            
            when(contactRepository.findByUserIdAndIsDeletedFalse(userId))
                .thenReturn(Arrays.asList(contact));
            when(contactRepository.save(any(Contact.class))).thenAnswer(i -> i.getArgument(0));

            int updated = quickFlowDetectionService.detectForUser(userId);

            assertEquals(1, updated);
            assertTrue(contact.isUsesQuickFlow());
        }

        @Test
        @DisplayName("Should skip contacts with null email")
        void skipContactsWithNullEmail() {
            String userId = "user-123";
            
            UserToken qfUser = new UserToken("qf-user-1", "quickflow@example.com", "google");
            when(userTokenRepository.findAll()).thenReturn(Arrays.asList(qfUser));
            
            Contact contactWithNullEmail = new Contact(userId, "No Email", null, "manual");
            contactWithNullEmail.setId("contact-1");
            
            when(contactRepository.findByUserIdAndIsDeletedFalse(userId))
                .thenReturn(Arrays.asList(contactWithNullEmail));

            int updated = quickFlowDetectionService.detectForUser(userId);

            assertEquals(0, updated);
            verify(contactRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should clear QuickFlow status when user is no longer registered")
        void clearQuickFlowStatusWhenNoLongerRegistered() {
            String userId = "user-123";
            
            // No QuickFlow users
            when(userTokenRepository.findAll()).thenReturn(Collections.emptyList());
            
            // Contact was previously marked as QuickFlow user
            Contact contact = new Contact(userId, "Former QF User", "former@example.com", "google");
            contact.setId("contact-1");
            contact.setUsesQuickFlow(true);
            contact.setQuickflowUserId("old-qf-id");
            
            when(contactRepository.findByUserIdAndIsDeletedFalse(userId))
                .thenReturn(Arrays.asList(contact));
            when(contactRepository.save(any(Contact.class))).thenAnswer(i -> i.getArgument(0));

            int updated = quickFlowDetectionService.detectForUser(userId);

            assertEquals(1, updated);
            assertFalse(contact.isUsesQuickFlow());
            assertNull(contact.getQuickflowUserId());
        }

        @Test
        @DisplayName("Should not update when status unchanged")
        void notUpdateWhenStatusUnchanged() {
            String userId = "user-123";
            
            UserToken qfUser = new UserToken("qf-user-1", "quickflow@example.com", "google");
            when(userTokenRepository.findAll()).thenReturn(Arrays.asList(qfUser));
            
            // Contact already marked correctly
            Contact contact = new Contact(userId, "QF User", "quickflow@example.com", "google");
            contact.setId("contact-1");
            contact.setUsesQuickFlow(true);
            contact.setQuickflowUserId("qf-user-1");
            
            when(contactRepository.findByUserIdAndIsDeletedFalse(userId))
                .thenReturn(Arrays.asList(contact));

            int updated = quickFlowDetectionService.detectForUser(userId);

            assertEquals(0, updated);
            verify(contactRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should return zero when no contacts exist")
        void returnZeroWhenNoContacts() {
            String userId = "user-123";
            
            when(userTokenRepository.findAll()).thenReturn(Arrays.asList(
                new UserToken("qf-1", "user@example.com", "google")
            ));
            when(contactRepository.findByUserIdAndIsDeletedFalse(userId))
                .thenReturn(Collections.emptyList());

            int updated = quickFlowDetectionService.detectForUser(userId);

            assertEquals(0, updated);
        }

        @Test
        @DisplayName("Should filter out null and empty emails from QuickFlow users")
        void filterOutNullAndEmptyEmails() {
            String userId = "user-123";
            
            UserToken userWithNullEmail = new UserToken("qf-1", null, "google");
            UserToken userWithEmptyEmail = new UserToken("qf-2", "", "google");
            UserToken userWithValidEmail = new UserToken("qf-3", "valid@example.com", "google");
            
            when(userTokenRepository.findAll()).thenReturn(Arrays.asList(
                userWithNullEmail, userWithEmptyEmail, userWithValidEmail
            ));
            
            Contact contact = new Contact(userId, "Valid", "valid@example.com", "google");
            contact.setId("contact-1");
            contact.setUsesQuickFlow(false);
            
            when(contactRepository.findByUserIdAndIsDeletedFalse(userId))
                .thenReturn(Arrays.asList(contact));
            when(contactRepository.save(any(Contact.class))).thenAnswer(i -> i.getArgument(0));

            int updated = quickFlowDetectionService.detectForUser(userId);

            assertEquals(1, updated);
            assertTrue(contact.isUsesQuickFlow());
        }
    }

    @Nested
    @DisplayName("detectQuickFlowUsers() - scheduled job")
    class DetectQuickFlowUsersTests {

        @Test
        @DisplayName("Should process all contacts from all users")
        void processAllContactsFromAllUsers() {
            // Setup QuickFlow users
            UserToken qfUser = new UserToken("qf-user-1", "quickflow@example.com", "google");
            when(userTokenRepository.findAll()).thenReturn(Arrays.asList(qfUser));
            
            // Setup contacts from different users
            Contact contact1 = new Contact("user-1", "Contact 1", "quickflow@example.com", "google");
            contact1.setId("c1");
            contact1.setUsesQuickFlow(false);
            
            Contact contact2 = new Contact("user-2", "Contact 2", "other@example.com", "google");
            contact2.setId("c2");
            contact2.setUsesQuickFlow(false);
            
            when(contactRepository.findAll()).thenReturn(Arrays.asList(contact1, contact2));
            when(contactRepository.save(any(Contact.class))).thenAnswer(i -> i.getArgument(0));

            quickFlowDetectionService.detectQuickFlowUsers();

            // Only contact1 should be updated (matches QuickFlow user)
            verify(contactRepository, times(1)).save(any(Contact.class));
            assertTrue(contact1.isUsesQuickFlow());
            assertFalse(contact2.isUsesQuickFlow());
        }
    }
}
