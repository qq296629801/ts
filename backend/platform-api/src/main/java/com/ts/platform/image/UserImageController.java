package com.ts.platform.image;

import com.ts.platform.ai.AiSessionService;
import com.ts.platform.common.ApiResponse;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/user")
public class UserImageController {

    private final AiSessionService sessionService;

    public UserImageController(AiSessionService sessionService) {
        this.sessionService = sessionService;
    }

    @GetMapping("/images")
    public ApiResponse<Map<String, Object>> list(
            Authentication auth,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        Long userId = (Long) auth.getPrincipal();
        return ApiResponse.ok(sessionService.listUserImages(userId, page, size));
    }
}
