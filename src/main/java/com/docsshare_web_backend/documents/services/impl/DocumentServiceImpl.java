package com.docsshare_web_backend.documents.services.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import com.docsshare_web_backend.categories.repositories.CategoryRepository;
import com.docsshare_web_backend.documents.dto.requests.DocumentFilterRequest;
import com.docsshare_web_backend.documents.dto.requests.DocumentRequest;
import com.docsshare_web_backend.documents.dto.responses.DocumentResponse;
import com.docsshare_web_backend.documents.enums.DocumentModerationStatus;
import com.docsshare_web_backend.documents.filters.DocumentFilter;
import com.docsshare_web_backend.documents.models.Document;
import com.docsshare_web_backend.documents.repositories.DocumentRepository;
import com.docsshare_web_backend.documents.services.DocumentService;
import com.docsshare_web_backend.users.repositories.UserRepository;

import jakarta.persistence.EntityNotFoundException;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class DocumentServiceImpl implements DocumentService {
    @Autowired
    private DocumentRepository documentRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    private Pageable getPageable(Pageable pageable) {
        return pageable != null ? pageable : Pageable.unpaged();
    }

    public static class DocumentMapper {
        public static DocumentResponse toDocumentResponse(Document document) {
            return DocumentResponse.builder()
                    .id(document.getId())
                    .title(document.getTitle())
                    .description(document.getDescription())
                    .filePath(document.getFilePath())
                    .slug(document.getSlug())
                    .price(document.getPrice())
                    .copyrightPath(document.getCopyrightPath())
                    .moderationStatus(
                            document.getModerationStatus() != null ? document.getModerationStatus().toString() : null)
                    .isPublic(document.isPublic())
                    .coAuthor(document.getCoAuthor() != null ? document.getCoAuthor().toString() : null)
                    .createdAt(document.getCreatedAt())
                    .authorName(document.getAuthor() != null ? document.getAuthor().getName() : "")
                    .category(document.getCategory() != null ? document.getCategory().getName() : "")
                    .build();
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Page<DocumentResponse> getAllDocuments(DocumentFilterRequest request, Pageable pageable) {
        Specification<Document> spec = DocumentFilter.filterByRequest(request);
        return documentRepository.findAll(spec, getPageable(pageable))
                .map(DocumentMapper::toDocumentResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public DocumentResponse getDocument(long id) {
        Document document = documentRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Document not found with id: " + id));

        return DocumentMapper.toDocumentResponse(document);
    }

    @Override
    @Transactional(readOnly = true)
    public DocumentResponse getDocumentBySlug(String slug){
        if (slug == null || slug.trim().isEmpty()) {
                throw new IllegalArgumentException("Slug cannot be null or empty");
            }
            Document document = documentRepository.findBySlug(slug)
                    .orElseThrow(() -> new EntityNotFoundException("Document not found with slug: " + slug));
    
            return DocumentMapper.toDocumentResponse(document);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<DocumentResponse> getDocumentsByUserId(DocumentFilterRequest request, long userId, Pageable pageable) {
        userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "User not found with id: " + userId));
        Specification<Document> spec = Specification
                .<Document>where((root, query, cb) -> cb.equal(root.get("user").get("id"), userId))
                .and(DocumentFilter.filterByRequest(request));

        return documentRepository.findAll(spec, pageable)
                .map(DocumentMapper::toDocumentResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<DocumentResponse> getDocumentsByCategoryId(DocumentFilterRequest request, long categoryId,
            Pageable pageable) {
        categoryRepository.findById(categoryId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Category not found with id: " + categoryId));
        Specification<Document> spec = Specification
                .<Document>where((root, query, cb) -> cb.equal(root.get("category").get("id"), categoryId))
                .and(DocumentFilter.filterByRequest(request));

        return documentRepository.findAll(spec, pageable)
                .map(DocumentMapper::toDocumentResponse);
    }

    @Override
    @Transactional
    public DocumentResponse createDocument(DocumentRequest request) {
        var author = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new EntityNotFoundException("User not found with id: " + request.getUserId()));

        var category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(
                        () -> new EntityNotFoundException("Category not found with id: " + request.getCategoryId()));

        Document document = Document.builder()
                .title(request.getTitle())
                .description(request.getDesciption())
                .filePath(request.getFilePath())
                .slug(request.getSlug())
                .price(request.getPrice())
                .copyrightPath(request.getCopyrightPath())
                .moderationStatus(DocumentModerationStatus.PENDING)
                .isPublic(request.isPublic())
                .coAuthor(request.getCoAuthor())
                .author(author)
                .category(category)
                .build();

        Document savedDocument = documentRepository.save(document);

        return DocumentMapper.toDocumentResponse(savedDocument);
    }
}
