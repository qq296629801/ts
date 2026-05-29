package com.ts.platform.auth;

import com.ts.platform.common.BusinessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class EmailCodeService {

    private static final Logger log = LoggerFactory.getLogger(EmailCodeService.class);
    private static final Duration CODE_TTL = Duration.ofMinutes(10);

    private final StringRedisTemplate redis;

    public EmailCodeService(StringRedisTemplate redis) {
        this.redis = redis;
    }

    public void send(String email) {
        if (email == null || !email.contains("@")) {
            throw new BusinessException(400, "邮箱格式不正确");
        }
        String code = String.format("%06d", ThreadLocalRandom.current().nextInt(1_000_000));
        redis.opsForValue().set("email:code:" + email, code, CODE_TTL);
        log.info("【开发】邮箱 {} 验证码: {}", email, code);
    }

    public void verify(String email, String code) {
        String stored = redis.opsForValue().get("email:code:" + email);
        if (stored == null || !stored.equals(code)) {
            throw new BusinessException(400, "验证码错误或已过期");
        }
        redis.delete("email:code:" + email);
    }
}
