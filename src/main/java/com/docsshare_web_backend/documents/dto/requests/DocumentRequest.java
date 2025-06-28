package com.docsshare_web_backend.documents.dto.requests;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentRequest {
    private String title;
    private String desciption;
    private String filePath;
    private Double price;
    private String copyrightPath;
    private String moderationStatus;
    private boolean isPublic;
    private String coAuthor;
    private Long userId;
    private Long categoryId;
}
