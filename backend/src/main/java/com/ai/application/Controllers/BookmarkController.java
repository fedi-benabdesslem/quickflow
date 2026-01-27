package com.ai.application.Controllers;

import com.ai.application.Repositories.BookmarkRepository;
import com.ai.application.model.Entity.Bookmark;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/bookmarks")
@CrossOrigin(origins = "http://localhost:5173")
public class BookmarkController {

    @Autowired
    private BookmarkRepository bookmarkRepository;

    // TODO: integrate with real authentication service
    private String getCurrentUserId() {
        return "user-123";
    }

    @GetMapping
    public ResponseEntity<List<Bookmark>> getBookmarks() {
        return ResponseEntity.ok(bookmarkRepository.findByUserId(getCurrentUserId()));
    }

    @PostMapping
    public ResponseEntity<?> addBookmark(@RequestBody Map<String, String> payload) {
        String itemId = payload.get("itemId");
        String itemType = payload.get("type");
        String userId = getCurrentUserId();

        if (itemId == null || itemType == null) {
            return ResponseEntity.badRequest().body("itemId and type are required");
        }

        if (bookmarkRepository.findByUserIdAndItemId(userId, itemId).isPresent()) {
            return ResponseEntity.ok().build(); // Already exists
        }

        Bookmark bookmark = new Bookmark(userId, itemId, itemType, "default");
        bookmarkRepository.save(bookmark);
        return ResponseEntity.ok(bookmark);
    }

    @DeleteMapping("/{itemId}")
    public ResponseEntity<?> removeBookmark(@PathVariable String itemId) {
        Optional<Bookmark> bookmark = bookmarkRepository.findByUserIdAndItemId(getCurrentUserId(), itemId);
        if (bookmark.isPresent()) {
            bookmarkRepository.delete(bookmark.get());
            return ResponseEntity.ok().build();
        }
        return ResponseEntity.notFound().build();
    }
}
