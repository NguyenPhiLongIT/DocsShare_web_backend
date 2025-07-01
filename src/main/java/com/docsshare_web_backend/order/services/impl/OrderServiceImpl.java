package com.docsshare_web_backend.order.services.impl;

import com.docsshare_web_backend.categories.repositories.CategoryRepository;
import com.docsshare_web_backend.documents.models.Document;
import com.docsshare_web_backend.documents.repositories.DocumentRepository;
import com.docsshare_web_backend.order.dto.requests.OrderFilterRequest;
import com.docsshare_web_backend.order.dto.requests.OrderRequest;
import com.docsshare_web_backend.order.dto.responses.OrderDetailResponse;
import com.docsshare_web_backend.order.dto.responses.OrderResponse;
import com.docsshare_web_backend.order.enums.OrderStatus;
import com.docsshare_web_backend.order.filters.OrderFilter;
import com.docsshare_web_backend.order.models.Order;
import com.docsshare_web_backend.order.models.OrderDetail;
import com.docsshare_web_backend.order.repositories.OrderDetailRepository;
import com.docsshare_web_backend.order.repositories.OrderRepository;
import com.docsshare_web_backend.order.services.OrderService;
import com.docsshare_web_backend.payment.models.Payment;
import com.docsshare_web_backend.payment.repositories.PaymentRepository;
import com.docsshare_web_backend.users.models.User;
import com.docsshare_web_backend.users.repositories.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
public class OrderServiceImpl implements OrderService {
        @Autowired
        private OrderRepository orderRepository;

        @Autowired
        private UserRepository userRepository;
    @Autowired
    private PaymentRepository paymentRepository;
    @Autowired
    private DocumentRepository documentRepository;
    @Autowired
    private OrderDetailRepository orderDetailRepository;

        private Pageable getPageable(Pageable pageable) {
                return pageable != null ? pageable : Pageable.unpaged();
        }

        public static class OrderDetailMapper {
                public static OrderDetailResponse toOrderDetailResponse(OrderDetail detail) {
                        return OrderDetailResponse.builder()
                                .id(detail.getId())
                                .documentId(detail.getDocument().getId())
                                .documentTitle(detail.getDocument().getTitle()) // nếu cần
                                .price(detail.getPrice())
                                .build();
                }
        }

        public static class OrderMapper {
                public static OrderResponse toOrderResponse(Order order) {
                        return OrderResponse.builder()
                                .id(order.getId())
                                .status(order.getStatus()) // nếu muốn rename field thì map tay
                                .createdAt(order.getCreatedAt())
                                .updatedAt(order.getUpdatedAt()) // giả sử có trường này trong entity
                                .userId(order.getUser() != null ? order.getUser().getId() : null)
                                .paymentId(order.getPayment() != null ? order.getPayment().getId() : null)
                                .orderDetails(order.getOrderDetails() != null
                                        ? order.getOrderDetails().stream()
                                        .map(OrderDetailMapper::toOrderDetailResponse)
                                        .toList()
                                        : null)
                                .build();
                }

        }

        @Override
        @Transactional(readOnly = true)
        public Page<OrderResponse> getAllOrder(OrderFilterRequest request, Pageable pageable) {
                Specification<Order> spec = OrderFilter.filterByRequest(request);
                return orderRepository.findAll(spec, getPageable(pageable))
                                .map(OrderMapper::toOrderResponse);
        }

        @Override
        @Transactional(readOnly = true)
        public OrderResponse getOrder(long id) {
                Order order = orderRepository.findById(id)
                                .orElseThrow(() -> new EntityNotFoundException("Order not found with id: " + id));

                return OrderMapper.toOrderResponse(order);
        }

