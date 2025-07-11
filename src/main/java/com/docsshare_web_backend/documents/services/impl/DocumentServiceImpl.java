package com.docsshare_web_backend.documents.services.impl;

import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.Optional;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;

import com.docsshare_web_backend.categories.repositories.CategoryRepository;
import com.docsshare_web_backend.documents.dto.requests.DocumentFilterRequest;
import com.docsshare_web_backend.documents.dto.requests.DocumentRequest;
import com.docsshare_web_backend.documents.dto.responses.DocumentResponse;
import com.docsshare_web_backend.documents.enums.DocumentModerationStatus;
import com.docsshare_web_backend.documents.filters.DocumentFilter;
import com.docsshare_web_backend.documents.models.Document;
import com.docsshare_web_backend.documents.models.DocumentCoAuthor;
import com.docsshare_web_backend.documents.dto.responses.DocumentCoAuthorResponse;
import com.docsshare_web_backend.documents.repositories.DocumentRepository;
import com.docsshare_web_backend.documents.services.DocumentCoAuthorService;
import com.docsshare_web_backend.documents.services.DocumentService;
import com.docsshare_web_backend.saved_documents.dto.responses.SavedDocumentsCountProjection;
import com.docsshare_web_backend.saved_documents.repositories.SavedDocumentsRepository;
import com.docsshare_web_backend.users.repositories.UserRepository;
import com.docsshare_web_backend.commons.services.GoogleDriveService;
import com.docsshare_web_backend.commons.utils.SlugUtils;

