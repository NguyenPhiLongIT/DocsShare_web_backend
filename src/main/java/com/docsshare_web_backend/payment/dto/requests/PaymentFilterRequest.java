package com.docsshare_web_backend.payment.dto.requests;

import com.docsshare_web_backend.payment.enums.PaymentStatus;
import lombok.Getter;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

@Setter
@Getter
public class PaymentFilterRequest {
    private String q;
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate createdDate_from;
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate createdDate_to;
    private Boolean isPublic;
    private PaymentStatus moderationStatus;
}
