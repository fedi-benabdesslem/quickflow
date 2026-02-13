package com.ai.application.Services;

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.model.Message;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.AccessToken;
import com.google.auth.oauth2.GoogleCredentials;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import jakarta.activation.DataHandler;
import jakarta.activation.DataSource;
import jakarta.mail.MessagingException;
import jakarta.mail.Session;
import jakarta.mail.internet.*;
import jakarta.mail.util.ByteArrayDataSource;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Base64;
import java.util.Date;
import java.util.Properties;

/**
 * Service for sending emails via Gmail API.
 * 
 * Uses OAuth access tokens to send emails from user's Gmail account.
 */
@Service
public class GmailService {

    private static final Logger logger = LoggerFactory.getLogger(GmailService.class);

    private static final String APPLICATION_NAME = "QuickFlow";
    private static final GsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();

    private final TokenRefreshService tokenRefreshService;
    private final TokenStorageService tokenStorageService;

    public GmailService(TokenRefreshService tokenRefreshService, TokenStorageService tokenStorageService) {
        this.tokenRefreshService = tokenRefreshService;
        this.tokenStorageService = tokenStorageService;
    }

    /**
     * Sends an email via Gmail API on behalf of the user.
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
     * Sends an email with a PDF attachment via Gmail API.
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
        logger.debug("sendEmailWithAttachment called");

        // Get or refresh access token
        String accessToken = tokenRefreshService.refreshTokenIfNeeded(supabaseId);
        if (accessToken == null) {
            logger.error("No valid access token - refresh failed or no token exists");
            throw new Exception("No valid access token available. Please sign in again.");
        }
        logger.debug("Got valid access token");

        // Get user's email address
        TokenStorageService.DecryptedTokens tokens = tokenStorageService.getDecryptedTokens(supabaseId);
        if (tokens == null) {
            logger.error("No stored tokens found for user");
            throw new Exception("No stored tokens found for user.");
        }
        String fromEmail = tokens.getEmail();
        logger.info("Sending email from: {} to: {}", maskEmail(fromEmail), maskEmail(to));

        // Create Gmail service
        Gmail gmail = createGmailService(accessToken);
        logger.debug("Gmail service created successfully");

        // Create MIME message
        MimeMessage mimeMessage = createMimeMessage(fromEmail, to, subject, htmlBody, pdfBytes, pdfFilename);
        logger.debug("MIME message created, subject: {}", subject);

        // Encode and send
        Message message = createMessageFromMimeMessage(mimeMessage);
        logger.debug("Sending message via Gmail API...");
        gmail.users().messages().send("me", message).execute();
        logger.info("Email sent successfully via Gmail API");

        return true;
    }

    /**
     * Masks an email address to protect PII in logs.
     * Shows only the domain and masks the local part.
     * Example: "user@example.com" -> "***@example.com"
     */
    private String maskEmail(String email) {
        if (email == null || !email.contains("@")) {
            return "***";
        }
        int atIndex = email.indexOf("@");
        if (atIndex <= 0) {
            return "***";
        }
        // Only show domain to protect PII
        return "***@" + email.substring(atIndex + 1);
    }

    /**
     * Creates a Gmail service instance with the provided access token.
     */
    private Gmail createGmailService(String accessToken) throws GeneralSecurityException, IOException {
        NetHttpTransport httpTransport = GoogleNetHttpTransport.newTrustedTransport();

        // Create credentials from access token
        GoogleCredentials credentials = GoogleCredentials.create(new AccessToken(accessToken, null));

        return new Gmail.Builder(httpTransport, JSON_FACTORY, new HttpCredentialsAdapter(credentials))
                .setApplicationName(APPLICATION_NAME)
                .build();
    }

    /**
     * Creates a MIME message with optional PDF attachment.
     */
    private MimeMessage createMimeMessage(String from, String to, String subject,
            String htmlBody, byte[] pdfBytes, String pdfFilename)
            throws MessagingException {

        Properties props = new Properties();
        Session session = Session.getDefaultInstance(props, null);

        MimeMessage message = new MimeMessage(session);
        message.setFrom(new InternetAddress(from));
        message.setRecipients(jakarta.mail.Message.RecipientType.TO, InternetAddress.parse(to));
        message.setSubject(subject);
        message.setSentDate(new Date());

        if (pdfBytes != null && pdfFilename != null) {
            // Create multipart message for attachment
            MimeMultipart multipart = new MimeMultipart();

            // HTML body part
            MimeBodyPart htmlPart = new MimeBodyPart();
            htmlPart.setContent(htmlBody, "text/html; charset=utf-8");
            multipart.addBodyPart(htmlPart);

            // PDF attachment part
            MimeBodyPart attachmentPart = new MimeBodyPart();
            DataSource dataSource = new ByteArrayDataSource(pdfBytes, "application/pdf");
            attachmentPart.setDataHandler(new DataHandler(dataSource));
            attachmentPart.setFileName(pdfFilename);
            multipart.addBodyPart(attachmentPart);

            message.setContent(multipart);
        } else {
            // Simple HTML message
            message.setContent(htmlBody, "text/html; charset=utf-8");
        }

        return message;
    }

    /**
     * Converts a MimeMessage to a Gmail API Message.
     */
    private Message createMessageFromMimeMessage(MimeMessage mimeMessage) throws MessagingException, IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        mimeMessage.writeTo(buffer);
        byte[] rawBytes = buffer.toByteArray();
        String encodedEmail = Base64.getUrlEncoder().encodeToString(rawBytes);

        Message message = new Message();
        message.setRaw(encodedEmail);
        return message;
    }
}
