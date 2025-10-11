package com.ai.application.Repositories;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ai.application.model.Entity.GeneratedOutput;

import java.util.Optional;

public interface GeneratedOutputRepository extends JpaRepository<GeneratedOutput, Long> {
    Optional<GeneratedOutput> findByRequestHash(String requestHash);
}