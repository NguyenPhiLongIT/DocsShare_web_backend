package com.docsshare_web_backend.payment.services;

import com.docsshare_web_backend.payment.dto.requests.PaymentFilterRequest;
import com.docsshare_web_backend.payment.dto.requests.PaymentRequest;
import com.docsshare_web_backend.payment.dto.responses.PaymentResponse;
import com.docsshare_web_backend.payment.dto.responses.TopPaymentSuccessResponse;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;

import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

@Service
public interface PaymentService {
   PaymentResponse createPayment(PaymentRequest paymentRequest);
   Page<PaymentResponse> getAllPayments(PaymentFilterRequest request,Pageable pageable);
   PaymentResponse getPaymentById(@PathVariable("paymentId") Long paymentId);
   List<TopPaymentSuccessResponse> getTopSuccessPayments(int top);

}
