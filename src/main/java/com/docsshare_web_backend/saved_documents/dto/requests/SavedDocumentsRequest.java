package com.docsshare_web_backend.saved_documents.dto.requests;

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
public class SavedDocumentsRequest {
    private Long documentId;
    private Long userId;
}
