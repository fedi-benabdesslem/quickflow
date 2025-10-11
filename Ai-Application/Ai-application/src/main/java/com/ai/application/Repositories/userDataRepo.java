package com.ai.application.Repositories;

import com.ai.application.model.Entity.userData;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface userDataRepo extends JpaRepository<userData, Long> {
    List<userData> findByUserId(Long userId);
}