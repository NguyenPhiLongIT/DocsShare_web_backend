package com.docsshare_web_backend.payment.services;

import com.docsshare_web_backend.payment.dto.requests.PaymentFilterRequest;
import com.docsshare_web_backend.payment.dto.requests.PaymentRequest;
import com.docsshare_web_backend.payment.dto.responses.PaymentResponse;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;

import java.awt.print.Pageable;

@Service
public interface PaymentService {
   PaymentResponse createPayment(PaymentRequest paymentRequest);
}
