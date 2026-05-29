package com.ts.gateway.security;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class LogSanitizerTest {

    @Test
    void redactsJwtAndApiKey() {
        String raw = "token=eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIxIn0.sig key=sk-abcdefghijklmnopqrstuvwxyz";
        String out = LogSanitizer.sanitize(raw);
        assertThat(out).doesNotContain("eyJhbGci");
        assertThat(out).doesNotContain("sk-abcdef");
        assertThat(out).contains("[JWT_REDACTED]");
        assertThat(out).contains("[API_KEY_REDACTED]");
    }
}
