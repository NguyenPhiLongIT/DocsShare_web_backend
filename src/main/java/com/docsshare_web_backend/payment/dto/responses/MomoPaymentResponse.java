package com.docsshare_web_backend.payment.dto.responses;

import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class MomoPaymentResponse {
    private int resultCode;
    private String message;
    private String payUrl;     // URL web thanh toán
    private String deeplink;   // deep link mở app MoMo
    private String qrCodeUrl;  // QR code nếu muốn quét
    private String orderId;
    private String requestId;

}
