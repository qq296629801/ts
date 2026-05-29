package com.ts.platform.image;

import com.ts.platform.ai.AiSessionService;
import com.ts.platform.common.ApiResponse;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/internal")
public class InternalImageController {

    private final AiSessionService sessionService;

    public InternalImageController(AiSessionService sessionService) {
        this.sessionService = sessionService;
    }

    /**
     * 生图成功：提交预扣、落库图片与消息。
     */
    @PostMapping("/images/complete")
    public ApiResponse<Map<String, Object>> complete(@RequestBody CompleteImageBody body) {
        String sessionId = body.sessionId();
        if (sessionId != null && !sessionId.isBlank()) {
            sessionService.ensureSession(body.userId(), sessionId, "IMAGE_GEN");
        }
        Map<String, Object> result = sessionService.completeImageGeneration(
                body.userId(),
                body.reservationId(),
                sessionId,
                body.imageUrl(),
                body.prompt());
        return ApiResponse.ok(result);
    }

    @PostMapping("/ai/messages")
    public ApiResponse<Map<String, String>> appendMessage(@RequestBody AppendMessageBody body) {
        String sessionId = sessionService.ensureSession(body.userId(), body.sessionId(), "CHAT");
        sessionService.appendMessage(
                body.userId(), sessionId, body.role(), body.contentType(), body.content());
        return ApiResponse.ok(Map.of("sessionId", sessionId));
    }

    public record CompleteImageBody(
            Long userId,
            Long reservationId,
            String sessionId,
            String imageUrl,
            String prompt) {
    }

    public record AppendMessageBody(
            Long userId,
            String sessionId,
            String role,
            String contentType,
            String content) {
    }
}
