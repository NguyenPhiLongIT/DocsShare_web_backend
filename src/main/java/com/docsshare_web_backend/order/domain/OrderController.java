package com.docsshare_web_backend.order.domain;

import com.docsshare_web_backend.order.dto.requests.OrderFilterRequest;
import com.docsshare_web_backend.order.dto.requests.OrderRequest;
import com.docsshare_web_backend.order.dto.responses.OrderResponse;
import com.docsshare_web_backend.order.enums.OrderStatus;
import com.docsshare_web_backend.order.services.OrderService;
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
@RequestMapping("/api/v1/order")
public class OrderController {
    @Autowired
    private OrderService orderService;

    @GetMapping
    public ResponseEntity<Page<OrderResponse>> getAllOrder(
            @ModelAttribute OrderFilterRequest request,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "9") int size,
            @RequestParam(defaultValue = "desc") String sort) {
        log.debug("Received request to get all order with filter: {}, page: {}, size: {}, sort: {}",
                request, page, size, sort);
        Sort sortOrder = sort.equals("asc") ? Sort.by("createdAt").ascending() : Sort.by("createdAt").descending();
        Pageable pageable = PageRequest.of(page, size, sortOrder);
        Page<OrderResponse> orders = orderService.getAllOrder(request, pageable);
        return ResponseEntity.ok(orders);
    }

    @GetMapping("/{orderId}")
    public ResponseEntity<OrderResponse> getOrder(@PathVariable long orderId) {
        log.debug("[OrderController] Get Order with id {}", orderId);
        return ResponseEntity.ok(orderService.getOrder(orderId));
    }
//
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

    @PostMapping("/create")
    public ResponseEntity<OrderResponse> createOrder(@RequestBody OrderRequest orderRequest) {
        log.debug("[OrderController] Create Order {}", orderRequest);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(orderService.createOrder(orderRequest));
    }

//    @PutMapping("/{orderId}/update")
//    public ResponseEntity<OrderResponse> updateDocument(
//            @PathVariable long documentId,
//            @RequestBody OrderRequest request) {
//        log.debug("[DocumentController] Update Document with id {}", documentId);
//        return ResponseEntity.ok(orderService.updateDocument(documentId, request));
//    }
//
    @PutMapping("/{orderId}/updateStatus")
    public ResponseEntity<OrderResponse> updateOrderStatus(@PathVariable long orderId, OrderStatus status){
        log.debug("[OrderController] Update moderationStatus in Order with id {}", orderId);
        return ResponseEntity.ok(orderService.updateOrderStatus(orderId, status));
    }
}
