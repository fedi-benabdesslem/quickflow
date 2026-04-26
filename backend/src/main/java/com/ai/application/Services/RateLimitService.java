package com.ai.application.Services;

import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Simple in-memory rate limiter for auth endpoints.
 * Tracks attempts per IP per endpoint with sliding time windows.
 */
@Service
public class RateLimitService {

    // key: "endpoint:ip" → list of timestamps
    private final Map<String, RateLimitBucket> buckets = new ConcurrentHashMap<>();

    // Rate limit configurations: endpoint → RateLimit
    private static final Map<String, RateLimit> LIMITS = Map.of(
            "/api/auth/login", new RateLimit(5, 60),
            "/api/auth/signup", new RateLimit(3, 60),
            "/api/auth/forgot-password", new RateLimit(3, 60),
            "/api/auth/verify-mfa", new RateLimit(5, 60),
            "/api/auth/refresh", new RateLimit(30, 60));

    /**
     * Check if the request should be allowed.
     * Returns the number of seconds to wait if rate limited, or 0 if allowed.
     */
    public int checkRateLimit(String endpoint, String ipAddress) {
        RateLimit limit = LIMITS.get(endpoint);
        if (limit == null)
            return 0; // No rate limit for this endpoint

        String key = endpoint + ":" + ipAddress;
        RateLimitBucket bucket = buckets.computeIfAbsent(key,
                k -> new RateLimitBucket(limit.maxRequests, limit.windowSeconds));

        return bucket.tryConsume();
    }

    /**
     * Cleanup old buckets (call periodically or let GC handle it for now).
     */
    public void cleanup() {
        long now = System.currentTimeMillis();
        buckets.entrySet().removeIf(e -> now - e.getValue().lastAccess > 120_000); // 2 minute idle cleanup
    }

    // ── Inner classes ──

    private static class RateLimit {
        final int maxRequests;
        final int windowSeconds;

        RateLimit(int maxRequests, int windowSeconds) {
            this.maxRequests = maxRequests;
            this.windowSeconds = windowSeconds;
        }
    }

    private static class RateLimitBucket {
        final int maxRequests;
        final int windowSeconds;
        final long[] timestamps;
        int head;
        long lastAccess;

        RateLimitBucket(int maxRequests, int windowSeconds) {
            this.maxRequests = maxRequests;
            this.windowSeconds = windowSeconds;
            this.timestamps = new long[maxRequests];
            this.head = 0;
            this.lastAccess = System.currentTimeMillis();
        }

        /**
         * Try to consume a request.
         * Returns 0 if allowed, or seconds to wait if rate limited.
         */
        synchronized int tryConsume() {
            long now = System.currentTimeMillis();
            lastAccess = now;

            long windowStart = now - (windowSeconds * 1000L);

            // Count requests in current window
            int count = 0;
            long oldestInWindow = now;
            for (long ts : timestamps) {
                if (ts > windowStart) {
                    count++;
                    if (ts < oldestInWindow) {
                        oldestInWindow = ts;
                    }
                }
            }

            if (count >= maxRequests) {
                // Rate limited — return seconds until oldest request falls out of window
                long retryAfterMs = (oldestInWindow + windowSeconds * 1000L) - now;
                return (int) Math.max(1, retryAfterMs / 1000);
            }

            // Record this request
            timestamps[head] = now;
            head = (head + 1) % maxRequests;
            return 0;
        }
    }
}
