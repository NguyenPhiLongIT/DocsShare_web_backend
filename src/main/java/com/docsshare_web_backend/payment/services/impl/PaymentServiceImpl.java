package com.docsshare_web_backend.payment.services.impl;

import com.docsshare_web_backend.order.models.Order;
import com.docsshare_web_backend.order.repositories.OrderRepository;
import com.docsshare_web_backend.payment.dto.requests.PaymentFilterRequest;
import com.docsshare_web_backend.payment.dto.requests.PaymentRequest;
import com.docsshare_web_backend.payment.dto.responses.PaymentResponse;
import com.docsshare_web_backend.payment.enums.PaymentStatus;
import com.docsshare_web_backend.payment.enums.PaymentMethod;
import com.docsshare_web_backend.payment.filters.PaymentFilter;
import com.docsshare_web_backend.payment.models.Payment;
import com.docsshare_web_backend.payment.repositories.PaymentRepository;
import com.docsshare_web_backend.payment.services.PaymentService;
import com.docsshare_web_backend.users.models.User;
import com.docsshare_web_backend.users.repositories.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.crossstore.ChangeSetPersister;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
public class PaymentServiceImpl implements PaymentService {

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private OrderRepository orderRepository; // cần thêm repo này

    private Pageable getPageable(Pageable pageable) {
        return pageable != null ? pageable : Pageable.unpaged();
    }

    public static class PaymentMapper {
        public static PaymentResponse toPaymentResponse(Payment payment) {
            return PaymentResponse.builder()
                    .id(payment.getId())
                    .orderId(payment.getOrder().getId())
                    .amount(Long.valueOf(payment.getAmount()))
                    .transactionId(payment.getTransactionId())
                    .status(payment.getStatus())
                    .paymentMethod(payment.getPaymentMethod())
                    .createdAt(payment.getCreatedAt())
                    .build();
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Page<PaymentResponse> getAllPayments(PaymentFilterRequest request, Pageable pageable) {
        Specification<Payment> spec = PaymentFilter.filterByRequest(request);
        return paymentRepository.findAll(spec,getPageable(pageable)).map(PaymentMapper::toPaymentResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public PaymentResponse getPaymentById(Long id) {
        Payment payment = paymentRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Payment not found with id: " + id));
        return PaymentMapper.toPaymentResponse(payment);
    }


    @Override
    @Transactional
    public PaymentResponse createPayment(PaymentRequest request) {
        // 1. Lấy đơn hàng từ orderId
        Order order = orderRepository.findById(Long.valueOf(request.getOrderId()))
                .orElseThrow(() -> new EntityNotFoundException("Order not found with id: " + request.getOrderId()));

        // 2. Tạo payment
        Payment payment = Payment.builder()
                .amount(Math.toIntExact(request.getAmount()))
                .paymentMethod(request.getPaymentMethod())
                .status(PaymentStatus.PENDING) // nếu muốn set mặc định
                .order(order) // liên kết với Order
                .build();
        log.info("Payment method received: {}", request.getPaymentMethod());

        order.setPayment(payment);
        orderRepository.save(order);

        // 3. Lưu
        Payment saved = paymentRepository.save(payment);

        // 4. Trả về DTO
        return PaymentMapper.toPaymentResponse(saved);
    }


}
