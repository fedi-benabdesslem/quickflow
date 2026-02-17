package com.ai.application.Config;

import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Configuration mapping email domains to their SMTP server settings.
 * Users never need to know SMTP host or port — we hardcode them.
 */
@Component
public class SmtpProviderConfig {

    /** SMTP settings record */
    public record SmtpSettings(String host, int port, boolean starttls, String providerName) {
    }

    private static final Map<String, SmtpSettings> PROVIDER_MAP = new HashMap<>();
    private static final Set<String> BLOCKED_DOMAINS = Set.of("protonmail.com", "proton.me");

    static {
        // Yahoo
        SmtpSettings yahoo = new SmtpSettings("smtp.mail.yahoo.com", 587, true, "Yahoo Mail");
        PROVIDER_MAP.put("yahoo.com", yahoo);
        PROVIDER_MAP.put("ymail.com", yahoo);

        // iCloud / Apple
        SmtpSettings icloud = new SmtpSettings("smtp.mail.me.com", 587, true, "iCloud Mail");
        PROVIDER_MAP.put("icloud.com", icloud);
        PROVIDER_MAP.put("me.com", icloud);
        PROVIDER_MAP.put("mac.com", icloud);

        // AOL
        PROVIDER_MAP.put("aol.com", new SmtpSettings("smtp.aol.com", 587, true, "AOL Mail"));

        // Zoho
        SmtpSettings zoho = new SmtpSettings("smtp.zoho.com", 587, true, "Zoho Mail");
        PROVIDER_MAP.put("zoho.com", zoho);
        PROVIDER_MAP.put("zohomail.com", zoho);

        // Fastmail
        PROVIDER_MAP.put("fastmail.com", new SmtpSettings("smtp.fastmail.com", 587, true, "Fastmail"));

        // GMX
        PROVIDER_MAP.put("gmx.com", new SmtpSettings("smtp.gmx.com", 587, true, "GMX Mail"));
        PROVIDER_MAP.put("gmx.net", new SmtpSettings("smtp.gmx.net", 587, true, "GMX Mail"));

        // mail.com
        PROVIDER_MAP.put("mail.com", new SmtpSettings("smtp.mail.com", 587, true, "Mail.com"));

        // Yandex
        SmtpSettings yandex = new SmtpSettings("smtp.yandex.com", 587, true, "Yandex Mail");
        PROVIDER_MAP.put("yandex.com", yandex);
        PROVIDER_MAP.put("yandex.ru", yandex);

        // Outlook/Hotmail/Live (for users who signed up with local auth, not OAuth)
        SmtpSettings outlook = new SmtpSettings("smtp.office365.com", 587, true, "Outlook");
        PROVIDER_MAP.put("outlook.com", outlook);
        PROVIDER_MAP.put("hotmail.com", outlook);
        PROVIDER_MAP.put("live.com", outlook);
    }

    /**
     * Get SMTP settings for an email domain.
     * 
     * @return SmtpSettings or null if domain is not supported
     */
    public SmtpSettings getSmtpSettings(String emailDomain) {
        if (emailDomain == null)
            return null;
        return PROVIDER_MAP.get(emailDomain.toLowerCase().trim());
    }

    /**
     * Get the user-friendly provider name for a domain.
     */
    public String getProviderName(String emailDomain) {
        SmtpSettings settings = getSmtpSettings(emailDomain);
        return settings != null ? settings.providerName() : null;
    }

    /**
     * Check if a domain is supported for SMTP sending.
     */
    public boolean isSupported(String emailDomain) {
        return emailDomain != null && PROVIDER_MAP.containsKey(emailDomain.toLowerCase().trim());
    }

    /**
     * Check if a domain is explicitly blocked (e.g., ProtonMail).
     */
    public boolean isBlocked(String emailDomain) {
        return emailDomain != null && BLOCKED_DOMAINS.contains(emailDomain.toLowerCase().trim());
    }

    /**
     * Extract domain from email address.
     */
    public static String extractDomain(String email) {
        if (email == null || !email.contains("@"))
            return null;
        return email.substring(email.lastIndexOf('@') + 1).toLowerCase().trim();
    }
}
