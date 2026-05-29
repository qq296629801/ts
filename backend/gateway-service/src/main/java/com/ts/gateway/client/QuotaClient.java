package com.ts.gateway.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.util.Map;

@Component
public class QuotaClient {

    private final WebClient webClient;
    private final String internalApiKey;

    public QuotaClient(
            WebClient.Builder builder,
            @Value("${app.platform-api.base-url}") String baseUrl,
            @Value("${app.internal-api-key}") String internalApiKey) {
        this.webClient = builder.baseUrl(baseUrl).build();
        this.internalApiKey = internalApiKey;
    }

    public Mono<ReserveResult> reserve(Long userId) {
        return webClient.post()
                .uri("/internal/quota/reserve")
                .header("X-Internal-Api-Key", internalApiKey)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("userId", userId))
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                .map(body -> {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> data = (Map<String, Object>) body.get("data");
                    return new ReserveResult(
                            ((Number) data.get("reservationId")).longValue(),
                            ((Number) data.get("remainingQuota")).intValue());
                });
    }

    public Mono<Void> commit(Long userId, Long reservationId) {
        return postAction("/internal/quota/commit", userId, reservationId);
    }

    public Mono<Void> rollback(Long userId, Long reservationId) {
        return postAction("/internal/quota/rollback", userId, reservationId);
    }

    private Mono<Void> postAction(String path, Long userId, Long reservationId) {
        return webClient.post()
                .uri(path)
                .header("X-Internal-Api-Key", internalApiKey)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("userId", userId, "reservationId", reservationId))
                .retrieve()
                .bodyToMono(Void.class);
    }

    public record ReserveResult(long reservationId, int remainingQuota) {
    }

}
