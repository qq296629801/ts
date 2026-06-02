package com.ts.platform.pay;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "t_pay_order")
public class PayOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_no", nullable = false, unique = true, length = 64)
    private String orderNo;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "package_id", nullable = false)
    private Long packageId;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    @Column(name = "quota_granted", nullable = false)
    private Integer quotaGranted;

    @Column(name = "pay_type", nullable = false, length = 20)
    private String payType = "WECHAT";

    @Column(nullable = false, length = 20)
    private String status = "PENDING";

    @Column(name = "wx_transaction_id", length = 64)
    private String wxTransactionId;

    @Column(name = "code_url", length = 512)
    private String codeUrl;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    @Column(name = "channel_trade_no", length = 64)
    private String channelTradeNo;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public Long getId() { return id; }
    public String getOrderNo() { return orderNo; }
    public void setOrderNo(String orderNo) { this.orderNo = orderNo; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public Long getPackageId() { return packageId; }
    public void setPackageId(Long packageId) { this.packageId = packageId; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public Integer getQuotaGranted() { return quotaGranted; }
    public void setQuotaGranted(Integer quotaGranted) { this.quotaGranted = quotaGranted; }
    public String getPayType() { return payType; }
    public void setPayType(String payType) { this.payType = payType; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getWxTransactionId() { return wxTransactionId; }
    public void setWxTransactionId(String wxTransactionId) { this.wxTransactionId = wxTransactionId; }
    public String getChannelTradeNo() { return channelTradeNo; }
    public void setChannelTradeNo(String channelTradeNo) { this.channelTradeNo = channelTradeNo; }
    public String getCodeUrl() { return codeUrl; }
    public void setCodeUrl(String codeUrl) { this.codeUrl = codeUrl; }
    public LocalDateTime getExpiresAt() { return expiresAt; }
    public void setExpiresAt(LocalDateTime expiresAt) { this.expiresAt = expiresAt; }
    public LocalDateTime getPaidAt() { return paidAt; }
    public void setPaidAt(LocalDateTime paidAt) { this.paidAt = paidAt; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
