package com.ai.application.Services;

import com.ai.application.Repositories.ContactRepository;
import com.ai.application.model.Entity.Contact;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for ContactService.
 * 
 * Tests cover:
 * - Contact CRUD operations
 * - Contact import and sync
 * - Contact search and filtering
 * - Favorite toggling
 * - Usage tracking
 * 
 * Note: Repository is mocked to isolate ContactService logic.
 */
@ExtendWith(MockitoExtension.class)
class ContactServiceTest {

    @Mock
    private ContactRepository contactRepository;

    private ContactService contactService;

    @BeforeEach
    void setUp() {
        contactService = new ContactService();
        try {
            java.lang.reflect.Field contactRepoField = ContactService.class.getDeclaredField("contactRepository");
            contactRepoField.setAccessible(true);
            contactRepoField.set(contactService, contactRepository);
        } catch (Exception e) {
            throw new RuntimeException("Failed to inject mocks", e);
        }
    }

    @Nested
    @DisplayName("createContact()")
    class CreateContactTests {

        @Test
        @DisplayName("Should create a manual contact")
        void createManualContact() {
            String userId = "user-123";
            String email = "new@example.com";
            
            when(contactRepository.findByUserIdAndEmail(userId, email)).thenReturn(Optional.empty());
            when(contactRepository.save(any(Contact.class))).thenAnswer(i -> {
                Contact c = i.getArgument(0);
                c.setId("contact-id-123");
                return c;
            });

            Contact result = contactService.createContact(userId, "John Doe", email, "1234567890", null);

            assertNotNull(result);
            assertEquals("John Doe", result.getName());
            assertEquals(email, result.getEmail());
            assertEquals("manual", result.getSource());
            assertEquals("1234567890", result.getPhone());
        }

