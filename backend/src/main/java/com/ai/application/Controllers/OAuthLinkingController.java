package com.ai.application.Controllers;

import com.ai.application.Repositories.UserTokenRepository;
import com.ai.application.Services.EncryptionService;
import com.ai.application.model.Entity.UserToken;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.Principal;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Map;

/**
 * Controller for OAuth account linking.
 * 
 * Allows email/password users to link their Google or Microsoft account
 * for email sending capabilities without changing their auth method.
 */
@RestController
@RequestMapping("/api/auth/link")
public class OAuthLinkingController {

    private static final String GOOGLE_AUTH_URL = "https://accounts.google.com/o/oauth2/v2/auth";
    private static final String GOOGLE_TOKEN_URL = "https://oauth2.googleapis.com/token";
    private static final String GOOGLE_USERINFO_URL = "https://www.googleapis.com/oauth2/v2/userinfo";

    private static final String MICROSOFT_AUTH_URL = "https://login.microsoftonline.com/%s/oauth2/v2.0/authorize";
    private static final String MICROSOFT_TOKEN_URL = "https://login.microsoftonline.com/%s/oauth2/v2.0/token";
    private static final String MICROSOFT_USERINFO_URL = "https://graph.microsoft.com/v1.0/me";

    private static final long LINKING_TOKEN_EXPIRY_MS = 10 * 60 * 1000; // 10 minutes

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

    @Value("${supabase.jwt.secret:}")
    private String jwtSecret;

    @Value("${oauth.linking.frontend-url:http://localhost:5173}")
    private String frontendUrl;

    @Value("${oauth.linking.backend-url:http://localhost:8080}")
    private String backendUrl;

    private final UserTokenRepository userTokenRepository;
    private final EncryptionService encryptionService;
    private final RestTemplate restTemplate;
    private final Gson gson;

    public OAuthLinkingController(UserTokenRepository userTokenRepository,
            EncryptionService encryptionService) {
        this.userTokenRepository = userTokenRepository;
        this.encryptionService = encryptionService;
        this.restTemplate = new RestTemplate();
        this.gson = new Gson();
    }

    /**
     * Generates an OAuth authorization URL for linking a provider.
     * Returns the URL that the frontend should redirect to.
     */
    @GetMapping("/{provider}")
    public ResponseEntity<?> getLinkingUrl(@PathVariable String provider, Principal principal) {
        String supabaseId = principal.getName();
        System.out.println("[OAuthLinking] Link request for provider: " + provider + ", user: " + supabaseId);

        // Validate provider
        if (!"google".equalsIgnoreCase(provider) && !"microsoft".equalsIgnoreCase(provider)) {
            return ResponseEntity.badRequest().body(Map.of(
                    "status", "error",
                    "message", "Unsupported provider. Use 'google' or 'microsoft'."));
        }

        // Generate a short-lived linking token (HMAC-signed)
        String linkingToken = generateLinkingToken(supabaseId, provider.toLowerCase());
        String callbackUrl = backendUrl + "/api/auth/link/callback";

        String authorizationUrl;
        if ("google".equalsIgnoreCase(provider)) {
            authorizationUrl = buildGoogleAuthUrl(linkingToken, callbackUrl);
        } else {
            authorizationUrl = buildMicrosoftAuthUrl(linkingToken, callbackUrl);
        }

        return ResponseEntity.ok(Map.of(
                "status", "success",
                "authorizationUrl", authorizationUrl));
    }

