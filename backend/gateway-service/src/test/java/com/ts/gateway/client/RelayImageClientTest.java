package com.ts.gateway.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.SocketPolicy;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.test.StepVerifier;

import java.net.http.HttpClient;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

class RelayImageClientTest {

    private MockWebServer server;
    private RelayImageClient client;

    @BeforeEach
    void setUp() throws Exception {
        server = new MockWebServer();
        server.start();
        String baseUrl = server.url("/v1/images/generations").toString();
        client = RelayImageClient.createForTest(
                new ObjectMapper(),
                HttpClient.newHttpClient(),
                baseUrl,
                "test-api-key",
                "gpt-image-2",
                "medium");
    }

    @AfterEach
    void tearDown() throws Exception {
        server.shutdown();
    }

    @Test
    void generate_parsesB64Json() throws Exception {
        server.enqueue(new MockResponse()
                .setBody("""
                        {"data":[{"b64_json":"aGVsbG8="}]}
                        """));

        StepVerifier.create(client.generate("一只猫", "1024x1024"))
                .assertNext(img -> {
                    assertThat(img.url()).isNull();
                    assertThat(img.base64Json()).isEqualTo("aGVsbG8=");
                })
                .verifyComplete();

        var req = server.takeRequest();
        assertThat(req.getHeader("Authorization")).isEqualTo("Bearer test-api-key");
        assertThat(req.getBody().readUtf8())
                .contains("\"model\":\"gpt-image-2\"")
                .contains("\"quality\":\"medium\"")
                .contains("\"size\":\"1024x1024\"")
                .contains("\"prompt\":\"一只猫\"");
    }

    @Test
    void generate_parsesUrl() {
        server.enqueue(new MockResponse()
                .setBody("""
                        {"data":[{"url":"https://cdn.example/img.png"}]}
                        """));

        StepVerifier.create(client.generate("狗", null))
                .assertNext(img -> {
                    assertThat(img.url()).isEqualTo("https://cdn.example/img.png");
                    assertThat(img.base64Json()).isNull();
                })
                .verifyComplete();
    }

    @Test
    void generate_defaultSizeWhenBlank() throws Exception {
        server.enqueue(new MockResponse()
                .setBody("{\"data\":[{\"b64_json\":\"e30=\"}]}"));

        StepVerifier.create(client.generate("x", "  "))
                .expectNextCount(1)
                .verifyComplete();

        assertThat(server.takeRequest().getBody().readUtf8()).contains("\"size\":\"1024x1024\"");
    }

    @Test
    void generate_http200WithErrorBody_noRetry() {
        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .setBody("{\"error\":{\"message\":\"upstream did not return any image output\"}}"));

        StepVerifier.create(client.generate("x", "1024x1024"))
                .expectErrorSatisfies(ex -> assertThat(ex)
                        .isInstanceOf(IllegalStateException.class)
                        .hasMessageContaining("upstream did not return any image output"))
                .verify(Duration.ofSeconds(5));

        assertThat(server.getRequestCount()).isEqualTo(1);
    }

    @Test
    void generate_upstreamHttpError_noRetry() {
        server.enqueue(new MockResponse()
                .setResponseCode(400)
                .setBody("{\"error\":{\"message\":\"invalid prompt\"}}"));

        StepVerifier.create(client.generate("bad", "1024x1024"))
                .expectErrorSatisfies(ex -> assertThat(ex)
                        .isInstanceOf(IllegalStateException.class)
                        .hasMessageContaining("上游 HTTP 400")
                        .hasMessageContaining("invalid prompt"))
                .verify(Duration.ofSeconds(5));

        assertThat(server.getRequestCount()).isEqualTo(1);
    }

    @Test
    void generate_emptyBody_failsWithoutRetry() {
        server.enqueue(new MockResponse().setResponseCode(200).setBody(""));

        StepVerifier.create(client.generate("x", "1024x1024"))
                .expectErrorSatisfies(ex -> assertThat(ex)
                        .isInstanceOf(IllegalStateException.class)
                        .hasMessageContaining("上游返回空响应体"))
                .verify(Duration.ofSeconds(5));

        assertThat(server.getRequestCount()).isEqualTo(1);
    }

    @Test
    void generate_retriesOnDisconnect_thenSuccess() {
        server.enqueue(new MockResponse().setSocketPolicy(SocketPolicy.DISCONNECT_AT_START));
        server.enqueue(new MockResponse()
                .setBody("{\"data\":[{\"b64_json\":\"cmV0cnk=\"}]}"));

        var img = client.generate("retry me", "1024x1024").block(Duration.ofSeconds(30));
        assertThat(img).isNotNull();
        assertThat(img.base64Json()).isEqualTo("cmV0cnk=");
        assertThat(server.getRequestCount()).isEqualTo(2);
    }
}
