package com.ts.platform.pay;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class OrderExpireScheduler {

    private static final Logger log = LoggerFactory.getLogger(OrderExpireScheduler.class);

    private final PayService payService;

    public OrderExpireScheduler(PayService payService) {
        this.payService = payService;
    }

    @Scheduled(fixedRate = 60_000)
    public void expireOrders() {
        int n = payService.expirePendingOrders();
        if (n > 0) {
            log.info("已自动取消 {} 笔超时待支付订单", n);
        }
    }
}
