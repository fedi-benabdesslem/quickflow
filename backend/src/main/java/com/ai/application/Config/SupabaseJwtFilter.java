package com.ai.application.Config;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.auth0.jwt.interfaces.JWTVerifier;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

/**
 * JWT Filter for validating Supabase authentication tokens.
 * 
 * Configuration:
 * Set the SUPABASE_JWT_SECRET environment variable with your Supabase project's
 * JWT secret.
 * Get it from: https://supabase.com/dashboard/project/_/settings/api
 */
@Component
public class SupabaseJwtFilter extends OncePerRequestFilter {

    @Value("${supabase.jwt.secret:23e314f6-014f-48d0-98e7-ef6a75bf1e6c}")
    private String jwtSecret;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);

            try {
                // Verify the JWT token
                Algorithm algorithm = Algorithm.HMAC256(jwtSecret);
                JWTVerifier verifier = JWT.require(algorithm)
                        .acceptLeeway(60) // Accept 60 seconds of clock skew
                        .build();

                DecodedJWT jwt = verifier.verify(token);

                // Extract user info from JWT claims
                String userId = jwt.getSubject(); // Supabase user ID
                String email = jwt.getClaim("email").asString();

                // Create authentication token
                UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                        userId,
                        null,
                        Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER")));

                // Store additional user info in authentication details
                authentication.setDetails(new SupabaseUserDetails(userId, email));

                SecurityContextHolder.getContext().setAuthentication(authentication);

            } catch (JWTVerificationException e) {
                // Token is invalid - clear security context
                SecurityContextHolder.clearContext();
                // Log the error but continue with filter chain
                // The security config will handle unauthorized access
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
