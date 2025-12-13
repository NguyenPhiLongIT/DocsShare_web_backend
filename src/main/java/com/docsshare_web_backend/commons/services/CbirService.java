package com.docsshare_web_backend.commons.services;

import java.io.File;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.util.MultiValueMap;

import com.docsshare_web_backend.commons.utils.InMemoryMultipartFile;
import com.docsshare_web_backend.commons.utils.MultipartInputStreamFileResource;
import com.fasterxml.jackson.databind.JsonNode;

@Service
public class CbirService {
    @Value("${ml.api.url}")
    private String apiUrl;

    @Autowired
    private GoogleDriveService googleDriveService;

    private final RestTemplate restTemplate = new RestTemplate();

    public List<ImageFeatureResult> extractImagesAndFeatures(MultipartFile file) {
        String url = apiUrl + "/extract-images";
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);

            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("file", new MultipartInputStreamFileResource(file.getInputStream(), file.getOriginalFilename()));

            HttpEntity<MultiValueMap<String, Object>> request = new HttpEntity<>(body, headers);

            ResponseEntity<Map> response = restTemplate.postForEntity(url, request, Map.class);

            if (response.getBody() != null && response.getBody().containsKey("images")) {
                List<Map<String, Object>> images = (List<Map<String, Object>>) response.getBody().get("images");
                List<ImageFeatureResult> results = new ArrayList<>();

                for (Map<String, Object> img : images) {
                    String filename = (String) img.get("filename");
                    String base64Data = (String) img.get("image_base64");
                    byte[] imageBytes = Base64.getDecoder().decode(base64Data);

                    MultipartFile imageFile = new InMemoryMultipartFile(
                            filename,
                            filename,
                            "image/jpeg",
                            imageBytes
                    );

                    String urlUploaded = googleDriveService.uploadFile(imageFile, "DocsShareImages");
                    List<Double> features = (List<Double>) img.get("features");

                    results.add(new ImageFeatureResult(filename, urlUploaded, features));
                }

                return results;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return List.of();
    }

    public JsonNode searchImage(MultipartFile file) {
        try {
            String url = apiUrl + "/search-image";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);

            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("file", new MultipartInputStreamFileResource(file.getInputStream(), file.getOriginalFilename()));

            HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);
            ResponseEntity<JsonNode> response = restTemplate.postForEntity(url, requestEntity, JsonNode.class);

            return response.getBody();
        } catch (Exception e) {
            throw new RuntimeException("Error calling Flask API: " + e.getMessage(), e);
        }
    }

    public void pushFeatureToFlask(Long id, Long documentId, String imagePath, List<Double> features) {
        String url = apiUrl + "/add-features";
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, Object> item = new HashMap<>();
            item.put("id", id);
            item.put("imagePath", imagePath);
            item.put("documentId", documentId);
            item.put("featureVector", features);

            Map<String, Object> body = new HashMap<>();
            body.put("items", List.of(item));

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
            restTemplate.postForEntity(url, request, Void.class);
        } catch (Exception e) {
            System.err.println("Failed to push feature to Flask: " + e.getMessage());
        }
    }

    public List<ImageFeatureResult> extractImagesAndFeatures(File file) {
        String url = apiUrl + "/extract-images";

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);

            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("file", new FileSystemResource(file));

            HttpEntity<MultiValueMap<String, Object>> request =
                    new HttpEntity<>(body, headers);

            ResponseEntity<Map> response =
                    restTemplate.postForEntity(url, request, Map.class);

            if (response.getBody() == null || !response.getBody().containsKey("images")) {
                return List.of();
            }

            List<Map<String, Object>> images =
                    (List<Map<String, Object>>) response.getBody().get("images");

            List<ImageFeatureResult> results = new ArrayList<>();

            for (Map<String, Object> img : images) {
                String filename = (String) img.get("filename");
                String base64Data = (String) img.get("image_base64");
                byte[] imageBytes = Base64.getDecoder().decode(base64Data);

                MultipartFile imageFile = new InMemoryMultipartFile(
                        filename,
                        filename,
                        "image/jpeg",
                        imageBytes
                );

                String uploadedUrl =
                        googleDriveService.uploadFile(imageFile, "DocsShareImages");

                List<Double> features = (List<Double>) img.get("features");

                results.add(new ImageFeatureResult(filename, uploadedUrl, features));
            }

            return results;

        } catch (Exception e) {
            e.printStackTrace();
            return List.of();
        } finally {
            try {
                file.delete(); // cleanup file temp
            } catch (Exception ignored) {}
        }
    }

    public static class ImageFeatureResult {
        private String filename;
        private String url;
        private List<Double> features;

        public ImageFeatureResult(String filename, String url, List<Double> features) {
            this.filename = filename;
            this.url = url;
            this.features = features;
        }

        public String getFilename() { return filename; }
        public void setFilename(String filename) { this.filename = filename; }

        public String getUrl() { return url; }
        public void setUrl(String url) { this.url = url; }

        public List<Double> getFeatures() { return features; }
        public void setFeatures(List<Double> features) { this.features = features; }
    }
    
}
