package com.docsshare_web_backend.commons.services;

import com.docsshare_web_backend.documents.dto.responses.DocumentCoAuthorResponse;
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
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
public class SemanticSearchService {

    @Value("${ml.api.url}")
    private String apiUrl;

    private final RestTemplate restTemplate = new RestTemplate();

    // ===================== DTOs =====================

    @Data
    @Schema(description = "Tài liệu trong kết quả tìm kiếm")
    public static class DocumentResult {

        @JsonProperty("doc_id")
        @Schema(description = "ID của tài liệu")
        private Long docId;

        @Schema(description = "Tiêu đề tài liệu")
        private String title;

        @Schema(description = "Tóm tắt nội dung tài liệu")
        private String summary;

        @Schema(description = "Loại tệp tài liệu")
        private String fileType;

        @Schema(description = "Lượt xem tài liệu")
        private Long views;

        @Schema(description = "Giá tài liệu")
        private Double price;

        @Schema(description = "Ngày tạo tài liệu")
        private LocalDateTime createdAt;

        @Schema(description = "Tên tác giả tài liệu")
        private String authorName;

        @Schema(description = "Tên thể loại tài liệu")
        private String category;

        @Schema(description = "Đồng tác giả tài liệu")
        private List<DocumentCoAuthorResponse> coAuthors;

        @Schema(description = "Số lượng lưu tài liệu")
        private Long saveCount;

        @Schema(description = "Độ tương đồng (0–1)")
        private Double similarity;
    }

    @Data
    @Schema(description = "Kết quả của từng chủ đề/topic trong tìm kiếm thông minh")
    public static class TopicResult {

        @JsonProperty("topic_id")
        @Schema(description = "ID của chủ đề")
        private Long topicId;

        @JsonProperty("topic_name")
        @Schema(description = "Tên chủ đề")
        private String topicName;

        @JsonProperty("topic_similarity")
        @Schema(description = "Độ tương đồng của chủ đề (0–1)")
        private Double topicSimilarity;

        @Schema(description = "Danh sách tài liệu trong chủ đề này")
        private List<DocumentResult> documents;
    }

    @Data
    @Schema(description = "Phản hồi của tìm kiếm thông minh")
    public static class SemanticResponse {

        @Schema(description = "Truy vấn người dùng nhập vào")
        private String query;

        @JsonProperty("sim_threshold")
        @Schema(description = "Ngưỡng độ tương đồng được sử dụng")
        private Double simThreshold;

        @Schema(description = "Danh sách kết quả tìm kiếm theo chủ đề")
        private List<TopicResult> results;
    }

    // ===================== SERVICE LOGIC =====================

    public SemanticResponse search(String query) {
        try {
            String encoded = URLEncoder.encode(query, StandardCharsets.UTF_8);
            String url = apiUrl + "/semantic/search?query=" + encoded;
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
