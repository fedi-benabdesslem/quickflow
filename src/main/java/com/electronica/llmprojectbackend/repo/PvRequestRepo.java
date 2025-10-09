package com.electronica.llmprojectbackend.repo;

import com.electronica.llmprojectbackend.model.PvRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PvRequestRepo extends JpaRepository<PvRequest, Integer> {
    List<PvRequest> findByUserId(Long userId);
    Optional<PvRequest> findByIdAndUserId(Integer id, Long userId);
}


