package com.trivine.llc.api.config;


import com.trivine.llc.api.service.utility.RequestContext;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.context.annotation.RequestScope;

@Configuration
public class RequestContextConfig {

    @Bean
    @RequestScope
    public RequestContext requestContext(HttpServletRequest request) {
        String id = request.getHeader("x-login-user-id");
        String role = request.getHeader("x-user-role");
        return new RequestContext(id != null ? Long.parseLong(id) : null, role);
    }
}

