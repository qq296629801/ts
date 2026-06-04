package com.ts.platform.quota;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "t_quota_log")
public class QuotaLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "change_amount", nullable = false)
    private Integer changeAmount;

    @Column(nullable = false, length = 32)
    private String reason;

    @Column(name = "ref_type", length = 32)
    private String refType;

    @Column(name = "ref_id", length = 64)
    private String refId;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public static QuotaLog of(Long userId, int change, String reason) {
        QuotaLog log = new QuotaLog();
        log.userId = userId;
        log.changeAmount = change;
        log.reason = reason;
        return log;
    }

    public QuotaLog withRef(String refType, String refId) {
        this.refType = refType;
        this.refId = refId;
        return this;
    }
}
