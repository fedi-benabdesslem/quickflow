package com.ai.application.Repositories;

import com.ai.application.model.Entity.UserToken;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository for UserToken entity.
 */
@Repository
public interface UserTokenRepository extends MongoRepository<UserToken, String> {

    /**
     * Find a user token by Supabase user ID.
     */
    Optional<UserToken> findBySupabaseId(String supabaseId);

    /**
     * Find a user token by email.
     */
    Optional<UserToken> findByEmail(String email);

    /**
     * Delete all tokens for a user.
     */
    void deleteBySupabaseId(String supabaseId);
}
