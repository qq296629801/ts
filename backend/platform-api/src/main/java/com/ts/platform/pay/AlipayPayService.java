package com.ts.platform.pay;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * 支付宝当面付/扫码。开发环境 Mock，返回可展示的占位 payUrl。
 */
@Service
public class AlipayPayService {

    private static final Logger log = LoggerFactory.getLogger(AlipayPayService.class);

    private final boolean mockEnabled;

    public AlipayPayService(@Value("${app.alipay.mock-enabled:true}") boolean mockEnabled) {
        this.mockEnabled = mockEnabled;
    }

    public String createPrecreateOrder(PayOrder order) {
        if (mockEnabled) {
            String mockUrl = "https://openapi.alipay.com/mock?order=" + order.getOrderNo();
            log.info("【Mock 支付宝】订单 {} 金额 {} 元，扫码链接: {}", order.getOrderNo(), order.getAmount(), mockUrl);
            return mockUrl;
        }
        throw new UnsupportedOperationException("请配置 app.alipay.mock-enabled=false 并实现真实支付宝预下单");
    }
}
