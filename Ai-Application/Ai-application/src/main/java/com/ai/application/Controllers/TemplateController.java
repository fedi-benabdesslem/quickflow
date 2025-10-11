package com.ai.application.Controllers;

import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import org.springframework.web.bind.annotation.*;

import com.ai.application.Services.TemplateService;
import com.ai.application.model.DTO.TemplateRequest;
import com.ai.application.model.DTO.TemplateResponse;

@RestController
@RequestMapping("/api/templates")
public class TemplateController {

    private final TemplateService templateService;

    @Autowired
    public TemplateController(TemplateService templateService) {
        this.templateService = templateService;
    }

    @PostMapping("/generate")
    public TemplateResponse generateTemplate(@RequestBody TemplateRequest request) {
        return templateService.processTemplate(request);
     
    }
    }
