package com.ai.application.Config;

import com.ai.application.Repositories.UserRepository;
import com.ai.application.Services.DomainDetectionService;
import com.ai.application.Services.EncryptionService;
import com.ai.application.Services.TokenService;
import com.ai.application.model.Entity.AuthConnection;
import com.ai.application.model.Entity.User;
import jakarta.servlet.ServletException;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Handles OAuth2 login success with three scenarios:
 * 1. Returning user (email exists in DB)
 * 2. New user (first OAuth login)
 * 3. Account linking (state contains linking token)
 */
@Component
public class OAuthSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private static final Logger logger = LoggerFactory.getLogger(OAuthSuccessHandler.class);

    private final UserRepository userRepository;
    private final TokenService tokenService;
    private final EncryptionService encryptionService;
    private final DomainDetectionService domainDetectionService;
    private final OAuth2AuthorizedClientService authorizedClientService;

    @Value("${app.frontend.url}")
    private String frontendUrl;

    public OAuthSuccessHandler(UserRepository userRepository,
            TokenService tokenService,
            EncryptionService encryptionService,
            DomainDetectionService domainDetectionService,
            OAuth2AuthorizedClientService authorizedClientService) {
        this.userRepository = userRepository;
        this.tokenService = tokenService;
        this.encryptionService = encryptionService;
        this.domainDetectionService = domainDetectionService;
        this.authorizedClientService = authorizedClientService;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication) throws IOException, ServletException {

        OAuth2AuthenticationToken oauthToken = (OAuth2AuthenticationToken) authentication;
        OAuth2User oauthUser = oauthToken.getPrincipal();
        String registrationId = oauthToken.getAuthorizedClientRegistrationId(); // "google" or "microsoft"

        // Normalize provider name
        String provider = "microsoft".equals(registrationId) || "azure".equals(registrationId)
                ? "microsoft"
                : registrationId;

        // Extract user info from OAuth
        String email = extractEmail(oauthUser, provider);
        String name = extractName(oauthUser, provider);
        String providerUserId = extractProviderUserId(oauthUser, provider);

        if (email == null || email.isEmpty()) {
            logger.error("No email returned from OAuth provider: {}", provider);
            response.sendRedirect(frontendUrl + "/auth?error=" + encode("No email from provider"));
            return;
        }

        email = email.trim().toLowerCase();

        // Get OAuth tokens from authorized client
        OAuth2AuthorizedClient authorizedClient = authorizedClientService.loadAuthorizedClient(
                registrationId, oauthToken.getName());

        String accessToken = authorizedClient != null && authorizedClient.getAccessToken() != null
                ? authorizedClient.getAccessToken().getTokenValue()
                : null;
        String refreshToken = authorizedClient != null && authorizedClient.getRefreshToken() != null
                ? authorizedClient.getRefreshToken().getTokenValue()
                : null;

        // Determine scopes
        List<String> scopes = new ArrayList<>();
        if (authorizedClient != null && authorizedClient.getAccessToken().getScopes() != null) {
            scopes.addAll(authorizedClient.getAccessToken().getScopes());
        }

        // Check for linking flow (state parameter contains linking token)
        String state = request.getParameter("state");
        if (state != null && !state.isEmpty()) {
            try {
                TokenService.LinkingInfo linkingInfo = tokenService.extractFromLinkingToken(state);
                handleLinking(linkingInfo, provider, providerUserId, email, accessToken, refreshToken, scopes,
                        response);
                return;
            } catch (TokenService.InvalidTokenException e) {
                // Not a linking token — proceed with normal login/signup
                logger.debug("State is not a linking token, proceeding with normal flow");
            }
        }

        // Check if user exists
        Optional<User> existingOpt = userRepository.findByEmail(email);

        if (existingOpt.isPresent()) {
            // SCENARIO 1: Returning user
            handleReturningUser(existingOpt.get(), provider, providerUserId, email,
                    accessToken, refreshToken, scopes, name, request, response);
        } else {
            // SCENARIO 2: New user
            handleNewUser(provider, providerUserId, email, name,
                    accessToken, refreshToken, scopes, request, response);
        }
    }

    /**
     * SCENARIO 1: User with this email already exists.
     */
    private void handleReturningUser(User user, String provider, String providerUserId, String providerEmail,
            String accessToken, String refreshToken, List<String> scopes,
            String name, HttpServletRequest request, HttpServletResponse response)
            throws IOException {

        // Add or update the connection
        AuthConnection connection = new AuthConnection(provider, providerUserId, providerEmail,
                user.hasProvider(provider) ? user.findConnection(provider).getConnectionType() : "linked");
        connection.setAccessTokenEncrypted(accessToken != null ? encryptionService.encrypt(accessToken) : null);
        connection.setRefreshTokenEncrypted(refreshToken != null ? encryptionService.encrypt(refreshToken) : null);
        connection.setTokenExpiresAt(LocalDateTime.now().plusHours(1));
        connection.setLastUsedAt(LocalDateTime.now());
        connection.setGrantedScopes(scopes);

        user.addOrUpdateConnection(connection);

        // Update profile photo from OAuth if not set
        if (user.getProfilePhotoUrl() == null || user.getProfilePhotoUrl().isEmpty()) {
            user.setProfilePhotoUrl(extractPhotoUrl(request));
        }

        // If user had no name set, use OAuth name
        if ((user.getName() == null || user.getName().isEmpty()) && name != null) {
            user.setName(name);
        }

        // Mark email as verified (OAuth = verified)
        user.setEmailVerified(true);

        user.setLastLoginAt(LocalDateTime.now());
        user.setLastLoginProvider(provider);
        user.setLoginCount(user.getLoginCount() + 1);
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);

        // Generate our app tokens
        issueTokensAndRedirect(user, provider, request, response);
    }

    /**
     * SCENARIO 2: New user signing up via OAuth.
     */
    private void handleNewUser(String provider, String providerUserId, String email, String name,
            String accessToken, String refreshToken, List<String> scopes,
            HttpServletRequest request, HttpServletResponse response)
            throws IOException {

        User user = new User();
        user.setEmail(email);
        user.setName(name != null ? name : email.split("@")[0]);
        user.setLocalAuthEnabled(false);
        user.setPrimaryOAuthProvider(provider);
        user.setEmailVerified(true);

        // Create primary connection
        AuthConnection connection = new AuthConnection(provider, providerUserId, email, "primary");
        connection.setAccessTokenEncrypted(accessToken != null ? encryptionService.encrypt(accessToken) : null);
        connection.setRefreshTokenEncrypted(refreshToken != null ? encryptionService.encrypt(refreshToken) : null);
        connection.setTokenExpiresAt(LocalDateTime.now().plusHours(1));
        connection.setGrantedScopes(scopes);

        user.addOrUpdateConnection(connection);

        // MX detection
        try {
            String domain = email.substring(email.indexOf('@') + 1);
            String hosting = domainDetectionService.detectProvider(domain);
            user.setDetectedHostingProvider(hosting);
        } catch (Exception e) {
            user.setDetectedHostingProvider("unknown");
        }

        user.setLastLoginAt(LocalDateTime.now());
        user.setLastLoginProvider(provider);
        user.setLoginCount(1);
        user = userRepository.save(user);

        issueTokensAndRedirect(user, provider, request, response);
    }

    /**
     * SCENARIO 3: Account linking (user already logged in).
     */
    private void handleLinking(TokenService.LinkingInfo linkingInfo, String provider,
            String providerUserId, String providerEmail,
            String accessToken, String refreshToken, List<String> scopes,
            HttpServletResponse response) throws IOException {

        Optional<User> userOpt = userRepository.findById(linkingInfo.getUserId());
        if (userOpt.isEmpty()) {
            response.sendRedirect(frontendUrl + "/profile?linked=error&message=" + encode("User not found"));
            return;
        }

        User user = userOpt.get();

        AuthConnection connection = new AuthConnection(provider, providerUserId, providerEmail, "linked");
        connection.setAccessTokenEncrypted(accessToken != null ? encryptionService.encrypt(accessToken) : null);
        connection.setRefreshTokenEncrypted(refreshToken != null ? encryptionService.encrypt(refreshToken) : null);
        connection.setTokenExpiresAt(LocalDateTime.now().plusHours(1));
        connection.setGrantedScopes(scopes);

        user.addOrUpdateConnection(connection);
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);

        response.sendRedirect(frontendUrl + "/profile?linked=success");
    }

    /**
     * Issue JWT + refresh token, set cookie, redirect to frontend.
     */
    private void issueTokensAndRedirect(User user, String provider,
            HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        String accessToken = tokenService.generateAccessToken(user);
        String refreshToken = tokenService.generateRefreshToken(user,
                request.getHeader("User-Agent"),
                request.getRemoteAddr(),
                provider);

        // Set refresh token as httpOnly cookie with SameSite=None for cross-origin
        // support
        String cookieValue = String.format(
                "refreshToken=%s; HttpOnly; Secure; SameSite=None; Path=/api/auth; Max-Age=%d",
                refreshToken, 30 * 24 * 60 * 60);
        response.addHeader("Set-Cookie", cookieValue);

        // Redirect to frontend callback page with access token
        response.sendRedirect(frontendUrl + "/auth/callback?token=" + accessToken);
    }

    // ── Helpers ──

    private String extractEmail(OAuth2User oauthUser, String provider) {
        String email = oauthUser.getAttribute("email");
        if (email != null)
            return email;

        if ("microsoft".equals(provider)) {
            email = oauthUser.getAttribute("preferred_username");
            if (email != null)
                return email;
            email = oauthUser.getAttribute("upn");
        }
        return email;
    }

    private String extractName(OAuth2User oauthUser, String provider) {
        String name = oauthUser.getAttribute("name");
        if (name != null)
            return name;

        if ("google".equals(provider)) {
            String given = oauthUser.getAttribute("given_name");
            String family = oauthUser.getAttribute("family_name");
            if (given != null)
                return (given + (family != null ? " " + family : "")).trim();
        }
        if ("microsoft".equals(provider)) {
            return oauthUser.getAttribute("displayName");
        }
        return null;
    }

    private String extractProviderUserId(OAuth2User oauthUser, String provider) {
        String sub = oauthUser.getAttribute("sub");
        if (sub != null)
            return sub;
        String id = oauthUser.getAttribute("id");
        if (id != null)
            return id;
        String oid = oauthUser.getAttribute("oid");
        return oid;
    }

    private String extractPhotoUrl(HttpServletRequest request) {
        // Could extract from OAuth user info, but profile photos often require separate
        // API calls
        return null;
    }

    private String encode(String value) {
        try {
            return URLEncoder.encode(value, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            return value;
        }
    }
}
