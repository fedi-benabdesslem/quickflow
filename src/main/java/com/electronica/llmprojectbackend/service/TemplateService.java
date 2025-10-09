package com.electronica.llmprojectbackend.service;

import com.electronica.llmprojectbackend.model.EmailRequest;
import com.electronica.llmprojectbackend.model.PvRequest;
import com.electronica.llmprojectbackend.model.TemplateType;
import com.electronica.llmprojectbackend.repo.EmailRequestRepo;
import com.electronica.llmprojectbackend.repo.PvRequestRepo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Slf4j
public class TemplateService {

    @Autowired
    private EmailRequestRepo emailRepo;

    @Autowired
    private PvRequestRepo pvRepo;

    @Autowired
    private LlmService llmService;

    @Transactional
    public EmailRequest processEmail(EmailRequest request) {
        try {
            log.info("Processing EMAIL request for user: {}", request.getUserId());
            if (request.getTemplateType() == null) {
                request.setTemplateType(TemplateType.EMAIL);
            }
            String llmOutput = llmService.processAndGetResponse(request);
            request.setLlmOutput(llmOutput);
            EmailRequest saved = emailRepo.save(request);
            log.info("Saved EMAIL request with ID: {}", saved.getId());
            return saved;
        } catch (Exception e) {
            log.error("Error processing EMAIL request", e);
            request.setLlmOutput("Error: " + e.getMessage());
            return emailRepo.save(request);
        }
    }

    @Transactional
    public PvRequest processPv(PvRequest request) {
        try {
            log.info("Processing PV request for user: {}", request.getUserId());
            if (request.getTemplateType() == null) {
                request.setTemplateType(TemplateType.PV);
            }
            String llmOutput = llmService.processAndGetResponse(request);
            request.setLlmOutput(llmOutput);
            PvRequest saved = pvRepo.save(request);
            log.info("Saved PV request with ID: {}", saved.getId());
            return saved;
        } catch (Exception e) {
            log.error("Error processing PV request", e);
            request.setLlmOutput("Error: " + e.getMessage());
            return pvRepo.save(request);
        }
    }

    // No legacy support methods anymore

    public Optional<EmailRequest> getEmailById(Integer id) {
        return emailRepo.findById(id);
    }

    public Optional<PvRequest> getPvById(Integer id) {
        return pvRepo.findById(id);
    }

    public Optional<EmailRequest> getEmailByUserAndId(Long userId, Integer requestId) {
        return emailRepo.findByIdAndUserId(requestId, userId);
    }

    public Optional<PvRequest> getPvByUserAndId(Long userId, Integer requestId) {
        return pvRepo.findByIdAndUserId(requestId, userId);
    }

    public List<EmailRequest> getAllEmails() {
        return emailRepo.findAll();
    }

    public List<PvRequest> getAllPvs() {
        return pvRepo.findAll();
    }
}