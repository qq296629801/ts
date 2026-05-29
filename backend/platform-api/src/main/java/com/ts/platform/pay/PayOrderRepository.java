package com.ts.platform.pay;

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
}
