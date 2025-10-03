package com.docsshare_web_backend.documents.services.impl;
import java.io.File;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.docsshare_web_backend.account.dto.responses.TopUserAddDocumentResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import com.docsshare_web_backend.categories.repositories.CategoryRepository;
import com.docsshare_web_backend.documents.dto.requests.DocumentFilterRequest;
import com.docsshare_web_backend.documents.dto.requests.DocumentRequest;
import com.docsshare_web_backend.documents.dto.requests.DocumentUpdateRequest;
import com.docsshare_web_backend.documents.dto.requests.DocumentUpdateStatusRequest;
import com.docsshare_web_backend.documents.dto.responses.DocumentResponse;
import com.docsshare_web_backend.documents.dto.responses.TopDocumentReportResponse;
import com.docsshare_web_backend.documents.enums.DocumentFileType;
import com.docsshare_web_backend.documents.enums.DocumentModerationStatus;
import com.docsshare_web_backend.documents.filters.DocumentFilter;
import com.docsshare_web_backend.documents.models.Document;
import com.docsshare_web_backend.documents.models.DocumentCoAuthor;
import com.docsshare_web_backend.documents.models.DocumentImage;
import com.docsshare_web_backend.documents.dto.responses.DocumentCoAuthorResponse;
import com.docsshare_web_backend.documents.repositories.DocumentImageRepository;
import com.docsshare_web_backend.documents.repositories.DocumentRepository;
import com.docsshare_web_backend.documents.services.DocumentCoAuthorService;
import com.docsshare_web_backend.documents.services.DocumentService;
import com.docsshare_web_backend.notification.enums.NotificationType;
import com.docsshare_web_backend.notification.models.Notification;
import com.docsshare_web_backend.notification.repositories.NotificationRepository;
import com.docsshare_web_backend.saved_documents.dto.responses.SavedDocumentsCountProjection;
import com.docsshare_web_backend.saved_documents.repositories.SavedDocumentsRepository;
import com.docsshare_web_backend.users.enums.UserType;
import com.docsshare_web_backend.users.models.User;
import com.docsshare_web_backend.users.repositories.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.docsshare_web_backend.commons.services.DocumentImageService;
import com.docsshare_web_backend.commons.services.GoogleDriveService;
import com.docsshare_web_backend.commons.utils.InMemoryMultipartFile;
import com.docsshare_web_backend.commons.utils.SlugUtils;

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

        @Autowired
        private NotificationRepository notificationRepository;

        @Autowired
        private DocumentImageService documentImageService;

        @Autowired
        private DocumentImageRepository documentImageRepository;

        private Pageable getPageable(Pageable pageable) {
                return pageable != null ? pageable : Pageable.unpaged();
        }

        public static class DocumentMapper {
                public static DocumentResponse toDocumentResponse(Document document, Long saveCount) {
                        List<DocumentCoAuthorResponse> coAuthors = 
                            document.getCoAuthors() != null
                                ? document.getCoAuthors().stream()
                                    .map(DocumentMapper::mapCoAuthorToResponse)
                                    .collect(Collectors.toList())
                                : Collections.emptyList();
                    
                        return DocumentResponse.builder()
                                .id(document.getId())
                                .title(document.getTitle())
                                .description(document.getDescription())
                                .filePath(document.getFilePath())
                                .fileType(document.getFileType() != null ? document.getFileType().toString() : null)
                                .slug(document.getSlug())
                                .price(document.getPrice())
                                .views(document.getViews() != null ? document.getViews() : 0L)
                                .copyrightPath(document.getCopyrightPath())
                                .moderationStatus(document.getModerationStatus() != null 
                                                    ? document.getModerationStatus().toString() 
                                                    : null)
                                .rejectedReason(document.getRejectedReason())
                                .isPublic(document.isPublic())
                                .createdAt(document.getCreatedAt())
                                .authorName(document.getAuthor() != null ? document.getAuthor().getName() : "")
                                .category(document.getCategory() != null ? document.getCategory().getName() : "")
                                .categoryId(document.getCategory() != null ? document.getCategory().getId() : null)
                                .coAuthors(coAuthors)
                                .saveCount(saveCount != null ? saveCount : 0L)
                                .build();
                    }
                    
                    private static DocumentCoAuthorResponse mapCoAuthorToResponse(DocumentCoAuthor coAuthor) {
                        if (coAuthor.getUser() != null) {
                            return DocumentCoAuthorResponse.builder()
                                    .id(coAuthor.getId())
                                    .userId(coAuthor.getUser().getId())
                                    .name(coAuthor.getUser().getName())
                                    .email(coAuthor.getUser().getEmail())
                                    .build();
                        } else {
                            return DocumentCoAuthorResponse.builder()
                                    .id(coAuthor.getId())
                                    .name(coAuthor.getName())
                                    .email(coAuthor.getEmail())
                                    .build();
                        }
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
                String fileExtension = null;
                DocumentFileType fileType = null;

                if (originalFilename != null && originalFilename.contains(".")) {
                        fileExtension = originalFilename.substring(originalFilename.lastIndexOf(".") + 1).toLowerCase();
                        try {
                                fileType = DocumentFileType.fromExtension(fileExtension);
                        } catch (IllegalArgumentException e) {
                                throw new RuntimeException("Unsupported file type: " + fileExtension, e);
                        }
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
                DocumentModerationStatus moderationStatus = switch (author.getUserType()) {
                        case ADMIN, STAFF -> DocumentModerationStatus.APPROVED;
                        default -> DocumentModerationStatus.PENDING;
                    };

                Document document = Document.builder()
                        .title(request.getTitle())
                        .description(request.getDescription())
                        .filePath(fileUrl)
                        .fileHash(fileHash)
                        .fileType(fileType)
                        .slug(null)
                        .price(request.getPrice())
                        .copyrightPath(request.getCopyrightPath())
                        .moderationStatus(moderationStatus)
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
                
                List<DocumentImageService.ImageFeatureResult> imageResults =
                        documentImageService.extractImagesAndFeatures(request.getFile());

                for (DocumentImageService.ImageFeatureResult img : imageResults) {
                        try {
                                String featuresJson = new ObjectMapper().writeValueAsString(img.getFeatures());
                                DocumentImage docImage = DocumentImage.builder()
                                        .document(savedDocument)
                                        .imagePath(img.getUrl())       
                                        .featureVector(featuresJson)  
                                        .build();

                                documentImageRepository.save(docImage);

                        } catch (Exception e) {
                                e.printStackTrace();
                        }
                }
                return DocumentMapper.toDocumentResponse(savedDocument);
        }

        @Override
        @Transactional
        public DocumentResponse updateDocument(long documentId, DocumentUpdateRequest request) {
                Document existingDocument = documentRepository.findById(documentId)
                                .orElseThrow(() -> new EntityNotFoundException(
                                                "Document not found with id: " + documentId));
                
                var author = userRepository.findById(request.getUserId())
                                .orElseThrow(() -> new EntityNotFoundException(
                                                "User not found with id: " + request.getUserId()));
                var category = categoryRepository.findById(request.getCategoryId())
                                .orElseThrow(() -> new EntityNotFoundException("Category not found with id: "
                                                + request.getCategoryId()));

                if ((existingDocument.getModerationStatus() == DocumentModerationStatus.APPROVED ||
                                existingDocument.getModerationStatus() == DocumentModerationStatus.REJECTED)
                   && (author.getUserType() == UserType.USER)) {
                        existingDocument.setModerationStatus(DocumentModerationStatus.PENDING);
                }

                existingDocument.setTitle(request.getTitle());
                existingDocument.setDescription(request.getDescription());
                existingDocument.setSlug(SlugUtils.generateSlug(request.getTitle(), documentId));
                existingDocument.setPrice(request.getPrice());
                existingDocument.setCopyrightPath(request.getCopyrightPath());
                existingDocument.setPublic(request.isPublic());
                existingDocument.setCategory(category);

                Document updatedDocument = documentRepository.save(existingDocument);
                return DocumentMapper.toDocumentResponse(updatedDocument);
        }

        @Override
        @Transactional
        public DocumentResponse updateDocumentStatus(long id, DocumentUpdateStatusRequest request) {
                Document existingDocument = documentRepository.findById(id)
                        .orElseThrow(() -> new EntityNotFoundException("Document not found with id: " + id));
                
                User sender = userRepository.findById(request.getSenderId())
                        .orElseThrow(() -> new EntityNotFoundException("Sender not found"));

                DocumentModerationStatus status = request.getStatus();
                existingDocument.setModerationStatus(status);

                if (status == DocumentModerationStatus.APPROVED) {
                        existingDocument.setRejectedReason(null); // clear nếu từng bị từ chối
                        Notification notification = Notification.builder()
                                .content("Document \"" + existingDocument.getTitle() + "\" has been approved.")
                                .createdAt(LocalDateTime.now())
                                .isRead(false)
                                .link("/documents/" + existingDocument.getSlug())
                                .targetId(existingDocument.getId())
                                .type(NotificationType.APPROVED)
                                .user(existingDocument.getAuthor())
                                .sender(sender)
                                .build();
                        notificationRepository.save(notification);
                } else {
                        if (status == DocumentModerationStatus.REJECTED) {
                                existingDocument.setRejectedReason(request.getRejectedReason());
                                Notification notification = Notification.builder()
                                        .content("Document \"" + existingDocument.getTitle() + "\" has been rejected")
                                        .createdAt(LocalDateTime.now())
                                        .isRead(false)
                                        .link("/documents/" + existingDocument.getSlug())
                                        .targetId(existingDocument.getId())
                                        .type(NotificationType.REJECTED)
                                        .user(existingDocument.getAuthor())
                                        .sender(sender)
                                        .build();
                                notificationRepository.save(notification);
                        } else {
                                existingDocument.setRejectedReason(null);
                        }
                }
                return DocumentMapper.toDocumentResponse(documentRepository.save(existingDocument));
        }

        @Override
        @Transactional
        public void deleteDocument(Long id) {
                Document document = documentRepository.findById(id)
                        .orElseThrow(() -> new EntityNotFoundException("Document not found with id: " + id));
            
                String filePath = document.getFilePath();
                documentRepository.delete(document);
                
                boolean stillUsed = documentRepository.existsByFilePath(filePath);
                if (!stillUsed) {
                        try {
                                String fileId = googleDriveService.extractFileIdFromUrl(filePath);
                                googleDriveService.deleteFile(fileId); // xóa trên Drive
                        } catch (Exception e) {
                                System.err.println("Failed to delete file from Google Drive: " + e.getMessage());
                        }
                }
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

        @Override
        @Transactional(readOnly = true)
        public List<TopDocumentReportResponse> getTopDocumentsBetween(LocalDate fromDate, LocalDate toDate, int top) {
                List<Object[]> results = documentRepository.findTopDocumentsBetweenDates(fromDate, toDate, top);

                return results.stream()
                                .map((Object[] row) -> TopDocumentReportResponse.builder()
                                .id(((Number) row[0]).longValue())
                                .title((String) row[1])
                                .description((String) row[2]) 
                                .fileType((String) row[3])
                                .slug((String) row[4]) 
                                .price(row[5] != null ? ((Number) row[5]).doubleValue() : null)
                                .createdAt(row[6] != null ? ((Timestamp) row[6]).toLocalDateTime() : null)
                                .authorName((String) row[7])
                                .category((String) row[8])
                                .viewCount(((Number) row[9]).longValue())
                                .saveCount(((Number) row[10]).longValue())
                                .relatedPostCount(((Number) row[11]).longValue())
                                .relatedCommentCount(((Number) row[12]).longValue())
                                .totalInteraction(((Number) row[13]).longValue())
                                .build()
                        )
                        .collect(Collectors.toList());
        }

        @Override
        public List<TopUserAddDocumentResponse> getTopUsersAddDocumentBetween(LocalDate from, LocalDate to, int top) {
                List<Object[]> results = documentRepository.findTopUsersAddDocumentBetweenDates(from, to, top);
                List<TopUserAddDocumentResponse> responseList = new ArrayList<>();
                for (Object[] row : results) {
                        Long userId = row[0] != null ? ((Number) row[0]).longValue() : null;
                        String userName = row[1] != null ? row[1].toString() : null;
                        Long documentCount = row[2] != null ? ((Number) row[2]).longValue() : 0L;
                        responseList.add(new TopUserAddDocumentResponse(userId, userName, Math.toIntExact(documentCount)));
                }
                return responseList;
        }

}
