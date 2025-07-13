package com.docsshare_web_backend.commons.domain;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;
import com.docsshare_web_backend.commons.services.GoogleOAuthService;
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.http.FileContent;

@RestController
@RequestMapping("/drive")
public class GoogleDriveController {
    @Autowired
    private GoogleOAuthService googleOAuthService;

    private Credential userCredential; // Chỉ demo, nên dùng session/token thực tế

    @GetMapping("/login")
    public ResponseEntity<?> login() throws Exception {
        String authUrl = googleOAuthService.getAuthorizationUrl();
        return ResponseEntity.status(302).header("Location", authUrl).build();
    }

    @GetMapping("/oauth2/callback")
    public String callback(@RequestParam("code") String code) throws Exception {
        System.out.println("OAuth Code: " + code);
        this.userCredential = googleOAuthService.getCredentialFromCode(code);
        return "Login thành công. Giờ bạn có thể upload! " + code;
    }

    @PostMapping("/upload")
    public String upload(@RequestParam("file") MultipartFile file) throws Exception {
        if (userCredential == null) {
            return "Bạn chưa đăng nhập.";
        }

        Drive drive = googleOAuthService.getDriveService(userCredential);

        File fileMetadata = new File();
        fileMetadata.setName(file.getOriginalFilename());

        java.io.File tempFile = java.io.File.createTempFile("upload-", file.getOriginalFilename());
        file.transferTo(tempFile);

        FileContent mediaContent = new FileContent(file.getContentType(), tempFile);
        File uploaded = drive.files().create(fileMetadata, mediaContent)
                .setFields("id, webViewLink")
                .execute();

        tempFile.delete();
        return "Upload thành công: " + uploaded.getWebViewLink();
    }
}