        @Test
        @DisplayName("Should throw exception for duplicate email")
        void throwExceptionForDuplicateEmail() {
            String userId = "user-123";
            String email = "existing@example.com";
            
            Contact existing = new Contact(userId, "Existing User", email, "google");
            when(contactRepository.findByUserIdAndEmail(userId, email)).thenReturn(Optional.of(existing));

            assertThrows(IllegalArgumentException.class,
                () -> contactService.createContact(userId, "New User", email, null, null));
            
            verify(contactRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("updateContact()")
    class UpdateContactTests {

        @Test
        @DisplayName("Should update manual contact fields")
        void updateManualContactFields() {
            String contactId = "contact-123";
            Contact contact = new Contact("user-123", "Old Name", "old@example.com", "manual");
            contact.setId(contactId);
            
            when(contactRepository.findById(contactId)).thenReturn(Optional.of(contact));
            when(contactRepository.findByUserIdAndEmail(anyString(), anyString())).thenReturn(Optional.empty());
            when(contactRepository.save(any(Contact.class))).thenAnswer(i -> i.getArgument(0));

            Contact result = contactService.updateContact(
                contactId, "New Name", "new@example.com", "555-1234", null, null, null);

            assertEquals("New Name", result.getName());
            assertEquals("new@example.com", result.getEmail());
            assertEquals("555-1234", result.getPhone());
        }

        @Test
        @DisplayName("Should only update metadata for OAuth contacts")
        void onlyUpdateMetadataForOAuthContacts() {
            String contactId = "contact-123";
            Contact contact = new Contact("user-123", "Google User", "google@example.com", "google");
            contact.setId(contactId);
            
            when(contactRepository.findById(contactId)).thenReturn(Optional.of(contact));
            when(contactRepository.save(any(Contact.class))).thenAnswer(i -> i.getArgument(0));

            Contact result = contactService.updateContact(
                contactId, "Changed Name", "changed@example.com", "555-1234", null, 
                Arrays.asList("group1"), true);

            // Name, email, phone should NOT change for OAuth contacts
            assertEquals("Google User", result.getName());
            assertEquals("google@example.com", result.getEmail());
            
            // But metadata should be updated
            assertEquals(Arrays.asList("group1"), result.getGroups());
            assertTrue(result.isFavorite());
        }

        @Test
        @DisplayName("Should throw exception when contact not found")
        void throwExceptionWhenNotFound() {
            when(contactRepository.findById("nonexistent")).thenReturn(Optional.empty());

            assertThrows(NoSuchElementException.class,
                () -> contactService.updateContact("nonexistent", null, null, null, null, null, null));
        }
    }

    @Nested
    @DisplayName("deleteContact()")
    class DeleteContactTests {

        @Test
        @DisplayName("Should soft delete manual contact")
        void softDeleteManualContact() {
            String contactId = "contact-123";
            Contact contact = new Contact("user-123", "Manual User", "manual@example.com", "manual");
            contact.setId(contactId);
            
            when(contactRepository.findById(contactId)).thenReturn(Optional.of(contact));
            when(contactRepository.save(any(Contact.class))).thenAnswer(i -> i.getArgument(0));

            contactService.deleteContact(contactId);

            assertTrue(contact.isDeleted());
            assertFalse(contact.isIgnored()); // Manual contacts are not ignored
            verify(contactRepository).save(contact);
        }

        @Test
        @DisplayName("Should soft delete and ignore OAuth contact")
        void softDeleteAndIgnoreOAuthContact() {
            String contactId = "contact-123";
            Contact contact = new Contact("user-123", "OAuth User", "oauth@example.com", "google");
            contact.setId(contactId);
            
            when(contactRepository.findById(contactId)).thenReturn(Optional.of(contact));
            when(contactRepository.save(any(Contact.class))).thenAnswer(i -> i.getArgument(0));

            contactService.deleteContact(contactId);

            assertTrue(contact.isDeleted());
            assertTrue(contact.isIgnored()); // OAuth contacts are ignored to prevent re-import
        }
    }

    @Nested
    @DisplayName("toggleFavorite()")
    class ToggleFavoriteTests {

        @Test
        @DisplayName("Should toggle favorite on")
        void toggleFavoriteOn() {
            String contactId = "contact-123";
            Contact contact = new Contact("user-123", "User", "user@example.com", "manual");
            contact.setId(contactId);
            contact.setFavorite(false);
            
            when(contactRepository.findById(contactId)).thenReturn(Optional.of(contact));
            when(contactRepository.save(any(Contact.class))).thenAnswer(i -> i.getArgument(0));

            Contact result = contactService.toggleFavorite(contactId);

            assertTrue(result.isFavorite());
        }

        @Test
        @DisplayName("Should toggle favorite off")
        void toggleFavoriteOff() {
            String contactId = "contact-123";
            Contact contact = new Contact("user-123", "User", "user@example.com", "manual");
            contact.setId(contactId);
            contact.setFavorite(true);
            
            when(contactRepository.findById(contactId)).thenReturn(Optional.of(contact));
            when(contactRepository.save(any(Contact.class))).thenAnswer(i -> i.getArgument(0));

            Contact result = contactService.toggleFavorite(contactId);

            assertFalse(result.isFavorite());
        }
    }

    @Nested
    @DisplayName("getContacts()")
    class GetContactsTests {

        @Test
        @DisplayName("Should get all contacts for user")
        void getAllContactsForUser() {
            String userId = "user-123";
            List<Contact> contacts = Arrays.asList(
                new Contact(userId, "Alice", "alice@example.com", "google"),
                new Contact(userId, "Bob", "bob@example.com", "manual")
            );
            
            when(contactRepository.findByUserIdAndIsDeletedFalse(userId)).thenReturn(contacts);

            List<Contact> result = contactService.getContacts(userId, null, null, null);

            assertEquals(2, result.size());
        }

        @Test
        @DisplayName("Should filter by favorites")
        void filterByFavorites() {
            String userId = "user-123";
            Contact favorite = new Contact(userId, "Favorite", "fav@example.com", "google");
            favorite.setFavorite(true);
            
            when(contactRepository.findByUserIdAndIsFavoriteAndIsDeletedFalse(userId, true))
                .thenReturn(Arrays.asList(favorite));

            List<Contact> result = contactService.getContacts(userId, "favorites", null, null);

            assertEquals(1, result.size());
            assertTrue(result.get(0).isFavorite());
        }

        @Test
        @DisplayName("Should filter by QuickFlow users")
        void filterByQuickFlowUsers() {
            String userId = "user-123";
            Contact qfUser = new Contact(userId, "QuickFlow User", "qf@example.com", "google");
            qfUser.setUsesQuickFlow(true);
            
            when(contactRepository.findByUserIdAndUsesQuickFlowAndIsDeletedFalse(userId, true))
                .thenReturn(Arrays.asList(qfUser));

            List<Contact> result = contactService.getContacts(userId, "quickflow", null, null);

            assertEquals(1, result.size());
            assertTrue(result.get(0).isUsesQuickFlow());
        }

        @Test
        @DisplayName("Should filter by source")
        void filterBySource() {
            String userId = "user-123";
            Contact googleContact = new Contact(userId, "Google User", "g@example.com", "google");
            
            when(contactRepository.findByUserIdAndSourceAndIsDeletedFalse(userId, "google"))
                .thenReturn(Arrays.asList(googleContact));

            List<Contact> result = contactService.getContacts(userId, null, "google", null);

            assertEquals(1, result.size());
            assertEquals("google", result.get(0).getSource());
        }

        @Test
        @DisplayName("Should sort by name ascending by default")
        void sortByNameAscending() {
            String userId = "user-123";
            Contact contactA = new Contact(userId, "Zara", "zara@example.com", "manual");
            Contact contactB = new Contact(userId, "Alice", "alice@example.com", "manual");
            Contact contactC = new Contact(userId, "Mike", "mike@example.com", "manual");
            
            when(contactRepository.findByUserIdAndIsDeletedFalse(userId))
                .thenReturn(new ArrayList<>(Arrays.asList(contactA, contactB, contactC)));

            List<Contact> result = contactService.getContacts(userId, null, null, null);

            assertEquals("Alice", result.get(0).getName());
            assertEquals("Mike", result.get(1).getName());
            assertEquals("Zara", result.get(2).getName());
        }

        @Test
        @DisplayName("Should sort by name descending")
        void sortByNameDescending() {
            String userId = "user-123";
            Contact contactA = new Contact(userId, "Alice", "alice@example.com", "manual");
            Contact contactB = new Contact(userId, "Zara", "zara@example.com", "manual");
            
            when(contactRepository.findByUserIdAndIsDeletedFalse(userId))
                .thenReturn(new ArrayList<>(Arrays.asList(contactA, contactB)));

            List<Contact> result = contactService.getContacts(userId, null, null, "name_desc");

            assertEquals("Zara", result.get(0).getName());
            assertEquals("Alice", result.get(1).getName());
        }
    }

    @Nested
    @DisplayName("searchContacts()")
    class SearchContactsTests {

        @Test
        @DisplayName("Should return empty list for short query")
        void returnEmptyForShortQuery() {
            List<Contact> result = contactService.searchContacts("user-123", "a", 10);

            assertTrue(result.isEmpty());
            verifyNoInteractions(contactRepository);
        }

        @Test
        @DisplayName("Should return empty list for null query")
        void returnEmptyForNullQuery() {
            List<Contact> result = contactService.searchContacts("user-123", null, 10);

            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("Should search and limit results")
        void searchAndLimitResults() {
            String userId = "user-123";
            List<Contact> searchResults = new ArrayList<>();
            for (int i = 0; i < 10; i++) {
                Contact c = new Contact(userId, "User " + i, "user" + i + "@example.com", "manual");
                c.setUsageCount(10 - i); // Different usage counts
                searchResults.add(c);
            }
            
            when(contactRepository.searchByNameOrEmail(userId, "User")).thenReturn(searchResults);

            List<Contact> result = contactService.searchContacts(userId, "User", 5);

            assertEquals(5, result.size());
        }

        @Test
        @DisplayName("Should sort favorites first")
        void sortFavoritesFirst() {
            String userId = "user-123";
            Contact regular = new Contact(userId, "Regular", "regular@example.com", "manual");
            regular.setFavorite(false);
            regular.setUsageCount(100);
            
            Contact favorite = new Contact(userId, "Favorite", "favorite@example.com", "manual");
            favorite.setFavorite(true);
            favorite.setUsageCount(1);
            
            when(contactRepository.searchByNameOrEmail(userId, "test"))
                .thenReturn(new ArrayList<>(Arrays.asList(regular, favorite)));

            List<Contact> result = contactService.searchContacts(userId, "test", 10);

            assertEquals("Favorite", result.get(0).getName());
            assertEquals("Regular", result.get(1).getName());
        }
    }

    @Nested
    @DisplayName("incrementUsage()")
    class IncrementUsageTests {

        @Test
        @DisplayName("Should increment usage count")
        void incrementUsageCount() {
            String contactId = "contact-123";
            Contact contact = new Contact("user-123", "User", "user@example.com", "manual");
            contact.setId(contactId);
            contact.setUsageCount(5);
            
            when(contactRepository.findById(contactId)).thenReturn(Optional.of(contact));
            when(contactRepository.save(any(Contact.class))).thenAnswer(i -> i.getArgument(0));

            contactService.incrementUsage(contactId);

            assertEquals(6, contact.getUsageCount());
            assertNotNull(contact.getLastUsed());
        }

        @Test
        @DisplayName("Should do nothing for non-existent contact")
        void doNothingForNonExistent() {
            when(contactRepository.findById("nonexistent")).thenReturn(Optional.empty());

            contactService.incrementUsage("nonexistent");

            verify(contactRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("getContactCount()")
    class GetContactCountTests {

        @Test
        @DisplayName("Should return count for user")
        void returnCountForUser() {
            String userId = "user-123";
            when(contactRepository.countByUserIdAndIsDeletedFalse(userId)).thenReturn(15L);

            long count = contactService.getContactCount(userId);

            assertEquals(15L, count);
        }
    }

    @Nested
    @DisplayName("importContacts()")
    class ImportContactsTests {

        @Test
        @DisplayName("Should import new contacts")
        void importNewContacts() {
            String userId = "user-123";
            String source = "google";
            
            Contact newContact = new Contact(userId, "New User", "new@example.com", source);
            newContact.setSourceId("google-id-123");
            
            when(contactRepository.findByUserIdAndSourceAndIsIgnoredFalse(userId, source))
                .thenReturn(new ArrayList<>());
            when(contactRepository.findByUserIdAndSourceId(userId, "google-id-123"))
                .thenReturn(Optional.empty());
            when(contactRepository.findByUserIdAndEmail(userId, "new@example.com"))
                .thenReturn(Optional.empty());
            when(contactRepository.save(any(Contact.class))).thenAnswer(i -> i.getArgument(0));

            Map<String, Integer> result = contactService.importContacts(userId, Arrays.asList(newContact), source);

            assertEquals(1, result.get("imported"));
            assertEquals(0, result.get("updated"));
        }

        @Test
        @DisplayName("Should update existing contacts")
        void updateExistingContacts() {
            String userId = "user-123";
            String source = "google";
            
            Contact existingContact = new Contact(userId, "Old Name", "existing@example.com", source);
            existingContact.setSourceId("google-id-123");
            
            Contact updatedContact = new Contact(userId, "New Name", "existing@example.com", source);
            updatedContact.setSourceId("google-id-123");
            
            when(contactRepository.findByUserIdAndSourceAndIsIgnoredFalse(userId, source))
                .thenReturn(new ArrayList<>(Arrays.asList(existingContact)));
            when(contactRepository.findByUserIdAndSourceId(userId, "google-id-123"))
                .thenReturn(Optional.of(existingContact));
            when(contactRepository.save(any(Contact.class))).thenAnswer(i -> i.getArgument(0));

            Map<String, Integer> result = contactService.importContacts(userId, Arrays.asList(updatedContact), source);

            assertEquals(0, result.get("imported"));
            assertEquals(1, result.get("updated"));
            assertEquals("New Name", existingContact.getName());
        }

        @Test
        @DisplayName("Should skip ignored contacts")
        void skipIgnoredContacts() {
            String userId = "user-123";
            String source = "google";
            
            Contact ignoredContact = new Contact(userId, "Ignored", "ignored@example.com", source);
            ignoredContact.setIgnored(true);
            
            Contact newContact = new Contact(userId, "New", "ignored@example.com", source);
            newContact.setSourceId("google-id-new");
            
            when(contactRepository.findByUserIdAndSourceAndIsIgnoredFalse(userId, source))
                .thenReturn(new ArrayList<>());
            when(contactRepository.findByUserIdAndSourceId(userId, "google-id-new"))
                .thenReturn(Optional.empty());
            when(contactRepository.findByUserIdAndEmail(userId, "ignored@example.com"))
                .thenReturn(Optional.of(ignoredContact));

            Map<String, Integer> result = contactService.importContacts(userId, Arrays.asList(newContact), source);

            assertEquals(0, result.get("imported"));
            verify(contactRepository, never()).save(newContact);
        }
    }
}
