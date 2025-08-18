package com.docsshare_web_backend.order.repositories;

import com.docsshare_web_backend.order.enums.OrderStatus;
import com.docsshare_web_backend.order.models.Order;
import com.docsshare_web_backend.order.models.OrderDetail;
import com.docsshare_web_backend.users.models.User;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface OrderDetailRepository extends JpaRepository<OrderDetail, Long>, JpaSpecificationExecutor<Order> {
    @Query("""
        SELECT od.document.id
        FROM OrderDetail od
        JOIN od.order o
        WHERE o.user.id = :userId
    """)
    List<Long> findDocumentIdsByUserId(@Param("userId") Long userId);
}
