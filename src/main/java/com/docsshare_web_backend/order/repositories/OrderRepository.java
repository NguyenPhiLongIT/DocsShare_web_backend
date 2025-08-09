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

import java.time.LocalDate;
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

    @Query(value = """
        SELECT 
            o.user_id AS userId,
            u.name AS userName,
            COUNT(o.id) AS completedOrderCount
        FROM orders o
        JOIN user u ON o.user_id = u.id
        WHERE o.created_at BETWEEN :from AND :to
            AND o.status = 'COMPLETED'
        GROUP BY o.user_id, u.name
        ORDER BY completedOrderCount DESC
        LIMIT :limit
        """, nativeQuery = true)
    List<Object[]> findTopUsersWithCompletedOrders(@Param("from") LocalDate from, @Param("to") LocalDate to, @Param("limit") int top);

}
