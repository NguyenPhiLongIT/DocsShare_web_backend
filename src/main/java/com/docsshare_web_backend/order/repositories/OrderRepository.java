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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
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


    @Query(value = """
    SELECT
        d.author_id AS userId,
        u.name AS userName,
        COUNT(od.id) AS soldOrders
    FROM orders o
    JOIN order_detail od ON o.id = od.order_id
    JOIN document d ON od.document_id = d.id
    JOIN user u ON d.author_id = u.id
    WHERE o.status = 'COMPLETED'
      AND o.updated_at BETWEEN :from AND :to
    GROUP BY d.author_id, u.name
    ORDER BY soldOrders DESC
    LIMIT :limit
    """, nativeQuery = true)
    List<Object[]> findTopSellerUsers(
            @Param("from") LocalDateTime from,
            @Param("to")   LocalDateTime to,
            @Param("limit") int top
    );

    @Query(value = """
    SELECT COALESCE(SUM(od.price * o.commission_rate), 0)
    FROM orders o
    JOIN order_detail od ON o.id = od.order_id
    WHERE o.status = 'COMPLETED'
      AND o.updated_at BETWEEN :from AND :to
    """, nativeQuery = true)
    BigDecimal getTotalRevenue(
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to
    );

    @Query(value = """
    SELECT 
        DAYOFWEEK(o.updated_at) AS dayOfWeek,
        SUM(od.price * o.commission_rate) AS revenue
    FROM orders o
    JOIN order_detail od ON o.id = od.order_id
    WHERE o.status = 'COMPLETED'
      AND o.updated_at BETWEEN :from AND :to
    GROUP BY DAYOFWEEK(o.updated_at)
    ORDER BY dayOfWeek
    """, nativeQuery = true)
    List<Object[]> getRevenueByDayOfWeek(
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to
    );

    @Query(value = """
    SELECT 
        MONTH(o.updated_at) AS month,
        SUM(od.price * o.commission_rate) AS revenue
    FROM orders o
    JOIN order_detail od ON o.id = od.order_id
    WHERE o.status = 'COMPLETED'
      AND o.updated_at BETWEEN :from AND :to
    GROUP BY MONTH(o.updated_at)
    ORDER BY month
    """, nativeQuery = true)
    List<Object[]> getRevenueByMonth(
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to
    );


    @Query(value = """
    SELECT 
        YEAR(o.updated_at) AS year,
        SUM(od.price * o.commission_rate) AS revenue
    FROM orders o
    JOIN order_detail od ON o.id = od.order_id
    WHERE o.status = 'COMPLETED'
      AND o.updated_at BETWEEN :from AND :to
    GROUP BY YEAR(o.updated_at)
    ORDER BY year
    """, nativeQuery = true)
    List<Object[]> getRevenueByYear(
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to
    );


}
