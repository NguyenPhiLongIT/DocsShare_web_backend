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
public class DocumentResponse {
    private Long id;
    private String title;
    private String description;
    private String filePath;
    private String slug;
    private Double price;
    private String copyrightPath;
    private String moderationStatus;
    private boolean isPublic;
    private LocalDateTime createdAt;
    private String authorName;
    private String category;
    private List<DocumentCoAuthorResponse> coAuthors;
}
