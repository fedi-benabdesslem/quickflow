package com.ai.application.Services;

import com.ai.application.Repositories.ContactRepository;
import com.ai.application.Repositories.UserRepository;
import com.ai.application.model.Entity.Contact;
import com.ai.application.model.Entity.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Service for detecting which contacts are also QuickFlow users.
 * Runs periodically to update the usesQuickFlow flag on contacts.
 */
@Service
public class QuickFlowDetectionService {

    private static final Logger logger = LoggerFactory.getLogger(QuickFlowDetectionService.class);

    @Autowired
    private ContactRepository contactRepository;

    @Autowired
    private UserRepository userRepository;

    /**
     * Scheduled job to detect QuickFlow users among contacts.
     * Runs every 6 hours.
     */
    @Scheduled(fixedRate = 6 * 60 * 60 * 1000, initialDelay = 60000)
    public void detectQuickFlowUsers() {
        logger.info("Starting QuickFlow user detection job...");

        try {
            // Get all registered QuickFlow user emails from User collection
            List<User> allUsers = userRepository.findAll();
            Set<String> quickFlowEmails = allUsers.stream()
                    .map(User::getEmail)
                    .filter(email -> email != null && !email.isEmpty())
                    .map(String::toLowerCase)
                    .collect(Collectors.toSet());

            logger.info("Found {} QuickFlow users", quickFlowEmails.size());

            // Get all contacts
            List<Contact> allContacts = contactRepository.findAll();
            int updated = 0;

            for (Contact contact : allContacts) {
                if (contact.getEmail() == null)
                    continue;

                String contactEmail = contact.getEmail().toLowerCase();
                boolean isQuickFlowUser = quickFlowEmails.contains(contactEmail);

                // Only update if the status changed
                if (contact.isUsesQuickFlow() != isQuickFlowUser) {
                    contact.setUsesQuickFlow(isQuickFlowUser);

                    // Find the QuickFlow user ID if they're a user
                    if (isQuickFlowUser) {
                        allUsers.stream()
                                .filter(u -> contactEmail.equalsIgnoreCase(u.getEmail()))
                                .findFirst()
                                .ifPresent(user -> contact.setQuickflowUserId(user.getId()));
                    } else {
                        contact.setQuickflowUserId(null);
                    }

                    contactRepository.save(contact);
                    updated++;
                }
            }

            logger.info("QuickFlow detection complete. Updated {} contacts.", updated);

        } catch (Exception e) {
            logger.error("Error during QuickFlow detection", e);
        }
    }

    /**
     * Manual trigger to detect QuickFlow users for a specific user's contacts.
     */
    public int detectForUser(String userId) {
        logger.info("Detecting QuickFlow users for user {}", userId);

        // Get QuickFlow user emails
        List<User> allUsers = userRepository.findAll();
        Set<String> quickFlowEmails = allUsers.stream()
                .map(User::getEmail)
                .filter(email -> email != null && !email.isEmpty())
                .map(String::toLowerCase)
                .collect(Collectors.toSet());

        // Get user's contacts
        List<Contact> userContacts = contactRepository.findByUserIdAndIsDeletedFalse(userId);
        int updated = 0;

        for (Contact contact : userContacts) {
            if (contact.getEmail() == null)
                continue;

            String contactEmail = contact.getEmail().toLowerCase();
            boolean isQuickFlowUser = quickFlowEmails.contains(contactEmail);

            if (contact.isUsesQuickFlow() != isQuickFlowUser) {
                contact.setUsesQuickFlow(isQuickFlowUser);

                if (isQuickFlowUser) {
                    allUsers.stream()
                            .filter(u -> contactEmail.equalsIgnoreCase(u.getEmail()))
                            .findFirst()
                            .ifPresent(user -> contact.setQuickflowUserId(user.getId()));
                } else {
                    contact.setQuickflowUserId(null);
                }

                contactRepository.save(contact);
                updated++;
            }
        }

        logger.info("Updated {} contacts for user {}", updated, userId);
        return updated;
    }
}
