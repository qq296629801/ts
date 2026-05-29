package com.ts.platform.auth;

import com.ts.platform.common.BusinessException;
import com.ts.platform.invite.InviteService;
import com.ts.platform.quota.QuotaLog;
import com.ts.platform.quota.QuotaLogRepository;
import com.ts.platform.security.JwtTokenProvider;
import com.ts.platform.user.User;
import com.ts.platform.user.UserQuota;
import com.ts.platform.user.UserQuotaRepository;
import com.ts.platform.user.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final UserQuotaRepository quotaRepository;
    private final QuotaLogRepository logRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final SmsCodeService smsCodeService;
    private final EmailCodeService emailCodeService;
    private final InviteService inviteService;
    private final int registerGiftQuota;

    public AuthService(
            UserRepository userRepository,
            UserQuotaRepository quotaRepository,
            QuotaLogRepository logRepository,
            PasswordEncoder passwordEncoder,
            JwtTokenProvider jwtTokenProvider,
            SmsCodeService smsCodeService,
            EmailCodeService emailCodeService,
            InviteService inviteService,
            @Value("${app.register-gift-quota}") int registerGiftQuota) {
        this.userRepository = userRepository;
        this.quotaRepository = quotaRepository;
        this.logRepository = logRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
        this.smsCodeService = smsCodeService;
        this.emailCodeService = emailCodeService;
        this.inviteService = inviteService;
        this.registerGiftQuota = registerGiftQuota;
    }

    @Transactional
    public Map<String, Object> register(RegisterRequest req) {
        validatePassword(req.password());
        validateNickname(req.nickname());

        if (req.phone() != null) {
            smsCodeService.verify(req.phone(), req.verifyCode());
            if (userRepository.existsByPhone(req.phone())) {
                throw new BusinessException(400, "手机号已注册");
            }
        } else if (req.email() != null) {
            emailCodeService.verify(req.email(), req.verifyCode());
            if (userRepository.existsByEmail(req.email())) {
                throw new BusinessException(400, "邮箱已注册");
            }
        } else {
            throw new BusinessException(400, "请提供手机号或邮箱");
        }

        User user = new User();
        user.setPhone(req.phone());
        user.setEmail(req.email());
        user.setNickname(req.nickname());
        user.setPasswordHash(passwordEncoder.encode(req.password()));
        user.setInviteCode(generateInviteCode());

        userRepository.save(user);

        UserQuota quota = new UserQuota();
        quota.setUserId(user.getId());
        quota.setBalance(registerGiftQuota);
        quota.setTotalGranted(registerGiftQuota);
        quotaRepository.save(quota);
        logRepository.save(QuotaLog.of(user.getId(), registerGiftQuota, "REGISTER_GIFT"));

        inviteService.applyRegisterInvite(user, req.inviteCode());
        int balance = quotaRepository.findByUserId(user.getId()).map(UserQuota::getBalance).orElse(registerGiftQuota);

        String token = jwtTokenProvider.createToken(user.getId(), user.getRole(), false);
        return Map.of(
                "accessToken", token,
                "userId", user.getId(),
                "remainingQuota", balance);
    }

    public Map<String, Object> login(LoginRequest req) {
        User user = findByAccount(req.account())
                .orElseThrow(() -> new BusinessException(401, "账号或密码错误"));

        if (user.getStatus() != 1) {
            throw new BusinessException(403, "账号已封禁");
        }
        if (user.getLockedUntil() != null && user.getLockedUntil().isAfter(LocalDateTime.now())) {
            throw new BusinessException(403, "账号已锁定，请稍后再试");
        }

        if (!passwordEncoder.matches(req.password(), user.getPasswordHash())) {
            int fails = user.getLoginFailCount() + 1;
            user.setLoginFailCount(fails);
            if (fails >= 5) {
                user.setLockedUntil(LocalDateTime.now().plusMinutes(30));
                user.setLoginFailCount(0);
            }
            userRepository.save(user);
            throw new BusinessException(401, "账号或密码错误");
        }

        user.setLoginFailCount(0);
        user.setLockedUntil(null);
        userRepository.save(user);

        String token = jwtTokenProvider.createToken(user.getId(), user.getRole(), req.rememberMe());
        int balance = quotaRepository.findByUserId(user.getId()).map(UserQuota::getBalance).orElse(0);
        return Map.of("accessToken", token, "remainingQuota", balance, "role", user.getRole());
    }

    private java.util.Optional<User> findByAccount(String account) {
        if (account.matches("^1[3-9]\\d{9}$")) {
            return userRepository.findByPhone(account);
        }
        return userRepository.findByEmail(account);
    }

    private static void validatePassword(String password) {
        if (password == null || password.length() < 8 || password.length() > 20
                || !password.matches(".*[A-Za-z].*") || !password.matches(".*\\d.*")) {
            throw new BusinessException(400, "密码需 8-20 位且包含字母与数字");
        }
    }

    private static void validateNickname(String nickname) {
        if (nickname == null || nickname.length() < 2 || nickname.length() > 20
                || !nickname.matches("^[\\u4e00-\\u9fa5A-Za-z0-9]+$")) {
            throw new BusinessException(400, "昵称 2-20 字符，不可含特殊符号");
        }
    }

    private static String generateInviteCode() {
        String chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
        StringBuilder sb = new StringBuilder(6);
        for (int i = 0; i < 6; i++) {
            sb.append(chars.charAt(ThreadLocalRandom.current().nextInt(chars.length())));
        }
        return sb.toString();
    }

    public record RegisterRequest(String phone, String email, String password, String nickname,
                                  String verifyCode, String inviteCode) {
    }

    public record LoginRequest(String account, String password, boolean rememberMe) {
    }
}
