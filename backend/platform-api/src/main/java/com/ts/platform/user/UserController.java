package com.ts.platform.user;

import com.ts.platform.common.ApiResponse;
import com.ts.platform.quota.QuotaService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/user")
public class UserController {

    private final UserRepository userRepository;
    private final QuotaService quotaService;

    public UserController(UserRepository userRepository, QuotaService quotaService) {
        this.userRepository = userRepository;
        this.quotaService = quotaService;
    }

    @GetMapping("/profile")
    public ApiResponse<Map<String, Object>> profile(Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        User user = userRepository.findById(userId).orElseThrow();
        return ApiResponse.ok(Map.of(
                "id", user.getId(),
                "nickname", user.getNickname(),
                "phone", user.getPhone() != null ? user.getPhone() : "",
                "email", user.getEmail() != null ? user.getEmail() : "",
                "inviteCode", user.getInviteCode()));
    }

    @GetMapping("/quota")
    public ApiResponse<Map<String, Object>> quota(Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        return ApiResponse.ok(Map.of("balance", quotaService.getBalance(userId)));
    }
}
