package com.ts.platform.ai;

import com.ts.platform.common.ApiResponse;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/ai")
public class AiSessionController {

    private final AiSessionService sessionService;

    public AiSessionController(AiSessionService sessionService) {
        this.sessionService = sessionService;
    }

    @GetMapping("/sessions")
    public ApiResponse<List<Map<String, Object>>> list(Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        return ApiResponse.ok(sessionService.listSessions(userId));
    }

    @PostMapping("/sessions")
    public ApiResponse<Map<String, Object>> create(
            Authentication auth,
            @RequestBody(required = false) Map<String, String> body) {
        Long userId = (Long) auth.getPrincipal();
        String mode = body != null ? body.get("mode") : "CHAT";
        return ApiResponse.ok(sessionService.createSession(userId, mode));
    }

    @PatchMapping("/sessions/{id}")
    public ApiResponse<Void> rename(
            Authentication auth,
            @PathVariable String id,
            @RequestBody Map<String, String> body) {
        Long userId = (Long) auth.getPrincipal();
        sessionService.renameSession(userId, id, body.get("title"));
        return ApiResponse.ok(null);
    }

    @DeleteMapping("/sessions/{id}")
    public ApiResponse<Void> delete(Authentication auth, @PathVariable String id) {
        Long userId = (Long) auth.getPrincipal();
        sessionService.deleteSession(userId, id);
        return ApiResponse.ok(null);
    }

    @GetMapping("/sessions/{id}/messages")
    public ApiResponse<Map<String, Object>> messages(
            Authentication auth,
            @PathVariable String id,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        Long userId = (Long) auth.getPrincipal();
        return ApiResponse.ok(sessionService.listMessages(userId, id, page, size));
    }
}
