package com.docsshare_web_backend.order.repositories;

import com.docsshare_web_backend.order.models.Order;
import com.docsshare_web_backend.users.models.User;
import com.docsshare_web_backend.order.enums.OrderStatus;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long>, JpaSpecificationExecutor<Order> {

//    // Tìm đơn hàng theo slug
//    Optional<Order> findBySlug(String slug);

    // Tìm danh sách đơn hàng theo user
    List<Order> findByUser(User user);

    // Tìm theo userId
    List<Order> findByUserId(Long userId);

    // Tìm tất cả theo trạng thái
    List<Order> findByStatus(OrderStatus status);

    // Eager fetch user + payment + orderDetails + document để tránh lazy loading lỗi
    @EntityGraph(attributePaths = {"user", "payment", "orderDetails", "orderDetails.document"})
    Optional<Order> findWithAllRelationsById(Long id);
}