    /**
     * OAuth callback endpoint — handles the redirect from Google/Microsoft.
     * This is a PUBLIC endpoint (no JWT required) because it's called by the
     * OAuth provider redirect, not by the authenticated frontend.
     * Authentication is via the signed linking token in the state parameter.
     */
    @GetMapping("/callback")
    public ResponseEntity<?> handleCallback(
            @RequestParam(required = false) String code,
            @RequestParam(required = false) String state,
            @RequestParam(required = false) String error) {

        System.out.println("[OAuthLinking] Callback received - code present: " + (code != null)
                + ", state present: " + (state != null) + ", error: " + error);

        // Handle OAuth errors
        if (error != null) {
            System.err.println("[OAuthLinking] OAuth error: " + error);
            return redirectToFrontend("error", "OAuth authorization was denied or failed.");
        }

        if (code == null || state == null) {
            return redirectToFrontend("error", "Missing authorization code or state.");
        }

        // Validate the linking token
        LinkingTokenClaims claims;
        try {
            claims = validateLinkingToken(state);
        } catch (Exception e) {
            System.err.println("[OAuthLinking] Invalid linking token: " + e.getMessage());
            return redirectToFrontend("error", "Link session expired. Please try again.");
        }

        String supabaseId = claims.supabaseId;
        String provider = claims.provider;
        String callbackUrl = backendUrl + "/api/auth/link/callback";

        System.out.println("[OAuthLinking] Valid linking token - user: " + supabaseId + ", provider: " + provider);

        try {
            // Exchange authorization code for tokens
            TokenResponse tokens;
            String linkedEmail;

            if ("google".equals(provider)) {
                tokens = exchangeGoogleCode(code, callbackUrl);
                linkedEmail = getGoogleUserEmail(tokens.accessToken);
            } else {
                tokens = exchangeMicrosoftCode(code, callbackUrl);
                linkedEmail = getMicrosoftUserEmail(tokens.accessToken);
            }

            if (tokens.accessToken == null) {
                return redirectToFrontend("error", "Failed to obtain tokens from provider.");
            }

            // Store linked provider tokens
            UserToken userToken = userTokenRepository.findBySupabaseId(supabaseId)
                    .orElse(null);

            if (userToken == null) {
                return redirectToFrontend("error", "User account not found.");
            }

            userToken.setLinkedProvider(provider);
            userToken.setLinkedProviderTokenEncrypted(encryptionService.encrypt(tokens.accessToken));
            if (tokens.refreshToken != null) {
                userToken.setLinkedProviderRefreshToken(encryptionService.encrypt(tokens.refreshToken));
            }
            userToken.setLinkedProviderEmail(linkedEmail);
            userToken.setUpdatedAt(LocalDateTime.now());
            userTokenRepository.save(userToken);

            System.out.println("[OAuthLinking] Successfully linked " + provider + " for user: " + supabaseId);
            return redirectToFrontend("success", null);

        } catch (Exception e) {
            System.err.println("[OAuthLinking] Token exchange failed: " + e.getMessage());
            e.printStackTrace();
            return redirectToFrontend("error", "Failed to complete account linking.");
        }
    }

    /**
     * Unlinks an OAuth provider from the user's account.
     */
    @DeleteMapping
    public ResponseEntity<?> unlinkProvider(Principal principal) {
        String supabaseId = principal.getName();
        System.out.println("[OAuthLinking] Unlink request for user: " + supabaseId);

        return userTokenRepository.findBySupabaseId(supabaseId)
                .map(userToken -> {
                    String previousProvider = userToken.getLinkedProvider();
                    userToken.clearLinkedProvider();
                    userTokenRepository.save(userToken);
                    System.out.println("[OAuthLinking] Unlinked " + previousProvider + " for user: " + supabaseId);
                    return ResponseEntity.ok(Map.of(
                            "status", "success",
                            "message", "Provider unlinked successfully."));
                })
                .orElse(ResponseEntity.badRequest().body(Map.of(
                        "status", "error",
                        "message", "User not found.")));
    }

    // ===== Linking Token (HMAC-signed) =====

    /**
     * Generates a simple HMAC-signed linking token:
     * payload = supabaseId|provider|expiryTimestamp
     * token = base64(payload) + "." + base64(hmac-sha256(payload))
     */
    private String generateLinkingToken(String supabaseId, String provider) {
        long expiry = System.currentTimeMillis() + LINKING_TOKEN_EXPIRY_MS;
        String payload = supabaseId + "|" + provider + "|" + expiry;
        String payloadB64 = Base64.getUrlEncoder().withoutPadding()
                .encodeToString(payload.getBytes(StandardCharsets.UTF_8));
        String signature = hmacSign(payload);
        return payloadB64 + "." + signature;
    }

    /**
     * Validates a linking token and returns the claims if valid.
     */
    private LinkingTokenClaims validateLinkingToken(String token) {
        String[] parts = token.split("\\.");
        if (parts.length != 2) {
            throw new RuntimeException("Invalid token format");
        }

        String payload = new String(Base64.getUrlDecoder().decode(parts[0]), StandardCharsets.UTF_8);
        String expectedSignature = hmacSign(payload);

        if (!expectedSignature.equals(parts[1])) {
            throw new RuntimeException("Invalid token signature");
        }

        String[] fields = payload.split("\\|");
        if (fields.length != 3) {
            throw new RuntimeException("Invalid token payload");
        }

        long expiry = Long.parseLong(fields[2]);
        if (System.currentTimeMillis() > expiry) {
            throw new RuntimeException("Token expired");
        }

        return new LinkingTokenClaims(fields[0], fields[1]);
    }

    private String hmacSign(String data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec keySpec = new SecretKeySpec(
                    jwtSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(keySpec);
            byte[] rawHmac = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(rawHmac);
        } catch (Exception e) {
            throw new RuntimeException("Failed to sign token", e);
        }
    }

