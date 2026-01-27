package com.ai.application.Repositories;

import com.ai.application.model.Entity.Meeting;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface MeetingRepository extends MongoRepository<Meeting, String> {
    java.util.List<Meeting> findByUserId(String userId);
}
