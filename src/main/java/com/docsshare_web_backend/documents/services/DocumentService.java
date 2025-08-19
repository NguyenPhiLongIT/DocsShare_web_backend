package com.docsshare_web_backend.documents.services;

import com.docsshare_web_backend.account.dto.responses.TopUserAddDocumentResponse;
import com.docsshare_web_backend.documents.dto.requests.DocumentFilterRequest;
import com.docsshare_web_backend.documents.dto.requests.DocumentRequest;
import com.docsshare_web_backend.documents.dto.requests.DocumentUpdateRequest;
import com.docsshare_web_backend.documents.dto.requests.DocumentUpdateStatusRequest;
import com.docsshare_web_backend.documents.dto.responses.DocumentResponse;
import com.docsshare_web_backend.documents.dto.responses.TopDocumentReportResponse;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
public interface DocumentService {
    Page<DocumentResponse> getAllDocuments(DocumentFilterRequest request, Pageable pageable);
    DocumentResponse getDocument(long id);
    DocumentResponse getDocumentBySlug(String slug);
    Page<DocumentResponse> getDocumentsByUserId(DocumentFilterRequest request, long userId, Pageable pageable);
    Page<DocumentResponse> getDocumentsByAuthorOrCoAuthorId(DocumentFilterRequest request, long userId, Pageable pageable);
    Page<DocumentResponse> getDocumentsByCategoryId(DocumentFilterRequest request, long categoryId, Pageable pageable);
    Page<DocumentResponse> getDocumentsNeedApproved(DocumentFilterRequest request, Pageable pageable);
    DocumentResponse createDocument(DocumentRequest request);
    DocumentResponse updateDocument(long documentId, DocumentUpdateRequest request);
    void deleteDocument(Long id);
    DocumentResponse updateDocumentStatus(long id, DocumentUpdateStatusRequest request);
    DocumentResponse incrementView(long documentId);
    List<TopDocumentReportResponse> getTopDocumentsBetween(LocalDate from, LocalDate to, int top);
    List<TopUserAddDocumentResponse> getTopUsersAddDocumentBetween(LocalDate from, LocalDate to, int top);

}