    // ===== OAuth URL Builders =====

    private String buildGoogleAuthUrl(String state, String callbackUrl) {
        return GOOGLE_AUTH_URL + "?"
                + "client_id=" + enc(googleClientId)
                + "&redirect_uri=" + enc(callbackUrl)
                + "&response_type=code"
                + "&scope=" + enc("https://www.googleapis.com/auth/gmail.send email profile")
                + "&access_type=offline"
                + "&prompt=consent"
                + "&state=" + enc(state);
    }

    private String buildMicrosoftAuthUrl(String state, String callbackUrl) {
        String baseUrl = String.format(MICROSOFT_AUTH_URL, microsoftTenantId);
        return baseUrl + "?"
                + "client_id=" + enc(microsoftClientId)
                + "&redirect_uri=" + enc(callbackUrl)
                + "&response_type=code"
                + "&scope=" + enc("email Mail.Send offline_access User.Read")
                + "&state=" + enc(state);
    }

    // ===== Token Exchange =====

    private TokenResponse exchangeGoogleCode(String code, String callbackUrl) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("client_id", googleClientId);
        body.add("client_secret", googleClientSecret);
        body.add("code", code);
        body.add("redirect_uri", callbackUrl);
        body.add("grant_type", "authorization_code");

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);
        ResponseEntity<String> response = restTemplate.postForEntity(GOOGLE_TOKEN_URL, request, String.class);

        if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
            JsonObject json = gson.fromJson(response.getBody(), JsonObject.class);
            return new TokenResponse(
                    json.get("access_token").getAsString(),
                    json.has("refresh_token") ? json.get("refresh_token").getAsString() : null);
        }

        return new TokenResponse(null, null);
    }

    private TokenResponse exchangeMicrosoftCode(String code, String callbackUrl) {
        String tokenUrl = String.format(MICROSOFT_TOKEN_URL, microsoftTenantId);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("client_id", microsoftClientId);
        body.add("client_secret", microsoftClientSecret);
        body.add("code", code);
        body.add("redirect_uri", callbackUrl);
        body.add("grant_type", "authorization_code");
        body.add("scope", "email Mail.Send offline_access User.Read");

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);
        ResponseEntity<String> response = restTemplate.postForEntity(tokenUrl, request, String.class);

        if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
            JsonObject json = gson.fromJson(response.getBody(), JsonObject.class);
            return new TokenResponse(
                    json.get("access_token").getAsString(),
                    json.has("refresh_token") ? json.get("refresh_token").getAsString() : null);
        }

        return new TokenResponse(null, null);
    }

    // ===== User Info =====

    private String getGoogleUserEmail(String accessToken) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(accessToken);
            HttpEntity<Void> request = new HttpEntity<>(headers);
            ResponseEntity<String> response = restTemplate.exchange(
                    GOOGLE_USERINFO_URL, HttpMethod.GET, request, String.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                JsonObject json = gson.fromJson(response.getBody(), JsonObject.class);
                return json.has("email") ? json.get("email").getAsString() : null;
            }
        } catch (Exception e) {
            System.err.println("[OAuthLinking] Failed to get Google user email: " + e.getMessage());
        }
        return null;
    }

    private String getMicrosoftUserEmail(String accessToken) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(accessToken);
            HttpEntity<Void> request = new HttpEntity<>(headers);
            ResponseEntity<String> response = restTemplate.exchange(
                    MICROSOFT_USERINFO_URL, HttpMethod.GET, request, String.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                JsonObject json = gson.fromJson(response.getBody(), JsonObject.class);
                if (json.has("mail") && !json.get("mail").isJsonNull()) {
                    return json.get("mail").getAsString();
                }
                if (json.has("userPrincipalName")) {
                    return json.get("userPrincipalName").getAsString();
                }
            }
        } catch (Exception e) {
            System.err.println("[OAuthLinking] Failed to get Microsoft user email: " + e.getMessage());
        }
        return null;
    }

    // ===== Redirect Helper =====

    private ResponseEntity<?> redirectToFrontend(String status, String errorMessage) {
        String redirectUrl = frontendUrl + "/profile?linked=" + status;
        if (errorMessage != null) {
            redirectUrl += "&error=" + enc(errorMessage);
        }
        return ResponseEntity.status(HttpStatus.FOUND)
                .header("Location", redirectUrl)
                .build();
    }

    private static String enc(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    // ===== Inner Classes =====

    private record LinkingTokenClaims(String supabaseId, String provider) {
    }

    private record TokenResponse(String accessToken, String refreshToken) {
    }
}
