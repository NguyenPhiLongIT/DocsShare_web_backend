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
    OrderResponse createOrder(OrderRequest request);
//    OrderResponse getOrderBySlug(String slug);
//    Page<OrderResponse> getOrderByUserId(OrderFilterRequest request, long userId, Pageable pageable);

//    OrderResponse updateOrder(long documentId, OrderRequest request);
    OrderResponse updateOrderStatus(long id, OrderStatus status);
}
