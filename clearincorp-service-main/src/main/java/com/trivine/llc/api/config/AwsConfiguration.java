package com.trivine.llc.api.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "aws")
@Data
public class AwsConfiguration {
    private String accessKeyId;
    private String secretAccessKey;
    private String region;

    private CognitoConfig cognito = new CognitoConfig();

    @Data
    public class CognitoConfig {
        private String accessKeyId;
        private String secretAccessKey;
        private String region;

        private String userPoolId;
        private String clientId;
        private String clientSecret;
        private String hmacAlgorithm;
        private String defaultPassword;

        public String getAccessKeyId() {
            return accessKeyId != null ? accessKeyId : AwsConfiguration.this.accessKeyId;
        }

        public String getSecretAccessKey() {
            return secretAccessKey != null ? secretAccessKey : AwsConfiguration.this.secretAccessKey;
        }

        public String getRegion() {
            return region != null ? region : AwsConfiguration.this.region;
        }
    }
}