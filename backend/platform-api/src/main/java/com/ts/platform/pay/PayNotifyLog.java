package com.ts.platform.pay;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "t_pay_notify_log")
public class PayNotifyLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "notify_id", nullable = false, unique = true, length = 128)
    private String notifyId;

    @Column(name = "order_no", nullable = false, length = 64)
    private String orderNo;

    @Column(name = "processed_at", nullable = false)
    private LocalDateTime processedAt = LocalDateTime.now();

    public String getNotifyId() { return notifyId; }
    public void setNotifyId(String notifyId) { this.notifyId = notifyId; }
    public String getOrderNo() { return orderNo; }
    public void setOrderNo(String orderNo) { this.orderNo = orderNo; }
}
