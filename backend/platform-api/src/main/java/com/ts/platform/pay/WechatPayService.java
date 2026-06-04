package com.ts.platform.pay;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * 微信支付 Native 下单。开发环境默认 Mock，返回可展示的占位 code_url。
 */
@Service
public class WechatPayService {

    private static final Logger log = LoggerFactory.getLogger(WechatPayService.class);

    private final boolean mockEnabled;

    public WechatPayService(@Value("${app.wechat.mock-enabled:true}") boolean mockEnabled) {
        this.mockEnabled = mockEnabled;
    }

    public String createNativeOrder(PayOrder order) {
        if (mockEnabled) {
            String mockUrl = "weixin://wxpay/bizpayurl?pr=MOCK_" + order.getOrderNo();
            log.info("【Mock 支付】订单 {} 金额 {} 元，Native 码: {}", order.getOrderNo(), order.getAmount(), mockUrl);
            return mockUrl;
        }
        // 生产环境：接入微信支付 v3 Native 下单 API
        throw new UnsupportedOperationException("请配置 app.wechat.mock-enabled=false 并实现真实微信支付");
    }

    /** 原路退款；开发环境 Mock 成功。 */
    public String refund(PayOrder order) {
        if (mockEnabled) {
            String refundId = "MOCK_WX_REFUND_" + order.getOrderNo();
            log.info("【Mock 退款】微信订单 {} 金额 {} 元，退款单: {}", order.getOrderNo(), order.getAmount(), refundId);
            return refundId;
        }
        throw new UnsupportedOperationException("请配置 app.wechat.mock-enabled=false 并实现真实微信退款");
    }
}
