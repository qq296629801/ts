package com.ts.platform.admin;

import com.ts.platform.common.ApiResponse;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin/templates")
public class AdminTemplateController {

    private final AdminTemplateService adminTemplateService;

    public AdminTemplateController(AdminTemplateService adminTemplateService) {
        this.adminTemplateService = adminTemplateService;
    }

    @GetMapping("/pending")
    public ApiResponse<Map<String, Object>> pending(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.ok(adminTemplateService.listPending(page, size));
    }

    @PostMapping("/{id}/audit")
    public ApiResponse<Void> audit(
            Authentication auth,
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {
        Long operatorId = (Long) auth.getPrincipal();
        adminTemplateService.audit(operatorId, id, body.get("action"), body.get("reason"));
        return ApiResponse.ok(null);
    }
}
