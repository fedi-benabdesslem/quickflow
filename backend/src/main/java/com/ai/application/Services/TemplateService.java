package com.ai.application.Services;

import com.ai.application.model.TemplateType;
import com.ai.application.model.DTO.TemplateRequest;
import com.ai.application.model.DTO.TemplateResponse;
import com.ai.application.model.Entity.GeneratedOutput;
import com.ai.application.Repositories.GeneratedOutputRepository;
import com.ai.application.model.DTO.MeetingRequest;
import com.ai.application.model.DTO.EmailRequest;
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
    private final GeneratedOutputRepository generatedOutputRepo;

    @Autowired
    public TemplateService(LLMService llmService, GeneratedOutputRepository generatedOutputRepo) {
        this.llmService = llmService;
        this.generatedOutputRepo = generatedOutputRepo;
    }

    public TemplateResponse processTemplate(TemplateRequest request) {
        String requestHash = computeRequestHash(request);
        Optional<GeneratedOutput> cached = generatedOutputRepo.findByRequestHash(requestHash);
        if (cached.isPresent()) {
            return new TemplateResponse(cached.get().getContent());
        }

        String userPrompt = buildUserPrompt(request);
        String output = llmService.generateContent(userPrompt, request.getTemplateType());

        String subject = null;
        String content = output;

        // Parse subject if it's an email template
        if (request.getTemplateType() == TemplateType.email) {
            String[] parts = parseEmailOutput(output);
            subject = parts[0];
            content = parts[1];
        }

        GeneratedOutput genOut = new GeneratedOutput();
        genOut.setRequestHash(requestHash);
        genOut.setContent(content);

        if (request.getUserId() != null) {
            // Optional: Store userId in GeneratedOutput if needed, but User entity is gone
            // genOut.setUserId(request.getUserId());
        }
        genOut.setCreatedAt(java.time.LocalDateTime.now());
        genOut.setModelUsed("llama3.2");
        genOut.setTokensUsed(0);
        generatedOutputRepo.save(genOut);

        return new TemplateResponse(content, subject);
    }

    private String[] parseEmailOutput(String output) {
        String subject = null;
        String content = output;

        // Try to find "Subject:" line
        String[] lines = output.split("\\n");
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i].trim();
            if (line.toLowerCase().startsWith("subject:")) {
                subject = line.substring(8).trim();
                // Everything after this line (and potentially next blank lines) is content
                StringBuilder contentBuilder = new StringBuilder();
                boolean contentStarted = false;
                for (int j = i + 1; j < lines.length; j++) {
                    String contentLine = lines[j]; // Don't trim to preserve spacing, or trim if desired
                    if (!contentStarted && contentLine.trim().isEmpty()) {
                        continue; // Skip leading blank lines after subject
                    }
                    contentStarted = true;
                    contentBuilder.append(contentLine).append("\n");
                }
                content = contentBuilder.toString().trim();
                break;
            }
        }

        return new String[] { subject, content };
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

    private String buildUserPrompt(TemplateRequest request) {
        TemplateType type = request.getTemplateType();
        StringBuilder userPrompt = new StringBuilder();

        String senderUsername = request.getUserId() != null ? request.getUserId() : "Anonymous User";
        userPrompt.append(String.format("Sender Username: %s\n", senderUsername));

        if (type == TemplateType.email && request instanceof EmailRequest) {
            EmailRequest emailReq = (EmailRequest) request;
            String senderEmail = emailReq.getSenderEmail() != null ? emailReq.getSenderEmail()
                    : "anonymous@example.com";
            userPrompt.append(String.format("Sender Email: %s\n\n", senderEmail));

            String recipientsText = "Unknown Recipients";
            if (emailReq.getRecipients() != null && !emailReq.getRecipients().isEmpty()) {
                recipientsText = String.join(", ", emailReq.getRecipients());
            }
            userPrompt.append(String.format("Recipients Emails: %s\n\n", recipientsText));

            List<String> bullets = getBulletsOrInput(request);
            String bulletText = bullets.stream().map(point -> "- " + point).collect(Collectors.joining("\n"));
            userPrompt.append("Bullet Points:\n").append(bulletText);

        } else if ((type == TemplateType.PV || type == TemplateType.meeting) && request instanceof MeetingRequest) {
            MeetingRequest meetingReq = (MeetingRequest) request;

            String peopleText = "Unknown Attendees";
            if (meetingReq.getPeople() != null && !meetingReq.getPeople().isEmpty()) {
                peopleText = String.join(", ", meetingReq.getPeople());
            }
            userPrompt.append(String.format("Attendees: %s\n", peopleText));

            String location = meetingReq.getLocation() != null ? meetingReq.getLocation() : "TBD";
            userPrompt.append(String.format("Location: %s\n", location));

            String timeRange = "TBD";
            if (meetingReq.getTimeBegin() != null && meetingReq.getTimeEnd() != null) {
                timeRange = String.format("%s to %s", meetingReq.getTimeBegin(), meetingReq.getTimeEnd());
            }
            userPrompt.append(String.format("Date & Time: %s %s\n",
                    meetingReq.getDate() != null ? meetingReq.getDate() : "TBD", timeRange));

            String subject = meetingReq.getSubject() != null ? meetingReq.getSubject() : "Meeting Summary";
            userPrompt.append(String.format("Subject: %s\n\n", subject));

            String details = meetingReq.getDetails() != null ? meetingReq.getDetails() : "";
            userPrompt.append("Additional Details:\n").append(details.isEmpty() ? "None provided." : details)
                    .append("\n\n");

            List<String> bullets = getBulletsOrInput(request);
            if (!bullets.isEmpty()) {
                String bulletText = bullets.stream().map(point -> "- " + point).collect(Collectors.joining("\n"));
                userPrompt.append("Agenda Items:\n").append(bulletText);
            }
        } else {
            throw new IllegalArgumentException("Unsupported template type or request instance: " + type + " ("
                    + request.getClass().getSimpleName() + ")");
        }

        return userPrompt.toString();
    }

    private List<String> getBulletsOrInput(TemplateRequest request) {
        return Optional.ofNullable(request.getBulletPoints())
                .filter(list -> !list.isEmpty())
                .orElseGet(() -> Optional.ofNullable(request.getInput())
                        .map(input -> List.of(input))
                        .orElse(Collections.emptyList()));
    }
}