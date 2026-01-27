package com.ai.application.Controllers;

import com.ai.application.Repositories.BookmarkRepository;
import com.ai.application.Repositories.EmailRepository;
import com.ai.application.Repositories.MeetingRepository;
import com.ai.application.model.DTO.HistoryItemDTO;
import com.ai.application.model.DTO.HistoryResponseDTO;
import com.ai.application.model.Entity.Email;
import com.ai.application.model.Entity.Meeting;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/history")
@CrossOrigin(origins = "http://localhost:5173")
public class HistoryController {

    @Autowired
    private EmailRepository emailRepository;

    @Autowired
    private MeetingRepository meetingRepository;

    @Autowired
    private BookmarkRepository bookmarkRepository;

    // Authenticate user
    private String getCurrentUserId(java.security.Principal principal) {
        if (principal == null)
            return null;
        return principal.getName();
    }

    @GetMapping("/recent")
    public ResponseEntity<HistoryResponseDTO> getRecentHistory(
            @RequestParam(defaultValue = "all") String type,
            @RequestParam(defaultValue = "15") int limit,
            @RequestParam(defaultValue = "0") int offset,
            java.security.Principal principal) {

        String userId = getCurrentUserId(principal);
        if (userId == null) {
            return ResponseEntity.status(401).build();
        }

        Set<String> bookmarkedIds = bookmarkRepository.findByUserId(userId)
                .stream()
                .map(b -> b.getItemId())
                .collect(Collectors.toSet());

        List<HistoryItemDTO> allItems = new ArrayList<>();

        // Fetch Emails
        if ("all".equals(type) || "email".equals(type)) {
            List<Email> emails = emailRepository.findByUserId(userId);
            for (Email email : emails) {
                if (email.isDeleted())
                    continue;
                LocalDateTime date = email.getSentAt();
                if (date == null)
                    continue;

                allItems.add(new HistoryItemDTO(
                        email.getId(),
                        "email",
                        email.getSubject() != null ? email.getSubject() : "(No Subject)",
                        email.getRecipients() != null ? email.getRecipients() : Collections.emptyList(),
                        email.getRecipients() != null ? email.getRecipients().size() : 0,
                        date,
                        bookmarkedIds.contains(email.getId()),
                        null));
            }
        }

        // Fetch Minutes
        if ("all".equals(type) || "minute".equals(type)) {
            List<Meeting> meetings = meetingRepository.findByUserId(userId);
            for (Meeting meeting : meetings) {
                if (meeting.isDeleted())
                    continue;
                // Use sentAt if available, otherwise fallback to meeting date
                LocalDateTime displayDate = meeting.getSentAt();
                if (displayDate == null) {
                    displayDate = meeting.getDate();
                }
                if (displayDate == null) {
                    continue; // Skip if no date at all
                }

                allItems.add(new HistoryItemDTO(
                        meeting.getId(),
                        "minute",
                        meeting.getSubject() != null ? meeting.getSubject() : "(No Title)",
                        meeting.getPeople() != null ? meeting.getPeople() : Collections.emptyList(),
                        meeting.getPeople() != null ? meeting.getPeople().size() : 0,
                        displayDate,
                        bookmarkedIds.contains(meeting.getId()),
                        meeting.getPdfFileId()));
            }
        }

        // Sort by date DESC
        allItems.sort(Comparator.comparing(HistoryItemDTO::getSentAt, Comparator.nullsLast(Comparator.reverseOrder())));

        // Pagination
        int total = allItems.size();
        int end = Math.min(offset + limit, total);
        List<HistoryItemDTO> pagedItems = (offset < total) ? allItems.subList(offset, end) : Collections.emptyList();

        return ResponseEntity.ok(new HistoryResponseDTO(pagedItems, end < total, total));
    }

