package com.trivine.llc.api.config;


import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderClient;

@Configuration
public class CognitoConfig {

    @Bean
    public CognitoIdentityProviderClient buildCognitoClient(AwsConfiguration awsConfig) {
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

}
