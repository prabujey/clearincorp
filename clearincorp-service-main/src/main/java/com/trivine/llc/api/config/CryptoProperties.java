package com.trivine.llc.api.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "crypto.local")
public class CryptoProperties {
    /**
     * Base64-encoded 256-bit (32-byte) AES master key.
     * Generate one with: openssl rand -base64 32
     */
    private String masterKeyB64;
}
