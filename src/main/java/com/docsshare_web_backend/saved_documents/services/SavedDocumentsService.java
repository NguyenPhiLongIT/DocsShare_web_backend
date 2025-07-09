package com.docsshare_web_backend.saved_documents.services;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.docsshare_web_backend.saved_documents.dto.requests.SavedDocumentsFilterRequest;
import com.docsshare_web_backend.saved_documents.dto.requests.SavedDocumentsRequest;
import com.docsshare_web_backend.saved_documents.dto.responses.SavedDocumentsResponse;

@Service
public interface SavedDocumentsService {
    Page<SavedDocumentsResponse> getSavedDocumentsByUserId(SavedDocumentsFilterRequest request, long userId, Pageable pageable);
    SavedDocumentsResponse saveDocument(SavedDocumentsRequest request);
    void unsaveDocument(SavedDocumentsRequest request);
}
