package com.ai.application.Controllers;

import com.ai.application.Repositories.UserRepository;
import com.ai.application.Services.EncryptionService;
import com.ai.application.model.Entity.AuthConnection;
import com.ai.application.model.Entity.User;
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
import java.util.Optional;

/**
 * Controller for OAuth account linking.
 *
 * Allows email/password users to link their Google or Microsoft account
 * for email sending capabilities without changing their auth method.
 *
 * Uses the new User model with AuthConnection, with a legacy UserToken
 * fallback.
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

    @Value("${spring.security.oauth2.client.registration.microsoft.client-id:}")
    private String microsoftClientId;

    @Value("${spring.security.oauth2.client.registration.microsoft.client-secret:}")
    private String microsoftClientSecret;

    @Value("${spring.security.oauth2.client.provider.microsoft.tenant-id:common}")
    private String microsoftTenantId;

    @Value("${app.jwt.secret}")
    private String jwtSecret;

    @Value("${app.frontend.url:http://localhost:5173}")
    private String frontendUrl;

    @Value("${app.backend.url:http://localhost:8080}")
    private String backendUrl;

    private final UserRepository userRepository;
    private final EncryptionService encryptionService;
    private final RestTemplate restTemplate;
    private final Gson gson;

    public OAuthLinkingController(UserRepository userRepository,
            EncryptionService encryptionService) {
        this.userRepository = userRepository;
        this.encryptionService = encryptionService;
        this.restTemplate = new RestTemplate();
        this.gson = new Gson();
    }

    /**
     * Generates an OAuth authorization URL for linking a provider.
     */
    @GetMapping("/{provider}")
    public ResponseEntity<?> getLinkingUrl(@PathVariable String provider, Principal principal) {
        String userId = principal.getName();
        System.out.println("[OAuthLinking] Link request for provider: " + provider + ", user: " + userId);

        if (!"google".equalsIgnoreCase(provider) && !"microsoft".equalsIgnoreCase(provider)) {
            return ResponseEntity.badRequest().body(Map.of(
                    "status", "error",
                    "message", "Unsupported provider. Use 'google' or 'microsoft'."));
        }

        String linkingToken = generateLinkingToken(userId, provider.toLowerCase());
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
     * OAuth callback — handles redirect from Google/Microsoft.
     * PUBLIC endpoint — auth is via the signed linking token in state parameter.
     */
    @GetMapping("/callback")
    public ResponseEntity<?> handleCallback(
            @RequestParam(required = false) String code,
            @RequestParam(required = false) String state,
            @RequestParam(required = false) String error) {

        System.out.println("[OAuthLinking] Callback received - code present: " + (code != null)
                + ", state present: " + (state != null) + ", error: " + error);

        if (error != null) {
            System.err.println("[OAuthLinking] OAuth error: " + error);
            return redirectToFrontend("error", "OAuth authorization was denied or failed.");
        }

        if (code == null || state == null) {
            return redirectToFrontend("error", "Missing authorization code or state.");
        }

        LinkingTokenClaims claims;
        try {
            claims = validateLinkingToken(state);
        } catch (Exception e) {
            System.err.println("[OAuthLinking] Invalid linking token: " + e.getMessage());
            return redirectToFrontend("error", "Link session expired. Please try again.");
        }

        String userId = claims.userId;
        String provider = claims.provider;
        String callbackUrl = backendUrl + "/api/auth/link/callback";

        System.out.println("[OAuthLinking] Valid linking token - user: " + userId + ", provider: " + provider);

        try {
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

            // Find user by ID first, then by email
            Optional<User> userOpt = userRepository.findById(userId);
            if (userOpt.isEmpty()) {
                userOpt = userRepository.findByEmail(userId);
            }

            if (userOpt.isEmpty()) {
                return redirectToFrontend("error", "User account not found.");
            }

            User user = userOpt.get();

            // Create or update AuthConnection for linked provider
            AuthConnection conn = user.findConnection(provider);
            if (conn == null) {
                conn = new AuthConnection();
                conn.setProvider(provider);
                conn.setConnectionType("linked");
                if (user.getAuthConnections() == null) {
                    user.setAuthConnections(new java.util.ArrayList<>());
                }
                user.getAuthConnections().add(conn);
            }

            conn.setAccessTokenEncrypted(encryptionService.encrypt(tokens.accessToken));
            if (tokens.refreshToken != null) {
                conn.setRefreshTokenEncrypted(encryptionService.encrypt(tokens.refreshToken));
            }
            conn.setProviderEmail(linkedEmail);
            conn.setConnectedAt(LocalDateTime.now());
            conn.setTokenExpiresAt(LocalDateTime.now().plusHours(1));

            user.setUpdatedAt(LocalDateTime.now());
            userRepository.save(user);

            System.out.println("[OAuthLinking] Successfully linked " + provider
                    + " for user: " + userId);
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
        String userId = principal.getName();
        System.out.println("[OAuthLinking] Unlink request for user: " + userId);

        // Try new User model first
        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            // Remove all linked (non-primary) AuthConnections
            if (user.getAuthConnections() != null) {
                user.getAuthConnections().removeIf(c -> "linked".equals(c.getConnectionType()));
                user.setUpdatedAt(LocalDateTime.now());
                userRepository.save(user);
            }
            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "message", "Provider unlinked successfully."));
        }

        // Fallback to legacy model
        return userTokenRepository.findByEmail(userId)
                .map(userToken -> {
                    String previousProvider = userToken.getLinkedProvider();
                    userToken.clearLinkedProvider();
                    userTokenRepository.save(userToken);
                    System.out.println("[OAuthLinking] Unlinked " + previousProvider + " for user: " + userId);
                    return ResponseEntity.ok(Map.of(
                            "status", "success",
                            "message", "Provider unlinked successfully."));
                })
                .orElse(ResponseEntity.badRequest().body(Map.of(
                        "status", "error",
                        "message", "User not found.")));
    }

    // ===== Linking Token (HMAC-signed) =====

    private String generateLinkingToken(String userId, String provider) {
        long expiry = System.currentTimeMillis() + LINKING_TOKEN_EXPIRY_MS;
        String payload = userId + "|" + provider + "|" + expiry;
        String payloadB64 = Base64.getUrlEncoder().withoutPadding()
                .encodeToString(payload.getBytes(StandardCharsets.UTF_8));
        String signature = hmacSign(payload);
        return payloadB64 + "." + signature;
    }

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

    private record LinkingTokenClaims(String userId, String provider) {
    }

    private record TokenResponse(String accessToken, String refreshToken) {
    }
}
