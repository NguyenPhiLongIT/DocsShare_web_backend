package com.docsshare_web_backend.order.repositories;

import com.docsshare_web_backend.order.models.Order;
import com.docsshare_web_backend.users.models.User;
import com.docsshare_web_backend.order.enums.OrderStatus;

import org.springframework.data.domain.Page;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import org.springframework.data.domain.Pageable;
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

    @Query("""
    SELECT DISTINCT o FROM Order o
    JOIN o.orderDetails od
    JOIN od.document d
    LEFT JOIN d.coAuthors ca
    WHERE d.author.id = :authorId OR ca.user.id = :authorId
""")
    Page<Order> findOrdersByAuthorOrCoauthor(@Param("authorId") Long authorId, Pageable pageable);

    @Query("SELECT COUNT(od) > 0 FROM Order o " +
            "JOIN o.orderDetails od " +
            "WHERE o.user.id = :userId " +
            "AND od.document.id = :documentId " +
            "AND o.status = 'COMPLETED'")
    boolean hasUserPaidForDocument(@Param("userId") Long userId, @Param("documentId") Long documentId);
    // Eager fetch user + payment + orderDetails + document để tránh lazy loading lỗi
    @EntityGraph(attributePaths = {"user", "payment", "orderDetails", "orderDetails.document"})
    Optional<Order> findWithAllRelationsById(Long id);
}
