package com.docsshare_web_backend.order.services;

import com.docsshare_web_backend.order.dto.requests.OrderFilterRequest;
import com.docsshare_web_backend.order.dto.requests.OrderRequest;
import com.docsshare_web_backend.order.dto.responses.OrderResponse;
import com.docsshare_web_backend.order.dto.responses.RevenueStatisticResponse;
import com.docsshare_web_backend.order.dto.responses.TopUserOrderCompletedResponse;
import com.docsshare_web_backend.order.enums.OrderStatus;
import com.docsshare_web_backend.order.dto.responses.TopSellerUserResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.PathVariable;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
public interface OrderService {
    Page<OrderResponse> getAllOrder(OrderFilterRequest request, Pageable pageable);
    OrderResponse getOrder(long id);
    Page<OrderResponse> getOrderByUserId(Long userId, Pageable pageable);
    Page<OrderResponse> getOrderByAuthorId(Long userId, Pageable pageable);
    OrderResponse createOrder(OrderRequest request);
    boolean hasAccessToDocument(Long userId, Long documentId);
    OrderResponse updateOrderStatus(long id, OrderStatus status);
    List<TopUserOrderCompletedResponse> getTopUsersWithCompletedOrders(LocalDate from, LocalDate to, int top);
    List<TopSellerUserResponse> getTopSellerUsers(
            LocalDate fromDate,
            LocalDate toDate,
            int top
    );
    BigDecimal getTotalRevenue(
            LocalDate fromDate,
            LocalDate toDate
    );
    List<RevenueStatisticResponse> getRevenueByWeekday(
            LocalDate from,
            LocalDate to
    );
    List<RevenueStatisticResponse> getRevenueByMonth(
            LocalDate from,
            LocalDate to
    );
    List<RevenueStatisticResponse> getRevenueByYear(
            LocalDate from,
            LocalDate to
    );
}
