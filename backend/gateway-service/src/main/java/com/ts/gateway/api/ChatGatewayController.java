package com.ts.gateway.api;

import com.ts.gateway.client.PlatformInternalClient;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

@RestController
@RequestMapping("/api/v1/ai")
public class ChatGatewayController {

    private final ChatModel chatModel;
    private final PlatformInternalClient platformClient;

    public ChatGatewayController(ChatModel chatModel, PlatformInternalClient platformClient) {
        this.chatModel = chatModel;
        this.platformClient = platformClient;
    }

    @PostMapping(value = "/chat", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> chat(
            ServerWebExchange exchange,
            @RequestBody ChatBody body) {
        Long userId = exchange.getAttribute("userId");
        if (userId == null) {
            return Flux.error(new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.UNAUTHORIZED, "未登录"));
        }
        String userText = body.messages().stream()
                .filter(m -> "user".equals(m.role()))
                .reduce((a, b) -> b)
                .map(ChatMessage::content)
                .orElse("");
        if (userText.isBlank()) {
            return Flux.error(new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.BAD_REQUEST, "消息不能为空"));
        }

        AtomicReference<String> sessionRef = new AtomicReference<>();
        AtomicReference<StringBuilder> assistantBuf = new AtomicReference<>(new StringBuilder());

        return platformClient.appendMessage(userId, body.sessionId(), "user", "TEXT", userText)
                .flatMapMany(sessionId -> {
                    sessionRef.set(sessionId);
                    return chatModel.stream(new Prompt(new UserMessage(userText)))
                            .map(resp -> {
                                String chunk = resp.getResult().getOutput().getContent();
                                if (chunk != null) {
                                    assistantBuf.get().append(chunk);
                                }
                                return ServerSentEvent.builder(chunk != null ? chunk : "").build();
                            });
                })
                .doOnComplete(() -> {
                    String reply = assistantBuf.get().toString();
                    if (!reply.isBlank() && sessionRef.get() != null) {
                        platformClient.appendMessage(
                                userId, sessionRef.get(), "assistant", "TEXT", reply).subscribe();
                    }
                });
    }

    public record ChatBody(String sessionId, List<ChatMessage> messages) {
    }

    public record ChatMessage(String role, String content) {
    }
}
