package com.ts.platform.quota;

import com.ts.platform.common.ApiResponse;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/internal/quota")
public class InternalQuotaController {

    private final QuotaService quotaService;

    public InternalQuotaController(QuotaService quotaService) {
        this.quotaService = quotaService;
    }

    @PostMapping("/reserve")
    public ApiResponse<Map<String, Object>> reserve(@RequestBody Map<String, Long> body) {
        Long userId = body.get("userId");
        return ApiResponse.ok(quotaService.reserve(userId));
    }

    @PostMapping("/commit")
    public ApiResponse<Void> commit(@RequestBody Map<String, Long> body) {
        quotaService.commit(body.get("userId"), body.get("reservationId"));
        return ApiResponse.ok(null);
    }

    @PostMapping("/rollback")
    public ApiResponse<Void> rollback(@RequestBody Map<String, Long> body) {
        quotaService.rollback(body.get("userId"), body.get("reservationId"));
        return ApiResponse.ok(null);
    }
}
