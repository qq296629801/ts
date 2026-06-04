package com.ts.platform.pay;

import com.ts.platform.common.BusinessException;
import com.ts.platform.invite.InviteService;
import com.ts.platform.quota.QuotaLog;
import com.ts.platform.quota.QuotaLogRepository;
import com.ts.platform.user.UserQuota;
import com.ts.platform.user.UserQuotaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
public class PayService {

    private static final Set<String> PAY_CHANNELS = Set.of("WECHAT", "ALIPAY");

    private final PayPackageRepository packageRepository;
    private final PayOrderRepository orderRepository;
    private final PayNotifyLogRepository notifyLogRepository;
    private final UserQuotaRepository quotaRepository;
    private final QuotaLogRepository quotaLogRepository;
    private final InviteService inviteService;
    private final WechatPayService wechatPayService;
    private final AlipayPayService alipayPayService;

    public PayService(
            PayPackageRepository packageRepository,
            PayOrderRepository orderRepository,
            PayNotifyLogRepository notifyLogRepository,
            UserQuotaRepository quotaRepository,
            QuotaLogRepository quotaLogRepository,
            InviteService inviteService,
            WechatPayService wechatPayService,
            AlipayPayService alipayPayService) {
        this.packageRepository = packageRepository;
        this.orderRepository = orderRepository;
        this.notifyLogRepository = notifyLogRepository;
        this.quotaRepository = quotaRepository;
        this.quotaLogRepository = quotaLogRepository;
        this.inviteService = inviteService;
        this.wechatPayService = wechatPayService;
        this.alipayPayService = alipayPayService;
    }

    public List<Map<String, Object>> listPackages() {
        return packageRepository.findByStatusOrderBySortOrderAsc(1).stream()
                .map(p -> Map.<String, Object>of(
                        "id", p.getId(),
                        "name", p.getName(),
                        "quota", p.getQuota(),
                        "price", p.getPrice()))
                .toList();
    }

    @Transactional
    public Map<String, Object> createOrder(Long userId, Long packageId) {
        return createOrder(userId, packageId, "WECHAT");
    }

    @Transactional
    public Map<String, Object> createOrder(Long userId, Long packageId, String payChannel) {
        String channel = normalizePayChannel(payChannel);
        PayPackage pkg = packageRepository.findById(packageId)
                .orElseThrow(() -> new BusinessException(404, "套餐不存在"));
        if (pkg.getStatus() != 1) {
            throw new BusinessException(400, "套餐已下架");
        }
        PayOrder order = new PayOrder();
        order.setOrderNo(generateOrderNo());
        order.setUserId(userId);
        order.setPackageId(pkg.getId());
        order.setAmount(pkg.getPrice());
        order.setQuotaGranted(pkg.getQuota());
        order.setPayType(channel);
        order.setExpiresAt(LocalDateTime.now().plusMinutes(15));
        String codeUrl = "ALIPAY".equals(channel)
                ? alipayPayService.createPrecreateOrder(order)
                : wechatPayService.createNativeOrder(order);
        order.setCodeUrl(codeUrl);
        orderRepository.save(order);
        return Map.of(
                "orderNo", order.getOrderNo(),
                "payType", order.getPayType(),
                "codeUrl", order.getCodeUrl(),
                "expiresAt", order.getExpiresAt().toString(),
                "amount", order.getAmount(),
                "quotaGranted", order.getQuotaGranted());
    }

    public Map<String, Object> listUserOrders(Long userId, int page, int size) {
        Page<PayOrder> result = orderRepository.findByUserIdOrderByCreatedAtDesc(
                userId, PageRequest.of(Math.max(page - 1, 0), Math.min(size, 50)));
        List<Map<String, Object>> items = result.getContent().stream().map(this::toView).toList();
        return Map.of("items", items, "total", result.getTotalElements(), "page", page, "size", size);
    }

    public Map<String, Object> getOrder(Long userId, String orderNo) {
        PayOrder order = orderRepository.findByOrderNoAndUserId(orderNo, userId)
                .orElseThrow(() -> new BusinessException(404, "订单不存在"));
        if ("PENDING".equals(order.getStatus()) && order.getExpiresAt().isBefore(LocalDateTime.now())) {
            order.setStatus("CANCELLED");
            orderRepository.save(order);
        }
        return toView(order);
    }

    @Transactional
    public void handleWechatNotify(String notifyId, String orderNo, String transactionId) {
        handlePayNotify(notifyId, orderNo, transactionId, "WECHAT");
    }

    @Transactional
    public void handleAlipayNotify(String notifyId, String orderNo, String transactionId) {
        handlePayNotify(notifyId, orderNo, transactionId, "ALIPAY");
    }

    @Transactional
    public void handlePayNotify(String notifyId, String orderNo, String channelTradeNo, String expectedPayType) {
        if (notifyLogRepository.existsByNotifyId(notifyId)) {
            return;
        }
        PayOrder order = orderRepository.findByOrderNo(orderNo)
                .orElseThrow(() -> new BusinessException(404, "订单不存在"));
        if (!expectedPayType.equals(order.getPayType())) {
            throw new BusinessException(400, "支付渠道与订单不匹配");
        }
        if ("PAID".equals(order.getStatus())) {
            return;
        }
        if ("CANCELLED".equals(order.getStatus())) {
            throw new BusinessException(400, "订单已取消");
        }
        if (order.getExpiresAt().isBefore(LocalDateTime.now())) {
            order.setStatus("CANCELLED");
            orderRepository.save(order);
            throw new BusinessException(400, "订单已超时");
        }
        markPaid(order, channelTradeNo, notifyId, expectedPayType);
    }

