package com.ai.application.Repositories;

import com.ai.application.model.Entity.Contact;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for Contact entity operations.
 */
@Repository
public interface ContactRepository extends MongoRepository<Contact, String> {

    // Find all contacts for a user (excluding deleted)
    List<Contact> findByUserIdAndIsDeletedFalse(String userId);

    // Find all contacts for a user with pagination
    Page<Contact> findByUserIdAndIsDeletedFalse(String userId, Pageable pageable);

    // Find a contact by user ID and email
    Optional<Contact> findByUserIdAndEmail(String userId, String email);

    // Find by user ID and source ID (for syncing)
    Optional<Contact> findByUserIdAndSourceId(String userId, String sourceId);

    // Find favorites for a user
    List<Contact> findByUserIdAndIsFavoriteAndIsDeletedFalse(String userId, boolean isFavorite);

    // Find by source (google, microsoft, manual)
    List<Contact> findByUserIdAndSourceAndIsDeletedFalse(String userId, String source);

    // Find contacts that use QuickFlow
    List<Contact> findByUserIdAndUsesQuickFlowAndIsDeletedFalse(String userId, boolean usesQuickFlow);

    // Search contacts by name or email (case-insensitive)
    @Query("{'userId': ?0, 'isDeleted': false, '$or': [{'name': {$regex: ?1, $options: 'i'}}, {'email': {$regex: ?1, $options: 'i'}}]}")
    List<Contact> searchByNameOrEmail(String userId, String searchTerm);

    // Find contacts by group
    List<Contact> findByUserIdAndGroupsContainingAndIsDeletedFalse(String userId, String groupName);

    // Find contacts by multiple emails (for QuickFlow detection)
    List<Contact> findByEmailIn(List<String> emails);

    // Find contacts by source for syncing
    List<Contact> findByUserIdAndSourceAndIsIgnoredFalse(String userId, String source);

    // Count contacts for a user
    long countByUserIdAndIsDeletedFalse(String userId);

    // Find most recently used contacts
    List<Contact> findByUserIdAndIsDeletedFalseOrderByUsageCountDescLastUsedDesc(String userId, Pageable pageable);
}
