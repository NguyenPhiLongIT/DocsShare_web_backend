package com.docsshare_web_backend.payment.domain;

import com.docsshare_web_backend.payment.dto.requests.MomoIpnRequest;
import com.docsshare_web_backend.payment.dto.requests.PaymentFilterRequest;
import com.docsshare_web_backend.payment.dto.requests.PaymentRequest;
import com.docsshare_web_backend.payment.dto.responses.MomoPaymentResponse;
import com.docsshare_web_backend.payment.dto.responses.PaymentResponse;
import com.docsshare_web_backend.payment.dto.responses.TopPaymentSuccessResponse;
import com.docsshare_web_backend.payment.enums.PaymentStatus;
import com.docsshare_web_backend.payment.services.MomoPaymentService;
import com.docsshare_web_backend.payment.services.PaymentService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/v1/payment")
public class PaymentController {
    @Autowired
    private PaymentService paymentService;

    @Autowired
    private MomoPaymentService momoPaymentService;

    @GetMapping
    public ResponseEntity<Page<PaymentResponse>> getAllPayments(
            @ModelAttribute PaymentFilterRequest request,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "9") int size,
            @RequestParam(defaultValue = "desc") String sort
    ){
        log.debug("Received request to get all payments with filter: {} , page: {}, size: {}, sort: {}",
                request, page, size, sort);
        Sort sortPayment = sort.equals("asc") ? Sort.by("createdAt").ascending() : Sort.by("createdAt").descending();
        Pageable pageable = PageRequest.of(page, size, sortPayment);
        Page<PaymentResponse> payments = paymentService.getAllPayments(request, pageable);
        return ResponseEntity.ok(payments);
    }

    @GetMapping("/{paymentId}")
    public ResponseEntity<PaymentResponse> getPaymentById(@PathVariable("paymentId") Long paymentId) {
        log.debug("Received request to get payment with ID: {}", paymentId);
        PaymentResponse payment = paymentService.getPaymentById(paymentId);
        return ResponseEntity.ok(payment);
    }

    @PostMapping("/create")
    public ResponseEntity<PaymentResponse> createDocument(@RequestBody PaymentRequest paymentRequest) {
        log.debug("[PaymentDocument] Create Payment {}", paymentRequest);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(paymentService.createPayment(paymentRequest));
    }

    @GetMapping("/momo/create")
    public ResponseEntity<MomoPaymentResponse> createMomoPayment(
            @RequestParam Long orderId,
            @RequestParam Integer amount) throws Exception {
        log.info("[MoMo] Create Payment for OrderId: {}, Amount: {}", orderId, amount);
        MomoPaymentResponse response = momoPaymentService.createPayment(orderId, amount);
        return ResponseEntity.ok(response);
    }

    // IPN callback từ MoMo
    @PostMapping("/momo/ipn")
    public ResponseEntity<String> handleMomoIpn(@RequestBody MomoIpnRequest ipnRequest) {
        log.info("[MoMo] IPN Callback: {}", ipnRequest);
        if (ipnRequest.getResultCode() == 0) {
            momoPaymentService.updatePaymentStatus(ipnRequest.getOrderId(), PaymentStatus.SUCCESS);
            return ResponseEntity.ok("IPN received - SUCCESS");
        } else {
            momoPaymentService.updatePaymentStatus(ipnRequest.getOrderId(), PaymentStatus.FAILED);
            return ResponseEntity.ok("IPN received - FAILED");
        }
    }

    // Redirect user sau khi thanh toán (chỉ cho Web)
    @GetMapping("/momo/return")
    public ResponseEntity<String> paymentReturn(@RequestParam String orderId, @RequestParam int resultCode) {
        log.debug("[MoMo] Return URL Callback - OrderId: {}, ResultCode: {}", orderId, resultCode);
        if (resultCode == 0) {
            return ResponseEntity.ok("Thanh toán thành công cho OrderId: " + orderId);
        } else {
            return ResponseEntity.ok("Thanh toán thất bại cho OrderId: " + orderId);
        }
    }
    @GetMapping("/top-success-payments")
    public ResponseEntity<List<TopPaymentSuccessResponse>> getTopSuccessPayments(
            @RequestParam(defaultValue = "10") int top) {
        List<TopPaymentSuccessResponse> result = paymentService.getTopSuccessPayments(top);
        return ResponseEntity.ok(result);
    }
}
