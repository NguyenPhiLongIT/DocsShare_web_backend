package com.docsshare_web_backend.notification.domain;

import com.docsshare_web_backend.notification.dto.requests.NotificationFilterRequest;
import com.docsshare_web_backend.notification.dto.requests.NotificationRequest;
import com.docsshare_web_backend.notification.dto.responses.NotificationResponse;
import com.docsshare_web_backend.notification.enums.NotificationType;
import com.docsshare_web_backend.notification.services.NotificationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/v1/notification")
public class NotificationController {
//    @Autowired
//    private NotificationService notificationService;
//
//    @GetMapping
//    public ResponseEntity<Page<NotificationResponse>> getAllNotifications(
//            @ModelAttribute NotificationFilterRequest request,
//            @RequestParam(defaultValue = "0") int page,
//            @RequestParam(defaultValue = "9") int size,
//            @RequestParam(defaultValue = "desc") String sort) {
//        log.debug("Received request to get all accounts with filter: {}, page: {}, size: {}, sort: {}",
//                request, page, size, sort);
//        Sort sortOrder = sort.equals("asc") ? Sort.by("createdAt").ascending() : Sort.by("createdAt").descending();
//        Pageable pageable = PageRequest.of(page, size, sortOrder);
//        Page<NotificationResponse> account = notificationService.getAllDocuments(request, pageable);
//        return ResponseEntity.ok(account);
//    }
//
//    @GetMapping("/{notificationId}")
//    public ResponseEntity<NotificationResponse> getAccountById(@PathVariable long accountId) {
//        log.debug("[AccountController] Get Account with id {}", accountId);
//        return ResponseEntity.ok(notificationService.getAccountById(accountId));
//    }

//    @GetMapping("/slug/{slug}")
//    public ResponseEntity<AccountResponse> getDocumentBySlug(@PathVariable String slug) {
//        log.debug("[DocumentController] Get Document by slug {}", slug);
//        return ResponseEntity.ok(accountService.getDocumentBySlug(slug));
//    }
//
//    @GetMapping("/category/{categoryId}")
//    public ResponseEntity<Page<AccountResponse>> getDocumentsByCategoryId(
//            @PathVariable long categoryId,
//            @ModelAttribute AccountFilterRequest request,
//            @RequestParam(defaultValue = "0") int page,
//            @RequestParam(defaultValue = "9") int size,
//            @RequestParam(defaultValue = "desc") String sort) {
//
//        Sort sortOrder = sort.equals("asc") ? Sort.by("createdAt").ascending() : Sort.by("createdAt").descending();
//        Pageable pageable = PageRequest.of(page, size, sortOrder);
//        Page<AccountResponse> documents = accountService.getDocumentsByCategoryId(request, categoryId, pageable);
//        return ResponseEntity.ok(documents);
//    }
//
//    @GetMapping("/need-approved")
//    public ResponseEntity<Page<AccountResponse>> getDocumentsNeedApproved(
//            @ModelAttribute AccountFilterRequest request,
//            @RequestParam(defaultValue = "0") int page,
//            @RequestParam(defaultValue = "9") int size,
//            @RequestParam(defaultValue = "desc") String sort){
//
//        Sort sortOrder = sort.equals("asc") ? Sort.by("createdAt").ascending() : Sort.by("createdAt").descending();
//        Pageable pageable = PageRequest.of(page, size, sortOrder);
//        Page<AccountResponse> documents = accountService.getDocumentsNeedApproved(request, pageable);
//        return ResponseEntity.ok(documents);
//    }
//
//    @PostMapping("/create")
//    public ResponseEntity<NotificationResponse> createDocument(@RequestBody NotificationRequest documentRequest) {
//        log.debug("[DocumentController] Create Document {}", documentRequest);
//        return ResponseEntity.status(HttpStatus.CREATED)
//                .body(notificationService.createDocument(documentRequest));
//    }
//
//    @PutMapping("/{accountId}/update")
//    public ResponseEntity<NotificationResponse> updateDocument(
//            @PathVariable long documentId,
//            @RequestBody NotificationRequest request) {
//        log.debug("[DocumentController] Update Document with id {}", documentId);
//        return ResponseEntity.ok(notificationService.updateDocument(documentId, request));
//    }
//
//    @PutMapping("/{accountId}/updateStatus")
//    public ResponseEntity<NotificationResponse> updateDocumentStatus(@PathVariable long documentId, NotificationType status){
//        log.debug("[DocumentController] Update moderationStatus in Document with id {}", documentId);
//        return ResponseEntity.ok(notificationService.updateDocumentStatus(documentId, status));
//    }
}
