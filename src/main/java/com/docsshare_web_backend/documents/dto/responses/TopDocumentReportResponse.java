package com.docsshare_web_backend.documents.dto.responses;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TopDocumentReportResponse {
    private Long id;
    private String title;
    private String description;
    private String fileType;
    private String slug;
    private Double price;
    private LocalDateTime createdAt;
    private String authorName;
    private String category;
    private Long viewCount;
    private Long saveCount;
    private Long relatedPostCount;
    private Long relatedCommentCount;
    private Long totalInteraction;
}