package com.electronica.llmprojectbackend.controller;

import com.electronica.llmprojectbackend.model.EmailRequest;
import com.electronica.llmprojectbackend.service.EmailService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/email")
@CrossOrigin(origins = "*") // Configure this properly for production
@Slf4j
public class EmailController {

    @Autowired
    private EmailService emailService;

    @PostMapping("/process")
    public ResponseEntity<EmailRequest> processEmail(@RequestBody EmailRequest request) {
        try {
            if (request.getUserId() == null) {
                return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
            }
            EmailRequest saved = emailService.processEmail(request);
            return new ResponseEntity<>(saved, HttpStatus.OK);
        } catch (Exception e) {
            log.error("Error processing email request", e);
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/{userId}/{requestId}")
    public ResponseEntity<EmailRequest> getEmail(@PathVariable Long userId, @PathVariable Integer requestId) {
        return emailService
                .getEmailByUserAndId(userId, requestId)
                .map(e -> new ResponseEntity<>(e, HttpStatus.OK))
                .orElse(new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }

    @GetMapping
    public ResponseEntity<List<EmailRequest>> listAllEmails() {
        return new ResponseEntity<>(emailService.getAllEmails(), HttpStatus.OK);
    }
}
