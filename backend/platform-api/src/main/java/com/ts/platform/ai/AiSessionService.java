package com.ts.platform.ai;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ts.platform.common.BusinessException;
import com.ts.platform.image.ImageAsset;
import com.ts.platform.image.ImageAssetRepository;
import com.ts.platform.quota.QuotaService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
public class AiSessionService {

    private final AiSessionRepository sessionRepository;
    private final AiMessageRepository messageRepository;
    private final ImageAssetRepository imageRepository;
    private final QuotaService quotaService;
    private final ObjectMapper objectMapper;

    public AiSessionService(
            AiSessionRepository sessionRepository,
            AiMessageRepository messageRepository,
            ImageAssetRepository imageRepository,
            QuotaService quotaService,
            ObjectMapper objectMapper) {
        this.sessionRepository = sessionRepository;
        this.messageRepository = messageRepository;
        this.imageRepository = imageRepository;
        this.quotaService = quotaService;
        this.objectMapper = objectMapper;
    }

    public List<Map<String, Object>> listSessions(Long userId) {
        return sessionRepository.findByUserIdOrderByUpdatedAtDesc(userId).stream()
                .map(s -> Map.<String, Object>of(
                        "id", s.getId(),
                        "title", s.getTitle() != null ? s.getTitle() : "新对话",
                        "mode", s.getMode(),
                        "updatedAt", s.getUpdatedAt().toString()))
                .toList();
    }

    @Transactional
    public Map<String, Object> createSession(Long userId, String mode) {
        AiSession session = new AiSession();
        session.setId(UUID.randomUUID().toString());
        session.setUserId(userId);
        session.setTitle("新对话");
        session.setMode(mode != null ? mode : "CHAT");
        sessionRepository.save(session);
        return Map.of("id", session.getId(), "title", session.getTitle());
    }

    @Transactional
    public void renameSession(Long userId, String sessionId, String title) {
        AiSession session = loadOwned(userId, sessionId);
        session.setTitle(title);
        session.touch();
        sessionRepository.save(session);
    }

    @Transactional
    public void deleteSession(Long userId, String sessionId) {
        AiSession session = loadOwned(userId, sessionId);
        sessionRepository.delete(session);
    }

    public Map<String, Object> listMessages(Long userId, String sessionId, int page, int size) {
        loadOwned(userId, sessionId);
        Page<AiMessage> result = messageRepository.findBySessionIdOrderByCreatedAtAsc(
                sessionId, PageRequest.of(page - 1, size));
        List<Map<String, Object>> items = result.getContent().stream()
                .map(this::toMessageView)
                .toList();
        return Map.of(
                "items", items,
                "page", page,
                "size", size,
                "total", result.getTotalElements());
    }

    @Transactional
    public String ensureSession(Long userId, String sessionId, String mode) {
        if (sessionId != null && !sessionId.isBlank()) {
            loadOwned(userId, sessionId);
            return sessionId;
        }
        return createSession(userId, mode).get("id").toString();
    }

    @Transactional
    public void appendMessage(Long userId, String sessionId, String role, String contentType, String content) {
        AiSession session = loadOwned(userId, sessionId);
        AiMessage msg = new AiMessage();
        msg.setSessionId(sessionId);
        msg.setRole(role);
        msg.setContentType(contentType);
        msg.setContent(content);
        messageRepository.save(msg);
        if (session.getTitle() == null || "新对话".equals(session.getTitle())) {
            session.setTitle(truncateTitle(content));
        }
        session.touch();
        sessionRepository.save(session);
    }

    @Transactional
    public Map<String, Object> completeImageGeneration(
            Long userId, Long reservationId, String sessionId, String imageUrl, String prompt) {
        quotaService.commit(userId, reservationId);

        ImageAsset asset = new ImageAsset();
        asset.setUserId(userId);
        asset.setSessionId(sessionId);
        asset.setImageUrl(imageUrl);
        asset.setPrompt(prompt);
        imageRepository.save(asset);

        if (sessionId != null && !sessionId.isBlank()) {
            appendMessage(userId, sessionId, "user", "IMAGE_GEN_REQ", toJson(Map.of(
                    "type", "image_gen", "prompt", prompt)));
            appendMessage(userId, sessionId, "assistant", "IMAGE_RESULT", toJson(Map.of(
                    "type", "image", "url", imageUrl, "prompt", prompt)));
        }

        int balance = quotaService.getBalance(userId);
        return Map.of("imageUrl", imageUrl, "remainingQuota", balance);
    }

    public Map<String, Object> listUserImages(Long userId, int page, int size) {
        Page<ImageAsset> result = imageRepository.findByUserIdOrderByCreatedAtDesc(
                userId, PageRequest.of(page - 1, size));
        List<Map<String, Object>> items = result.getContent().stream()
                .map(img -> Map.<String, Object>of(
                        "id", img.getId(),
                        "imageUrl", img.getImageUrl(),
                        "prompt", img.getPrompt() != null ? img.getPrompt() : "",
                        "sessionId", img.getSessionId() != null ? img.getSessionId() : "",
                        "createdAt", img.getCreatedAt().toString()))
                .toList();
        return Map.of("items", items, "page", page, "size", size, "total", result.getTotalElements());
    }

    private AiSession loadOwned(Long userId, String sessionId) {
        return sessionRepository.findByIdAndUserId(sessionId, userId)
                .orElseThrow(() -> new BusinessException(404, "会话不存在"));
    }

    private Map<String, Object> toMessageView(AiMessage m) {
        return Map.of(
                "id", m.getId(),
                "role", m.getRole(),
                "contentType", m.getContentType(),
                "content", m.getContent(),
                "createdAt", m.getCreatedAt().toString());
    }

    private static String truncateTitle(String text) {
        if (text == null) {
            return "新对话";
        }
        String plain = text.length() > 20 ? text.substring(0, 20) : text;
        return plain.isBlank() ? "新对话" : plain;
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            return String.valueOf(value);
        }
    }
}
