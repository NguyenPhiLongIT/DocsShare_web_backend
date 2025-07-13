package com.docsshare_web_backend.payment.dto.requests;

import com.docsshare_web_backend.payment.enums.PaymentMethod;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentRequest {

    @NotNull
    private String orderId;

    @NotNull
    private PaymentMethod paymentMethod;

    @NotNull
    private Long amount;
}
