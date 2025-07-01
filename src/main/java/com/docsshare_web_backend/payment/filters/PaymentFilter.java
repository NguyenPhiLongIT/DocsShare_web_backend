package com.docsshare_web_backend.payment.filters;

import com.docsshare_web_backend.commons.filters.CommonFilter;
import com.docsshare_web_backend.payment.dto.requests.PaymentFilterRequest;
import com.docsshare_web_backend.payment.models.Payment;
import org.springframework.data.jpa.domain.Specification;

public class PaymentFilter {
    public static Specification<Payment> filterByRequest(PaymentFilterRequest request) {
        return CommonFilter.filter(request, Payment.class);
    }
}
