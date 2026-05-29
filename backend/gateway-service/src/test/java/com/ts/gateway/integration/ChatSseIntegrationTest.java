package com.ts.gateway.integration;

import com.ts.gateway.api.ChatGatewayController;
import com.ts.gateway.client.PlatformInternalClient;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.reactive.ReactiveSecurityAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import com.ts.gateway.security.JwtWebFilter;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@WebFluxTest(controllers = ChatGatewayController.class, excludeAutoConfiguration = ReactiveSecurityAutoConfiguration.class)
@Import(JwtWebFilter.class)
@ActiveProfiles("test")
@TestPropertySource(properties = "app.jwt.secret=test-secret-key-at-least-32-bytes-long!!")
class ChatSseIntegrationTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockBean
    private ChatModel chatModel;

    @MockBean
    private PlatformInternalClient platformClient;

    @Test
    void chat_streamsWithoutQuota() {
        when(platformClient.appendMessage(anyLong(), any(), eq("user"), eq("TEXT"), anyString()))
                .thenReturn(Mono.just("sess-1"));
        when(chatModel.stream(any(Prompt.class))).thenReturn(Flux.just(
                new ChatResponse(List.of(new Generation(new AssistantMessage("你")))),
                new ChatResponse(List.of(new Generation(new AssistantMessage("好"))))));
        when(platformClient.appendMessage(anyLong(), eq("sess-1"), eq("assistant"), eq("TEXT"), anyString()))
                .thenReturn(Mono.empty());

        webTestClient.post().uri("/api/v1/ai/chat")
                .header(HttpHeaders.AUTHORIZATION, bearer(2L))
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of(
                        "sessionId", "s0",
                        "messages", List.of(Map.of("role", "user", "content", "你好"))))
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentTypeCompatibleWith(MediaType.TEXT_EVENT_STREAM);
    }

    private static String bearer(long userId) {
        return "Bearer " + io.jsonwebtoken.Jwts.builder()
                .subject(String.valueOf(userId))
                .claim("role", "USER")
                .signWith(io.jsonwebtoken.security.Keys.hmacShaKeyFor(
                        "test-secret-key-at-least-32-bytes-long!!".getBytes(java.nio.charset.StandardCharsets.UTF_8)))
                .compact();
    }
}
