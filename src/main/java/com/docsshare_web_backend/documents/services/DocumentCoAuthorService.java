package com.docsshare_web_backend.documents.services;
import org.springframework.stereotype.Service;

import com.docsshare_web_backend.documents.dto.requests.DocumentCoAuthorRequest;
import com.docsshare_web_backend.documents.dto.responses.DocumentCoAuthorResponse;

@Service
public interface DocumentCoAuthorService {
    DocumentCoAuthorResponse addCoAuthor(long documentId, DocumentCoAuthorRequest request);
    void removeCoAuthor(Long documentId, String email);
}
