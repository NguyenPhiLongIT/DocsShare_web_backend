package com.docsshare_web_backend.payment.dto.responses;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TopPaymentSuccessResponse {
    private Long paymentId;
    private Integer amount;
    private String transactionId;
    private String paymentMethod;
    private String order_id; // nếu muốn lấy mã đơn hàng liên quan
}
