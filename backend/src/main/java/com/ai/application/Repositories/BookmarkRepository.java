package com.ai.application.Repositories;

import com.ai.application.model.Entity.Bookmark;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface BookmarkRepository extends MongoRepository<Bookmark, String> {
    List<Bookmark> findByUserId(String userId);

    List<Bookmark> findByUserIdAndCategoryName(String userId, String categoryName);

    Optional<Bookmark> findByUserIdAndItemId(String userId, String itemId);

    void deleteByUserIdAndCategoryName(String userId, String categoryName);

    // Check if category exists for user (using count or exists)
    boolean existsByUserIdAndCategoryName(String userId, String categoryName);
}
