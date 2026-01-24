package com.ai.application.Services;

import com.ai.application.Repositories.MeetingTemplateRepository;
import com.ai.application.model.DTO.MeetingTemplateData;
import com.ai.application.model.Entity.MeetingTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class MeetingTemplateService {

    @Autowired
    private MeetingTemplateRepository meetingTemplateRepository;

    public MeetingTemplate saveTemplate(String userId, String name, String description,
            MeetingTemplateData templateData) {
        if (meetingTemplateRepository.existsByUserIdAndName(userId, name)) {
            throw new IllegalArgumentException("Template with this name already exists");
        }

        MeetingTemplate template = new MeetingTemplate();
        template.setUserId(userId);
        template.setName(name);
        template.setDescription(description);
        template.setTemplateData(templateData);
        template.setUsageCount(0);

        return meetingTemplateRepository.save(template);
    }

    public List<MeetingTemplate> getUserTemplates(String userId) {
        return meetingTemplateRepository.findByUserIdOrderByLastUsedDesc(userId);
    }

    public MeetingTemplate getTemplate(String userId, String templateId) {
        Optional<MeetingTemplate> templateOpt = meetingTemplateRepository.findById(templateId);

        if (templateOpt.isEmpty()) {
            throw new RuntimeException("Template not found");
        }

        MeetingTemplate template = templateOpt.get();
        if (!template.getUserId().equals(userId)) {
            throw new RuntimeException("Unauthorized access to template");
        }

        return template;
    }

    public MeetingTemplate updateTemplate(String userId, String templateId, String name, String description,
            MeetingTemplateData templateData) {
        MeetingTemplate template = getTemplate(userId, templateId);

        if (name != null && !name.equals(template.getName())) {
            // checking if new name exists
            if (meetingTemplateRepository.existsByUserIdAndName(userId, name)) {
                throw new IllegalArgumentException("Template with this name already exists");
            }
            template.setName(name);
        }

        if (description != null) {
            template.setDescription(description);
        }

        if (templateData != null) {
            template.setTemplateData(templateData);
        }

        template.setUpdatedAt(LocalDateTime.now());

        return meetingTemplateRepository.save(template);
    }

    public void deleteTemplate(String userId, String templateId) {
        MeetingTemplate template = getTemplate(userId, templateId);
        meetingTemplateRepository.delete(template);
    }

    public void trackUsage(String userId, String templateId) {
        MeetingTemplate template = getTemplate(userId, templateId);
        template.setUsageCount(template.getUsageCount() + 1);
        template.setLastUsed(LocalDateTime.now());
        meetingTemplateRepository.save(template);
    }
}
