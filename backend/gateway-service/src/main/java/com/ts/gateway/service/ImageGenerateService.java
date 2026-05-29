package com.ts.gateway.service;

import com.ts.gateway.client.PlatformInternalClient;
import com.ts.gateway.client.QuotaClient;
import com.ts.gateway.client.RelayImageClient;
import com.ts.gateway.storage.OssClient;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.UUID;

@Service
public class ImageGenerateService {

    private final RelayImageClient relayImageClient;
    private final QuotaClient quotaClient;
    private final PlatformInternalClient platformClient;
    private final OssClient ossClient;

    public ImageGenerateService(
            RelayImageClient relayImageClient,
            QuotaClient quotaClient,
            PlatformInternalClient platformClient,
            OssClient ossClient) {
        this.relayImageClient = relayImageClient;
        this.quotaClient = quotaClient;
        this.platformClient = platformClient;
        this.ossClient = ossClient;
    }

    public Mono<Map<String, Object>> generate(Long userId, String prompt, String size, String sessionId) {
        return quotaClient.reserve(userId)
                .onErrorMap(WebClientResponseException.class, this::mapQuotaError)
                .flatMap(reserved -> relayImageClient.generate(prompt, size)
                        .flatMap(image -> persistAndComplete(userId, reserved, image, prompt, sessionId))
                        .onErrorResume(ex -> quotaClient.rollback(userId, reserved.reservationId())
                                .then(Mono.error(mapRelayError(ex)))));
    }

    private Mono<Map<String, Object>> persistAndComplete(
            Long userId,
            QuotaClient.ReserveResult reserved,
            RelayImageClient.GeneratedImage image,
            String prompt,
            String sessionId) {
        String objectKey = "images/" + userId + "/" + UUID.randomUUID() + ".png";
        Mono<String> storedUrlMono;
        if (image.url() != null && !image.url().isBlank()) {
            storedUrlMono = ossClient.uploadFromUrl(image.url(), objectKey);
        } else if (image.base64Json() != null && !image.base64Json().isBlank()) {
            storedUrlMono = ossClient.uploadFromBase64(image.base64Json(), objectKey);
        } else {
            return quotaClient.rollback(userId, reserved.reservationId())
                    .then(Mono.error(new ResponseStatusException(HttpStatus.BAD_GATEWAY, "上游未返回图片数据")));
        }
        return storedUrlMono
                .flatMap(storedUrl -> platformClient.completeImage(
                        userId, reserved.reservationId(), sessionId, storedUrl, prompt))
                .map(data -> Map.of(
                        "taskId", UUID.randomUUID().toString(),
                        "imageUrl", data.get("imageUrl"),
                        "remainingQuota", data.get("remainingQuota")));
    }

    private Throwable mapQuotaError(WebClientResponseException ex) {
        int code = ex.getStatusCode().value();
        if (code == 402) {
            return new ResponseStatusException(HttpStatus.PAYMENT_REQUIRED, "次数不足，请充值");
        }
        if (code == 409) {
            return new ResponseStatusException(HttpStatus.CONFLICT, "已有进行中的生图任务");
        }
        return ex;
    }

    private static ResponseStatusException mapRelayError(Throwable ex) {
        String detail = rootMessage(ex);
        String userMsg = toUserRelayMessage(detail);
        return new ResponseStatusException(HttpStatus.BAD_GATEWAY, userMsg);
    }

    /** 将中继英文错误转为用户可读说明（次数已在调用方回滚） */
    private static String toUserRelayMessage(String detail) {
        if (detail == null || detail.isBlank()) {
            return "图像生成失败，次数已回滚，请稍后重试";
        }
        String lower = detail.toLowerCase();
        if (lower.contains("upstream did not return any image output")) {
            return "中继服务未能出图（上游未返回图片），请检查 API Key/余额或稍后重试；若持续失败需联系中继服务商";
        }
        if (lower.contains("eof reached while reading")
                || lower.contains("prematurely closed")
                || lower.contains("connection reset")) {
            return "连接中继超时或中断，次数已回滚，请稍后重试（避免连续多次点击）";
        }
        if (detail.startsWith("上游错误: ")) {
            return "中继返回错误，次数已回滚：" + detail.substring("上游错误: ".length());
        }
        if (detail.startsWith("上游 HTTP ")) {
            return "中继请求失败，次数已回滚：" + detail;
        }
        return "图像生成失败，次数已回滚：" + detail;
    }

    private static String rootMessage(Throwable ex) {
        Throwable c = ex;
        while (c.getCause() != null) {
            c = c.getCause();
        }
        return c.getMessage() != null ? c.getMessage() : ex.getMessage();
    }
}
