package com.docsshare_web_backend.commons.services;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Service
@Slf4j
public class SemanticEmbeddingService {

    private final RestTemplate restTemplate;
    private final String flaskBaseUrl;

    public SemanticEmbeddingService(
            RestTemplate restTemplate,
            @Value("${semantic.flask.base-url:http://localhost:5000}") String flaskBaseUrl
    ) {
        this.restTemplate = restTemplate;

        if (flaskBaseUrl.endsWith("/")) {
            this.flaskBaseUrl = flaskBaseUrl.substring(0, flaskBaseUrl.length() - 1);
        } else {
            this.flaskBaseUrl = flaskBaseUrl;
        }
        log.info("[SemanticEmbeddingService] Using Flask base URL: {}", this.flaskBaseUrl);
    }

    /**
     * Gửi thông tin document sang Flask để embed.
     *
     * @return true nếu gọi Flask thành công (2xx), false nếu lỗi
     */
    public boolean embedDocument(Long docId,
                                 String title,
                                 String summary,
                                 String description,
                                 Long categoryId) {

        if (docId == null) {
            log.warn("[SemanticEmbeddingService] docId is null, skip embedding");
            return false;
        }

        String url = flaskBaseUrl + "/semantic/embed-doc-internal";

        Map<String, Object> payload = new HashMap<>();
        payload.put("docId", docId);
        payload.put("title", title);
        payload.put("summary", summary);
        payload.put("description", description);
        payload.put("categoryId", categoryId);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(payload, headers);

        try {
            log.info("[SemanticEmbeddingService] Trigger embedding for document {} -> {}", docId, url);
            ResponseEntity<String> response = restTemplate.postForEntity(url, requestEntity, String.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("[SemanticEmbeddingService] Embedded document {} successfully. Response: {}",
                        docId, response.getBody());
                return true;
            } else {
                log.warn("[SemanticEmbeddingService] Non-2xx response for doc {}: {} - {}",
                        docId, response.getStatusCodeValue(), response.getBody());
                return false;
            }
        } catch (RestClientException e) {
            // Không ném RuntimeException để tránh làm fail toàn bộ luồng tạo doc
            log.error("[SemanticEmbeddingService] Error calling Flask for doc {}: {}", docId, e.getMessage(), e);
            return false;
        } catch (Exception e) {
            log.error("[SemanticEmbeddingService] Unexpected error embedding doc {}: {}", docId, e.getMessage(), e);
            return false;
        }
    }
}
