package com.docsshare_web_backend.commons.configs;

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.drive.Drive;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Collections;

@Configuration
public class GoogleDriveConfig {
    private static final String SERVICE_ACCOUNT_KEY_PATH = "feisty-pottery-443410-i5-d1576707f921.json";
    private static final String APPLICATION_NAME = "DocsShareUpload";

    @Bean
    public Drive getDriveService() throws GeneralSecurityException, IOException {
        final NetHttpTransport httpTransport = GoogleNetHttpTransport.newTrustedTransport();
        GoogleCredential credential = GoogleCredential
                .fromStream(new ClassPathResource(SERVICE_ACCOUNT_KEY_PATH).getInputStream())
                .createScoped(Collections.singleton("https://www.googleapis.com/auth/drive"));

        return new Drive.Builder(
                httpTransport,
                JacksonFactory.getDefaultInstance(),
                credential)
                .setApplicationName(APPLICATION_NAME)
                .build();
    }
}
