package com.ts.platform.pay;

import com.ts.platform.common.ApiResponse;
import org.springframework.context.annotation.Profile;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/pay")
public class PayController {

    private final PayService payService;

    public PayController(PayService payService) {
        this.payService = payService;
    }

    @GetMapping("/packages")
    public ApiResponse<?> packages() {
        return ApiResponse.ok(payService.listPackages());
    }

    @PostMapping("/create-order")
    public ApiResponse<Map<String, Object>> createOrder(
            Authentication auth, @RequestBody Map<String, Long> body) {
        Long userId = (Long) auth.getPrincipal();
        return ApiResponse.ok(payService.createOrder(userId, body.get("packageId")));
    }

    @GetMapping("/order/{orderNo}")
    public ApiResponse<Map<String, Object>> order(Authentication auth, @PathVariable String orderNo) {
        Long userId = (Long) auth.getPrincipal();
        return ApiResponse.ok(payService.getOrder(userId, orderNo));
    }

    @PostMapping("/wx-notify")
    public Map<String, String> wxNotify(@RequestBody Map<String, String> body) {
        payService.handleWechatNotify(
                body.getOrDefault("notifyId", "wx-" + body.get("orderNo")),
                body.get("orderNo"),
                body.getOrDefault("transactionId", "WX_MOCK"));
        return Map.of("code", "SUCCESS", "message", "成功");
    }

    /** 开发环境模拟支付成功 */
    @Profile("dev")
    @PostMapping("/dev/simulate/{orderNo}")
    public ApiResponse<Void> simulate(@PathVariable String orderNo) {
        payService.simulatePayDev(orderNo);
        return ApiResponse.ok(null);
    }
}
