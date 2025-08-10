package com.docsshare_web_backend.payment.services.impl;

import com.docsshare_web_backend.order.models.Order;
import com.docsshare_web_backend.order.repositories.OrderRepository;
import com.docsshare_web_backend.payment.dto.requests.MomoPaymentRequest;
import com.docsshare_web_backend.payment.dto.responses.MomoPaymentResponse;
import com.docsshare_web_backend.payment.enums.PaymentMethod;
import com.docsshare_web_backend.payment.enums.PaymentStatus;
import com.docsshare_web_backend.payment.models.Payment;
import com.docsshare_web_backend.payment.repositories.PaymentRepository;
import com.docsshare_web_backend.payment.services.MomoPaymentService;
import com.docsshare_web_backend.payment.utils.HmacSHA256;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import com.docsshare_web_backend.order.enums.OrderStatus;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
public class MomoPaymentServiceImpl implements MomoPaymentService {

    @Value("${momo.partner-code}")
    private String partnerCode;

    @Value("${momo.access-key}")
    private String accessKey;

    @Value("${momo.secret-key}")
    private String secretKey;

    @Value("${momo.create-endpoint}")
    private String createEndpoint;

    @Value("${momo.return-url}")
    private String returnUrl;

    @Value("${momo.ipn-url}")
    private String ipnUrl;

    @Value("${momo.request-type}")
    private String requestType;

    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;

    public MomoPaymentServiceImpl(PaymentRepository paymentRepository, OrderRepository orderRepository) {
        this.paymentRepository = paymentRepository;
        this.orderRepository = orderRepository;
    }
    
    @Override
    @Transactional
    public MomoPaymentResponse createPayment(Long orderId, Integer amount) throws Exception {
        // ✅ 1. Tạo record Payment (PENDING)
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found with id: " + orderId));

        Payment payment = Payment.builder()
                .amount(amount)
                .paymentMethod(PaymentMethod.MOMO)
                .transactionId(UUID.randomUUID().toString()) // orderId bên MoMo = transactionId của mình
                .status(PaymentStatus.PENDING)
                .order(order) // TODO: nếu có Order, set ở đây
                .build();
        paymentRepository.save(payment);

        order.setPayment(payment);
        orderRepository.save(order);

        // ✅ 2. Tạo chữ ký và request
        String requestId = UUID.randomUUID().toString();
        String extraData = "";
        String orderInfo = "Thanh toán MoMo cho đơn hàng #" + orderId;

        String rawSignature = "accessKey=" + accessKey +
                "&amount=" + amount +
                "&extraData=" + extraData +
                "&ipnUrl=" + ipnUrl +
                "&orderId=" + payment.getTransactionId() +
                "&orderInfo=" + orderInfo +
                "&partnerCode=" + partnerCode +
                "&redirectUrl=" + returnUrl +
                "&requestId=" + requestId +
                "&requestType=" + requestType;

        String signature = HmacSHA256.sign(rawSignature, secretKey);

        MomoPaymentRequest momoRequest = MomoPaymentRequest.builder()
                .partnerCode(partnerCode)
                .partnerName("MoMoTest")
                .requestId(requestId)
                .amount(amount.toString())
                .orderId(payment.getTransactionId())
                .orderInfo(orderInfo)
                .redirectUrl(returnUrl)
                .ipnUrl(ipnUrl)
                .lang("vi")
                .requestType(requestType)
                .extraData(extraData)
                .signature(signature)
                .build();

        RestTemplate restTemplate = new RestTemplate();
        MomoPaymentResponse response = restTemplate.postForObject(createEndpoint, momoRequest, MomoPaymentResponse.class);

        if (response == null || response.getResultCode() != 0) {
            throw new RuntimeException("Tạo giao dịch MoMo thất bại");
        }
        
        log.info("[MoMo] Request: {}", momoRequest);
        log.info("[MoMo] Response: {}", response);
        return response;
    }

    @Override
    @Transactional
    public void updatePaymentStatus(String transactionId, PaymentStatus status) {
        Payment payment = paymentRepository.findByTransactionId(transactionId)
                .orElseThrow(() -> new RuntimeException("Payment not found"));

        payment.setStatus(status);


        // ✅ Update luôn Order
        Order order = payment.getOrder();
        if (status == PaymentStatus.SUCCESS) {
            order.setStatus(OrderStatus.COMPLETED);
        } else if (status == PaymentStatus.FAILED) {
            order.setStatus(OrderStatus.CANCELLED);
        }

        log.info("Payment status updated to: {}", payment.getStatus());
        log.info("Order status updated to: {}", order.getStatus());


        order.setPayment(payment);
        orderRepository.save(order);

        paymentRepository.save(payment);
    }

}