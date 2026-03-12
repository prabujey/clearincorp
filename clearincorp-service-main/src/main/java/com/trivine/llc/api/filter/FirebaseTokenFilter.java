package com.trivine.llc.api.filter;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseToken;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Firebase Authentication Filter.
 * Validates Firebase ID tokens and sets up Spring Security context.
 *
 * This filter replaces JwtAuthenticationFilter when using Firebase Auth
 * instead of AWS Cognito.
 *
 * To enable: Set auth.provider=firebase in application.yml
 */
@Component
@ConditionalOnProperty(name = "auth.provider", havingValue = "firebase")
@Slf4j
public class FirebaseTokenFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String idToken = extractToken(request);

        if (idToken != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            try {
                // Verify the Firebase ID token
                FirebaseToken decodedToken = FirebaseAuth.getInstance().verifyIdToken(idToken);

                String uid = decodedToken.getUid();
                String email = decodedToken.getEmail();

                // Extract custom claims for roles (set via Firebase Admin SDK)
                Map<String, Object> claims = decodedToken.getClaims();
                String role = extractRole(claims);

                List<SimpleGrantedAuthority> authorities =
                    Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + role.toUpperCase()));

                // Create authentication token
                UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(email, null, authorities);

                // Store additional details (uid, email, claims)
                Map<String, Object> details = new HashMap<>();
                details.put("uid", uid);
                details.put("email", email);
                details.put("claims", claims);
                authentication.setDetails(details);

                SecurityContextHolder.getContext().setAuthentication(authentication);
                log.debug("Authenticated Firebase user: {} with role: {}", email, role);

            } catch (FirebaseAuthException e) {
                log.debug("Invalid or expired Firebase token: {}", e.getMessage());
                // Don't set authentication - let security config handle unauthenticated requests
            } catch (Exception e) {
                log.warn("Error validating Firebase token: {}", e.getMessage());
            }
        }

        filterChain.doFilter(request, response);
    }

    /**
     * Extract role from Firebase custom claims.
     * Custom claims should be set via Firebase Admin SDK when creating/updating users.
     */
    private String extractRole(Map<String, Object> claims) {
        // Check for 'role' claim (singular)
        if (claims.containsKey("role")) {
            return String.valueOf(claims.get("role"));
        }

        // Check for 'roles' claim (array) - take first role
        if (claims.containsKey("roles")) {
            Object roles = claims.get("roles");
            if (roles instanceof List<?> roleList && !roleList.isEmpty()) {
                return String.valueOf(roleList.get(0));
            }
        }

        // Check for admin flag
        if (Boolean.TRUE.equals(claims.get("admin"))) {
            return "ADMIN";
        }

        // Default role
        return "USER";
    }

    /**
     * Extract token from Authorization header or cookies.
     */
    private String extractToken(HttpServletRequest request) {
        // First try Authorization header
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }

        // Fall back to cookie (for SSR/browser-based auth)
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if ("idToken".equals(cookie.getName()) || "accessToken".equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }

        return null;
    }
}
