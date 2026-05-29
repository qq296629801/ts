package com.ts.platform.invite;

import com.ts.platform.common.ApiResponse;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/user")
public class InviteController {

    private final InviteService inviteService;

    public InviteController(InviteService inviteService) {
        this.inviteService = inviteService;
    }

    @GetMapping("/invite")
    public ApiResponse<Map<String, Object>> invite(Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        return ApiResponse.ok(inviteService.inviteInfo(userId));
    }
}
