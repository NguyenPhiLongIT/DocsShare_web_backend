package com.docsshare_web_backend.commons.domain;

import com.docsshare_web_backend.commons.services.GoogleDriveService;

import lombok.extern.slf4j.Slf4j;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.MediaType;
import org.springframework.http.HttpHeaders;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@RestController
@RequestMapping("/api/file")
public class FileController {
    @Autowired
    private GoogleDriveService googleDriveService;

    @PostMapping(value="/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> uploadFile(@RequestParam("file") MultipartFile file,
            @RequestParam(value = "folderName", defaultValue = "DocsShareStorage") String folderName) {
        try {
            String fileUrl = googleDriveService.uploadFile(file, folderName);
            return ResponseEntity.ok().body(Map.of("url", fileUrl));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Upload failed: " + e.getMessage()));
        }
    }

    @DeleteMapping("/delete")
    public ResponseEntity<?> deleteFile(@RequestParam String filePath) {
        try {
            String fileId = googleDriveService.extractFileIdFromUrl(filePath);
            googleDriveService.deleteFile(fileId);
            return ResponseEntity.ok().body(Map.of("message", "File deleted successfully"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Delete failed: " + e.getMessage()));
        }
    }


    @GetMapping("/download")
    public ResponseEntity<?> downloadFile(@RequestParam String filePath) {
        try {
            String fileId = googleDriveService.extractFileIdFromUrl(filePath);
            OutputStream outputStream = googleDriveService.downloadFile(fileId);
            byte[] fileBytes = ((ByteArrayOutputStream) outputStream).toByteArray();

            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileId + "\"")
                    .body(fileBytes);
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Download failed: " + e.getMessage()));
        }
    }
}
