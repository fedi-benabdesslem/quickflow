package com.electronica.llmprojectbackend.controller;

import com.electronica.llmprojectbackend.model.EmailRequest;
import com.electronica.llmprojectbackend.model.PvRequest;
import com.electronica.llmprojectbackend.service.TemplateService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*") // Configure this properly for production
@Slf4j
public class UserRequestController {

    @Autowired
    private TemplateService templateService;

    @PostMapping("/process/email")
    public ResponseEntity<EmailRequest> processEmail(@RequestBody EmailRequest request) {
        try {
            if (request.getUserId() == null) {
                return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
            }
            EmailRequest saved = templateService.processEmail(request);
            return new ResponseEntity<>(saved, HttpStatus.OK);
        } catch (Exception e) {
            log.error("Error processing email request", e);
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PostMapping("/process/pv")
    public ResponseEntity<PvRequest> processPv(@RequestBody PvRequest request) {
        try {
            if (request.getUserId() == null) {
                return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
            }
            PvRequest saved = templateService.processPv(request);
            return new ResponseEntity<>(saved, HttpStatus.OK);
        } catch (Exception e) {
            log.error("Error processing pv request", e);
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }


    @GetMapping("/process/email/{userId}/{requestId}")
    public ResponseEntity<EmailRequest> getEmail(@PathVariable Long userId, @PathVariable Integer requestId) {
        return templateService
                .getEmailByUserAndId(userId, requestId)
                .map(e -> new ResponseEntity<>(e, HttpStatus.OK))
                .orElse(new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }

    @GetMapping("/process/pv/{userId}/{requestId}")
    public ResponseEntity<PvRequest> getPv(@PathVariable Long userId, @PathVariable Integer requestId) {
        return templateService
                .getPvByUserAndId(userId, requestId)
                .map(p -> new ResponseEntity<>(p, HttpStatus.OK))
                .orElse(new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }

    @GetMapping("/process/email")
    public ResponseEntity<List<EmailRequest>> listAllEmails() {
        return new ResponseEntity<>(templateService.getAllEmails(), HttpStatus.OK);
    }

    @GetMapping("/process/pv")
    public ResponseEntity<List<PvRequest>> listAllPvs() {
        return new ResponseEntity<>(templateService.getAllPvs(), HttpStatus.OK);
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> healthCheck() {
        Map<String, String> health = new HashMap<>();
        health.put("status", "UP");
        health.put("service", "LLM Project Backend");
        return new ResponseEntity<>(health, HttpStatus.OK);
    }
}