package com.ts.platform.pay;

import org.springframework.data.jpa.repository.JpaRepository;

public interface PayNotifyLogRepository extends JpaRepository<PayNotifyLog, Long> {

    boolean existsByNotifyId(String notifyId);
}
