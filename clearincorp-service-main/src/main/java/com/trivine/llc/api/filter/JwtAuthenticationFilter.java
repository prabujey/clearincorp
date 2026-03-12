package com.trivine.llc.api.filter;

import com.trivine.llc.api.config.AwsConfiguration;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderClient;
import software.amazon.awssdk.services.cognitoidentityprovider.model.GetUserRequest;
import software.amazon.awssdk.services.cognitoidentityprovider.model.GetUserResponse;
import software.amazon.awssdk.services.cognitoidentityprovider.model.NotAuthorizedException;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * JWT Authentication Filter that validates Cognito access tokens.
 * Extracts token from Authorization header or cookies.
 */
@Component
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final CognitoIdentityProviderClient cognitoClient;

    public JwtAuthenticationFilter(@org.springframework.lang.Nullable CognitoIdentityProviderClient cognitoClient) {
        this.cognitoClient = cognitoClient;
        if (cognitoClient == null) {
            log.warn("Cognito client is null - JWT authentication is disabled");
        }
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        // Skip authentication if Cognito is not configured
        if (cognitoClient == null) {
            filterChain.doFilter(request, response);
            return;
        }

        String accessToken = extractToken(request);

        if (accessToken != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            try {
                GetUserResponse userResponse = cognitoClient.getUser(
                        GetUserRequest.builder()
                                .accessToken(accessToken)
                                .build()
                );

                String username = userResponse.username();
                List<SimpleGrantedAuthority> authorities = userResponse.userAttributes().stream()
                        .filter(attr -> "cognito:groups".equals(attr.name()))
                        .findFirst()
                        .map(attr -> Arrays.stream(attr.value().split(","))
                                .map(role -> new SimpleGrantedAuthority("ROLE_" + role.trim().toUpperCase()))
                                .toList())
                        .orElse(Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER")));

                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(username, null, authorities);
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                SecurityContextHolder.getContext().setAuthentication(authentication);
                log.debug("Authenticated user: {}", username);

            } catch (NotAuthorizedException e) {
                log.debug("Invalid or expired token");
                // Don't set authentication - let security config handle it
            } catch (Exception e) {
                log.warn("Error validating token: {}", e.getMessage());
            }
        }

        filterChain.doFilter(request, response);
    }

    private String extractToken(HttpServletRequest request) {
        // First try Authorization header
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }

        // Fall back to cookie
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if ("accessToken".equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }

        return null;
    }
}
