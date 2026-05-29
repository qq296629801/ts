package com.ts.gateway.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import java.net.http.HttpClient;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 真实中继功能测试（默认不跑）。需配置有效 Key 与中继 URL：
 * <pre>
 *   export RELAY_IT=1
 *   export IMAGE_API_URL=https://dm-fox.rjj.cc/codex/v1/images/generations
 *   export OPENAI_API_KEY=sk-...
 *   mvn -pl gateway-service test -Dtest=RelayImageClientRelayIT
 * </pre>
 */
@EnabledIfEnvironmentVariable(named = "RELAY_IT", matches = "1")
class RelayImageClientRelayIT {

    @Test
    void generate_againstRealRelay() {
        String apiUrl = env("IMAGE_API_URL");
        String apiKey = env("OPENAI_API_KEY");
        Assumptions.assumeTrue(apiKey != null && !apiKey.isBlank(), "缺少 OPENAI_API_KEY");
        Assumptions.assumeTrue(apiUrl != null && !apiUrl.isBlank(), "缺少 IMAGE_API_URL");

        String model = System.getenv().getOrDefault("IMAGE_MODEL", "gpt-image-2");
        String quality = System.getenv().getOrDefault("IMAGE_QUALITY", "medium");

        RelayImageClient client = new RelayImageClient(
                new ObjectMapper(),
                HttpClient.newBuilder().version(HttpClient.Version.HTTP_1_1).build(),
                apiUrl,
                apiKey,
                model,
                quality);

        var img = client.generate("functional test red circle", "1024x1024")
                .block(Duration.ofMinutes(6));
        assertThat(img).isNotNull();
        assertThat(img.url() != null || img.base64Json() != null).isTrue();
    }

    private static String env(String name) {
        String v = System.getenv(name);
        return v != null ? v.trim() : null;
    }
}
