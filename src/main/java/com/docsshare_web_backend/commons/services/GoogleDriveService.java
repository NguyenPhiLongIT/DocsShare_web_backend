package com.docsshare_web_backend.commons.services;

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
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collections;

@Service
public class GoogleDriveService {

    @Autowired
    private Drive driveService;

    public String uploadFile(MultipartFile file, String folderName) throws IOException {
        String folderId = createFolderIfNotExists(folderName);
        java.io.File convertedFile = convertMultiPartToFile(file);
        
        File fileMetadata = new File();
        fileMetadata.setName(file.getOriginalFilename())
                   .setParents(Collections.singletonList(folderId));

        FileContent mediaContent = new FileContent(file.getContentType(), convertedFile);
        File uploadedFile = driveService.files().create(fileMetadata, mediaContent)
                .setFields("id,webViewLink")
                .execute();
        convertedFile.delete();
        return uploadedFile.getWebViewLink();
    }

    private String createFolderIfNotExists(String folderName) throws IOException {
        // Check if folder exists
        String query = String.format("mimeType='application/vnd.google-apps.folder' and name='%s' and trashed=false", folderName);
        var result = driveService.files().list()
            .setQ(query)
            .setSpaces("drive")
            .setFields("files(id, name)")
            .execute();

        // Return existing folder id if found
        if (!result.getFiles().isEmpty()) {
            return result.getFiles().get(0).getId();
        }

        return createNewFolder(folderName);
    }

    private String createNewFolder(String folderName) throws IOException {
        File fileMetadata = new File();
        fileMetadata.setName(folderName);
        fileMetadata.setMimeType("application/vnd.google-apps.folder");

        File folder = driveService.files().create(fileMetadata)
            .setFields("id")
            .execute();

        return folder.getId();
    }

    public void deleteFile(String fileId) throws IOException {
        try {
            driveService.files().delete(fileId).execute();
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

}
