package com.docsshare_web_backend.payment.services.impl;

import com.docsshare_web_backend.order.models.Order;
import com.docsshare_web_backend.order.repositories.OrderRepository;
import com.docsshare_web_backend.payment.dto.requests.PaymentFilterRequest;
import com.docsshare_web_backend.payment.dto.requests.PaymentRequest;
import com.docsshare_web_backend.payment.dto.responses.PaymentResponse;
import com.docsshare_web_backend.payment.dto.responses.TopPaymentSuccessResponse;
import com.docsshare_web_backend.payment.enums.PaymentStatus;
import com.docsshare_web_backend.payment.filters.PaymentFilter;
import com.docsshare_web_backend.payment.models.Payment;
import com.docsshare_web_backend.payment.repositories.PaymentRepository;
import com.docsshare_web_backend.payment.services.PaymentService;
import jakarta.persistence.EntityNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

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
    @Override
    public List<TopPaymentSuccessResponse> getTopSuccessPayments(int top) {
        List<Object[]> results = paymentRepository.findTopSuccessPayments(top);
        List<TopPaymentSuccessResponse> responseList = new ArrayList<>();
        for (Object[] row : results) {
            Long paymentId = row[0] != null ? ((Number) row[0]).longValue() : null;
            Integer amount = row[1] != null ? ((Number) row[1]).intValue() : null;
            String transactionId = row[2] != null ? row[2].toString() : null;
            String paymentMethod = row[3] != null ? row[3].toString() : null;
            String order_id = row[4] != null ? row[4].toString() : null;
            responseList.add(new TopPaymentSuccessResponse(paymentId, amount, transactionId, paymentMethod, order_id));
        }
        return responseList;
    }

}
