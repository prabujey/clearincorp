package com.trivine.llc.api.config;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

import java.net.URI;

/**
 * Cloudflare R2 Storage Configuration.
 * R2 is S3-compatible, so we use the AWS SDK with a custom endpoint.
 *
 * To enable R2 instead of S3, set: storage.provider=r2 in application.yml
 *
 * Key advantages of R2:
 * - Zero egress fees (no charges for data transfer out)
 * - S3-compatible API (minimal code changes)
 * - Global edge network
 *
 * Configuration in application.yml:
 * r2:
 *   account-id: ${R2_ACCOUNT_ID}
 *   access-key: ${R2_ACCESS_KEY}
 *   secret-key: ${R2_SECRET_KEY}
 *   bucket-name: your-bucket-name
 */
@Configuration
@ConditionalOnProperty(name = "storage.provider", havingValue = "r2")
@Slf4j
public class R2StorageConfig {

    @Bean
    @ConfigurationProperties(prefix = "r2")
    public R2Properties r2Properties() {
        return new R2Properties();
    }

    @Bean
    public S3Client r2Client(R2Properties props) {
        String endpoint = String.format("https://%s.r2.cloudflarestorage.com", props.getAccountId());

        log.info("Initializing Cloudflare R2 client with endpoint: {}", endpoint);

        return S3Client.builder()
            .endpointOverride(URI.create(endpoint))
            .credentialsProvider(StaticCredentialsProvider.create(
                AwsBasicCredentials.create(props.getAccessKey(), props.getSecretKey())
            ))
            .region(Region.of("auto"))
            .forcePathStyle(true) // Required for R2 compatibility
            .build();
    }

    @Bean
    public S3Presigner r2Presigner(R2Properties props) {
        String endpoint = String.format("https://%s.r2.cloudflarestorage.com", props.getAccountId());

        log.info("Initializing Cloudflare R2 presigner");

        return S3Presigner.builder()
            .endpointOverride(URI.create(endpoint))
            .credentialsProvider(StaticCredentialsProvider.create(
                AwsBasicCredentials.create(props.getAccessKey(), props.getSecretKey())
            ))
            .region(Region.of("auto"))
            .build();
    }

    @Data
    public static class R2Properties {
        private String accountId;
        private String accessKey;
        private String secretKey;
        private String bucketName;
        private String marketplaceBucket;

        // Optional: Public bucket URL for direct access (if configured in R2)
        private String publicUrl;
    }
}
