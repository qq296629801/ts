package com.ts.gateway.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.util.retry.Retry;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * 调用 OpenAI 兼容的 /v1/images/generations 中继（支持 url 或 b64_json 响应）。
 * 使用 JDK HttpClient + HTTP/1.1，避免 Reactor Netty 在长连接大响应时被上游约 60s 掐断。
 */
@Component
public class RelayImageClient {

    private static final Logger log = LoggerFactory.getLogger(RelayImageClient.class);

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final String apiUrl;
    private final String apiKey;
    private final String model;
    private final String quality;

    public RelayImageClient(
            ObjectMapper objectMapper,
            @Value("${app.image.api-url}") String apiUrl,
            @Value("${app.image.api-key}") String apiKey,
            @Value("${app.image.model:gpt-image-2}") String model,
            @Value("${app.image.quality:medium}") String quality) {
        this(objectMapper, defaultHttpClient(), apiUrl, apiKey, model, quality);
    }

    /** 供单元测试注入 MockWebServer 对应的 HttpClient */
    RelayImageClient(
            ObjectMapper objectMapper,
            HttpClient httpClient,
            String apiUrl,
            String apiKey,
            String model,
            String quality) {
        this.httpClient = httpClient;
        this.objectMapper = objectMapper;
        this.apiUrl = apiUrl;
        this.apiKey = apiKey;
        this.model = model;
        this.quality = quality;
    }

    private static HttpClient defaultHttpClient() {
        return HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(Duration.ofSeconds(30))
                .build();
    }

    public Mono<GeneratedImage> generate(String prompt, String size) {
        return Mono.fromCallable(() -> callUpstreamBlocking(prompt, size))
                .subscribeOn(Schedulers.boundedElastic())
                .retryWhen(Retry.backoff(3, Duration.ofSeconds(3))
                        .maxBackoff(Duration.ofSeconds(20))
                        .filter(RelayImageClient::isRetryable)
                        .doBeforeRetry(sig -> log.warn("中继生图重试第 {} 次: {}",
                                sig.totalRetries() + 1, rootMessage(sig.failure()))))
                .doOnSuccess(img -> log.info("中继生图成功: {}", img.url() != null ? "url" : "b64_json"))
                .doOnError(ex -> log.warn("中继生图最终失败: {}", rootMessage(ex)));
    }

    private GeneratedImage callUpstreamBlocking(String prompt, String size) throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("model", model);
        body.put("prompt", prompt);
        body.put("size", size != null && !size.isBlank() ? size : "1024x1024");
        body.put("quality", quality);
        body.put("n", 1);

        log.info("请求中继生图: url={}, model={}, size={}, quality={}", apiUrl, model, body.get("size"), quality);

        String jsonBody = objectMapper.writeValueAsString(body);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(apiUrl))
                .timeout(Duration.ofMinutes(5))
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        String json = response.body() != null ? response.body() : "";
        if (response.statusCode() >= 400) {
            throw new IllegalStateException(
                    "上游 HTTP " + response.statusCode() + ": " + summarize(json));
        }
        return parseResponse(json);
    }

    private static boolean isRetryable(Throwable ex) {
        Throwable root = ex;
        while (root.getCause() != null) {
            root = root.getCause();
        }
        if (root instanceof IOException) {
            return true;
        }
        String msg = rootMessage(ex);
        return msg.contains("prematurely closed")
                || msg.contains("EOF reached while reading")
                || msg.contains("Connection reset")
                || msg.contains("header parser received no bytes")
                || msg.contains("GOAWAY");
    }

    private static String rootMessage(Throwable ex) {
        Throwable c = ex;
        while (c.getCause() != null) {
            c = c.getCause();
        }
        return c.getMessage() != null ? c.getMessage() : ex.getClass().getSimpleName();
    }

    private GeneratedImage parseResponse(String json) {
        if (json == null || json.isBlank()) {
            throw new IllegalStateException("上游返回空响应体");
        }
        try {
            JsonNode root = objectMapper.readTree(json);
            if (root.has("error")) {
                JsonNode err = root.get("error");
                String msg = err.isObject()
                        ? err.path("message").asText(err.toString())
                        : err.asText();
                throw new IllegalStateException("上游错误: " + msg);
            }
            JsonNode data = root.get("data");
            if (data == null || !data.isArray() || data.isEmpty()) {
                StringBuilder keys = new StringBuilder();
                root.fieldNames().forEachRemaining(n -> {
                    if (keys.length() > 0) {
                        keys.append(',');
                    }
                    keys.append(n);
                });
                log.warn("生图响应无 data, 顶层字段=[{}], 响应预览={}", keys, summarize(json));
                throw new IllegalStateException("上游未返回 data 数组，字段: " + keys);
            }
            JsonNode first = data.get(0);
            if (first.hasNonNull("url")) {
                return GeneratedImage.fromUrl(first.get("url").asText());
            }
            if (first.hasNonNull("b64_json")) {
                String b64 = first.get("b64_json").asText();
                log.info("收到 b64_json, 约 {} KB", b64.length() / 1024);
                return GeneratedImage.fromBase64(b64);
            }
            throw new IllegalStateException("data[0] 缺少 url 或 b64_json");
        } catch (IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            log.warn("解析生图 JSON 失败, 预览={}", summarize(json), e);
            throw new IllegalStateException("解析生图响应失败: " + e.getMessage(), e);
        }
    }

    private static String summarize(String json) {
        if (json == null) {
            return "";
        }
        if (json.length() <= 500) {
            return json;
        }
        return json.substring(0, 500) + "...(len=" + json.length() + ")";
    }

    public record GeneratedImage(String url, String base64Json) {
        public static GeneratedImage fromUrl(String url) {
            return new GeneratedImage(url, null);
        }

        public static GeneratedImage fromBase64(String b64) {
            return new GeneratedImage(null, b64);
        }
    }
}
