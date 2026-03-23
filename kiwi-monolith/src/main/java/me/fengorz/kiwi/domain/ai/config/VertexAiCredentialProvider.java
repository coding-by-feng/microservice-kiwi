/*
 * Copyright [2019~2025] [codingByFeng]
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package me.fengorz.kiwi.domain.ai.config;

import com.google.auth.oauth2.AccessToken;
import com.google.auth.oauth2.GoogleCredentials;
import lombok.extern.slf4j.Slf4j;
import me.fengorz.kiwi.common.exception.ServiceException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Collections;

/**
 * Provides OAuth2 access tokens for Vertex AI API calls using Google Application Default Credentials.
 *
 * @author codingByFeng
 */
@Slf4j
@Component
public class VertexAiCredentialProvider {

    private static final String VERTEX_AI_SCOPE = "https://www.googleapis.com/auth/cloud-platform";

    @Value("${spring.cloud.gcp.credentials.location:}")
    private String credentialsLocation;

    private GoogleCredentials credentials;

    @PostConstruct
    public void init() {
        try {
            if (credentialsLocation != null && !credentialsLocation.isBlank()) {
                String path = credentialsLocation.replace("file:", "");
                credentials = GoogleCredentials.fromStream(new FileInputStream(path))
                        .createScoped(Collections.singletonList(VERTEX_AI_SCOPE));
            } else {
                credentials = GoogleCredentials.getApplicationDefault()
                        .createScoped(Collections.singletonList(VERTEX_AI_SCOPE));
            }
            log.info("Vertex AI credentials initialized successfully");
        } catch (IOException e) {
            log.error("Failed to initialize Vertex AI credentials", e);
            throw new ServiceException("Failed to initialize Vertex AI credentials: " + e.getMessage(), e);
        }
    }

    /**
     * Get the underlying GoogleCredentials for use with Google Cloud client libraries.
     */
    public GoogleCredentials getCredentials() {
        return credentials;
    }

    /**
     * Get a valid access token, refreshing if necessary.
     */
    public String getAccessToken() {
        try {
            credentials.refreshIfExpired();
            AccessToken token = credentials.getAccessToken();
            if (token == null) {
                throw new ServiceException("Failed to obtain Vertex AI access token");
            }
            return token.getTokenValue();
        } catch (IOException e) {
            log.error("Failed to refresh Vertex AI access token", e);
            throw new ServiceException("Failed to refresh Vertex AI access token: " + e.getMessage(), e);
        }
    }
}
