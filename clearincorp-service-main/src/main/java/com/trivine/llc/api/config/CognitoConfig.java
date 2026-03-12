package com.trivine.llc.api.config;


import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderClient;

@Configuration
@Slf4j
public class CognitoConfig {

    @Bean
    @ConditionalOnProperty(name = "aws.cognito.enabled", havingValue = "true", matchIfMissing = false)
    public CognitoIdentityProviderClient buildCognitoClient(AwsConfiguration awsConfig) {
        log.info("Initializing AWS Cognito client");
        return CognitoIdentityProviderClient.builder()
                .region(Region.of(awsConfig.getCognito().getRegion()))
                .credentialsProvider(
                        StaticCredentialsProvider.create(
                                AwsBasicCredentials.create(
                                        awsConfig.getCognito().getAccessKeyId(),
                                        awsConfig.getCognito().getSecretAccessKey()
                                )
                        )
                )
                .build();
    }

    @Bean
    @ConditionalOnProperty(name = "aws.cognito.enabled", havingValue = "false", matchIfMissing = true)
    public CognitoIdentityProviderClient buildMockCognitoClient() {
        log.warn("AWS Cognito is disabled. Using mock client - authentication will not work!");
        return null;
    }

}
