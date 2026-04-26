package com.ai.application.Config;

import com.ai.application.Repositories.UserRepository;
import com.ai.application.Services.TokenService;
import com.ai.application.model.Entity.User;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.Optional;

/**
 * JWT authentication filter that replaces SupabaseJwtFilter.
 * Validates our own JWT access tokens and sets the SecurityContext.
 */
@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(JwtAuthFilter.class);

    private final TokenService tokenService;
    private final UserRepository userRepository;

    public JwtAuthFilter(TokenService tokenService, UserRepository userRepository) {
        this.tokenService = tokenService;
        this.userRepository = userRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);

            try {
                String userId = tokenService.extractUserId(token);

                if (userId != null) {
                    Optional<User> userOpt = userRepository.findById(userId);

                    if (userOpt.isPresent()) {
                        User user = userOpt.get();

                        if (!user.isAccountDisabled()) {
                            String role = user.getRole() != null ? user.getRole() : "USER";

                            UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                                    userId,
                                    null,
                                    Collections.singletonList(
                                            new SimpleGrantedAuthority("ROLE_" + role)));

                            // Store user details for downstream access
                            authentication.setDetails(new AuthUserDetails(
                                    userId, user.getEmail(), user.getName(), user.getRole()));

                            SecurityContextHolder.getContext().setAuthentication(authentication);
                            logger.debug("JWT verified for user: {}", userId);
                        }
                    }
                }
            } catch (Exception e) {
                SecurityContextHolder.clearContext();
                logger.debug("JWT verification failed: {}", e.getMessage());
            }
        }

        filterChain.doFilter(request, response);
    }

    /**
     * User details extracted from JWT and stored in authentication.
     */
    public static class AuthUserDetails {
        private final String userId;
        private final String email;
        private final String name;
        private final String role;

        public AuthUserDetails(String userId, String email, String name, String role) {
            this.userId = userId;
            this.email = email;
            this.name = name;
            this.role = role;
        }

        public String getUserId() {
            return userId;
        }

        public String getEmail() {
            return email;
        }

        public String getName() {
            return name;
        }

        public String getRole() {
            return role;
        }
    }
}
