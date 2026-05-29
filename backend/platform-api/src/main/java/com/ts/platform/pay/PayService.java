package com.ts.platform.pay;

import com.ts.platform.common.BusinessException;
import com.ts.platform.invite.InviteService;
import com.ts.platform.quota.QuotaLog;
import com.ts.platform.quota.QuotaLogRepository;
import com.ts.platform.user.UserQuota;
import com.ts.platform.user.UserQuotaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class PayService {

    private final PayPackageRepository packageRepository;
    private final PayOrderRepository orderRepository;
    private final PayNotifyLogRepository notifyLogRepository;
    private final UserQuotaRepository quotaRepository;
    private final QuotaLogRepository quotaLogRepository;
    private final InviteService inviteService;
    private final WechatPayService wechatPayService;

    public PayService(
            PayPackageRepository packageRepository,
            PayOrderRepository orderRepository,
            PayNotifyLogRepository notifyLogRepository,
            UserQuotaRepository quotaRepository,
            QuotaLogRepository quotaLogRepository,
            InviteService inviteService,
            WechatPayService wechatPayService) {
        this.packageRepository = packageRepository;
        this.orderRepository = orderRepository;
        this.notifyLogRepository = notifyLogRepository;
        this.quotaRepository = quotaRepository;
        this.quotaLogRepository = quotaLogRepository;
        this.inviteService = inviteService;
        this.wechatPayService = wechatPayService;
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
        order.setExpiresAt(LocalDateTime.now().plusMinutes(15));
        String codeUrl = wechatPayService.createNativeOrder(order);
        order.setCodeUrl(codeUrl);
        orderRepository.save(order);
        return Map.of(
                "orderNo", order.getOrderNo(),
                "codeUrl", order.getCodeUrl(),
                "expiresAt", order.getExpiresAt().toString(),
                "amount", order.getAmount(),
                "quotaGranted", order.getQuotaGranted());
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
    public void handleWechatNotify(String notifyId, String orderNo, String wxTransactionId) {
        if (notifyLogRepository.existsByNotifyId(notifyId)) {
            return;
        }
        PayOrder order = orderRepository.findByOrderNo(orderNo)
                .orElseThrow(() -> new BusinessException(404, "订单不存在"));
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
        markPaid(order, wxTransactionId, notifyId);
    }

    @Transactional
    public void simulatePayDev(String orderNo) {
        String notifyId = "dev-" + UUID.randomUUID();
        handleWechatNotify(notifyId, orderNo, "MOCK_WX_" + System.currentTimeMillis());
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

    private void markPaid(PayOrder order, String wxTransactionId, String notifyId) {
        order.setStatus("PAID");
        order.setWxTransactionId(wxTransactionId);
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

    private static String generateOrderNo() {
        return "PO" + System.currentTimeMillis() + UUID.randomUUID().toString().substring(0, 6).toUpperCase();
    }

    private static Map<String, Object> toView(PayOrder order) {
        return Map.of(
                "orderNo", order.getOrderNo(),
                "status", order.getStatus(),
                "amount", order.getAmount(),
                "quotaGranted", order.getQuotaGranted(),
                "codeUrl", order.getCodeUrl() != null ? order.getCodeUrl() : "",
                "expiresAt", order.getExpiresAt().toString(),
                "paidAt", order.getPaidAt() != null ? order.getPaidAt().toString() : "");
    }
}
