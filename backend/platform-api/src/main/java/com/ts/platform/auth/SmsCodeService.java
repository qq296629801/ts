package com.ts.platform.auth;

import com.ts.platform.common.BusinessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class SmsCodeService {

    private static final Logger log = LoggerFactory.getLogger(SmsCodeService.class);
    private static final Duration CODE_TTL = Duration.ofMinutes(5);
    private static final Duration SEND_LIMIT = Duration.ofSeconds(60);

    private final StringRedisTemplate redis;

    public SmsCodeService(StringRedisTemplate redis) {
        this.redis = redis;
    }

    public void send(String phone) {
        if (!phone.matches("^1[3-9]\\d{9}$")) {
            throw new BusinessException(400, "手机号格式不正确");
        }
        String limitKey = "sms:limit:" + phone;
        if (Boolean.TRUE.equals(redis.hasKey(limitKey))) {
            throw new BusinessException(429, "发送过于频繁，请 60 秒后再试");
        }
        String code = String.format("%06d", ThreadLocalRandom.current().nextInt(1_000_000));
        redis.opsForValue().set("sms:code:" + phone, code, CODE_TTL);
        redis.opsForValue().set(limitKey, "1", SEND_LIMIT);
        // 开发环境：日志输出验证码（生产接阿里云短信）
        log.info("【开发】手机 {} 验证码: {}", phone, code);
    }

    public void verify(String phone, String code) {
        String stored = redis.opsForValue().get("sms:code:" + phone);
        if (stored == null || !stored.equals(code)) {
            throw new BusinessException(400, "验证码错误或已过期");
        }
        redis.delete("sms:code:" + phone);
    }
}
