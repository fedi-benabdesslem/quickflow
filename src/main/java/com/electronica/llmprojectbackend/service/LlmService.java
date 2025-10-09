package com.electronica.llmprojectbackend.service;

import com.electronica.llmprojectbackend.model.EmailRequest;
import com.electronica.llmprojectbackend.model.PvRequest;

public interface LlmService {
    String processAndGetResponse(EmailRequest request);
    String processAndGetResponse(PvRequest request);
}