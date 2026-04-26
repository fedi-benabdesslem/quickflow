package com.ai.application.Repositories;

import com.ai.application.model.Entity.UserSession;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SessionRepository extends MongoRepository<UserSession, String> {

    List<UserSession> findByUserIdAndRevokedFalse(String userId);

    List<UserSession> findByUserId(String userId);

    void deleteByUserId(String userId);
}
