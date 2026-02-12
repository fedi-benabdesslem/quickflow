package com.ai.application.Controllers;

import com.ai.application.Services.TokenStorageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.Map;

/**
 * Controller for authentication-related endpoints.
 * 
 * Handles OAuth token storage after successful login.
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final TokenStorageService tokenStorageService;

    @Autowired
    public AuthController(TokenStorageService tokenStorageService) {
        this.tokenStorageService = tokenStorageService;
    }

    /**
     * Stores OAuth tokens after user login.
     * 
     * Called from frontend after successful OAuth sign-in to save
     * the provider tokens for later use in sending emails.
     * 
     * Request body:
     * {
     * "accessToken": "...",
     * "refreshToken": "...",
     * "provider": "google" | "azure" | "email",
     * "expiresIn": 3600, // seconds
     * "email": "user@example.com"
     * }
     */
    @PostMapping("/store-tokens")
    public ResponseEntity<?> storeTokens(@RequestBody Map<String, Object> body, Principal principal) {
        System.out.println("[AuthController] /store-tokens endpoint called");

        if (principal == null) {
            System.err.println("[AuthController] ERROR: No principal - user not authenticated");
            return ResponseEntity.status(401).body(Map.of(
                    "status", "error",
                    "message", "Not authenticated"));
        }

        String supabaseId = principal.getName();
        String accessToken = (String) body.get("accessToken");
        String refreshToken = (String) body.get("refreshToken");
        String provider = (String) body.get("provider");
        String email = (String) body.get("email");

        System.out.println("[AuthController] Storing tokens for user: " + supabaseId);
        System.out.println("[AuthController] Provider: " + provider);

        // Handle expiresIn - can be integer or long
        long expiresIn = 3600; // default 1 hour
        Object expiresInObj = body.get("expiresIn");
        if (expiresInObj != null) {
            if (expiresInObj instanceof Number) {
                expiresIn = ((Number) expiresInObj).longValue();
            }
        }
        System.out.println("[AuthController] Token expires in: " + expiresIn + " seconds");

        // Validate required fields
        if (accessToken == null || accessToken.isEmpty()) {
            System.err.println("[AuthController] ERROR: Access token is empty or null");
            return ResponseEntity.badRequest().body(Map.of(
                    "status", "error",
                    "message", "Access token is required"));
        }

        if (provider == null || provider.isEmpty()) {
            provider = "email";
        }

        if (email == null || email.isEmpty()) {
            email = supabaseId + "@placeholder.com";
        }

        try {
            tokenStorageService.storeTokens(
                    supabaseId,
                    email,
                    provider,
                    accessToken,
                    refreshToken,
                    expiresIn);

            System.out.println("[AuthController] SUCCESS: Tokens stored successfully for user: " + supabaseId);
            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "message", "Tokens stored successfully"));
        } catch (Exception e) {
            System.err.println("[AuthController] ERROR: Failed to store tokens: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of(
                    "status", "error",
                    "message", "Failed to store tokens"));
        }
    }

    /**
     * Checks if user has stored OAuth tokens and can send emails.
     */
    @GetMapping("/email-capability")
    public ResponseEntity<?> checkEmailCapability(Principal principal) {
        if (principal == null) {
            return ResponseEntity.status(401).body(Map.of(
                    "status", "error",
                    "message", "Not authenticated"));
        }

        String supabaseId = principal.getName();

        return tokenStorageService.getUserToken(supabaseId)
                .map(token -> ResponseEntity.ok(Map.of(
                        "status", "success",
                        "canSendEmail", token.canSendEmail(),
                        "provider", token.getProvider())))
                .orElseGet(() -> ResponseEntity.ok(Map.of(
                        "status", "success",
                        "canSendEmail", false,
                        "provider", "none")));
    }
}