    @Transactional
    public void simulatePayDev(String orderNo) {
        PayOrder order = orderRepository.findByOrderNo(orderNo)
                .orElseThrow(() -> new BusinessException(404, "订单不存在"));
        String notifyId = "dev-" + UUID.randomUUID();
        String tradeNo = ("ALIPAY".equals(order.getPayType()) ? "MOCK_ALI_" : "MOCK_WX_")
                + System.currentTimeMillis();
        handlePayNotify(notifyId, orderNo, tradeNo, order.getPayType());
    }

    @Transactional
    public Map<String, Object> refundPaidOrder(String orderNo) {
        PayOrder order = orderRepository.findByOrderNo(orderNo)
                .orElseThrow(() -> new BusinessException(404, "订单不存在"));
        if ("REFUNDED".equals(order.getStatus())) {
            return refundResultView(order);
        }
        if (!"PAID".equals(order.getStatus())) {
            throw new BusinessException(400, "仅已支付订单可退款");
        }
        String refundNotifyId = "refund-" + orderNo;
        if (notifyLogRepository.existsByNotifyId(refundNotifyId)) {
            return refundResultView(order);
        }
        UserQuota quota = quotaRepository.findByUserId(order.getUserId()).orElseThrow();
        int toDeduct = order.getQuotaGranted();
        if (quota.getBalance() < toDeduct) {
            throw new BusinessException(400, "用户余额不足，无法扣回赠送次数");
        }
        String channelRefundId = "ALIPAY".equals(order.getPayType())
                ? alipayPayService.refund(order)
                : wechatPayService.refund(order);

        order.setStatus("REFUNDED");
        order.setRefundedAt(LocalDateTime.now());
        order.setRefundNotifyId(refundNotifyId);
        orderRepository.save(order);

        PayNotifyLog notifyLog = new PayNotifyLog();
        notifyLog.setNotifyId(refundNotifyId);
        notifyLog.setOrderNo(orderNo);
        notifyLogRepository.save(notifyLog);

        quota.setBalance(quota.getBalance() - toDeduct);
        quota.setTotalGranted(Math.max(0, quota.getTotalGranted() - toDeduct));
        quota.touch();
        quotaRepository.save(quota);
        quotaLogRepository.save(
                QuotaLog.of(order.getUserId(), -toDeduct, "REFUND").withRef("ORDER", orderNo));

        return Map.of(
                "orderNo", order.getOrderNo(),
                "status", order.getStatus(),
                "refundedAt", order.getRefundedAt().toString(),
                "channelRefundId", channelRefundId,
                "quotaDeducted", toDeduct);
    }

    private static Map<String, Object> refundResultView(PayOrder order) {
        return Map.of(
                "orderNo", order.getOrderNo(),
                "status", order.getStatus(),
                "refundedAt", order.getRefundedAt() != null ? order.getRefundedAt().toString() : "",
                "quotaDeducted", order.getQuotaGranted());
    }

    @Transactional
    public int expirePendingOrders() {
        List<PayOrder> expired = orderRepository.findByStatusAndExpiresAtBefore("PENDING", LocalDateTime.now());
        for (PayOrder order : expired) {
            order.setStatus("CANCELLED");
            orderRepository.save(order);
        }
        return expired.size();
    }

    private void markPaid(PayOrder order, String channelTradeNo, String notifyId, String payType) {
        order.setStatus("PAID");
        order.setChannelTradeNo(channelTradeNo);
        if ("WECHAT".equals(payType)) {
            order.setWxTransactionId(channelTradeNo);
        }
        order.setPaidAt(LocalDateTime.now());
        orderRepository.save(order);

        PayNotifyLog log = new PayNotifyLog();
        log.setNotifyId(notifyId);
        log.setOrderNo(order.getOrderNo());
        notifyLogRepository.save(log);

        UserQuota quota = quotaRepository.findByUserId(order.getUserId()).orElseThrow();
        quota.setBalance(quota.getBalance() + order.getQuotaGranted());
        quota.setTotalGranted(quota.getTotalGranted() + order.getQuotaGranted());
        quota.touch();
        quotaRepository.save(quota);
        quotaLogRepository.save(QuotaLog.of(order.getUserId(), order.getQuotaGranted(), "RECHARGE"));

        inviteService.onFirstPay(order.getUserId());
    }

    private static String normalizePayChannel(String payChannel) {
        if (payChannel == null || payChannel.isBlank()) {
            return "WECHAT";
        }
        String c = payChannel.trim().toUpperCase();
        if (!PAY_CHANNELS.contains(c)) {
            throw new BusinessException(400, "不支持的支付渠道");
        }
        return c;
    }

    private static String generateOrderNo() {
        return "PO" + System.currentTimeMillis() + UUID.randomUUID().toString().substring(0, 6).toUpperCase();
    }

    private Map<String, Object> toView(PayOrder order) {
        return Map.of(
                "orderNo", order.getOrderNo(),
                "payType", order.getPayType(),
                "status", order.getStatus(),
                "amount", order.getAmount(),
                "quotaGranted", order.getQuotaGranted(),
                "codeUrl", order.getCodeUrl() != null ? order.getCodeUrl() : "",
                "expiresAt", order.getExpiresAt().toString(),
                "paidAt", order.getPaidAt() != null ? order.getPaidAt().toString() : "");
    }
}
