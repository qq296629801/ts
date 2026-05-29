package com.ts.gateway.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Map;

@Component
public class PlatformInternalClient {

    private final WebClient webClient;
    private final String internalApiKey;

    public PlatformInternalClient(
            WebClient.Builder builder,
            @Value("${app.platform-api.base-url}") String baseUrl,
            @Value("${app.internal-api-key}") String internalApiKey) {
        this.webClient = builder.baseUrl(baseUrl).build();
        this.internalApiKey = internalApiKey;
    }

    public Mono<Map<String, Object>> completeImage(
            Long userId, Long reservationId, String sessionId, String imageUrl, String prompt) {
        return post("/internal/images/complete", Map.of(
                "userId", userId,
                "reservationId", reservationId,
                "sessionId", sessionId != null ? sessionId : "",
                "imageUrl", imageUrl,
                "prompt", prompt));
    }

    public Mono<Void> rollback(Long userId, Long reservationId) {
        return webClient.post()
                .uri("/internal/quota/rollback-only")
                .header("X-Internal-Api-Key", internalApiKey)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("userId", userId, "reservationId", reservationId))
                .retrieve()
                .bodyToMono(Void.class);
    }

    public Mono<String> appendMessage(
            Long userId, String sessionId, String role, String contentType, String content) {
        return post("/internal/ai/messages", Map.of(
                "userId", userId,
                "sessionId", sessionId != null ? sessionId : "",
                "role", role,
                "contentType", contentType,
                "content", content))
                .map(data -> (String) data.get("sessionId"));
    }

    @SuppressWarnings("unchecked")
    private Mono<Map<String, Object>> post(String path, Map<String, Object> body) {
        return webClient.post()
                .uri(path)
                .header("X-Internal-Api-Key", internalApiKey)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                .map(resp -> (Map<String, Object>) resp.get("data"));
    }
}
