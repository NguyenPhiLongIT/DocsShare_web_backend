package com.docsshare_web_backend.payment.domain;

import com.docsshare_web_backend.payment.dto.requests.PaymentFilterRequest;
import com.docsshare_web_backend.payment.dto.responses.PaymentResponse;
import com.docsshare_web_backend.payment.services.PaymentService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/v1/payment")
public class PaymentController {
//    @Autowired
//    private PaymentService paymentService;
//
//    @GetMapping
//    public ResponseEntity<Page<PaymentResponse>> getAllPayments(
//            @ModelAttribute PaymentFilterRequest request,
//            @RequestParam(defaultValue = "0") int page,
//            @RequestParam(defaultValue = "9") int size,
//            @RequestParam(defaultValue = "desc") String sort) {
//        log.debug("Received request to get all documents with filter: {}, page: {}, size: {}, sort: {}",
//                request, page, size, sort);
//        Sort sortOrder = sort.equals("asc") ? Sort.by("createdAt").ascending() : Sort.by("createdAt").descending();
//        Pageable pageable = PageRequest.of(page, size, sortOrder);
//        Page<PaymentResponse> documents = paymentService.getAllDocuments(request, pageable);
//        return ResponseEntity.ok(documents);
//    }
//
//    @GetMapping("/{paymentId}")
//    public ResponseEntity<PaymentResponse> getDocument(@PathVariable long documentId) {
//        log.debug("[DocumentController] Get Document with id {}", documentId);
//        return ResponseEntity.ok(paymentService.getDocument(documentId));
//    }

//    @GetMapping("/slug/{slug}")
//    public ResponseEntity<OrderResponse> getDocumentBySlug(@PathVariable String slug) {
//        log.debug("[DocumentController] Get Document by slug {}", slug);
//        return ResponseEntity.ok(orderService.getDocumentBySlug(slug));
//    }
//
//    @GetMapping("/category/{categoryId}")
//    public ResponseEntity<Page<OrderResponse>> getDocumentsByCategoryId(
//            @PathVariable long categoryId,
//            @ModelAttribute OrderFilterRequest request,
//            @RequestParam(defaultValue = "0") int page,
//            @RequestParam(defaultValue = "9") int size,
//            @RequestParam(defaultValue = "desc") String sort) {
//
//        Sort sortOrder = sort.equals("asc") ? Sort.by("createdAt").ascending() : Sort.by("createdAt").descending();
//        Pageable pageable = PageRequest.of(page, size, sortOrder);
//        Page<OrderResponse> documents = orderService.getDocumentsByCategoryId(request, categoryId, pageable);
//        return ResponseEntity.ok(documents);
//    }
//
//    @GetMapping("/need-approved")
//    public ResponseEntity<Page<OrderResponse>> getDocumentsNeedApproved(
//            @ModelAttribute OrderFilterRequest request,
//            @RequestParam(defaultValue = "0") int page,
//            @RequestParam(defaultValue = "9") int size,
//            @RequestParam(defaultValue = "desc") String sort){
//
//        Sort sortOrder = sort.equals("asc") ? Sort.by("createdAt").ascending() : Sort.by("createdAt").descending();
//        Pageable pageable = PageRequest.of(page, size, sortOrder);
//        Page<OrderResponse> documents = orderService.getDocumentsNeedApproved(request, pageable);
//        return ResponseEntity.ok(documents);
//    }
//
//    @PostMapping("/create")
//    public ResponseEntity<OrderResponse> createDocument(@RequestBody OrderRequest orderRequest) {
//        log.debug("[DocumentController] Create Document {}", orderRequest);
//        return ResponseEntity.status(HttpStatus.CREATED)
//                .body(orderService.createDocument(orderRequest));
//    }
//
//    @PutMapping("/{documentId}/update")
//    public ResponseEntity<OrderResponse> updateDocument(
//            @PathVariable long documentId,
//            @RequestBody OrderRequest request) {
//        log.debug("[DocumentController] Update Document with id {}", documentId);
//        return ResponseEntity.ok(orderService.updateDocument(documentId, request));
//    }
//
//    @PutMapping("/{documentId}/updateStatus")
//    public ResponseEntity<OrderResponse> updateDocumentStatus(@PathVariable long documentId, PaymentStatus status){
//        log.debug("[DocumentController] Update moderationStatus in Document with id {}", documentId);
//        return ResponseEntity.ok(orderService.updateDocumentStatus(documentId, status));
//    }
}
