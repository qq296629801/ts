package com.ts.platform.template;

import com.ts.platform.common.ApiResponse;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/templates")
public class TemplateController {

    private final TemplateService templateService;

    public TemplateController(TemplateService templateService) {
        this.templateService = templateService;
    }

    @GetMapping("/categories")
    public ApiResponse<List<Map<String, Object>>> categories() {
        return ApiResponse.ok(templateService.categories());
    }

    @GetMapping
    public ApiResponse<Map<String, Object>> list(
            Authentication auth,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "latest") String sort,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        Long userId = auth != null ? (Long) auth.getPrincipal() : null;
        return ApiResponse.ok(templateService.list(userId, categoryId, keyword, sort, page, size));
    }

    @GetMapping("/{id}")
    public ApiResponse<Map<String, Object>> detail(Authentication auth, @PathVariable Long id) {
        Long userId = auth != null ? (Long) auth.getPrincipal() : null;
        return ApiResponse.ok(templateService.get(userId, id));
    }

    @PostMapping("/publish")
    public ApiResponse<Map<String, Object>> publish(
            Authentication auth, @RequestBody Map<String, Object> body) {
        Long userId = (Long) auth.getPrincipal();
        return ApiResponse.ok(templateService.publish(userId, body));
    }

    @PostMapping("/{id}/use")
    public ApiResponse<Map<String, Object>> use(Authentication auth, @PathVariable Long id) {
        Long userId = (Long) auth.getPrincipal();
        return ApiResponse.ok(templateService.useTemplate(userId, id));
    }

    @PostMapping("/{id}/like")
    public ApiResponse<Map<String, Object>> like(Authentication auth, @PathVariable Long id) {
        Long userId = (Long) auth.getPrincipal();
        return ApiResponse.ok(templateService.toggleLike(userId, id));
    }
}
