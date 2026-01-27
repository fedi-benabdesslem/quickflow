package com.ai.application.Services;

import com.ai.application.Repositories.ContactRepository;
import com.ai.application.model.Entity.Contact;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;

import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for managing contacts including import, sync, and CRUD operations.
 */
@Service
public class ContactService {

    private static final Logger logger = LoggerFactory.getLogger(ContactService.class);

    @Autowired
    private ContactRepository contactRepository;

    /**
     * Import contacts from a provider (Google/Microsoft).
     * Updates existing contacts and creates new ones.
     * Removes contacts that are no longer in the source.
     * 
     * @param userId   The user's ID
     * @param contacts List of contacts from the provider
     * @param source   "google" or "microsoft"
     * @return Map with counts of imported, updated, and deleted contacts
     */
    public Map<String, Integer> importContacts(String userId, List<Contact> contacts, String source) {
        logger.info("Importing {} contacts from {} for user {}", contacts.size(), source, userId);

        int imported = 0;
        int updated = 0;
        int deleted = 0;

        // Get existing contacts from this source
        List<Contact> existingContacts = contactRepository.findByUserIdAndSourceAndIsIgnoredFalse(userId, source);
        existingContacts.stream()
                .filter(c -> c.getSourceId() != null)
                .map(Contact::getSourceId)
                .collect(Collectors.toSet());

        Set<String> newSourceIds = contacts.stream()
                .filter(c -> c.getSourceId() != null)
                .map(Contact::getSourceId)
                .collect(Collectors.toSet());

        // Process each contact from the provider
        for (Contact contact : contacts) {
            contact.setUserId(userId);
            contact.setSource(source);
            contact.markSynced();

            // Check if contact already exists by email or sourceId
            Optional<Contact> existing = Optional.empty();

            if (contact.getSourceId() != null) {
                existing = contactRepository.findByUserIdAndSourceId(userId, contact.getSourceId());
            }

            if (existing.isEmpty() && contact.getEmail() != null) {
                existing = contactRepository.findByUserIdAndEmail(userId, contact.getEmail());
            }

            if (existing.isPresent()) {
                // Update existing contact (preserve user-specific fields)
                Contact existingContact = existing.get();
                existingContact.setName(contact.getName());
                existingContact.setEmail(contact.getEmail());
                existingContact.setPhone(contact.getPhone());
                existingContact.setPhoto(contact.getPhoto());
                existingContact.setSourceId(contact.getSourceId());
                existingContact.markSynced();
                existingContact.setDeleted(false); // Restore if was deleted
                contactRepository.save(existingContact);
                updated++;
            } else {
                // Check if this contact should be ignored
                Optional<Contact> ignored = contactRepository.findByUserIdAndEmail(userId, contact.getEmail());
                if (ignored.isPresent() && ignored.get().isIgnored()) {
                    continue; // Skip ignored contacts
                }

                // Create new contact
                contactRepository.save(contact);
                imported++;
            }
        }

        // Mark contacts as deleted if they're no longer in the source
        // (User deleted them from Google/Microsoft)
        for (Contact existing : existingContacts) {
            if (existing.getSourceId() != null && !newSourceIds.contains(existing.getSourceId())) {
                existing.setDeleted(true);
                existing.setUpdatedAt(LocalDateTime.now());
                contactRepository.save(existing);
                deleted++;
            }
        }

        Map<String, Integer> result = new HashMap<>();
        result.put("imported", imported);
        result.put("updated", updated);
        result.put("deleted", deleted);
        result.put("total", imported + updated);

        logger.info("Import complete: {} imported, {} updated, {} deleted", imported, updated, deleted);
        return result;
    }

    /**
     * Get all contacts for a user with optional filtering.
     */
    public List<Contact> getContacts(String userId, String filter, String source, String sortBy) {
        List<Contact> contacts;

        if ("favorites".equalsIgnoreCase(filter)) {
            contacts = contactRepository.findByUserIdAndIsFavoriteAndIsDeletedFalse(userId, true);
        } else if ("quickflow".equalsIgnoreCase(filter)) {
            contacts = contactRepository.findByUserIdAndUsesQuickFlowAndIsDeletedFalse(userId, true);
        } else if (source != null && !source.isEmpty() && !"all".equalsIgnoreCase(source)) {
            contacts = contactRepository.findByUserIdAndSourceAndIsDeletedFalse(userId, source.toLowerCase());
        } else {
            contacts = contactRepository.findByUserIdAndIsDeletedFalse(userId);
        }

        // Sort
        if ("name_desc".equalsIgnoreCase(sortBy)) {
            contacts.sort((a, b) -> {
                String nameA = a.getName() != null ? a.getName() : "";
                String nameB = b.getName() != null ? b.getName() : "";
                return nameB.compareToIgnoreCase(nameA);
            });
        } else if ("recent".equalsIgnoreCase(sortBy)) {
            contacts.sort((a, b) -> {
                LocalDateTime aDate = a.getCreatedAt() != null ? a.getCreatedAt() : LocalDateTime.MIN;
                LocalDateTime bDate = b.getCreatedAt() != null ? b.getCreatedAt() : LocalDateTime.MIN;
                return bDate.compareTo(aDate);
            });
        } else {
            // Default: name A-Z
            contacts.sort((a, b) -> {
                String nameA = a.getName() != null ? a.getName() : "";
                String nameB = b.getName() != null ? b.getName() : "";
                return nameA.compareToIgnoreCase(nameB);
            });
        }

        return contacts;
    }

