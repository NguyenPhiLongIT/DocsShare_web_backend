package com.docsshare_web_backend.payment.repositories;

import com.docsshare_web_backend.payment.models.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRepository
    extends JpaRepository<Payment, Long>, JpaSpecificationExecutor<Payment> {
    Optional<Payment> findByTransactionId(String transactionId);
    @Query(value = """
        SELECT 
            p.id AS paymentId,
            p.amount,
            p.transaction_id AS transactionId,
            p.payment_method AS paymentMethod,
            o.id AS order_id
        FROM payment p
        LEFT JOIN orders o ON p.order_id = o.id
        WHERE p.status = 'SUCCESS'
        ORDER BY p.amount DESC
        LIMIT :limit
        """, nativeQuery = true)
    List<Object[]> findTopSuccessPayments(@Param("limit") int top);

}
