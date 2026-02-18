package com.ai.application.Services;

import org.springframework.stereotype.Service;

import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import java.util.Hashtable;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Detects email hosting provider (Google Workspace / Microsoft 365) via DNS MX
 * record lookup.
 * Results are cached in-memory for 24 hours.
 */
@Service
public class DomainDetectionService {

    private static final long CACHE_TTL_MS = 24 * 60 * 60 * 1000L; // 24 hours

    private final ConcurrentHashMap<String, CachedResult> cache = new ConcurrentHashMap<>();

    /**
     * Detects the hosting provider for the given email address.
     * 
     * @return "google", "microsoft", or "unknown"
     */
    public String detectProvider(String email) {
        if (email == null || !email.contains("@")) {
            return "unknown";
        }

        String domain = email.substring(email.lastIndexOf('@') + 1).toLowerCase().trim();

        // Quick check for well-known consumer domains
        if (domain.equals("gmail.com") || domain.equals("googlemail.com")) {
            return "google";
        }
        if (domain.equals("outlook.com") || domain.equals("hotmail.com") || domain.equals("live.com")) {
            return "microsoft";
        }

        // Check cache
        CachedResult cached = cache.get(domain);
        if (cached != null && !cached.isExpired()) {
            return cached.provider;
        }

        // Perform MX lookup
        String provider = lookupMxProvider(domain);
        cache.put(domain, new CachedResult(provider));
        return provider;
    }

    /**
     * Returns human-readable name for a provider code.
     */
    public String getProviderName(String providerCode) {
        return switch (providerCode) {
            case "google" -> "Google Workspace";
            case "microsoft" -> "Microsoft 365";
            default -> null;
        };
    }

    /**
     * Performs DNS MX record lookup and matches against known provider patterns.
     */
    private String lookupMxProvider(String domain) {
        try {
            Hashtable<String, String> env = new Hashtable<>();
            env.put("java.naming.factory.initial", "com.sun.jndi.dns.DnsContextFactory");
            env.put("com.sun.jndi.dns.timeout.initial", "5000");
            env.put("com.sun.jndi.dns.timeout.retries", "2");

            DirContext ctx = new InitialDirContext(env);
            Attributes attrs = ctx.getAttributes(domain, new String[] { "MX" });
            Attribute mxAttr = attrs.get("MX");
            ctx.close();

            if (mxAttr == null || mxAttr.size() == 0) {
                return "unknown";
            }

            // Check all MX records
            for (int i = 0; i < mxAttr.size(); i++) {
                String mxRecord = mxAttr.get(i).toString().toLowerCase();

                // Google Workspace MX patterns
                if (mxRecord.contains("google.com") || mxRecord.contains("googlemail.com")
                        || mxRecord.contains("smtp.google.com") || mxRecord.contains("aspmx.l.google.com")) {
                    return "google";
                }

                // Microsoft 365 MX patterns
                if (mxRecord.contains("outlook.com") || mxRecord.contains("microsoft.com")
                        || mxRecord.contains("protection.outlook.com") || mxRecord.contains("mail.protection")) {
                    return "microsoft";
                }
            }

            return "unknown";

        } catch (Exception e) {
            System.err.println("[DomainDetectionService] MX lookup failed for " + domain + ": " + e.getMessage());
            return "unknown";
        }
    }

    /**
     * Simple cache entry with TTL.
     */
    private static class CachedResult {
        final String provider;
        final long timestamp;

        CachedResult(String provider) {
            this.provider = provider;
            this.timestamp = System.currentTimeMillis();
        }

        boolean isExpired() {
            return System.currentTimeMillis() - timestamp > CACHE_TTL_MS;
        }
    }
}
