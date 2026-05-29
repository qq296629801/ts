package com.ts.platform.user;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "t_user_quota")
public class UserQuota {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false, unique = true)
    private Long userId;

    @Column(nullable = false)
    private Integer balance = 0;

    @Column(name = "total_granted", nullable = false)
    private Integer totalGranted = 0;

    @Column(name = "total_used", nullable = false)
    private Integer totalUsed = 0;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();

    public Long getId() { return id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public Integer getBalance() { return balance; }
    public void setBalance(Integer balance) { this.balance = balance; }
    public Integer getTotalGranted() { return totalGranted; }
    public void setTotalGranted(Integer totalGranted) { this.totalGranted = totalGranted; }
    public Integer getTotalUsed() { return totalUsed; }
    public void setTotalUsed(Integer totalUsed) { this.totalUsed = totalUsed; }
    public void touch() { this.updatedAt = LocalDateTime.now(); }
}
