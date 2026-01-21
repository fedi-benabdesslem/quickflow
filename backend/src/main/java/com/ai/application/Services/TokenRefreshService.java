package com.ai.application.Services;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

/**
 * Service for refreshing expired OAuth tokens.
 * 
 * Handles automatic token refresh for Google and Microsoft OAuth providers
 * when access tokens expire.
 */
@Service
public class TokenRefreshService {

    private static final String GOOGLE_TOKEN_URL = "https://oauth2.googleapis.com/token";
    private static final String MICROSOFT_TOKEN_URL = "https://login.microsoftonline.com/%s/oauth2/v2.0/token";

    @Value("${google.oauth.client-id:}")
    private String googleClientId;

    @Value("${google.oauth.client-secret:}")
    private String googleClientSecret;

    @Value("${microsoft.oauth.client-id:}")
    private String microsoftClientId;

    @Value("${microsoft.oauth.client-secret:}")
    private String microsoftClientSecret;

    @Value("${microsoft.oauth.tenant-id:common}")
    private String microsoftTenantId;

    private final TokenStorageService tokenStorageService;
    private final RestTemplate restTemplate;
    private final Gson gson;

    public TokenRefreshService(TokenStorageService tokenStorageService) {
        this.tokenStorageService = tokenStorageService;
        this.restTemplate = new RestTemplate();
        this.gson = new Gson();
    }

    /**
     * Refreshes an expired access token and updates the database.
     * Returns the new access token, or null if refresh fails.
     */
    public String refreshTokenIfNeeded(String supabaseId) {
        TokenStorageService.DecryptedTokens tokens = tokenStorageService.getDecryptedTokens(supabaseId);

        if (tokens == null) {
            return null;
        }

        // If token is not expired, return existing
        if (!tokens.isExpiredOrExpiringSoon()) {
            return tokens.getAccessToken();
        }

        // Token is expired, try to refresh
        String provider = tokens.getProvider();
        String refreshToken = tokens.getRefreshToken();

        if (refreshToken == null || refreshToken.isEmpty()) {
            return null;
        }

        try {
            RefreshResult result = switch (provider.toLowerCase()) {
                case "google" -> refreshGoogleToken(refreshToken);
                case "azure" -> refreshMicrosoftToken(refreshToken);
                default -> null;
            };

            if (result != null && result.accessToken != null) {
                // Update tokens in database
                tokenStorageService.updateTokensAfterRefresh(
                        supabaseId,
                        result.accessToken,
                        result.expiresIn);
                return result.accessToken;
            }
        } catch (Exception e) {
            System.err.println("Failed to refresh token for user " + supabaseId + ": " + e.getMessage());
        }

        return null;
    }

    /**
     * Refreshes a Google OAuth token using the refresh token.
     */
    private RefreshResult refreshGoogleToken(String refreshToken) {
        if (googleClientId.isEmpty() || googleClientSecret.isEmpty()) {
            System.err.println("Google OAuth credentials not configured. Cannot refresh token.");
            return null;
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("client_id", googleClientId);
        body.add("client_secret", googleClientSecret);
        body.add("refresh_token", refreshToken);
        body.add("grant_type", "refresh_token");

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<String> response = restTemplate.postForEntity(GOOGLE_TOKEN_URL, request, String.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                JsonObject json = gson.fromJson(response.getBody(), JsonObject.class);
                String accessToken = json.get("access_token").getAsString();
                long expiresIn = json.get("expires_in").getAsLong();
                return new RefreshResult(accessToken, expiresIn);
            }
        } catch (Exception e) {
            System.err.println("Google token refresh failed: " + e.getMessage());
        }

        return null;
    }

    /**
     * Refreshes a Microsoft OAuth token using the refresh token.
     */
    private RefreshResult refreshMicrosoftToken(String refreshToken) {
        if (microsoftClientId.isEmpty() || microsoftClientSecret.isEmpty()) {
            System.err.println("Microsoft OAuth credentials not configured. Cannot refresh token.");
            return null;
        }

        String tokenUrl = String.format(MICROSOFT_TOKEN_URL, microsoftTenantId);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("client_id", microsoftClientId);
        body.add("client_secret", microsoftClientSecret);
        body.add("refresh_token", refreshToken);
        body.add("grant_type", "refresh_token");
        body.add("scope", "Mail.Send offline_access");

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<String> response = restTemplate.postForEntity(tokenUrl, request, String.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                JsonObject json = gson.fromJson(response.getBody(), JsonObject.class);
                String accessToken = json.get("access_token").getAsString();
                long expiresIn = json.get("expires_in").getAsLong();
                return new RefreshResult(accessToken, expiresIn);
            }
        } catch (Exception e) {
            System.err.println("Microsoft token refresh failed: " + e.getMessage());
        }

        return null;
    }

    /**
     * Helper class to hold refresh result.
     */
    private static class RefreshResult {
        final String accessToken;
        final long expiresIn;

        RefreshResult(String accessToken, long expiresIn) {
            this.accessToken = accessToken;
            this.expiresIn = expiresIn;
        }
    }
}
