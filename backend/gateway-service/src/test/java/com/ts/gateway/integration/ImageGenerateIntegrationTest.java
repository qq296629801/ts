package com.ts.gateway.integration;

import com.ts.gateway.client.PlatformInternalClient;
import com.ts.gateway.client.QuotaClient;
import com.ts.gateway.client.RelayImageClient;
import com.ts.gateway.service.ImageGenerateService;
import com.ts.gateway.storage.OssClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ImageGenerateIntegrationTest {

    @Mock
    private RelayImageClient relayImageClient;
    @Mock
    private QuotaClient quotaClient;
    @Mock
    private PlatformInternalClient platformClient;
    @Mock
    private OssClient ossClient;

    @InjectMocks
    private ImageGenerateService imageGenerateService;

    @BeforeEach
    void setUp() {
        imageGenerateService = new ImageGenerateService(relayImageClient, quotaClient, platformClient, ossClient);
    }

    @Test
    void generate_success_commit_b64() {
        when(quotaClient.reserve(1L)).thenReturn(Mono.just(new QuotaClient.ReserveResult(10L, 2)));
        when(relayImageClient.generate(anyString(), anyString()))
                .thenReturn(Mono.just(RelayImageClient.GeneratedImage.fromBase64("aGVsbG8=")));
        when(ossClient.uploadFromBase64(anyString(), anyString())).thenReturn(Mono.just("http://oss/x.png"));
        when(platformClient.completeImage(anyLong(), anyLong(), any(), anyString(), anyString()))
                .thenReturn(Mono.just(Map.of("imageUrl", "http://oss/x.png", "remainingQuota", 2)));

        StepVerifier.create(imageGenerateService.generate(1L, "猫", "1024x1024", "s1"))
                .expectNextMatches(m -> m.get("imageUrl").equals("http://oss/x.png"))
                .verifyComplete();

        verify(quotaClient, never()).rollback(anyLong(), anyLong());
    }

    @Test
    void generate_upstreamFail_rollback() {
        when(quotaClient.reserve(1L)).thenReturn(Mono.just(new QuotaClient.ReserveResult(11L, 2)));
        when(relayImageClient.generate(anyString(), anyString()))
                .thenReturn(Mono.error(new RuntimeException("upstream down")));
        when(quotaClient.rollback(1L, 11L)).thenReturn(Mono.empty());

        StepVerifier.create(imageGenerateService.generate(1L, "猫", "1024x1024", "s1"))
                .expectErrorMatches(e -> e instanceof ResponseStatusException
                        && ((ResponseStatusException) e).getStatusCode() == HttpStatus.BAD_GATEWAY)
                .verify();

        verify(quotaClient).rollback(1L, 11L);
    }
}
