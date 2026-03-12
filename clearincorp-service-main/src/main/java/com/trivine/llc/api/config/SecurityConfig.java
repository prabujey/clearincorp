package com.trivine.llc.api.config;

import com.trivine.llc.api.filter.JwtAuthenticationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }

    /**
     * Public endpoints that don't require authentication.
     * These are typically login, health checks, and public API endpoints.
     */
    private static final String[] PUBLIC_ENDPOINTS = {
            // Health and actuator
            "/health/**",
            "/actuator/**",

            // Authentication endpoints
            "/email/generate-otp",
            "/email/validate-otp",
            "/email/refresh",
            "/email/logout",

            // Public company data
            "/company/states",
            "/company/suffix",

            // Swagger/OpenAPI
            "/swagger-ui/**",
            "/swagger-ui.html",
            "/v3/api-docs/**",
            "/swagger-resources/**",
            "/webjars/**",

            // WebSocket
            "/ws/**",
            "/websocket/**",

            // Public business directory/marketplace
            "/business/directory/**",
            "/business/search/**",
            "/business/category/**",

            // Chatbot (public facing)
            "/chatbot/**",

            // Entity check (public)
            "/entity-check/**"
    };

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                // Disable CSRF for stateless API (using JWT)
                .csrf(csrf -> csrf.disable())

                // Stateless session management
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // Configure authorization rules
                .authorizeHttpRequests(auth -> auth
                        // Public endpoints - no auth required
                        .requestMatchers(PUBLIC_ENDPOINTS).permitAll()

                        // OPTIONS requests for CORS preflight
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                        // All other requests require authentication
                        .anyRequest().authenticated()
                )

                // Add JWT filter before UsernamePasswordAuthenticationFilter
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)

                .build();
    }
}
