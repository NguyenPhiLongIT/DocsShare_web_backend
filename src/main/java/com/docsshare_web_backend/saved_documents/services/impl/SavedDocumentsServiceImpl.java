package com.docsshare_web_backend.saved_documents.services.impl;

import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.docsshare_web_backend.documents.models.Document;
import com.docsshare_web_backend.documents.repositories.DocumentRepository;
import com.docsshare_web_backend.saved_documents.dto.requests.SavedDocumentsFilterRequest;
import com.docsshare_web_backend.saved_documents.dto.requests.SavedDocumentsRequest;
import com.docsshare_web_backend.saved_documents.dto.responses.SavedDocumentsResponse;
import com.docsshare_web_backend.saved_documents.filters.SavedDocumentsFilter;
import com.docsshare_web_backend.saved_documents.models.SavedDocuments;
import com.docsshare_web_backend.saved_documents.repositories.SavedDocumentsRepository;
import com.docsshare_web_backend.saved_documents.services.SavedDocumentsService;
import com.docsshare_web_backend.users.models.User;
import com.docsshare_web_backend.users.repositories.UserRepository;

import jakarta.persistence.EntityNotFoundException;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class SavedDocumentsServiceImpl implements SavedDocumentsService{
    @Autowired
    private SavedDocumentsRepository savedDocumentsRepository;

    @Autowired
    private DocumentRepository documentRepository;

    @Autowired
    private UserRepository userRepository;

    private Pageable getPageable(Pageable pageable) {
        return pageable != null ? pageable : Pageable.unpaged();
    }
    
    public static class SavedDocumentsMapper {
        public static SavedDocumentsResponse toSavedDocumentsResponse(SavedDocuments savedDocuments){
            return SavedDocumentsResponse.builder()
                    .id(savedDocuments.getId())
                    .documentId(savedDocuments.getDocument().getId())
                    .documentTitle(savedDocuments.getDocument().getTitle())
                    .slug(savedDocuments.getDocument().getSlug())
                    .category(savedDocuments.getDocument().getCategory().getName())
                    .authorName(savedDocuments.getDocument().getAuthor().getName())
                    .savedAt(savedDocuments.getSavedAt())
                    .build();
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Page<SavedDocumentsResponse> getSavedDocumentsByUserId(SavedDocumentsFilterRequest request, long userId, Pageable pageable) {
        userRepository.findById(userId)
            .orElseThrow(() -> new EntityNotFoundException(
                        "User not found with id: " + userId));
        Specification<SavedDocuments> spec = Specification
            .<SavedDocuments>where((root, query, cb) -> cb.equal(root.get("user").get("id"), userId))
            .and(SavedDocumentsFilter.filterByRequest(request));
        return savedDocumentsRepository.findAll(spec, pageable).map(SavedDocumentsMapper::toSavedDocumentsResponse);
    }

    @Override
    @Transactional
    public SavedDocumentsResponse saveDocument(SavedDocumentsRequest request) {
        Document document = documentRepository.findById(request.getDocumentId())
                .orElseThrow(() -> new EntityNotFoundException("Document not found with id: " + request.getDocumentId()));

        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new EntityNotFoundException("User not found with id: " + request.getUserId()));
        
        Optional<SavedDocuments> existing = savedDocumentsRepository.findByUserIdAndDocumentId(request.getUserId(), request.getDocumentId());
        if (existing.isPresent()) {
            return SavedDocumentsMapper.toSavedDocumentsResponse(existing.get());
        }

        SavedDocuments saved = new SavedDocuments();
        saved.setUser(user);
        saved.setDocument(document);
        saved.setSavedAt(LocalDateTime.now());

        SavedDocuments savedResult = savedDocumentsRepository.save(saved);
        return SavedDocumentsMapper.toSavedDocumentsResponse(savedResult);
    }

    @Override
    @Transactional
    public void unsaveDocument(SavedDocumentsRequest request) {
        Optional<SavedDocuments> existing = savedDocumentsRepository.findByUserIdAndDocumentId(request.getUserId(), request.getDocumentId());
        if (existing.isPresent()) {
            savedDocumentsRepository.delete(existing.get());
        } else {
            throw new EntityNotFoundException("Saved document not found for userId=" + request.getUserId() + " and documentId=" + request.getDocumentId());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isDocumentSaved(SavedDocumentsRequest request) {
        return savedDocumentsRepository.findByUserIdAndDocumentId(request.getUserId(), request.getDocumentId()).isPresent();
    }

}
