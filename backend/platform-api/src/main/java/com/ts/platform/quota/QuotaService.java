package com.ts.platform.quota;

import com.ts.platform.common.BusinessException;
import com.ts.platform.user.UserQuota;
import com.ts.platform.user.UserQuotaRepository;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;

@Service
public class QuotaService {

    private static final String INFLIGHT_KEY = "gen:inflight:";
    private static final Duration INFLIGHT_TTL = Duration.ofMinutes(2);

    private final UserQuotaRepository quotaRepository;
    private final QuotaReservationRepository reservationRepository;
    private final QuotaLogRepository logRepository;
    private final StringRedisTemplate redis;

    public QuotaService(
            UserQuotaRepository quotaRepository,
            QuotaReservationRepository reservationRepository,
            QuotaLogRepository logRepository,
            StringRedisTemplate redis) {
        this.quotaRepository = quotaRepository;
        this.reservationRepository = reservationRepository;
        this.logRepository = logRepository;
        this.redis = redis;
    }

    @Transactional
    public Map<String, Object> reserve(Long userId) {
        String inflightKey = INFLIGHT_KEY + userId;
        Boolean locked = redis.opsForValue().setIfAbsent(inflightKey, "1", INFLIGHT_TTL);
        if (Boolean.FALSE.equals(locked)) {
            throw new BusinessException(409, "已有进行中的生图任务，请稍候");
        }

        UserQuota quota = quotaRepository.findByUserId(userId)
                .orElseThrow(() -> new BusinessException(402, "次数不足"));
        if (quota.getBalance() < 1) {
            redis.delete(inflightKey);
            throw new BusinessException(402, "次数不足，请充值");
        }

        quota.setBalance(quota.getBalance() - 1);
        quota.touch();
        quotaRepository.save(quota);

        QuotaReservation reservation = new QuotaReservation();
        reservation.setUserId(userId);
        reservation.setStatus("PENDING");
        reservation.setExpiresAt(LocalDateTime.now().plus(INFLIGHT_TTL));
        reservationRepository.save(reservation);

        return Map.of(
                "reservationId", reservation.getId(),
                "remainingQuota", quota.getBalance());
    }

    @Transactional
    public void commit(Long userId, Long reservationId) {
        QuotaReservation r = loadReservation(userId, reservationId);
        if ("COMMITTED".equals(r.getStatus())) {
            return;
        }
        r.setStatus("COMMITTED");
        reservationRepository.save(r);

        UserQuota quota = quotaRepository.findByUserId(userId).orElseThrow();
        quota.setTotalUsed(quota.getTotalUsed() + 1);
        quota.touch();
        quotaRepository.save(quota);

        logRepository.save(QuotaLog.of(userId, -1, "IMAGE_GEN"));
        redis.delete(INFLIGHT_KEY + userId);
    }

    @Transactional
    public void rollback(Long userId, Long reservationId) {
        QuotaReservation r = loadReservation(userId, reservationId);
        if ("ROLLED_BACK".equals(r.getStatus()) || "COMMITTED".equals(r.getStatus())) {
            redis.delete(INFLIGHT_KEY + userId);
            return;
        }
        r.setStatus("ROLLED_BACK");
        reservationRepository.save(r);

        UserQuota quota = quotaRepository.findByUserId(userId).orElseThrow();
        quota.setBalance(quota.getBalance() + 1);
        quota.touch();
        quotaRepository.save(quota);

        logRepository.save(QuotaLog.of(userId, 1, "ROLLBACK"));
        redis.delete(INFLIGHT_KEY + userId);
    }

    public int getBalance(Long userId) {
        return quotaRepository.findByUserId(userId).map(UserQuota::getBalance).orElse(0);
    }

    private QuotaReservation loadReservation(Long userId, Long reservationId) {
        return reservationRepository.findByIdAndUserId(reservationId, userId)
                .orElseThrow(() -> new BusinessException(404, "预扣记录不存在"));
    }
}
