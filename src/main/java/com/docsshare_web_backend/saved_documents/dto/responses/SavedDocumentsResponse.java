package com.docsshare_web_backend.saved_documents.dto.responses;

import java.time.LocalDateTime;

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
public class SavedDocumentsResponse {
    private Long id;
    private Long documentId;
    private String documentTitle;
    private String slug;
    private String category;
    private String authorName;
    private LocalDateTime savedAt;
}
