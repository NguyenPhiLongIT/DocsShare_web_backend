package com.docsshare_web_backend.saved_documents.dto.requests;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class SavedDocumentsFilterRequest {
    private String q;
}
