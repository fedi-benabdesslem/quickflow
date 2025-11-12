package com.electronica.llmprojectbackend.controller;

import com.electronica.llmprojectbackend.model.PvRequest;
import com.electronica.llmprojectbackend.service.PvService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/pv")
@CrossOrigin(origins = "*") // Configure this properly for production
@Slf4j
public class PvController {

    @Autowired
    private PvService pvService;

    @PostMapping("/process")
    public ResponseEntity<PvRequest> processPv(@RequestBody PvRequest request) {
        try {
            if (request.getUserId() == null) {
                return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
            }
            PvRequest saved = pvService.processPv(request);
            return new ResponseEntity<>(saved, HttpStatus.OK);
        } catch (Exception e) {
            log.error("Error processing pv request", e);
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/{userId}/{requestId}")
    public ResponseEntity<PvRequest> getPv(@PathVariable Long userId, @PathVariable Integer requestId) {
        return pvService
                .getPvByUserAndId(userId, requestId)
                .map(p -> new ResponseEntity<>(p, HttpStatus.OK))
                .orElse(new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }

    @GetMapping
    public ResponseEntity<List<PvRequest>> listAllPvs() {
        return new ResponseEntity<>(pvService.getAllPvs(), HttpStatus.OK);
    }
}
