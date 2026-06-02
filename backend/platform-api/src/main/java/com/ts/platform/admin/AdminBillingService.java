package com.ts.platform.admin;

import com.ts.platform.pay.PayOrder;
import com.ts.platform.pay.PayOrderRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class AdminBillingService {

    private final PayOrderRepository orderRepository;

    public AdminBillingService(PayOrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    public Map<String, Object> listOrders(
            Long userId,
            String status,
            String payType,
            LocalDateTime from,
            LocalDateTime to,
            int page,
            int size) {
        Page<PayOrder> result = orderRepository.searchAdmin(
                userId, status, payType, from, to,
                PageRequest.of(Math.max(page - 1, 0), Math.min(size, 100)));
        List<Map<String, Object>> items = result.getContent().stream().map(this::toAdminView).toList();
        return Map.of(
                "items", items,
                "total", result.getTotalElements(),
                "page", page,
                "size", size);
    }

    public Map<String, Object> summary(LocalDateTime from, LocalDateTime to) {
        LocalDateTime start = from != null ? from : LocalDateTime.now().minusDays(30);
        LocalDateTime end = to != null ? to : LocalDateTime.now();
        List<Object[]> rows = orderRepository.summarizePaidByPayType(start, end);
        BigDecimal totalAmount = BigDecimal.ZERO;
        long totalCount = 0;
        List<Map<String, Object>> byPayType = new ArrayList<>();
        for (Object[] row : rows) {
            String pt = (String) row[0];
            long count = (Long) row[1];
            BigDecimal amount = (BigDecimal) row[2];
            totalCount += count;
            totalAmount = totalAmount.add(amount != null ? amount : BigDecimal.ZERO);
            byPayType.add(Map.of(
                    "payType", pt,
                    "count", count,
                    "amount", amount != null ? amount : BigDecimal.ZERO));
        }
        return Map.of(
                "from", start.toString(),
                "to", end.toString(),
                "paidCount", totalCount,
                "totalAmount", totalAmount,
                "byPayType", byPayType);
    }

    private Map<String, Object> toAdminView(PayOrder o) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("orderNo", o.getOrderNo());
        m.put("userId", o.getUserId());
        m.put("packageId", o.getPackageId());
        m.put("amount", o.getAmount());
        m.put("quotaGranted", o.getQuotaGranted());
        m.put("payType", o.getPayType());
        m.put("status", o.getStatus());
        m.put("channelTradeNo", o.getChannelTradeNo() != null ? o.getChannelTradeNo() : "");
        m.put("paidAt", o.getPaidAt() != null ? o.getPaidAt().toString() : "");
        m.put("createdAt", o.getCreatedAt() != null ? o.getCreatedAt().toString() : "");
        return m;
    }
}
