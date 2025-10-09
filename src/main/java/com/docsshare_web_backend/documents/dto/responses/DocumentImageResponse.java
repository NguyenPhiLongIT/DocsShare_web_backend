package com.docsshare_web_backend.documents.dto.responses;

import java.time.LocalDateTime;
import java.util.List;

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
public class DocumentImageResponse {
    private Long id;
    private String imagePath;
    private LocalDateTime uploadedAt;
    private Long documentId;
    private List<Double> featureVector;
}
