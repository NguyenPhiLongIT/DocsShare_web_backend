package com.docsshare_web_backend.documents.dto.responses;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentResponse {
    private Long id;
    private String title;
    private String description;
    private String typeFile;
    private String filePath;
    private Double price;
    private String moderationStatus;
    private boolean isPublic;
    private String coAuthor;
    private LocalDateTime createdDate;
}
