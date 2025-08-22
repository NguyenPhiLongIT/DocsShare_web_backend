package com.docsshare_web_backend.documents.services.impl;

import java.time.LocalDateTime;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.docsshare_web_backend.documents.dto.requests.DocumentCoAuthorRequest;
import com.docsshare_web_backend.documents.dto.responses.DocumentCoAuthorResponse;
import com.docsshare_web_backend.documents.dto.responses.DocumentResponse;
import com.docsshare_web_backend.documents.models.Document;
import com.docsshare_web_backend.documents.models.DocumentCoAuthor;
import com.docsshare_web_backend.documents.repositories.DocumentCoAuthorRepository;
import com.docsshare_web_backend.documents.repositories.DocumentRepository;
import com.docsshare_web_backend.documents.services.DocumentCoAuthorService;
import com.docsshare_web_backend.users.models.User;
import com.docsshare_web_backend.users.repositories.UserRepository;

import jakarta.persistence.EntityNotFoundException;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class DocumentCoAuthorServiceImpl implements DocumentCoAuthorService {
    @Autowired
    private DocumentCoAuthorRepository documentCoAuthorRepository;

    @Autowired UserRepository userRepository;

    @Autowired
    private DocumentRepository documentRepository;

    public static class DocumentCoAuthorMapper {
        public static DocumentCoAuthorResponse toDocumentCoAuthorResponse(DocumentCoAuthor coAuthor) {
            return DocumentCoAuthorResponse.builder()
                .name(coAuthor.getName())
                .email(coAuthor.getEmail())
                .build();
        }
    }

    @Override
    @Transactional
    public DocumentCoAuthorResponse addCoAuthor(long documentId, DocumentCoAuthorRequest request){
        Document existingDocument = documentRepository.findById(documentId)
                                .orElseThrow(() -> new EntityNotFoundException(
                                                "Document not found with id: " + documentId));
        if (documentCoAuthorRepository.existsByDocumentIdAndEmail(documentId, request.getEmail())) {
            throw new IllegalStateException("This email is already added as a co-author.");
        }

        var userOptional = userRepository.findByEmail(request.getEmail());
        DocumentCoAuthor newCoAuthor;
        if (userOptional.isPresent()) {
            newCoAuthor = DocumentCoAuthor.builder()
                    .user(userOptional.get())
                    .name(null)      // để null
                    .email(null)     // để null
                    .isConfirmed(true)
                    .document(existingDocument)
                    .createdAt(LocalDateTime.now())
                    .build();
        } else {
            if (request.getName() == null || request.getName().trim().isEmpty()) {
                throw new IllegalArgumentException(
                    "Name is required for co-author when email is not associated with a user.");
            }
    
            newCoAuthor = DocumentCoAuthor.builder()
                    .name(request.getName())
                    .email(request.getEmail())
                    .user(null)
                    .isConfirmed(false)
                    .document(existingDocument)
                    .createdAt(LocalDateTime.now())
                    .build();
        }
    
        DocumentCoAuthor savedCoAuthor = documentCoAuthorRepository.save(newCoAuthor);
        return DocumentCoAuthorMapper.toDocumentCoAuthorResponse(savedCoAuthor);
    }

    @Override
    @Transactional
    public void removeCoAuthor(Long documentId, String email) {
        var userOptional = userRepository.findByEmail(email);

        DocumentCoAuthor existingCoAuthor;

        if (userOptional.isPresent()) {
            existingCoAuthor = documentCoAuthorRepository
                    .findByDocumentIdAndUserId(documentId, userOptional.get().getId())
                    .orElseThrow(() -> new EntityNotFoundException(
                            "Co-author (user) with email " + email + " not found for document id: " + documentId));
        } else {
            existingCoAuthor = documentCoAuthorRepository
                    .findByDocumentIdAndEmail(documentId, email)
                    .orElseThrow(() -> new EntityNotFoundException(
                            "Co-author (guest) with email " + email + " not found for document id: " + documentId));
        }

        documentCoAuthorRepository.delete(existingCoAuthor);
    }

}
