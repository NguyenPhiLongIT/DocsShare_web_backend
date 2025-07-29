package com.docsshare_web_backend.payment.dto.requests;

import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class MomoIpnRequest {
    private String partnerCode;
    private String orderId;
    private String requestId;
    private long amount;
    private String orderInfo;
    private int resultCode;
    private String message;
    private String extraData;
    private String signature;
}