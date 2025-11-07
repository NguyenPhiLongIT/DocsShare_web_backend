package com.docsshare_web_backend.commons.services;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Slf4j
@Service
public class SemanticSearchService {

    @Value("${ml.api.url}")
    private String apiUrl;  // ví dụ: http://127.0.0.1:5000

    private final RestTemplate restTemplate = new RestTemplate();

    // ===================== DTOs =====================

    @Data
    @Schema(description = "Kết quả của từng danh mục trong tìm kiếm thông minh")
    public static class SemanticResult {

        @JsonProperty("category_id")
        @Schema(description = "ID của danh mục")
        private Long categoryId;

        @JsonProperty("category_name")
        @Schema(description = "Tên danh mục")
        private String categoryName;

        @Schema(description = "Tóm tắt nội dung danh mục")
        private String summary;

        @Schema(description = "Độ tương đồng (0–1)")
        private Double similarity;
    }

    @Data
    @Schema(description = "Phản hồi của tìm kiếm thông minh")
    public static class SemanticResponse {

        @Schema(description = "Truy vấn người dùng nhập vào")
        private String query;

        @Schema(description = "Danh sách kết quả tìm kiếm thông minh")
        private SemanticResult[] results;
    }

    // ===================== SERVICE LOGIC =====================

    public SemanticResponse search(String query) {
        try {
            String encoded = URLEncoder.encode(query, StandardCharsets.UTF_8);
            String url = apiUrl + "/semantic/search?query=" + encoded + "&top_k=5";
            log.info("🔍 Gọi Flask Semantic API: {}", url);

            ResponseEntity<SemanticResponse> response =
                    restTemplate.getForEntity(url, SemanticResponse.class);

            return response.getBody();
        } catch (Exception e) {
            log.error("❌ Lỗi khi gọi Flask semantic search", e);
            throw new RuntimeException("Semantic search service unavailable", e);
        }
    }
}
