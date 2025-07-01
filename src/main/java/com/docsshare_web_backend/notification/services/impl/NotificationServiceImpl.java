package com.docsshare_web_backend.notification.services.impl;

import com.docsshare_web_backend.categories.repositories.CategoryRepository;
import com.docsshare_web_backend.notification.dto.requests.NotificationFilterRequest;
import com.docsshare_web_backend.notification.dto.requests.NotificationRequest;
import com.docsshare_web_backend.notification.dto.responses.NotificationResponse;
import com.docsshare_web_backend.notification.enums.NotificationType;
import com.docsshare_web_backend.notification.filters.NotificationFilter;
import com.docsshare_web_backend.notification.models.Notification;
import com.docsshare_web_backend.notification.repositories.NotificationRepository;
import com.docsshare_web_backend.notification.services.NotificationService;
import com.docsshare_web_backend.users.repositories.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
public class NotificationServiceImpl implements NotificationService {
//        @Autowired
//        private NotificationRepository notificationRepository;
//
//        @Autowired
//        private UserRepository userRepository;
//
//        @Autowired
//        private CategoryRepository categoryRepository;
//
//        private Pageable getPageable(Pageable pageable) {
//                return pageable != null ? pageable : Pageable.unpaged();
//        }
//
//        public static class DocumentMapper {
//                public static NotificationResponse toDocumentResponse(Notification notification) {
//                        return NotificationResponse.builder()
//                                        .id(notification.getId())
//                                        .title(notification.getTitle())
//                                        .description(notification.getDescription())
//                                        .filePath(notification.getFilePath())
//                                        .slug(notification.getSlug())
//                                        .price(notification.getPrice())
//                                        .copyrightPath(notification.getCopyrightPath())
//                                        .moderationStatus(
//                                                        notification.getModerationStatus() != null
//                                                                        ? notification.getModerationStatus().toString()
//                                                                        : null)
//                                        .isPublic(notification.isPublic())
//                                        .coAuthor(notification.getCoAuthor() != null ? notification.getCoAuthor().toString()
//                                                        : null)
//                                        .createdAt(notification.getCreatedAt())
//                                        .authorName(notification.getAuthor() != null ? notification.getAuthor().getName() : "")
//                                        .category(notification.getCategory() != null ? notification.getCategory().getName()
//                                                        : "")
//                                        .build();
//                }
//        }
//
//        @Override
//        @Transactional(readOnly = true)
//        public Page<NotificationResponse> getAllDocuments(NotificationFilterRequest request, Pageable pageable) {
//                Specification<Notification> spec = NotificationFilter.filterByRequest(request);
//                return notificationRepository.findAll(spec, getPageable(pageable))
//                                .map(DocumentMapper::toDocumentResponse);
//        }
//
//        @Override
//        @Transactional(readOnly = true)
//        public NotificationResponse getAccountById(long id) {
//                Notification notification = notificationRepository.findById(id)
//                                .orElseThrow(() -> new EntityNotFoundException("Document not found with id: " + id));
//
//                return DocumentMapper.toDocumentResponse(notification);
//        }
//
//        @Override
//        @Transactional(readOnly = true)
//        public NotificationResponse getDocumentBySlug(String slug) {
//                if (slug == null || slug.trim().isEmpty()) {
//                        throw new IllegalArgumentException("Slug cannot be null or empty");
//                }
//                Notification notification = notificationRepository.findBySlug(slug)
//                                .orElseThrow(() -> new EntityNotFoundException(
//                                                "Document not found with slug: " + slug));
//
//                return DocumentMapper.toDocumentResponse(notification);
//        }
//
//        @Override
//        @Transactional(readOnly = true)
//        public Page<NotificationResponse> getDocumentsByUserId(NotificationFilterRequest request, long userId,
//                                                               Pageable pageable) {
//                userRepository.findById(userId)
//                                .orElseThrow(() -> new EntityNotFoundException(
//                                                "User not found with id: " + userId));
//                Specification<Notification> spec = Specification
//                                .<Notification>where((root, query, cb) -> cb.equal(root.get("user").get("id"), userId))
//                                .and(NotificationFilter.filterByRequest(request));
//
//                return notificationRepository.findAll(spec, pageable)
//                                .map(DocumentMapper::toDocumentResponse);
//        }
//
//        @Override
//        @Transactional(readOnly = true)
//        public Page<NotificationResponse> getDocumentsByCategoryId(NotificationFilterRequest request, long categoryId,
//                                                                   Pageable pageable) {
//                categoryRepository.findById(categoryId)
//                                .orElseThrow(() -> new EntityNotFoundException(
//                                                "Category not found with id: " + categoryId));
//                Specification<Notification> spec = Specification
//                                .<Notification>where((root, query, cb) -> cb.equal(root.get("category").get("id"),
//                                                categoryId))
//                                .and(NotificationFilter.filterByRequest(request));
//
//                return notificationRepository.findAll(spec, pageable)
//                                .map(DocumentMapper::toDocumentResponse);
//        }
//
//        @Override
//        @Transactional(readOnly = true)
//        public Page<NotificationResponse> getDocumentsNeedApproved(NotificationFilterRequest request, Pageable pageable) {
//                Specification<Notification> spec = Specification
//                                .<Notification>where((root, query, cb) -> cb.equal(root.get("moderationStatus"),
//                                                NotificationType.COMMENT))
//                                .and((root, query, cb) -> cb.isTrue(root.get("isPublic")));
//
//                return notificationRepository.findAll(spec, pageable)
//                                .map(DocumentMapper::toDocumentResponse);
//        }
//
//        @Override
//        @Transactional
//        public NotificationResponse createDocument(NotificationRequest request) {
//                var author = userRepository.findById(request.getUserId())
//                                .orElseThrow(() -> new EntityNotFoundException(
//                                                "User not found with id: " + request.getUserId()));
//
//                var category = categoryRepository.findById(request.getCategoryId())
//                                .orElseThrow(
//                                                () -> new EntityNotFoundException("Category not found with id: "
//                                                                + request.getCategoryId()));
//
//                Notification notification = Notification.builder()
//                                .title(request.getTitle())
//                                .description(request.getDesciption())
//                                .filePath(request.getFilePath())
//                                .slug(request.getSlug())
//                                .price(request.getPrice())
//                                .copyrightPath(request.getCopyrightPath())
//                                .moderationStatus(NotificationType.COMMENT)
//                                .isPublic(request.isPublic())
//                                .coAuthor(request.getCoAuthor())
//                                .author(author)
//                                .category(category)
//                                .build();
//
//                Notification savedNotification = notificationRepository.save(notification);
//
//                return DocumentMapper.toDocumentResponse(savedNotification);
//        }
//
//        @Override
//        @Transactional
//        public NotificationResponse updateDocument(long documentId, NotificationRequest request) {
//                Notification existingNotification = notificationRepository.findById(documentId)
//                                .orElseThrow(() -> new EntityNotFoundException(
//                                                "Document not found with id: " + documentId));
//
//                if (existingNotification.getModerationStatus() == NotificationType.APPROVED ||
//                                existingNotification.getModerationStatus() == NotificationType.REJECTED) {
//                        existingNotification.setModerationStatus(NotificationType.COMMENT);
//                }
//
//                existingNotification.setTitle(request.getTitle());
//                existingNotification.setDescription(request.getDesciption());
//                existingNotification.setFilePath(request.getFilePath());
//                existingNotification.setSlug(request.getSlug());
//                existingNotification.setPrice(request.getPrice());
//                existingNotification.setCopyrightPath(request.getCopyrightPath());
//                existingNotification.setCoAuthor(request.getCoAuthor());
//                existingNotification.setPublic(request.isPublic());
//
//                Notification updatedNotification = notificationRepository.save(existingNotification);
//                return DocumentMapper.toDocumentResponse(updatedNotification);
//        }
//
//        @Override
//        @Transactional
//        public NotificationResponse updateDocumentStatus(long id, NotificationType status) {
//                Notification existingNotification = notificationRepository.findById(id)
//                                .orElseThrow(() -> new EntityNotFoundException("Document not found with id: " + id));
//                existingNotification.setModerationStatus(status);
//                if (status == NotificationType.APPROVED) {
//                        existingNotification.setPublic(true);
//                } else {
//                        existingNotification.setPublic(false);
//                }
//                return DocumentMapper.toDocumentResponse(notificationRepository.save(existingNotification));
//        }

}
