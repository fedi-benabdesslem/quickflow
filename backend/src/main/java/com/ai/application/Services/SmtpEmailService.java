package com.ai.application.Services;

import com.ai.application.Config.SmtpProviderConfig;
import com.ai.application.Config.SmtpProviderConfig.SmtpSettings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import jakarta.activation.DataHandler;
import jakarta.activation.DataSource;
import jakarta.mail.*;
import jakarta.mail.internet.*;
import jakarta.mail.util.ByteArrayDataSource;

import java.util.Date;
import java.util.Properties;

/**
 * Service for sending emails via SMTP using app-specific passwords.
 * Supports Yahoo, iCloud, AOL, Zoho, Fastmail, GMX, Yandex, Outlook.
 */
@Service
public class SmtpEmailService {

    private static final Logger logger = LoggerFactory.getLogger(SmtpEmailService.class);

    private final SmtpProviderConfig smtpProviderConfig;

    public SmtpEmailService(SmtpProviderConfig smtpProviderConfig) {
        this.smtpProviderConfig = smtpProviderConfig;
    }

    /**
     * Sends an email via SMTP.
     */
    public boolean sendEmail(String fromEmail, String appPassword, String to,
            String subject, String htmlBody) throws SmtpSendException {
        return sendEmailWithAttachment(fromEmail, appPassword, to, subject, htmlBody, null, null);
    }

    /**
     * Sends an email with optional PDF attachment via SMTP.
     */
    public boolean sendEmailWithAttachment(String fromEmail, String appPassword, String to,
            String subject, String htmlBody,
            byte[] pdfBytes, String pdfFilename) throws SmtpSendException {
        String domain = SmtpProviderConfig.extractDomain(fromEmail);
        SmtpSettings settings = smtpProviderConfig.getSmtpSettings(domain);

        if (settings == null) {
            throw new SmtpSendException("Your email provider is not supported for sending.", "unsupported_provider");
        }

        logger.info("Sending email via SMTP from: {}@{} to: {}", "***", domain, maskEmail(to));

        Properties props = createSmtpProperties(settings);
        Session session = Session.getInstance(props, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(fromEmail, appPassword);
            }
        });

        try {
            MimeMessage message = createMimeMessage(session, fromEmail, to, subject, htmlBody, pdfBytes, pdfFilename);
            Transport.send(message);
            logger.info("Email sent successfully via SMTP ({}).", settings.providerName());
            return true;
        } catch (AuthenticationFailedException e) {
            logger.warn("SMTP authentication failed for {}@{}", "***", domain);
            throw new SmtpSendException(
                    "Your app password is incorrect. Please update it in your profile settings.", "auth_failed");
        } catch (MessagingException e) {
            String msg = e.getMessage();
            logger.error("SMTP send error for {}@{}: {}", "***", domain, msg);

            if (msg != null && (msg.contains("timeout") || msg.contains("timed out") || msg.contains("connect"))) {
                throw new SmtpSendException(
                        "Could not connect to your email server. Please try again.", "connection_error");
            }
            throw new SmtpSendException("Failed to send email. Please try again later.", "send_failed");
        }
    }

    /**
     * Validates an SMTP connection by authenticating without sending.
     * 
     * @return true if authentication succeeds
     */
    public boolean validateConnection(String email, String appPassword) throws SmtpSendException {
        String domain = SmtpProviderConfig.extractDomain(email);
        SmtpSettings settings = smtpProviderConfig.getSmtpSettings(domain);

        if (settings == null) {
            throw new SmtpSendException("Your email provider is not supported.", "unsupported_provider");
        }

        logger.info("Validating SMTP connection for {}@{} via {}", "***", domain, settings.host());

        Properties props = createSmtpProperties(settings);

        try {
            Session session = Session.getInstance(props);
            Transport transport = session.getTransport("smtp");
            transport.connect(settings.host(), settings.port(), email, appPassword);
            transport.close();
            logger.info("SMTP validation successful for {}@{}", "***", domain);
            return true;
        } catch (AuthenticationFailedException e) {
            logger.warn("SMTP validation failed - bad credentials for {}@{}", "***", domain);
            throw new SmtpSendException(
                    "Invalid app password. Please check your password and try again.", "auth_failed");
        } catch (MessagingException e) {
            logger.error("SMTP validation error for {}@{}: {}", "***", domain, e.getMessage());
            throw new SmtpSendException(
                    "Could not connect to your email server. Please try again.", "connection_error");
        }
    }

    private Properties createSmtpProperties(SmtpSettings settings) {
        Properties props = new Properties();
        props.put("mail.smtp.host", settings.host());
        props.put("mail.smtp.port", String.valueOf(settings.port()));
        props.put("mail.smtp.auth", "true");
        if (settings.starttls()) {
            props.put("mail.smtp.starttls.enable", "true");
            props.put("mail.smtp.starttls.required", "true");
        }
        props.put("mail.smtp.connectiontimeout", "10000");
        props.put("mail.smtp.timeout", "10000");
        props.put("mail.smtp.writetimeout", "10000");
        return props;
    }

    private MimeMessage createMimeMessage(Session session, String from, String to, String subject,
            String htmlBody, byte[] pdfBytes, String pdfFilename)
            throws MessagingException {

        MimeMessage message = new MimeMessage(session);
        message.setFrom(new InternetAddress(from));
        message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(to));
        message.setSubject(subject);
        message.setSentDate(new Date());

        if (pdfBytes != null && pdfFilename != null) {
            MimeMultipart multipart = new MimeMultipart();

            MimeBodyPart htmlPart = new MimeBodyPart();
            htmlPart.setContent(htmlBody, "text/html; charset=utf-8");
            multipart.addBodyPart(htmlPart);

            MimeBodyPart attachmentPart = new MimeBodyPart();
            DataSource dataSource = new ByteArrayDataSource(pdfBytes, "application/pdf");
            attachmentPart.setDataHandler(new DataHandler(dataSource));
            attachmentPart.setFileName(pdfFilename);
            multipart.addBodyPart(attachmentPart);

            message.setContent(multipart);
        } else {
            message.setContent(htmlBody, "text/html; charset=utf-8");
        }

        return message;
    }

    private String maskEmail(String email) {
        if (email == null || !email.contains("@"))
            return "***";
        return "***@" + email.substring(email.indexOf('@') + 1);
    }

    /**
     * Custom exception for SMTP errors with user-friendly messages.
     */
    public static class SmtpSendException extends Exception {
        private final String code;

        public SmtpSendException(String message, String code) {
            super(message);
            this.code = code;
        }

        public String getCode() {
            return code;
        }
    }
}
