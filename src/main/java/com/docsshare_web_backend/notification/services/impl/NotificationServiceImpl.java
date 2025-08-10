package com.docsshare_web_backend.notification.services.impl;

import com.docsshare_web_backend.documents.models.Document;
import com.docsshare_web_backend.documents.repositories.DocumentRepository;
import com.docsshare_web_backend.forum_posts.models.ForumPost;
import com.docsshare_web_backend.forum_posts.repositories.ForumPostRepository;
import com.docsshare_web_backend.notification.dto.requests.NotificationFilterRequest;
import com.docsshare_web_backend.notification.dto.requests.NotificationShareRequest;
import com.docsshare_web_backend.notification.dto.responses.NotificationResponse;
import com.docsshare_web_backend.notification.enums.NotificationType;
import com.docsshare_web_backend.notification.filters.NotificationFilter;
import com.docsshare_web_backend.notification.models.Notification;
import com.docsshare_web_backend.notification.repositories.NotificationRepository;
import com.docsshare_web_backend.notification.services.NotificationService;
import com.docsshare_web_backend.users.models.User;
import com.docsshare_web_backend.users.repositories.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class NotificationServiceImpl implements NotificationService {
    @Autowired
    private NotificationRepository notificationRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private DocumentRepository documentRepository;
    @Autowired
    private ForumPostRepository forumPostRepository;

    private Pageable getPageable(Pageable pageable) {
        return pageable != null ? pageable : Pageable.unpaged();
    }

    public static class NotificationMapper {
        public static NotificationResponse toNotificationResponse(Notification notification) {
            return NotificationResponse.builder()
                    .id(notification.getId())
                    .content(notification.getContent())
                    .type(notification.getType())
                    .isRead(notification.isRead())
                    .link(notification.getLink())
                    .targetId(notification.getTargetId())
                    .createdAt(notification.getCreatedAt())
                    .senderName(notification.getUser().getName())
                    .senderId(notification.getSender().getId())
                    .userId(notification.getUser().getId())
                    .build();
        }
    }
    private Notification buildNotification(User receiver, User sender, String content, String link, Long targetId, NotificationType type) {
        Notification notification = new Notification();
        notification.setUser(receiver);
        notification.setSender(sender);
        notification.setContent(content);
        notification.setLink(link);
        notification.setTargetId(targetId);
        notification.setType(type);
        notification.setRead(false);
        notification.setCreatedAt(java.time.LocalDateTime.now());
        return notification;
    }
    
    private User getUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found with ID " + userId));
    }

    private Document getDocument(Long id) {
        return documentRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Document not found with ID " + id));
    }

    private ForumPost getForumPost(Long id) {
        return forumPostRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Forum post not found with ID " + id));
    }

    @Override
    public void shareContent(NotificationShareRequest request) {
        User sender = getUser(request.getSenderId());
    
        Long targetId = request.getTargetId();
        NotificationType type = request.getType();
    
        String content;
        String link;
    
        if (type == NotificationType.DOCUMENT_SHARE) {
            Document doc = getDocument(targetId);
            content = sender.getName() + " has shared a document: " + doc.getTitle();
            link = "/documents/" + doc.getSlug();
    
        } else if (type == NotificationType.POST_SHARE) {
            ForumPost post = getForumPost(targetId);
            content = sender.getName() + " has shared a forum post: " + post.getTitle();
            link = "/forum/" + post.getId();
    
        } else {
            throw new IllegalArgumentException("Unsupported NotificationType: " + type);
        }
    
        List<Notification> notifications = request.getReceiverIds().stream().map(receiverId -> {
            User receiver = getUser(receiverId);
            return buildNotification(receiver, sender, content, link, targetId, type);
        }).collect(Collectors.toList());
    
        notificationRepository.saveAll(notifications);
    }
     
    @Override
    public Page<NotificationResponse> getNotificationsByUserId(NotificationFilterRequest request, long userId, Pageable pageable) {
        userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException(
                "User not found with id: " + userId));
        Specification<Notification> spec = Specification
                .<Notification>where((root, query, cb) -> cb.equal(root.get("user").get("id"), userId))
                .and(NotificationFilter.filterByRequest(request));
        return notificationRepository.findAll(spec, pageable).map(NotificationMapper::toNotificationResponse);
    }
}
