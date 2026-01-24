package com.ai.application.Repositories;

import com.ai.application.model.Entity.MeetingTemplate;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.List;

public interface MeetingTemplateRepository extends MongoRepository<MeetingTemplate, String> {
    List<MeetingTemplate> findByUserId(String userId);

    List<MeetingTemplate> findByUserIdOrderByLastUsedDesc(String userId);

    boolean existsByUserIdAndName(String userId, String name);
}
