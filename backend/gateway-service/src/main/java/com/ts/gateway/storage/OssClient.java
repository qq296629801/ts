package com.ts.gateway.storage;

import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.io.ByteArrayInputStream;
import java.util.Base64;

@Component
public class OssClient {

    private static final Logger log = LoggerFactory.getLogger(OssClient.class);

    private final MinioClient minioClient;
    private final String bucket;
    private final String publicBaseUrl;
    private final WebClient webClient;

    public OssClient(
            @Value("${app.minio.endpoint}") String endpoint,
            @Value("${app.minio.public-endpoint:}") String publicEndpoint,
            @Value("${app.minio.access-key}") String accessKey,
            @Value("${app.minio.secret-key}") String secretKey,
            @Value("${app.minio.bucket}") String bucket) {
        this.publicBaseUrl = (publicEndpoint != null && !publicEndpoint.isBlank())
                ? publicEndpoint.replaceAll("/$", "")
                : endpoint.replaceAll("/$", "");
        this.bucket = bucket;
        this.minioClient = MinioClient.builder()
                .endpoint(endpoint)
                .credentials(accessKey, secretKey)
                .build();
        this.webClient = WebClient.create();
    }

    public Mono<String> uploadFromBase64(String base64, String objectKey) {
        return Mono.fromCallable(() -> Base64.getDecoder().decode(base64))
                .flatMap(bytes -> putBytes(bytes, objectKey))
                .doOnError(e -> log.error("OSS 上传失败(b64): {}", objectKey, e));
    }

    public Mono<String> uploadFromUrl(String sourceUrl, String objectKey) {
        return webClient.get().uri(sourceUrl).retrieve().bodyToMono(byte[].class)
                .flatMap(bytes -> putBytes(bytes, objectKey))
                .doOnError(e -> log.error("OSS 上传失败: {}", objectKey, e));
    }

    private Mono<String> putBytes(byte[] bytes, String objectKey) {
        return Mono.fromRunnable(() -> {
                    try {
                        minioClient.putObject(PutObjectArgs.builder()
                                .bucket(bucket)
                                .object(objectKey)
                                .stream(new ByteArrayInputStream(bytes), bytes.length, -1)
                                .contentType("image/png")
                                .build());
                    } catch (Exception e) {
                        throw new RuntimeException("图片持久化失败", e);
                    }
                }).subscribeOn(Schedulers.boundedElastic())
                .thenReturn(publicUrl(objectKey));
    }

    private String publicUrl(String objectKey) {
        return publicBaseUrl + "/" + bucket + "/" + objectKey;
    }
}
