package com.docsshare_web_backend.order.services;

import com.docsshare_web_backend.order.dto.requests.OrderFilterRequest;
import com.docsshare_web_backend.order.dto.requests.OrderRequest;
import com.docsshare_web_backend.order.dto.responses.OrderResponse;
import com.docsshare_web_backend.order.enums.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.PathVariable;

@Service
public interface OrderService {
    Page<OrderResponse> getAllOrder(OrderFilterRequest request, Pageable pageable);
    OrderResponse getOrder(long id);
    Page<OrderResponse> getOrderByUserId(Long userId, Pageable pageable);
    Page<OrderResponse> getOrderByAuthorId(Long userId, Pageable pageable);
    OrderResponse createOrder(OrderRequest request);
    boolean hasUserPaidForDocument(Long userId, Long documentId);
    OrderResponse updateOrderStatus(long id, OrderStatus status);
}
