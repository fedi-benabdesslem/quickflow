package com.ai.application.Repositories;

import org.springframework.data.mongodb.repository.MongoRepository;
import com.ai.application.model.Entity.GeneratedOutput;
import java.util.Optional;

public interface GeneratedOutputRepository extends MongoRepository<GeneratedOutput, String> {
    Optional<GeneratedOutput> findByRequestHash(String requestHash);
}