package com.trivine.llc.api.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Firebase Admin SDK Configuration.
 * Initializes Firebase for server-side token verification.
 *
 * To enable Firebase auth, set: auth.provider=firebase in application.yml
 * and provide the Firebase service account JSON via FIREBASE_CREDENTIALS_JSON env variable.
 */
@Configuration
@ConditionalOnProperty(name = "auth.provider", havingValue = "firebase")
@Slf4j
public class FirebaseConfig {

    @Value("${firebase.credentials-json:#{null}}")
    private String firebaseCredentialsJson;

    @Value("${firebase.project-id:#{null}}")
    private String projectId;

    @PostConstruct
    public void initialize() {
        if (FirebaseApp.getApps().isEmpty()) {
            try {
                FirebaseOptions options;

                if (firebaseCredentialsJson != null && !firebaseCredentialsJson.isBlank()) {
                    // Use service account credentials from environment variable
                    GoogleCredentials credentials = GoogleCredentials.fromStream(
                        new ByteArrayInputStream(firebaseCredentialsJson.getBytes(StandardCharsets.UTF_8))
                    );

                    options = FirebaseOptions.builder()
                        .setCredentials(credentials)
                        .build();

                    log.info("Firebase initialized with service account credentials");
                } else if (projectId != null && !projectId.isBlank()) {
                    // Use Application Default Credentials (for GCP environments)
                    options = FirebaseOptions.builder()
                        .setCredentials(GoogleCredentials.getApplicationDefault())
                        .setProjectId(projectId)
                        .build();

                    log.info("Firebase initialized with Application Default Credentials");
                } else {
                    log.error("Firebase credentials not configured. Set FIREBASE_CREDENTIALS_JSON or firebase.project-id");
                    return;
                }

                FirebaseApp.initializeApp(options);
                log.info("Firebase Admin SDK initialized successfully");

            } catch (IOException e) {
                log.error("Failed to initialize Firebase Admin SDK: {}", e.getMessage());
                throw new RuntimeException("Failed to initialize Firebase", e);
            }
        } else {
            log.info("Firebase already initialized");
        }
    }
}