    /**
     * Search contacts by name or email.
     */
    public List<Contact> searchContacts(String userId, String query, int limit) {
        if (query == null || query.trim().length() < 2) {
            return Collections.emptyList();
        }

        List<Contact> results = contactRepository.searchByNameOrEmail(userId, query.trim());

        // Sort by: favorites first, then by usage count, then alphabetically
        results.sort((a, b) -> {
            if (a.isFavorite() != b.isFavorite()) {
                return a.isFavorite() ? -1 : 1;
            }
            if (a.getUsageCount() != b.getUsageCount()) {
                return b.getUsageCount() - a.getUsageCount();
            }
            String nameA = a.getName() != null ? a.getName() : "";
            String nameB = b.getName() != null ? b.getName() : "";
            return nameA.compareToIgnoreCase(nameB);
        });

        return results.stream().limit(limit).collect(Collectors.toList());
    }

    /**
     * Get frequently used contacts for autocomplete.
     */
    public List<Contact> getRecentContacts(String userId, int limit) {
        return contactRepository.findByUserIdAndIsDeletedFalseOrderByUsageCountDescLastUsedDesc(
                userId, PageRequest.of(0, limit));
    }

    /**
     * Create a manual contact.
     */
    public Contact createContact(String userId, String name, String email, String phone, String photo) {
        // Check for duplicate email
        Optional<Contact> existing = contactRepository.findByUserIdAndEmail(userId, email);
        if (existing.isPresent()) {
            throw new IllegalArgumentException("A contact with this email already exists");
        }

        Contact contact = new Contact(userId, name, email, "manual");
        contact.setPhone(phone);
        contact.setPhoto(photo);

        return contactRepository.save(contact);
    }

    /**
     * Update a contact.
     */
    public Contact updateContact(String id, String name, String email, String phone, String photo,
            List<String> groups, Boolean isFavorite) {
        Contact contact = contactRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Contact not found"));

        // Only update editable fields based on source
        if ("manual".equals(contact.getSource())) {
            if (name != null)
                contact.setName(name);
            if (email != null) {
                // Check for duplicate email (excluding current contact)
                Optional<Contact> existing = contactRepository.findByUserIdAndEmail(contact.getUserId(), email);
                if (existing.isPresent() && !existing.get().getId().equals(id)) {
                    throw new IllegalArgumentException("A contact with this email already exists");
                }
                contact.setEmail(email);
            }
            if (phone != null)
                contact.setPhone(phone);
            if (photo != null)
                contact.setPhoto(photo);
        }

        // These fields are always editable
        if (groups != null)
            contact.setGroups(groups);
        if (isFavorite != null)
            contact.setFavorite(isFavorite);

        contact.setUpdatedAt(LocalDateTime.now());
        return contactRepository.save(contact);
    }

    /**
     * Delete a contact (soft delete).
     */
    public void deleteContact(String id) {
        Contact contact = contactRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Contact not found"));

        contact.setDeleted(true);

        // If OAuth contact, mark as ignored so it doesn't come back on sync
        if (!"manual".equals(contact.getSource())) {
            contact.setIgnored(true);
        }

        contact.setUpdatedAt(LocalDateTime.now());
        contactRepository.save(contact);
    }

    /**
     * Toggle favorite status.
     */
    public Contact toggleFavorite(String id) {
        Contact contact = contactRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Contact not found"));

        contact.setFavorite(!contact.isFavorite());
        contact.setUpdatedAt(LocalDateTime.now());

        return contactRepository.save(contact);
    }

    /**
     * Increment usage count (called when contact is selected in forms).
     */
    public void incrementUsage(String id) {
        contactRepository.findById(id).ifPresent(contact -> {
            contact.incrementUsage();
            contactRepository.save(contact);
        });
    }

    /**
     * Get contact count for a user.
     */
    public long getContactCount(String userId) {
        return contactRepository.countByUserIdAndIsDeletedFalse(userId);
    }

    /**
     * Find contacts by email for QuickFlow detection.
     */
    public List<Contact> findByEmails(List<String> emails) {
        return contactRepository.findByEmailIn(emails);
    }

    /**
     * Get last sync time for a source.
     */
    public LocalDateTime getLastSyncTime(String userId, String source) {
        List<Contact> contacts = contactRepository.findByUserIdAndSourceAndIsIgnoredFalse(userId, source);
        return contacts.stream()
                .map(Contact::getLastSynced)
                .filter(Objects::nonNull)
                .max(LocalDateTime::compareTo)
                .orElse(null);
    }
}