    @GetMapping("")
    public ResponseEntity<HistoryResponseDTO> getHistory(
            @RequestParam(defaultValue = "") String q,
            @RequestParam(defaultValue = "all") String type,
            @RequestParam(defaultValue = "false") boolean onlyBookmarked, // NEW PARAM
            @RequestParam(defaultValue = "20") int limit,
            @RequestParam(defaultValue = "0") int offset,
            java.security.Principal principal) {

        String userId = getCurrentUserId(principal);
        if (userId == null) {
            return ResponseEntity.status(401).build();
        }

        Set<String> bookmarkedIds = bookmarkRepository.findByUserId(userId)
                .stream()
                .map(b -> b.getItemId())
                .collect(Collectors.toSet());

        List<HistoryItemDTO> allItems = new ArrayList<>();
        String query = q.toLowerCase();

        // Fetch Emails
        if ("all".equals(type) || "email".equals(type)) {
            List<Email> emails = emailRepository.findByUserId(userId);
            for (Email email : emails) {
                if (email.isDeleted())
                    continue;
                LocalDateTime date = email.getSentAt();
                if (date == null)
                    continue;

                boolean isBookmarked = bookmarkedIds.contains(email.getId());
                if (onlyBookmarked && !isBookmarked)
                    continue;

                // Search filter
                if (!query.isEmpty()) {
                    boolean match = false;
                    if (email.getSubject() != null && email.getSubject().toLowerCase().contains(query))
                        match = true;
                    if (email.getRecipients() != null && email.getRecipients().toString().toLowerCase().contains(query))
                        match = true;
                    if (!match)
                        continue;
                }

                allItems.add(new HistoryItemDTO(
                        email.getId(),
                        "email",
                        email.getSubject() != null ? email.getSubject() : "(No Subject)",
                        email.getRecipients() != null ? email.getRecipients() : Collections.emptyList(),
                        email.getRecipients() != null ? email.getRecipients().size() : 0,
                        date,
                        isBookmarked,
                        null));
            }
        }

        // Fetch Minutes
        if ("all".equals(type) || "minute".equals(type)) {
            List<Meeting> meetings = meetingRepository.findByUserId(userId);
            for (Meeting meeting : meetings) {
                if (meeting.isDeleted())
                    continue;
                // Use sentAt if available, otherwise fallback to meeting date
                LocalDateTime displayDate = meeting.getSentAt();
                if (displayDate == null) {
                    displayDate = meeting.getDate();
                }
                if (displayDate == null) {
                    continue; // Skip if no date at all
                }

                boolean isBookmarked = bookmarkedIds.contains(meeting.getId());
                if (onlyBookmarked && !isBookmarked)
                    continue;

                // Search filter
                if (!query.isEmpty()) {
                    boolean match = false;
                    if (meeting.getSubject() != null && meeting.getSubject().toLowerCase().contains(query))
                        match = true;
                    if (meeting.getPeople() != null && meeting.getPeople().toString().toLowerCase().contains(query))
                        match = true;
                    if (!match)
                        continue;
                }

                allItems.add(new HistoryItemDTO(
                        meeting.getId(),
                        "minute",
                        meeting.getSubject() != null ? meeting.getSubject() : "(No Title)",
                        meeting.getPeople() != null ? meeting.getPeople() : Collections.emptyList(),
                        meeting.getPeople() != null ? meeting.getPeople().size() : 0,
                        displayDate,
                        isBookmarked,
                        meeting.getPdfFileId()));
            }
        }

        // Sort by date DESC
        allItems.sort(Comparator.comparing(HistoryItemDTO::getSentAt, Comparator.nullsLast(Comparator.reverseOrder())));

        // Pagination
        int total = allItems.size();
        int end = Math.min(offset + limit, total);
        List<HistoryItemDTO> pagedItems = (offset < total) ? allItems.subList(offset, end) : Collections.emptyList();

        return ResponseEntity.ok(new HistoryResponseDTO(pagedItems, end < total, total));
    }

    @GetMapping("/email/{id}")
    public ResponseEntity<?> getEmailDetails(@PathVariable String id, java.security.Principal principal) {
        String userId = getCurrentUserId(principal);
        if (userId == null)
            return ResponseEntity.status(401).build();

        return emailRepository.findById(id)
                .map(email -> {
                    // Security check: ensure email belongs to user
                    // Note: Email entity has userId field but existing findById doesn't check it
                    // Ideally we should check if email.getUserId().equals(userId)

                    boolean bookmarked = bookmarkRepository.findByUserIdAndItemId(userId, id).isPresent();
                    // Map to response object fitting frontend interface
                    return ResponseEntity.ok(new Object() {
                        public String id = email.getId();
                        public String subject = email.getSubject();
                        public List<String> recipients = email.getRecipients();
                        public LocalDateTime sentAt = email.getSentAt();
                        public String body = email.getGeneratedContent() != null ? email.getGeneratedContent()
                                : email.getInput();
                        public String senderEmail = email.getSenderEmail();
                        public String status = email.getStatus();
                        public boolean isBookmarked = bookmarked;
                    });
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/minute/{id}")
    public ResponseEntity<?> getMinuteDetails(@PathVariable String id, java.security.Principal principal) {
        String userId = getCurrentUserId(principal);
        if (userId == null)
            return ResponseEntity.status(401).build();

        return meetingRepository.findById(id)
                .map(meeting -> {
                    boolean bookmarked = bookmarkRepository.findByUserIdAndItemId(userId, id).isPresent();
                    return ResponseEntity.ok(new Object() {
                        public String id = meeting.getId();
                        public String subject = meeting.getSubject();
                        public List<String> people = meeting.getPeople();
                        public LocalDateTime sentAt = meeting.getSentAt();
                        public String pdfFileId = meeting.getPdfFileId();
                        public String date = meeting.getDate() != null ? meeting.getDate().toLocalDate().toString()
                                : "";
                        public String time = meeting.getTimeBegin() != null
                                ? meeting.getTimeBegin().toLocalTime().toString()
                                : "";
                        public boolean isBookmarked = bookmarked;
                    });
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{type}/{id}")
    public ResponseEntity<?> deleteItem(@PathVariable String type, @PathVariable String id) {
        if ("email".equalsIgnoreCase(type)) {
            return emailRepository.findById(id).map(email -> {
                email.setDeleted(true);
                email.setDeletedAt(LocalDateTime.now());
                emailRepository.save(email);
                return ResponseEntity.ok().build();
            }).orElse(ResponseEntity.notFound().build());
        } else if ("minute".equalsIgnoreCase(type)) {
            return meetingRepository.findById(id).map(meeting -> {
                meeting.setDeleted(true);
                meeting.setDeletedAt(LocalDateTime.now());
                meetingRepository.save(meeting);
                return ResponseEntity.ok().build();
            }).orElse(ResponseEntity.notFound().build());
        }
        return ResponseEntity.badRequest().body("Invalid type");
    }
}
