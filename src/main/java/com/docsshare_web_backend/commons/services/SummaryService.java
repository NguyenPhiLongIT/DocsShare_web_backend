package com.docsshare_web_backend.commons.services;

import com.docsshare_web_backend.commons.utils.MultipartInputStreamFileResource;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.util.MultiValueMap;

@Service
public class SummaryService {
    @Value("${ml.api.url}")
    private String apiUrl;

    private final RestTemplate restTemplate = new RestTemplate();

    public String summarizeFile(MultipartFile file) {
        String url = apiUrl + "/summarize";
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);
    
            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("file", new MultipartInputStreamFileResource(file.getInputStream(), file.getOriginalFilename()));
    
            HttpEntity<MultiValueMap<String, Object>> request = new HttpEntity<>(body, headers);
    
            ResponseEntity<Map> response = restTemplate.postForEntity(url, request, Map.class);
            return response.getBody().get("description").toString();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public String extractText(MultipartFile file) {
        String url = apiUrl + "/extract-text";
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);
    
            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("file", new MultipartInputStreamFileResource(file.getInputStream(), file.getOriginalFilename()));
    
            HttpEntity<MultiValueMap<String, Object>> request = new HttpEntity<>(body, headers);
    
            ResponseEntity<Map> response = restTemplate.postForEntity(url, request, Map.class);
            return response.getBody().get("text").toString();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}
