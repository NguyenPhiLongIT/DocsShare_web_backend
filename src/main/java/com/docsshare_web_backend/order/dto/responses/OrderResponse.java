package com.docsshare_web_backend.order.dto.responses;

import com.docsshare_web_backend.order.enums.OrderStatus;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderResponse {
    private Long id;
    private OrderStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Long userId;
    private Long paymentId;
    private Double commissionRate;

    private List<OrderDetailResponse> items; // Danh sách các tài liệu trong đơn hàng
}
