package com.docsshare_web_backend.documents.services;

import com.docsshare_web_backend.documents.dto.requests.DocumentFilterRequest;
import com.docsshare_web_backend.documents.dto.requests.DocumentRequest;
import com.docsshare_web_backend.documents.dto.responses.DocumentResponse;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
public interface DocumentService {
    Page<DocumentResponse> getAllDocuments(DocumentFilterRequest request, Pageable pageable);
    DocumentResponse getDocument(long id);
    Page<DocumentResponse> getDocumentsByUserId(DocumentFilterRequest request, long userId, Pageable pageable);
    Page<DocumentResponse> getDocumentsByCategoryId(DocumentFilterRequest request, long categoryId, Pageable pageable);
    DocumentResponse createDocument(DocumentRequest request);
    
}
