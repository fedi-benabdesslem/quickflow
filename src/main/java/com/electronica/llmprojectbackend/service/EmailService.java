package com.electronica.llmprojectbackend.service;

import com.electronica.llmprojectbackend.model.EmailRequest;
import com.electronica.llmprojectbackend.model.TemplateType;
import com.electronica.llmprojectbackend.repo.EmailRequestRepo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Slf4j
public class EmailService {

    @Autowired
    private EmailRequestRepo emailRepo;

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

    public Optional<EmailRequest> getEmailById(Integer id) {
        return emailRepo.findById(id);
    }

    public Optional<EmailRequest> getEmailByUserAndId(Long userId, Integer requestId) {
        return emailRepo.findByIdAndUserId(requestId, userId);
    }

    public List<EmailRequest> getAllEmails() {
        return emailRepo.findAll();
    }
}
