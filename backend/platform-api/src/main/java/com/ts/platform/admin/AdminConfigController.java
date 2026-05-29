package com.ts.platform.admin;

import com.ts.platform.common.ApiResponse;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin/config")
public class AdminConfigController {

    private final AdminConfigService configService;

    public AdminConfigController(AdminConfigService configService) {
        this.configService = configService;
    }

    @GetMapping
    public ApiResponse<Map<String, String>> getAll() {
        return ApiResponse.ok(configService.getAll());
    }

    @PutMapping
    public ApiResponse<Void> update(@RequestBody Map<String, String> body) {
        configService.update(body);
        return ApiResponse.ok(null);
    }
}