        @Override
        @Transactional
        public OrderResponse createOrder(OrderRequest request) {
                // 1. Lấy thông tin người dùng theo userId từ request
                User user = userRepository.findById(request.getUserId())
                        .orElseThrow(() -> new EntityNotFoundException("User not found with id: " + request.getUserId()));

                // 2. Lấy thông tin thanh toán nếu có (nếu payment là optional)
                Payment payment = null;
                if (request.getPaymentId() != null) {
                        payment = paymentRepository.findById(request.getPaymentId())
                                .orElseThrow(() -> new EntityNotFoundException("Payment not found with id: " + request.getPaymentId()));
                }

                // 3. Tạo đơn hàng (Order)
                Order order = Order.builder()
                        .status(OrderStatus.PENDING)
                        .user(user)
                        .payment(payment)
                        .build();
                Order savedOrder = orderRepository.save(order);

                // 4. Tạo danh sách chi tiết đơn hàng (OrderDetail)
                List<OrderDetail> orderDetails = request.getItems().stream().map(item -> {
                        Document document = documentRepository.findById(item.getDocumentId())
                                .orElseThrow(() -> new EntityNotFoundException("Document not found with id: " + item.getDocumentId()));

                        return OrderDetail.builder()
                                .order(savedOrder)
                                .document(document)
                                .quantity(item.getQuantity())
                                .price(item.getPrice())
                                .build();
                }).toList();

                orderDetailRepository.saveAll(orderDetails);

                // 5. Gán lại orderDetails cho đơn hàng để trả về DTO đúng
                savedOrder.setOrderDetails(orderDetails);

                // 6. Trả về OrderResponse DTO
                return OrderMapper.toOrderResponse(savedOrder);
        }


//
//        @Override
//        @Transactional(readOnly = true)
//        public OrderResponse getDocumentBySlug(String slug) {
//                if (slug == null || slug.trim().isEmpty()) {
//                        throw new IllegalArgumentException("Slug cannot be null or empty");
//                }
//                Order order = orderRepository.findBySlug(slug)
//                                .orElseThrow(() -> new EntityNotFoundException(
//                                                "Document not found with slug: " + slug));
//
//                return DocumentMapper.toDocumentResponse(order);
//        }
//
//        @Override
//        @Transactional(readOnly = true)
//        public Page<OrderResponse> getDocumentsByUserId(OrderFilterRequest request, long userId,
//                                                        Pageable pageable) {
//                userRepository.findById(userId)
//                                .orElseThrow(() -> new EntityNotFoundException(
//                                                "User not found with id: " + userId));
//                Specification<Order> spec = Specification
//                                .<Order>where((root, query, cb) -> cb.equal(root.get("user").get("id"), userId))
//                                .and(OrderFilter.filterByRequest(request));
//
//                return orderRepository.findAll(spec, pageable)
//                                .map(DocumentMapper::toDocumentResponse);
//        }
//
//        @Override
//        @Transactional(readOnly = true)
//        public Page<OrderResponse> getDocumentsByCategoryId(OrderFilterRequest request, long categoryId,
//                                                            Pageable pageable) {
//                categoryRepository.findById(categoryId)
//                                .orElseThrow(() -> new EntityNotFoundException(
//                                                "Category not found with id: " + categoryId));
//                Specification<Order> spec = Specification
//                                .<Order>where((root, query, cb) -> cb.equal(root.get("category").get("id"),
//                                                categoryId))
//                                .and(OrderFilter.filterByRequest(request));
//
//                return orderRepository.findAll(spec, pageable)
//                                .map(DocumentMapper::toDocumentResponse);
//        }
//
//        @Override
//        @Transactional(readOnly = true)
//        public Page<OrderResponse> getDocumentsNeedApproved(OrderFilterRequest request, Pageable pageable) {
//                Specification<Order> spec = Specification
//                                .<Order>where((root, query, cb) -> cb.equal(root.get("moderationStatus"),
//                                                OrderStatus.PENDING))
//                                .and((root, query, cb) -> cb.isTrue(root.get("isPublic")));
//
//                return orderRepository.findAll(spec, pageable)
//                                .map(DocumentMapper::toDocumentResponse);
//        }
//

//        @Override
//        @Transactional
//        public OrderResponse updateDocument(long documentId, OrderRequest request) {
//                Order existingOrder = orderRepository.findById(documentId)
//                                .orElseThrow(() -> new EntityNotFoundException(
//                                                "Document not found with id: " + documentId));
//
//                if (existingOrder.getModerationStatus() == OrderStatus.APPROVED ||
//                                existingOrder.getModerationStatus() == OrderStatus.CANCELLED) {
//                        existingOrder.setModerationStatus(OrderStatus.PENDING);
//                }
//
//                existingOrder.setTitle(request.getTitle());
//                existingOrder.setDescription(request.getDesciption());
//                existingOrder.setFilePath(request.getFilePath());
//                existingOrder.setSlug(request.getSlug());
//                existingOrder.setPrice(request.getPrice());
//                existingOrder.setCopyrightPath(request.getCopyrightPath());
//                existingOrder.setCoAuthor(request.getCoAuthor());
//                existingOrder.setPublic(request.isPublic());
//
//                Order updatedOrder = orderRepository.save(existingOrder);
//                return DocumentMapper.toDocumentResponse(updatedOrder);
//        }
//
        @Override
        @Transactional
        public OrderResponse updateOrderStatus(long id, OrderStatus status) {
                Order existingOrder = orderRepository.findById(id)
                                .orElseThrow(() -> new EntityNotFoundException("Order not found with id: " + id));
                existingOrder.setStatus(status);
            existingOrder.setStatus(status);
            existingOrder.setUpdatedAt(LocalDateTime.now()); // nếu bạn có field updatedAt

            return OrderMapper.toOrderResponse(orderRepository.save(existingOrder));
        }

}
