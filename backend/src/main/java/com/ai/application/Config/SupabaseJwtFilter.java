package com.ai.application.Config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;

/**
 * JWT Filter for validating Supabase authentication tokens.
 * 
 * This filter verifies tokens by calling the Supabase Auth API,
 * which is the recommended approach by Supabase documentation.
 * 
 * Configuration:
 * Set SUPABASE_URL and SUPABASE_ANON_KEY environment variables.
 */
@Component
public class SupabaseJwtFilter extends OncePerRequestFilter {

    @Value("${supabase.url:https://qemqauonfieivgqzhwiz.supabase.co}")
    private String supabaseUrl;

    @Value("${supabase.anon.key:eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InFlbXFhdW9uZmllaXZncXpod2l6Iiwicm9sZSI6ImFub24iLCJpYXQiOjE3Njg5MjU2ODcsImV4cCI6MjA4NDUwMTY4N30._drP_O9gzRsdtJJqByyiekHCbppH3ac5C5Uiq-6bE0A}")
    private String supabaseAnonKey;

    private final RestTemplate restTemplate = new RestTemplate();

    @Override
    protected void doFilterInternal(HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);

            try {
                // Verify the JWT token by calling Supabase Auth API
                // This is the recommended approach per Supabase docs
                String authUrl = supabaseUrl + "/auth/v1/user";

                HttpHeaders headers = new HttpHeaders();
                headers.set("apikey", supabaseAnonKey);
                headers.set("Authorization", "Bearer " + token);

                HttpEntity<String> entity = new HttpEntity<>(headers);

                ResponseEntity<Map> authResponse = restTemplate.exchange(
                        authUrl,
                        HttpMethod.GET,
                        entity,
                        Map.class);

                if (authResponse.getStatusCode().is2xxSuccessful() && authResponse.getBody() != null) {
                    Map<String, Object> userData = authResponse.getBody();
                    String userId = (String) userData.get("id");
                    String email = (String) userData.get("email");

                    // Create authentication token
                    UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                            userId,
                            null,
                            Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER")));

                    // Store additional user info in authentication details
                    authentication.setDetails(new SupabaseUserDetails(userId, email));

                    SecurityContextHolder.getContext().setAuthentication(authentication);
                    logger.debug("JWT verified successfully for user: " + userId);
                } else {
                    SecurityContextHolder.clearContext();
                    logger.debug("JWT verification failed: Invalid response from Supabase");
                }
            } catch (Exception e) {
                // Token is invalid - clear security context
                SecurityContextHolder.clearContext();
                logger.debug("JWT verification failed: " + e.getMessage());
            }
        }

        filterChain.doFilter(request, response);
    }

    /**
     * Simple record to hold Supabase user details extracted from JWT.
     */
    public static class SupabaseUserDetails {
        private final String userId;
        private final String email;

        public SupabaseUserDetails(String userId, String email) {
            this.userId = userId;
            this.email = email;
        }

        public String getUserId() {
            return userId;
        }

        public String getEmail() {
            return email;
        }
    }
}
