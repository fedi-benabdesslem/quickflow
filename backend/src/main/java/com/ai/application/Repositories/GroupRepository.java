package com.ai.application.Repositories;

import com.ai.application.model.Entity.Group;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for Group entity operations.
 */
@Repository
public interface GroupRepository extends MongoRepository<Group, String> {

    // Find all groups for a user
    List<Group> findByUserId(String userId);

    // Find a group by user ID and name
    Optional<Group> findByUserIdAndName(String userId, String name);

    // Find groups containing a specific member
    List<Group> findByUserIdAndMemberIdsContaining(String userId, String memberId);

    // Count groups for a user
    long countByUserId(String userId);
}
