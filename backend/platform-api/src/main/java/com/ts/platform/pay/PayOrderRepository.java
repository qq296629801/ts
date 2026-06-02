package com.ts.platform.pay;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface PayOrderRepository extends JpaRepository<PayOrder, Long> {

    long countByStatus(String status);

    @Query("SELECT COALESCE(SUM(o.amount), 0) FROM PayOrder o WHERE o.status = :status AND o.paidAt >= :since")
    BigDecimal sumAmountByStatusAndPaidAtAfter(@Param("status") String status, @Param("since") LocalDateTime since);

    Optional<PayOrder> findByOrderNo(String orderNo);

    Optional<PayOrder> findByOrderNoAndUserId(String orderNo, Long userId);

    List<PayOrder> findByStatusAndExpiresAtBefore(String status, LocalDateTime time);

    Page<PayOrder> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    @Query("""
            SELECT o FROM PayOrder o WHERE
            (:userId IS NULL OR o.userId = :userId) AND
            (:status IS NULL OR :status = '' OR o.status = :status) AND
            (:payType IS NULL OR :payType = '' OR o.payType = :payType) AND
            (:from IS NULL OR o.createdAt >= :from) AND
            (:to IS NULL OR o.createdAt <= :to)
            ORDER BY o.createdAt DESC
            """)
    Page<PayOrder> searchAdmin(
            @Param("userId") Long userId,
            @Param("status") String status,
            @Param("payType") String payType,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to,
            Pageable pageable);

    @Query("""
            SELECT o.payType, COUNT(o), SUM(o.amount) FROM PayOrder o
            WHERE o.status = 'PAID' AND o.paidAt >= :from AND o.paidAt <= :to
            GROUP BY o.payType
            """)
    List<Object[]> summarizePaidByPayType(
            @Param("from") LocalDateTime from, @Param("to") LocalDateTime to);
}
