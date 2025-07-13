package com.docsshare_web_backend.order.dto.requests;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderRequest {
    private Long userId;

    private Long paymentId;

    private Double commissionRate;

    @NotEmpty(message = "Order must have at least one item")
    private List<OrderItemRequest> items;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class OrderItemRequest {
        @NotNull(message = "Document ID is required")
        private Long documentId;

        @NotNull(message = "Price is required")
        private Integer price; // Giá tại thời điểm mua
    }
}
