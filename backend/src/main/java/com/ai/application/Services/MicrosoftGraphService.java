package com.ai.application.Services;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Base64;

/**
 * Service for sending emails via Microsoft Graph API.
 * 
 * Uses OAuth access tokens to send emails from user's Outlook/Office 365
 * account.
 * Note: This implementation is ready but untested (requires Azure premium
 * account).
 * 
 * Uses REST API directly instead of SDK for simpler maintenance.
 */
@Service
public class MicrosoftGraphService {

    private static final String GRAPH_SEND_MAIL_URL = "https://graph.microsoft.com/v1.0/me/sendMail";

    @Value("${microsoft.oauth.client-id:}")
    private String clientId;

    @Value("${microsoft.oauth.client-secret:}")
    private String clientSecret;

    @Value("${microsoft.oauth.tenant-id:common}")
    private String tenantId;

    private final TokenRefreshService tokenRefreshService;
    private final TokenStorageService tokenStorageService;
    private final RestTemplate restTemplate;
    private final Gson gson;

    public MicrosoftGraphService(TokenRefreshService tokenRefreshService, TokenStorageService tokenStorageService) {
        this.tokenRefreshService = tokenRefreshService;
        this.tokenStorageService = tokenStorageService;
        this.restTemplate = new RestTemplate();
        this.gson = new Gson();
    }

    /**
     * Sends an email via Microsoft Graph API on behalf of the user.
     * 
     * @param supabaseId User's Supabase ID
     * @param to         Recipient email addresses (comma-separated)
     * @param subject    Email subject
     * @param htmlBody   Email body in HTML format
     * @return true if email sent successfully
     * @throws Exception if sending fails
     */
    public boolean sendEmail(String supabaseId, String to, String subject, String htmlBody) throws Exception {
        return sendEmailWithAttachment(supabaseId, to, subject, htmlBody, null, null);
    }

    /**
     * Sends an email using a provided access token directly (for linked OAuth
     * accounts).
     */
    public boolean sendEmailWithAccessToken(String accessToken,
            String to, String subject, String htmlBody) throws Exception {
        return sendEmailWithAccessToken(accessToken, to, subject, htmlBody, null, null);
    }

    /**
     * Sends an email with optional attachment using a provided access token (for
     * linked OAuth accounts).
     */
    public boolean sendEmailWithAccessToken(String accessToken,
            String to, String subject, String htmlBody,
            byte[] pdfBytes, String pdfFilename) throws Exception {
        JsonObject emailPayload = buildEmailPayload(to, subject, htmlBody, pdfBytes, pdfFilename);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(accessToken);

        HttpEntity<String> request = new HttpEntity<>(gson.toJson(emailPayload), headers);

        try {
            ResponseEntity<String> response = restTemplate.postForEntity(
                    GRAPH_SEND_MAIL_URL, request, String.class);
            return response.getStatusCode().is2xxSuccessful();
        } catch (Exception e) {
            throw new Exception("Failed to send email via linked Microsoft Graph: " + e.getMessage(), e);
        }
    }

    /**
     * Sends an email with a PDF attachment via Microsoft Graph API.
     * 
     * @param supabaseId  User's Supabase ID
     * @param to          Recipient email addresses (comma-separated)
     * @param subject     Email subject
     * @param htmlBody    Email body in HTML format
     * @param pdfBytes    PDF file bytes (can be null for no attachment)
     * @param pdfFilename Filename for the PDF attachment
     * @return true if email sent successfully
     * @throws Exception if sending fails
     */
    public boolean sendEmailWithAttachment(String supabaseId, String to, String subject,
            String htmlBody, byte[] pdfBytes, String pdfFilename) throws Exception {
        // Get or refresh access token
        String accessToken = tokenRefreshService.refreshTokenIfNeeded(supabaseId);
        if (accessToken == null) {
            throw new Exception("No valid access token available. Please sign in again.");
        }

        // Build the email payload
        JsonObject emailPayload = buildEmailPayload(to, subject, htmlBody, pdfBytes, pdfFilename);

        // Set up headers
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(accessToken);

        HttpEntity<String> request = new HttpEntity<>(gson.toJson(emailPayload), headers);

        try {
            ResponseEntity<String> response = restTemplate.postForEntity(
                    GRAPH_SEND_MAIL_URL, request, String.class);

            return response.getStatusCode().is2xxSuccessful();
        } catch (Exception e) {
            throw new Exception("Failed to send email via Microsoft Graph: " + e.getMessage(), e);
        }
    }

    /**
     * Builds the JSON payload for sending an email via Microsoft Graph API.
     */
    private JsonObject buildEmailPayload(String to, String subject, String htmlBody,
            byte[] pdfBytes, String pdfFilename) {
        JsonObject payload = new JsonObject();

        // Build message object
        JsonObject message = new JsonObject();
        message.addProperty("subject", subject);

        // Body
        JsonObject body = new JsonObject();
        body.addProperty("contentType", "HTML");
        body.addProperty("content", htmlBody);
        message.add("body", body);

        // Recipients - build array
        com.google.gson.JsonArray toRecipients = new com.google.gson.JsonArray();
        for (String recipient : to.split(",")) {
            JsonObject recipientObj = new JsonObject();
            JsonObject emailAddress = new JsonObject();
            emailAddress.addProperty("address", recipient.trim());
            recipientObj.add("emailAddress", emailAddress);
            toRecipients.add(recipientObj);
        }
        message.add("toRecipients", toRecipients);

        // Add attachment if provided
        if (pdfBytes != null && pdfFilename != null) {
            com.google.gson.JsonArray attachments = new com.google.gson.JsonArray();

            JsonObject attachment = new JsonObject();
            attachment.addProperty("@odata.type", "#microsoft.graph.fileAttachment");
            attachment.addProperty("name", pdfFilename);
            attachment.addProperty("contentType", "application/pdf");
            attachment.addProperty("contentBytes", Base64.getEncoder().encodeToString(pdfBytes));

            attachments.add(attachment);
            message.add("attachments", attachments);
        }

        payload.add("message", message);
        payload.addProperty("saveToSentItems", true);

        return payload;
    }
}
