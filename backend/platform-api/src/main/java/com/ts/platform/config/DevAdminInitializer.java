package com.ts.platform.config;

import com.ts.platform.user.User;
import com.ts.platform.user.UserQuota;
import com.ts.platform.user.UserQuotaRepository;
import com.ts.platform.user.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
@Profile("dev")
public class DevAdminInitializer {

    private static final Logger log = LoggerFactory.getLogger(DevAdminInitializer.class);

    @Bean
    CommandLineRunner seedAdmin(UserRepository userRepository, UserQuotaRepository quotaRepository, PasswordEncoder encoder) {
        return args -> {
            User admin = userRepository.findByPhone("19900000000").orElseGet(() -> {
                User u = new User();
                u.setPhone("19900000000");
                u.setNickname("系统管理员");
                u.setPasswordHash(encoder.encode("Admin1234"));
                u.setInviteCode("ADMIN0");
                u.setRole("ADMIN");
                userRepository.save(u);
                UserQuota quota = new UserQuota();
                quota.setUserId(u.getId());
                quotaRepository.save(quota);
                log.info("已创建开发环境管理员：手机 19900000000 / 密码 Admin1234");
                return u;
            });
            quotaRepository.findByUserId(admin.getId()).ifPresent(q -> {
                if (q.getBalance() < 10) {
                    q.setBalance(100);
                    q.setTotalGranted(Math.max(q.getTotalGranted(), 100));
                    quotaRepository.save(q);
                    log.info("已为管理员补充开发测试次数至 {}", q.getBalance());
                }
            });
        };
    }
}
