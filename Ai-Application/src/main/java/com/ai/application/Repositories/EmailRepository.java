package com.ai.application.Repositories;

import com.ai.application.model.Entity.Email;

import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EmailRepository extends MongoRepository<Email, String> {
		Optional<Email> findEmailById(String id);
}
