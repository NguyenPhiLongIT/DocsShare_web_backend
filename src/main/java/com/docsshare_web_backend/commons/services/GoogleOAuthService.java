package com.docsshare_web_backend.commons.services;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleTokenResponse;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.BasicAuthentication;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.auth.oauth2.BearerToken;
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;

@Service
public class GoogleOAuthService {
    @Value("${google.oauth.client-id}")
    private String clientId;

    @Value("${google.oauth.client-secret}")
    private String clientSecret;

    @Value("${google.oauth.redirect-uri}")
    private String redirectUri;

    @Value("${google.oauth.refresh-token}")
    private String refreshToken;

    private static final List<String> SCOPES = List.of(DriveScopes.DRIVE_FILE);

    public String getAuthorizationUrl() throws Exception {
        GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
                GoogleNetHttpTransport.newTrustedTransport(),
                JacksonFactory.getDefaultInstance(),
                clientId,
                clientSecret,
                SCOPES)
            .setAccessType("offline")
            .build();

        return flow.newAuthorizationUrl()
                .setRedirectUri(redirectUri)
                .build();
    }

    public Credential getCredentialFromCode(String code) throws Exception {
        GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
                GoogleNetHttpTransport.newTrustedTransport(),
                JacksonFactory.getDefaultInstance(),
                clientId,
                clientSecret,
                SCOPES)
            .setAccessType("offline")
            .build();

        GoogleTokenResponse response = flow.newTokenRequest(code)
                .setRedirectUri(redirectUri)
                .execute();

        return flow.createAndStoreCredential(response, "user");
    }

    public Drive getDriveService(Credential credential) throws Exception {
        return new Drive.Builder(
                GoogleNetHttpTransport.newTrustedTransport(),
                JacksonFactory.getDefaultInstance(),
                credential)
            .setApplicationName("DocsShareOAuth")
            .build();
    }

    public Credential getServiceAccountCredential() throws IOException, GeneralSecurityException {
        return new Credential.Builder(BearerToken.authorizationHeaderAccessMethod())
                .setTransport(GoogleNetHttpTransport.newTrustedTransport())
                .setJsonFactory(JacksonFactory.getDefaultInstance())
                .setTokenServerEncodedUrl("https://oauth2.googleapis.com/token")
                .setClientAuthentication(new BasicAuthentication(clientId, clientSecret))
                .build()
                .setRefreshToken(refreshToken)
                .setAccessToken(null); // sẽ được tự refresh khi dùng
    }
    
    public Drive getDriveServiceUsingRefreshToken() throws IOException, GeneralSecurityException {
        Credential credential = getServiceAccountCredential();
        credential.refreshToken(); // bắt buộc gọi để lấy access token ban đầu
    
        return new Drive.Builder(
                GoogleNetHttpTransport.newTrustedTransport(),
                JacksonFactory.getDefaultInstance(),
                credential)
                .setApplicationName("DocsShareOAuth")
                .build();
    }
    
}
