package com.electronica.llmprojectbackend.service;

import com.electronica.llmprojectbackend.model.EmailRequest;
import com.electronica.llmprojectbackend.model.PvRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.RestClientException;

import java.util.Map;

@Service
@Slf4j
public class LlmServiceImpl implements LlmService {

    @Value("${llm.service.url:http://localhost:8081/api/llm}")
    private String llmServiceUrl;

    private final RestTemplate restTemplate;

    public LlmServiceImpl() {
        this.restTemplate = new RestTemplate();
    }

    @Override
    public String processAndGetResponse(EmailRequest request) {
        return sendToLlm("/process/email", request);
    }

    @Override
    public String processAndGetResponse(PvRequest request) {
        return sendToLlm("/process/pv", request);
    }

    private String sendToLlm(String path, Object payload) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Object> entity = new HttpEntity<>(payload, headers);

            log.info("Sending request to LLM service at: {}{}", llmServiceUrl, path);

            ResponseEntity<Map> response = restTemplate.exchange(
                    llmServiceUrl + path,
                    HttpMethod.POST,
                    entity,
                    Map.class
            );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Map<String, Object> responseBody = response.getBody();
                String output = (String) responseBody.get("output");
                log.info("Successfully received response from LLM service");
                return output != null ? output : "No output generated";
            } else {
                log.warn("LLM service returned non-successful status: {}", response.getStatusCode());
                return "Error: LLM service returned status " + response.getStatusCode();
            }
        } catch (RestClientException e) {
            log.error("Error communicating with LLM service", e);
            return "Error: Unable to communicate with LLM service - " + e.getMessage();
        } catch (Exception e) {
            log.error("Unexpected error while processing LLM request", e);
            return "Error: Unexpected error occurred - " + e.getMessage();
        }
    }
}