package com.ai.application.Repositories;

import com.ai.application.model.Entity.Template;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.List;

public interface TemplateRepository extends MongoRepository<Template, String> {
    List<Template> findByUserId(String userId);

    List<Template> findByUserIdOrderByLastUsedDesc(String userId);

    boolean existsByUserIdAndName(String userId, String name);
}
