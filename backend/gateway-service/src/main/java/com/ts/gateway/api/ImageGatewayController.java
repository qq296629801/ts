package com.ts.gateway.api;

import com.ts.gateway.service.ImageGenerateService;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/ai")
public class ImageGatewayController {

    private final ImageGenerateService imageGenerateService;

    public ImageGatewayController(ImageGenerateService imageGenerateService) {
        this.imageGenerateService = imageGenerateService;
    }

    @PostMapping("/image/generate")
    public Mono<Map<String, Object>> generate(
            ServerWebExchange exchange,
            @RequestBody GenerateBody body) {
        Long userId = exchange.getAttribute("userId");
        if (userId == null) {
            return Mono.error(new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.UNAUTHORIZED, "未登录"));
        }
        if (body.prompt() == null || body.prompt().isBlank()) {
            return Mono.error(new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.BAD_REQUEST, "提示词不能为空"));
        }
        return imageGenerateService.generate(userId, body.prompt(), body.size(), body.sessionId());
    }

    public record GenerateBody(String prompt, String size, String sessionId) {
    }
}
