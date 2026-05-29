package com.ts.gateway.contract;

import com.ts.gateway.api.ImageGatewayController;
import com.ts.gateway.security.JwtWebFilter;
import com.ts.gateway.service.ImageGenerateService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.reactive.ReactiveSecurityAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@WebFluxTest(controllers = ImageGatewayController.class, excludeAutoConfiguration = ReactiveSecurityAutoConfiguration.class)
@Import(JwtWebFilter.class)
@ActiveProfiles("test")
@TestPropertySource(properties = "app.jwt.secret=test-secret-key-at-least-32-bytes-long!!")
class ImageGatewayContractTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockBean
    private ImageGenerateService imageGenerateService;

    @Test
    void generate_unauthorized() {
        webTestClient.post().uri("/api/v1/ai/image/generate")
                .bodyValue(Map.of("prompt", "猫", "size", "1024x1024"))
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    void generate_ok() {
        when(imageGenerateService.generate(anyLong(), anyString(), any(), any()))
                .thenReturn(Mono.just(Map.of("imageUrl", "http://img/1.png", "remainingQuota", 2)));

        webTestClient.post().uri("/api/v1/ai/image/generate")
                .header(HttpHeaders.AUTHORIZATION, testToken(1L))
                .bodyValue(Map.of("prompt", "猫", "size", "1024x1024", "sessionId", "s1"))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.imageUrl").exists();
    }

    @Test
    void generate_insufficientQuota() {
        when(imageGenerateService.generate(anyLong(), anyString(), any(), any()))
                .thenReturn(Mono.error(new ResponseStatusException(HttpStatus.PAYMENT_REQUIRED, "次数不足")));

        webTestClient.post().uri("/api/v1/ai/image/generate")
                .header(HttpHeaders.AUTHORIZATION, testToken(1L))
                .bodyValue(Map.of("prompt", "猫", "size", "1024x1024"))
                .exchange()
                .expectStatus().isEqualTo(402);
    }

    @Test
    void generate_conflict() {
        when(imageGenerateService.generate(anyLong(), anyString(), any(), any()))
                .thenReturn(Mono.error(new ResponseStatusException(HttpStatus.CONFLICT, "已有进行中的生图任务")));

        webTestClient.post().uri("/api/v1/ai/image/generate")
                .header(HttpHeaders.AUTHORIZATION, testToken(1L))
                .bodyValue(Map.of("prompt", "猫", "size", "1024x1024"))
                .exchange()
                .expectStatus().isEqualTo(409);
    }

    private static String testToken(long userId) {
        return "Bearer " + io.jsonwebtoken.Jwts.builder()
                .subject(String.valueOf(userId))
                .claim("role", "USER")
                .signWith(io.jsonwebtoken.security.Keys.hmacShaKeyFor(
                        "test-secret-key-at-least-32-bytes-long!!".getBytes(java.nio.charset.StandardCharsets.UTF_8)))
                .compact();
    }
}
