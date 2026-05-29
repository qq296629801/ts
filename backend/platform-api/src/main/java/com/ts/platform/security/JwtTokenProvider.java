package com.ts.platform.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;

@Component
public class JwtTokenProvider {

    private final SecretKey key;
    private final int accessExpirationDays;
    private final int rememberMeExpirationDays;

    public JwtTokenProvider(
            @Value("${app.jwt.secret}") String secret,
            @Value("${app.jwt.access-expiration-days}") int accessExpirationDays,
            @Value("${app.jwt.remember-me-expiration-days}") int rememberMeExpirationDays) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessExpirationDays = accessExpirationDays;
        this.rememberMeExpirationDays = rememberMeExpirationDays;
    }

    public String createToken(Long userId, String role, boolean rememberMe) {
        int days = rememberMe ? rememberMeExpirationDays : accessExpirationDays;
        Instant exp = Instant.now().plus(days, ChronoUnit.DAYS);
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim("role", role)
                .expiration(Date.from(exp))
                .issuedAt(new Date())
                .signWith(key)
                .compact();
    }

    public Claims parse(String token) {
        return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
    }

    public Long getUserId(String token) {
        return Long.parseLong(parse(token).getSubject());
    }
}