import org.springframework.web.multipart.MultipartFile;

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

        @Autowired
        private DocumentCoAuthorService documentCoAuthorService;

        @Autowired
        private SavedDocumentsRepository savedDocumentsRepository;

        @Autowired
        private GoogleDriveService googleDriveService;

        private Pageable getPageable(Pageable pageable) {
                return pageable != null ? pageable : Pageable.unpaged();
        }

        public static class DocumentMapper {
                public static DocumentResponse toDocumentResponse(Document document, Long saveCount) {
                        List<DocumentCoAuthorResponse> coAuthors = document.getCoAuthors() != null
                                ? document.getCoAuthors().stream()
                                .map(coAuthor -> DocumentCoAuthorResponse.builder()
                                        .name(coAuthor.getName())
                                        .email(coAuthor.getEmail())
                                        .build())
                                .collect(Collectors.toList())
                                : Collections.emptyList(); 

                        return DocumentResponse.builder()
                                .id(document.getId())
                                .title(document.getTitle())
                                .description(document.getDescription())
                                .filePath(document.getFilePath())
                                .fileType(document.getFileType())
                                .slug(document.getSlug())
                                .price(document.getPrice())
                                .views(document.getViews() != null ? document.getViews() : 0L)
                                .copyrightPath(document.getCopyrightPath())
                                .moderationStatus(
                                                document.getModerationStatus() != null
                                                                ? document.getModerationStatus().toString()
                                                                : null)
                                .isPublic(document.isPublic())
                                .createdAt(document.getCreatedAt())
                                .authorName(document.getAuthor() != null ? document.getAuthor().getName() : "")
                                .category(document.getCategory() != null ? document.getCategory().getName()
                                                : "")
                                .coAuthors(coAuthors)
                                .saveCount(saveCount != null ? saveCount : 0L)
                                .build();
                }

                public static DocumentResponse toDocumentResponse(Document document) {
                        return toDocumentResponse(document, 0L);
                }
                    
        }
        private List<DocumentResponse> mapDocumentsToResponse(List<Document> documents) {
                List<Long> documentIds = documents.stream().map(Document::getId).toList();

                Map<Long, Long> saveCountMap = savedDocumentsRepository.countSavesByDocumentIds(documentIds)
                        .stream()
                        .collect(Collectors.toMap(
                                SavedDocumentsCountProjection::getDocumentId,
                                SavedDocumentsCountProjection::getSaveCount
                        ));

                return documents.stream()
                        .map(doc -> DocumentMapper.toDocumentResponse(
                                doc,
                                saveCountMap.getOrDefault(doc.getId(), 0L)
                        )).toList();
        }

        private Page<DocumentResponse> mapDocumentsToResponse(Page<Document> page) {
                List<DocumentResponse> responses = mapDocumentsToResponse(page.getContent());
                return new PageImpl<>(responses, page.getPageable(), page.getTotalElements());
        }

        private DocumentResponse mapDocumentToResponse(Document document) {
                Long saveCount = savedDocumentsRepository.countSavesByDocumentIds(List.of(document.getId())).stream()
                        .findFirst()
                        .map(SavedDocumentsCountProjection::getSaveCount)
                        .orElse(0L);

                return DocumentMapper.toDocumentResponse(document, saveCount);
        }    

        @Override
        @Transactional(readOnly = true)
        public Page<DocumentResponse> getAllDocuments(DocumentFilterRequest request, Pageable pageable) {
                Specification<Document> spec = DocumentFilter.filterByRequest(request);
                Page<Document> documents = documentRepository.findAll(spec, getPageable(pageable));
                return mapDocumentsToResponse(documents);
        }

        @Override
        @Transactional(readOnly = true)
        public DocumentResponse getDocument(long id) {
                Document document = documentRepository.findById(id)
                                .orElseThrow(() -> new EntityNotFoundException("Document not found with id: " + id));

                return mapDocumentToResponse(document);
        }

        @Override
        @Transactional(readOnly = true)
        public DocumentResponse getDocumentBySlug(String slug) {
                if (slug == null || slug.trim().isEmpty()) {
                        throw new IllegalArgumentException("Slug cannot be null or empty");
                }
                Document document = documentRepository.findBySlug(slug)
                                .orElseThrow(() -> new EntityNotFoundException(
                                                "Document not found with slug: " + slug));

                return mapDocumentToResponse(document);
        }

        @Override
        @Transactional(readOnly = true)
        public Page<DocumentResponse> getDocumentsByUserId(DocumentFilterRequest request, long userId,
                        Pageable pageable) {
                userRepository.findById(userId)
                                .orElseThrow(() -> new EntityNotFoundException(
                                                "User not found with id: " + userId));
                Specification<Document> spec = Specification
                                .<Document>where((root, query, cb) -> cb.equal(root.get("author").get("id"), userId))
                                .and(DocumentFilter.filterByRequest(request));

                Page<Document> documents = documentRepository.findAll(spec, getPageable(pageable));
                return mapDocumentsToResponse(documents);
        }

        @Override
        @Transactional(readOnly = true)
        public Page<DocumentResponse> getDocumentsByAuthorOrCoAuthorId(DocumentFilterRequest request, long userId,
                        Pageable pageable) {
                userRepository.findById(userId)
                                .orElseThrow(() -> new EntityNotFoundException(
                                                "User not found with id: " + userId));

                try {
                        Specification<Document> spec = buildAuthorOrCoAuthorSpecification(userId)
                                .and(DocumentFilter.filterByRequest(request));
                
                        Page<Document> documents = documentRepository.findAll(spec, getPageable(pageable));
                        return mapDocumentsToResponse(documents);
                } catch (Exception e) {
                log.error("Error fetching documents for user {}: {}", userId, e.getMessage());
                throw new RuntimeException("Failed to fetch documents", e);
                }
        }

        private Specification<Document> buildAuthorOrCoAuthorSpecification(long userId) {
                return (root, query, cb) -> {
                    // Add distinct to prevent duplicate results
                    query.distinct(true);
                    
                    // Create subquery for coauthor check to improve performance
                    var subquery = query.subquery(Long.class);
                    var coAuthorRoot = subquery.from(DocumentCoAuthor.class);
                    subquery.select(coAuthorRoot.get("document").get("id"))
                            .where(cb.and(
                                cb.equal(coAuthorRoot.get("user").get("id"), userId),
                                cb.isTrue(coAuthorRoot.get("isConfirmed"))
                            ));
            
                    // Combine conditions using OR
                    return cb.or(
                        cb.equal(root.get("author").get("id"), userId),
                        cb.in(root.get("id")).value(subquery)
                    );
                };
            }

        @Override
        @Transactional(readOnly = true)
        public Page<DocumentResponse> getDocumentsByCategoryId(DocumentFilterRequest request, long categoryId,
                        Pageable pageable) {
                categoryRepository.findById(categoryId)
                                .orElseThrow(() -> new EntityNotFoundException(
                                                "Category not found with id: " + categoryId));
                Specification<Document> spec = Specification
                                .<Document>where((root, query, cb) -> cb.equal(root.get("category").get("id"),
                                                categoryId))
                                .and(DocumentFilter.filterByRequest(request));
                
                Page<Document> documents = documentRepository.findAll(spec, getPageable(pageable));
                return mapDocumentsToResponse(documents);
        }

        @Override
        @Transactional(readOnly = true)
        public Page<DocumentResponse> getDocumentsNeedApproved(DocumentFilterRequest request, Pageable pageable) {
                Specification<Document> spec = Specification
                                .<Document>where((root, query, cb) -> cb.equal(root.get("moderationStatus"),
                                                DocumentModerationStatus.PENDING))
                                .and((root, query, cb) -> cb.isTrue(root.get("isPublic")));

                Page<Document> documents = documentRepository.findAll(spec, getPageable(pageable));
                return mapDocumentsToResponse(documents);
        }

        @Override
        @Transactional
        public DocumentResponse createDocument(DocumentRequest request) {
                var author = userRepository.findById(request.getUserId())
                                .orElseThrow(() -> new EntityNotFoundException(
                                                "User not found with id: " + request.getUserId()));

                var category = categoryRepository.findById(request.getCategoryId())
                                .orElseThrow(
                                                () -> new EntityNotFoundException("Category not found with id: "
                                                                + request.getCategoryId()));

                String originalFilename = request.getFile().getOriginalFilename();
                String fileType = null;
                if (originalFilename != null && originalFilename.contains(".")) {
                        fileType = originalFilename.substring(originalFilename.lastIndexOf(".") + 1).toLowerCase();
                }
                String fileHash;
                try {
                        fileHash = googleDriveService.calculateSHA256Hash(request.getFile());
                } catch (Exception e) {
                        throw new RuntimeException("Failed to calculate file hash", e);
                }
                List<Document> existingDocuments = documentRepository.findByFileHash(fileHash);
                String fileUrl;
                if (!existingDocuments.isEmpty()) {
                        fileUrl = existingDocuments.get(0).getFilePath(); // Dùng file đã tồn tại
                        fileType = existingDocuments.get(0).getFileType();
                } else {
                        try {
                                fileUrl = googleDriveService.uploadFile(request.getFile(), "DocsShareStorage");
                        } catch (Exception e) {
                                throw new RuntimeException("Failed to upload file to Google Drive", e);
                        }
                }

                Document document = Document.builder()
                                .title(request.getTitle())
                                .description(request.getDescription())
                                .filePath(fileUrl)
                                .fileHash(fileHash)
                                .fileType(fileType)
                                .slug(null)
                                .price(request.getPrice())
                                .copyrightPath(request.getCopyrightPath())
                                .moderationStatus(DocumentModerationStatus.PENDING)
                                .isPublic(request.isPublic())
                                .author(author)
                                .category(category)
                                .build();

                Document savedDocument = documentRepository.save(document);
                savedDocument.setSlug(SlugUtils.generateSlug(savedDocument.getTitle(), savedDocument.getId()));
                savedDocument = documentRepository.save(savedDocument);
                if (request.getCoAuthor() != null && !request.getCoAuthor().isEmpty()) {
                        for (var coAuthorRequest : request.getCoAuthor()) {
                            documentCoAuthorService.addCoAuthor(savedDocument.getId(), coAuthorRequest);
                        }
                    }
                return DocumentMapper.toDocumentResponse(savedDocument);
        }

        @Override
        @Transactional
        public DocumentResponse updateDocument(long documentId, DocumentRequest request) {
                Document existingDocument = documentRepository.findById(documentId)
                                .orElseThrow(() -> new EntityNotFoundException(
                                                "Document not found with id: " + documentId));

                if (existingDocument.getModerationStatus() == DocumentModerationStatus.APPROVED ||
                                existingDocument.getModerationStatus() == DocumentModerationStatus.REJECTED) {
                        existingDocument.setModerationStatus(DocumentModerationStatus.PENDING);
                }

                existingDocument.setTitle(request.getTitle());
                existingDocument.setDescription(request.getDescription());
                if (request.getFile() != null) {
                        try {
                                String fileUrl = googleDriveService.uploadFile(request.getFile(), "documents");
                                existingDocument.setFilePath(fileUrl);
                        } catch (Exception e) {
                                throw new RuntimeException("Failed to upload file to Google Drive", e);
                        }
                }
                existingDocument.setSlug(request.getSlug());
                existingDocument.setPrice(request.getPrice());
                existingDocument.setCopyrightPath(request.getCopyrightPath());
                existingDocument.setPublic(request.isPublic());

                Document updatedDocument = documentRepository.save(existingDocument);
                return DocumentMapper.toDocumentResponse(updatedDocument);
        }

        @Override
        @Transactional
        public DocumentResponse updateDocumentStatus(long id, DocumentModerationStatus status) {
                Document existingDocument = documentRepository.findById(id)
                                .orElseThrow(() -> new EntityNotFoundException("Document not found with id: " + id));
                existingDocument.setModerationStatus(status);
                if (status == DocumentModerationStatus.APPROVED) {
                        existingDocument.setPublic(true);
                } else {
                        existingDocument.setPublic(false);
                }
                return DocumentMapper.toDocumentResponse(documentRepository.save(existingDocument));
        }

        @Override
        @Transactional
        public DocumentResponse incrementView(long documentId){
                Document document = documentRepository.findById(documentId)
                                .orElseThrow(() -> new EntityNotFoundException("Document not found with id: " + documentId));
                if (document.getViews() == null) {
                        document.setViews(0L);
                }
                document.setViews(document.getViews() + 1);
                return DocumentMapper.toDocumentResponse(documentRepository.save(document));
        }

}
