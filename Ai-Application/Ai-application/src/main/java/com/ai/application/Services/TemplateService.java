package com.ai.application.Services;

import com.ai.application.model.TemplateType;
import com.ai.application.model.DTO.TemplateRequest;
import com.ai.application.model.DTO.TemplateResponse;
import com.ai.application.model.Entity.GeneratedOutput;
import com.ai.application.model.Entity.userData;
import com.ai.application.Repositories.userDataRepo;
import com.ai.application.Repositories.GeneratedOutputRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.security.MessageDigest;
import java.nio.charset.StandardCharsets;

@Service
public class TemplateService {

    private final LLMService llmService;
    private final userDataRepo userDataRepo;
    private final GeneratedOutputRepository generatedOutputRepo;

    @Autowired
    public TemplateService(LLMService llmService, userDataRepo userDataRepo, GeneratedOutputRepository generatedOutputRepo) {
        this.llmService = llmService;
        this.userDataRepo = userDataRepo;
        this.generatedOutputRepo = generatedOutputRepo;
    }

    public TemplateResponse processTemplate(TemplateRequest request) {
        String requestHash = computeRequestHash(request);
        Optional<GeneratedOutput> cached = generatedOutputRepo.findByRequestHash(requestHash);
        if (cached.isPresent()) {
            return new TemplateResponse(cached.get().getContent());
        }

        String llmInput = buildLlmInput(request);
        String output = llmService.generateContent(llmInput, request.getTemplateType());

        GeneratedOutput genOut = new GeneratedOutput();
        genOut.setRequestHash(requestHash);
        genOut.setContent(output);
        genOut.setUserId(request.getUserId());
        genOut.setCreatedAt(java.time.LocalDateTime.now());
        genOut.setModelUsed("llama3.2");
        genOut.setTokensUsed(0); 
        generatedOutputRepo.save(genOut);

        return new TemplateResponse(output);
    }

    private String computeRequestHash(TemplateRequest request) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            String json = mapper.writeValueAsString(request);
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(json.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : hashBytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException("Failed to compute request hash", e);
        }
    }

    public String buildLlmInput(TemplateRequest request) {
        List<String> bullets = Optional.ofNullable(request.getBulletPoints())
                .filter(list -> !list.isEmpty())
                .orElseGet(() -> Optional.ofNullable(request.getInput())
                        .map(input -> List.of(input))
                        .orElse(Collections.emptyList()));

        List<userData> userDataList = request.getUserData();
        if (userDataList == null || userDataList.isEmpty()) {
            userDataList = userDataRepo.findByUserId(request.getUserId());
        }

        String bulletText = bullets.stream()
                .map(point -> "- " + point)
                .collect(Collectors.joining("\n"));

        String userInfo = userDataList.stream()
                .map(data -> data.getInfoKey() + ": " + data.getInfoValue())
                .collect(Collectors.joining("\n"));

        return String.format("""
                User Information:
                %s

                Bullet Points:
                %s
                """, userInfo, bulletText);
    }
}