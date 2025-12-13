package com.docsshare_web_backend.commons.services;

import com.docsshare_web_backend.documents.enums.DocumentFileType;
import com.google.api.client.http.FileContent;
import com.google.api.client.http.InputStreamContent;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;

import jakarta.xml.bind.DatatypeConverter;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collections;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class GoogleDriveService {

    @Autowired
    private Drive driveService;

    @Autowired
    private GoogleOAuthService googleOAuthService;

    public String uploadFile(MultipartFile file, String folderName) throws IOException, GeneralSecurityException  {
        Drive drive = googleOAuthService.getDriveServiceUsingRefreshToken();

        String folderId = createFolderIfNotExists(drive, folderName);
        java.io.File convertedFile = convertMultiPartToFile(file);

        File fileMetadata = new File();
        fileMetadata.setName(file.getOriginalFilename())
                   .setParents(Collections.singletonList(folderId));

        String contentType = resolveContentType(file);
        FileContent mediaContent = new FileContent(contentType, convertedFile);
        File uploadedFile = drive.files().create(fileMetadata, mediaContent)
                .setFields("id,webViewLink")
                .execute();

        convertedFile.delete();
        return uploadedFile.getWebViewLink();
    }

    private String createFolderIfNotExists(Drive drive, String folderName) throws IOException {
        String query = String.format("mimeType='application/vnd.google-apps.folder' and name='%s' and trashed=false", folderName);
        FileList result = drive.files().list()
                .setQ(query)
                .setSpaces("drive")
                .setFields("files(id, name)")
                .execute();
    
        if (!result.getFiles().isEmpty()) {
            return result.getFiles().get(0).getId(); // folder đã tồn tại
        }
    
        return createNewFolder(drive, folderName);
    }
    
    private String createNewFolder(Drive drive, String folderName) throws IOException {
        File fileMetadata = new File();
        fileMetadata.setName(folderName);
        fileMetadata.setMimeType("application/vnd.google-apps.folder");
    
        File folder = drive.files().create(fileMetadata)
                .setFields("id")
                .execute();
    
        return folder.getId();
    }
    
    
    public void deleteFile(String fileId) throws Exception  {
        try {
            Drive drive = googleOAuthService.getDriveServiceUsingRefreshToken();
            drive.files().delete(fileId).execute();
        } catch (IOException e) {
            throw new IOException("Error deleting file: " + e.getMessage(), e);
        }
    }

    public OutputStream downloadFile(String fileId) throws IOException {
        OutputStream outputStream = new ByteArrayOutputStream();
        driveService.files().get(fileId)
            .executeMediaAndDownloadTo(outputStream);
        return outputStream;
    }

    private java.io.File convertMultiPartToFile(MultipartFile file) throws IOException {
        java.io.File convertedFile = new java.io.File(System.getProperty("java.io.tmpdir") + "/" + file.getOriginalFilename());
        FileOutputStream fos = new FileOutputStream(convertedFile);
        fos.write(file.getBytes());
        fos.close();
        return convertedFile;
    }

    public String calculateSHA256Hash(MultipartFile file) throws IOException, NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest(file.getBytes());
        return DatatypeConverter.printHexBinary(hash).toLowerCase();
    }

    public String extractFileIdFromUrl(String filePath) {
        Pattern pattern = Pattern.compile("https://drive\\.google\\.com/file/d/([a-zA-Z0-9_-]+)");
        Matcher matcher = pattern.matcher(filePath);
        if (matcher.find()) {
            return matcher.group(1);
        } else {
            throw new IllegalArgumentException("Invalid Google Drive file path: " + filePath);
        }
    }

    private String resolveContentType(MultipartFile file) {
        String contentType = file.getContentType();
        if (contentType != null) {
            return contentType;
        }
    
        String filename = file.getOriginalFilename();
        if (filename == null) return "application/octet-stream";
    
        String ext = filename.substring(filename.lastIndexOf(".") + 1).toLowerCase();
    
        switch (ext) {
            case "txt": return "text/plain";
            case "csv": return "text/csv";
            case "pdf": return "application/pdf";
            case "doc": return "application/msword";
            case "docx": return "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
            case "xls": return "application/vnd.ms-excel";
            case "xlsx": return "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
            case "ppt": return "application/vnd.ms-powerpoint";
            case "pptx": return "application/vnd.openxmlformats-officedocument.presentationml.presentation";
            default: return "application/octet-stream";
        }
    }

    public void downloadFile(String fileId, OutputStream outputStream) throws IOException {
        driveService.files()
            .get(fileId)
            .executeMediaAndDownloadTo(outputStream);
    }
    
    public java.io.File downloadToTempFile(String driveUrlOrId, DocumentFileType fileType) {
        try {
            java.io.File tempFile = java.io.File.createTempFile(
                "doc_" + System.currentTimeMillis(),
                "." + fileType.name().toLowerCase()
            );
    
            // Nếu input là URL view, extract fileId
            String fileId = extractFileIdFromUrl(driveUrlOrId);
    
            try (OutputStream os = new FileOutputStream(tempFile)) {
                downloadFile(fileId, os);
            }
    
            return tempFile;
    
        } catch (Exception e) {
            throw new RuntimeException("Failed to download file from Google Drive", e);
        }
    }

}
