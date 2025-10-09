package com.electronica.llmprojectbackend.repo;

import com.electronica.llmprojectbackend.model.EmailRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EmailRequestRepo extends JpaRepository<EmailRequest, Integer> {
    List<EmailRequest> findByUserId(Long userId);
    Optional<EmailRequest> findByIdAndUserId(Integer id, Long userId);
}


